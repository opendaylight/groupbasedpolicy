/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.lib;

import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;

/**
 * Represents a participant in an OpFlex domain. It
 * contains references to the {@link OpflexRpcServer} that
 * it communicates with (i.e. peer), and indicates the roles
 * for that server.
 *
 * @author tbachman
 */
public class OpflexAgent {

    String identity;
    String domain;
    List<Role> roles;
    JsonRpcEndpoint endpoint;
    OpflexRpcServer opflexServer;

    public OpflexAgent() {}

    /**
     * Get the OpFlex administrative domain for this agent
     *
     * @return
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Set the OpFlex administrative domain for this agent
     *
     * @param domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Get the identity for this agent, as a String
     *
     * @return
     */
    public String getIdentity() {
        return identity;
    }

    /**
     * Set the identity for the agent.
     *
     * @param identity
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    /**
     * Associate an {@link OpflexRpcServer} with this agent.
     *
     * @return
     */
    public OpflexRpcServer getOpflexServer() {
        return opflexServer;
    }

    /**
     * Get the {@link OpflexRpcServer} associated with this agent
     *
     * @param server
     */
    public void setOpflexServer(OpflexRpcServer server) {
        this.opflexServer = server;
    }

    /**
     * Get the roles for this agent
     *
     * @return
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Set the list of roles for this agent
     *
     * @param roles
     */
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    /**
     * Get the {@link JsonRpcEndpoint} for this agent
     *
     * @return
     */
    public JsonRpcEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Set the {@link JsonRpcEndpoint} for this agent
     *
     * @param endpoint
     */
    public void setEndpoint(JsonRpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

}
