/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.ConnectionService;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;

/**
 * The {@link OpflexRpcServer}s respond to OpFlex clients
 * which create {@link OpflexAgent} objects when they
 * are established. The servers don't own the connections,
 * which allows the clients to continue operation even if
 * the server is closed
 *
 * @author tbachman
 *
 */
public class OpflexRpcServer {

    private String identity;
    private String domain;
    private List<Role> roles;
    private RpcServer rpcServer;
    private ConnectionService connectionService;
    private RpcBroker rpcBroker;

    private String address;
    private int port;

    private void parseAndSetIdentity(String id) {
        if (id.split(":").length == 2) {
            this.identity = id;
            this.address = id.split(":")[0];
            this.port =  Integer.parseInt(id.split(":")[1]);
        }
    }

    public OpflexRpcServer(String domain, String identity, List<Role> roles) {
        this.domain = domain;
        this.roles = roles;
        parseAndSetIdentity(identity);
        rpcServer = new RpcServer(address, port);
        rpcServer.setContext(this);
    }

    public String getDomain() {
        return domain;
    }

    public String getId() {
        return this.identity;
    }

    public RpcServer getRpcServer() {
        return rpcServer;
    }

    public ConnectionService getConnectionService() {
        return connectionService;
    }

    public void setConnectionService(ConnectionService service) {
        this.connectionService = service;
    }

    public RpcBroker getRpcBroker() {
        return this.rpcBroker;
    }

    public void setRpcBroker(RpcBroker rpcBroker) {
        this.rpcBroker = rpcBroker;
    }

    public List<Role> getRoles() {
        return this.roles;
    }

    /**
     * Start the {@link OpflexRpcServer}. This adds the supported
     * messages to the server, based on the roles that were
     * configured. It creates an {@link RpcServer} object,
     * passes it the context owned by the {@link OpflexRpcServer},
     * and starts the server in its own thread.
     *
     * TODO: should use executor service instead?
     */
    public void start() {
        rpcServer.setConnectionService(connectionService);
        rpcServer.setRpcBroker(rpcBroker);

        for ( Role role : roles ) {
            rpcServer.addMessageList(role.getMessages());
        }

        new Thread() {
            private RpcServer server;

            public Thread initializeServerParams(RpcServer srv) {
                this.server = srv;
                return this;
            }
            @Override
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                }
            }
        }.initializeServerParams(rpcServer).start();

    }

    /**
     * Check to see if two servers are the same. They
     * need to be in the same Opflex Domain, have the same
     * identity, and the same roles, or they can be
     * identical objects. Note that it purposely does
     * not compare the RpcServer, as the purpose for
     * this method is to see if there is already a server
     * fulfilling this configuration (which is the reason
     * it's a new method, instead of overriding toString).
     *
     * @param srv The server to compare against
     * @return true if they are equivalent
     */
    public boolean sameServer(OpflexRpcServer srv) {
        if (this == srv)
            return true;
        if (srv == null)
            return false;
        if (!this.identity.equals(srv.identity))
            return false;
        if (this.domain == null ||
                !this.domain.equals(srv.getDomain()))
            return false;
        if (this.roles == null && srv.roles == null)
            return true;
        if (this.roles == null || srv.roles == null)
            return false;
        if (this.roles.size() == srv.roles.size()
                && this.roles.containsAll(srv.roles))
            return true;
        return false;
    }
}
