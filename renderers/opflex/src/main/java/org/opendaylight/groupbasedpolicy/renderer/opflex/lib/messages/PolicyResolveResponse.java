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

import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class PolicyResolveResponse extends RpcMessage {

    public static final String POLICY_MESSAGE_RESPONSE = "policy_resolve_response";

    static public class Result {

        private List<ManagedObject> policy;

        public List<ManagedObject> getPolicy() {
            return policy;
        }

        public void setPolicy(List<ManagedObject> policy) {
            this.policy = policy;
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
    public void setMethod(String method) {}

    public Result getResult() {
        return this.result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public PolicyResolveResponse(String name) {
        this.name = name;
    }

    public PolicyResolveResponse() {
        this.name = POLICY_MESSAGE_RESPONSE;
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
