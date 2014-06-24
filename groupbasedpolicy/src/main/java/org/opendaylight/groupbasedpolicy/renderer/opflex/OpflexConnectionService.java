/*
 * Copyright (C) 2013 Red Hat, Inc.
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Evan Zeller, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcDecoder;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcMessageMap;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.groupbasedpolicy.renderer.opflex.ChannelConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

/*
 * Defines the JSON RPC methods supported, and creates 
 * the connection/discovery server 
 */
public class OpflexConnectionService {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexConnectionService.class);

    // Properties that can be set in config.ini
    private static final String OPFLEX_LISTENPORT = "opflex.listenPort";
    private static final Integer defaultOpflexPort = 6670;

    private static Integer opflexListenPort = defaultOpflexPort;
    private ConcurrentMap<String, Connection> opflexConnections;
    private Channel serverListenChannel = null;
    private DataBrokerService dataProvider;
    private JsonRpcMessageMap messageMap;

    public void setDataProvider(DataBrokerService salDataProvider) {
        // TODO: use this for OpFlex configuration data changes
        this.dataProvider = salDataProvider;
        
        // TODO: Get configuration for various servers,
        //       to be sent in OpFlex Identity message
        start();
   }

    public void start() {
        opflexConnections = new ConcurrentHashMap<String, Connection>();
        int listenPort = defaultOpflexPort;
        String portString = System.getProperty(OPFLEX_LISTENPORT);
        if (portString != null) {
            listenPort = Integer.decode(portString).intValue();
        }
        opflexListenPort = listenPort;
        this.messageMap = new JsonRpcMessageMap();
        // TODO: Add supported OpFlex messages/methods to map
        
        startOpflexManager();
    }
    
    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    public void stopping() {
        for (Connection connection : opflexConnections.values()) {
            connection.disconnect();
        }
        serverListenChannel.disconnect();
    }

    // TODO: Add sending of Identity message to peer
    private void handleNewConnection(String identifier, Channel channel, OpflexConnectionService instance) throws InterruptedException, ExecutionException {
        Connection connection = new Connection(identifier, channel);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonRpcEndpoint endpoint = new JsonRpcEndpoint(objectMapper, channel, messageMap);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(endpoint);
        channel.pipeline().addLast(binderHandler);

        connection.setEndpoint(endpoint);
        opflexConnections.put(identifier, connection);

        ChannelConnectionHandler handler = new ChannelConnectionHandler();
        handler.setPeer(connection);
        handler.setConnectionService(this);
        ChannelFuture closeFuture = channel.closeFuture();
        closeFuture.addListener(handler);
        // Keeping the Initial inventory update(s) on its own thread.
        new Thread() {
            Connection connection;
            String identifier;

            @Override
            public void run() {
                try {
                    initializeInventoryForNewNode(connection);
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Failed to initialize inventory for node with identifier " + identifier, e);
                    opflexConnections.remove(identifier);
                }
            }
            public Thread initializeConnectionParams(String identifier, Connection connection) {
                this.identifier = identifier;
                this.connection = connection;
                return this;
            }
        }.initializeConnectionParams(identifier, connection).start();
    }

    public void channelClosed(Connection peer) throws Exception {
        logger.info("Connection to Node : {} closed", peer);
        this.opflexConnections.remove(peer);
    }

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException {
        Channel channel = connection.getChannel();
        InetAddress address = ((InetSocketAddress)channel.remoteAddress()).getAddress();
        int port = ((InetSocketAddress)channel.remoteAddress()).getPort();
        // TODO: Left-over place holder... should we keep this information around?
    }

    private void startOpflexManager() {
        new Thread() {
            @Override
            public void run() {
                opflexManager();
            }
        }.start();
    }

    private void opflexManager() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel channel) throws Exception {
                     logger.debug("New Passive channel created : "+ channel.toString());
                     InetAddress address = channel.remoteAddress().getAddress();
                     int port = channel.remoteAddress().getPort();
                     String identifier = address.getHostAddress()+":"+port;
                     channel.pipeline().addLast(
                             new LoggingHandler(LogLevel.INFO),
                             new JsonRpcDecoder(100000),
                             new StringEncoder(CharsetUtil.UTF_8));

                     handleNewConnection(identifier, channel, OpflexConnectionService.this);
                     logger.debug("Connected Node : "+identifier);
                 }
             });
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture f = b.bind(opflexListenPort).sync();
            serverListenChannel =  f.channel();
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
