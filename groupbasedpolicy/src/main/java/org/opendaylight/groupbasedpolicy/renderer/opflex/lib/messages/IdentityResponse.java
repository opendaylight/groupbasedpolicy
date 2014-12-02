/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class IdentityResponse extends RpcMessage {

    public static final String IDENTITY_MESSAGE_RESPONSE = "send_identity_response";

    static public class Peer {
        private List<String> role;
        private String connectivity_info;

        public List<String> getRole() {
            return role;
        }

        public void setRole(List<String> role) {
            this.role = role;
        }

        public String getConnectivity_info() {
            return connectivity_info;
        }

        public void setConnectivity_info(String connectivity_info) {
            this.connectivity_info = connectivity_info;
        }

        public Peer() {
        }
    }

    static public class Result {
        private String name;
        private String domain;
        private List<String> my_role;
        private List<Peer> peers;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Result() {
            my_role = new ArrayList<String>();
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public List<String> getMy_role() {
            return my_role;
        }

        public void setMy_role(List<String> my_role) {
            this.my_role = my_role;
        }

        public List<Peer> getPeers() {
            return peers;
        }

        public void setPeers(List<Peer> peers) {
            this.peers = peers;
        }

    }
    private JsonNode id;
    private Result result;
    private OpflexError error;

    @JsonIgnore
    private String name;
    @JsonIgnore
    private String method;

    @Override
    public JsonNode getId() {
        return id;
    }

    @Override
    public void setId(JsonNode id) {
        this.id = id;
    }

    public OpflexError getError() {
        return error;
    }

    public void setError(OpflexError error) {
        this.error = error;
    }

    @Override
    public String getMethod() {
        return null;
    }

    @Override
    public void setMethod(String method) {
    }

    public Result getResult() {
        return this.result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public IdentityResponse(String name) {
        this.name = name;
    }

    public IdentityResponse() {
        this.name = IDENTITY_MESSAGE_RESPONSE;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    @Override
    public boolean valid() {
        return true;
    }

}
