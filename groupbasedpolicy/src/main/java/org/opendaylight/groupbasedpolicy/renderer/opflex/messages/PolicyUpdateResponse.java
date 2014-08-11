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

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class PolicyUpdateResponse extends RpcMessage {

    public static final String UPDATE_MESSAGE_RESPONSE = "update_policy_response";

    static public class Result {
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

    public PolicyUpdateResponse(String name) {
        this.name = name;
    }

    public PolicyUpdateResponse() {
        this.name = UPDATE_MESSAGE_RESPONSE;
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
