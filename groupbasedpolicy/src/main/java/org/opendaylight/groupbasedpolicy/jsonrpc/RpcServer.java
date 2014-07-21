/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

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
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A (soon-to-be) generic RPC server. It creates {@link JsonRpcEndpoint} objects
 * for each new connection. The RpcServer has a set of {@link RpcMessage}
 * types that it supports, and it passes on these supported messages
 * to the {@link JsonRpcEndpoint} objects that it creates.
 *
 * TODO: add serialization type, and refactor so serialization determines
 *       concrete RpcEndpoint object (only JsonRpcEndpoint right now).
 * TODO: This and other classes are tightly coupled to netty -- make abstraction?
 */
public class RpcServer {
    protected static final Logger logger =
            LoggerFactory.getLogger(RpcServer.class);

    String identity;
    int listenPort;
    Channel channel;
    Object context;
    RpcMessageMap messageMap;
    ConnectionService connectionService;
    RpcBroker broker;

    public RpcServer() {
        messageMap = new RpcMessageMap();
    }

    public RpcServer(String identity, int port) {
        messageMap = new RpcMessageMap();
        this.listenPort = port;
        this.identity = identity;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public void addMessage(RpcMessage message) {
        this.messageMap.add(message);
    }

    public void addMessageList(List<RpcMessage> messageList) {
        this.messageMap.addList(messageList);
    }

    public void setConnectionService(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public void setRpcBroker(RpcBroker broker) {
        this.broker = broker;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return this.channel;
    }

    void handleNewConnection(String identifier, Channel newChannel)
            throws InterruptedException, ExecutionException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonRpcEndpoint endpoint = new JsonRpcEndpoint(identifier, connectionService,
                objectMapper, newChannel, messageMap, broker);
        endpoint.setContext(context);
        JsonRpcServiceBinderHandler binderHandler =
                new JsonRpcServiceBinderHandler(endpoint);
        newChannel.pipeline().addLast(binderHandler);

        connectionService.addConnection(endpoint);

        ChannelFuture closeFuture = newChannel.closeFuture();
        closeFuture.addListener(endpoint);
    }

    public void start() {
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
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            logger.debug("New Passive channel created : "
                                    + ch.toString());
                            InetAddress address = ch.remoteAddress()
                                    .getAddress();
                            int port = ch.remoteAddress().getPort();
                            String identifier = address.getHostAddress() + ":"
                                    + port;
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new JsonRpcDecoder(100000),
                                    new StringEncoder(CharsetUtil.UTF_8));

                            handleNewConnection(identifier, ch);
                            logger.warn("Connected Node : " + identifier);
                        }
                    });
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR,
                    new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture f = b.bind(identity, listenPort).sync();
            String id = f.channel().localAddress().toString();
            logger.warn("Connected Node : " + id);

            this.channel = f.channel();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
