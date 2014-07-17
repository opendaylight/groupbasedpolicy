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

import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;

public class OpflexAgent {
    String identity;
    String domain;
    List<Role> roles;
    JsonRpcEndpoint endpoint;
    OpflexRpcServer opflexServer;

    public OpflexAgent() {
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public OpflexRpcServer getOpflexServer() {
        return opflexServer;
    }

    public void setOpflexServer(OpflexRpcServer server) {
        this.opflexServer = server;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public JsonRpcEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(JsonRpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

}
