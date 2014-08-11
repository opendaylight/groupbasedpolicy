/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.messages;

import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class PolicyResolutionResponse extends RpcMessage {

    public static final String POLICY_MESSAGE_RESPONSE = "resolve_policy_response";

    static public class Result {
        private List<ManagedObject> policy;
        private int prr;
        public List<ManagedObject> getPolicy() {
            return policy;
        }
        public void setPolicy(List<ManagedObject> policy) {
            this.policy = policy;
        }
        public int getPrr() {
            return prr;
        }
        public void setPrr(int prr) {
            this.prr = prr;
        }
    }
    static public class Error {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    private String id;
    private Result result;
    private Error error;

    @JsonIgnore
    private String name;
    @JsonIgnore
    private String method;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
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

    public PolicyResolutionResponse(String name) {
        this.name = name;
    }

    public PolicyResolutionResponse() {
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
