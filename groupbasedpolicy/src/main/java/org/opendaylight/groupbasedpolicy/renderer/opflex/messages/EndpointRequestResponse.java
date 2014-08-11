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
public class EndpointRequestResponse extends RpcMessage {

    public static final String REQUEST_MESSAGE_RESPONSE = "endpoint_request_response";

    static public class Endpoint {
        private String subject;
        private String context;
        private String policy_name;
        private String location;
        private List<String> identifier;
        private List<String> data;
        private String status;
        private int prr;
        public String getSubject() {
            return subject;
        }
        public void setSubject(String subject) {
            this.subject = subject;
        }
        public String getContext() {
            return context;
        }
        public void setContext(String context) {
            this.context = context;
        }
        public String getPolicy_name() {
            return policy_name;
        }
        public void setPolicy_name(String policy_name) {
            this.policy_name = policy_name;
        }
        public String getLocation() {
            return location;
        }
        public void setLocation(String location) {
            this.location = location;
        }
        public List<String> getIdentifier() {
            return identifier;
        }
        public void setIdentifier(List<String> identifier) {
            this.identifier = identifier;
        }
        public List<String> getData() {
            return data;
        }
        public void setData(List<String> data) {
            this.data = data;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public int getPrr() {
            return prr;
        }
        public void setPrr(int prr) {
            this.prr = prr;
        }
    }

    static public class Result {
        List<Endpoint> endpoint;

        public List<Endpoint> getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(List<Endpoint> endpoint) {
            this.endpoint = endpoint;
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

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public EndpointRequestResponse(String name) {
        this.name = name;
    }

    public EndpointRequestResponse() {
        this.name = REQUEST_MESSAGE_RESPONSE;
    }
    @JsonIgnore
    @Override
    public boolean valid() {
        return true;
    }
    
}
