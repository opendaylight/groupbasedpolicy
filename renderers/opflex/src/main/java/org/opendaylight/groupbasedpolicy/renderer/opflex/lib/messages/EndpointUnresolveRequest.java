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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class EndpointUnresolveRequest extends RpcMessage {

    public static final String EP_UNRESOLVE_REQUEST_MESSAGE = "endpoint_unresolve";

    static public class Params {

        private String subject;
        private Uri endpoint_uri;
        private EndpointIdentity endpoint_ident;

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public Uri getEndpoint_uri() {
            return endpoint_uri;
        }

        public void setEndpoint_uri(Uri endpoint_uri) {
            this.endpoint_uri = endpoint_uri;
        }

        public EndpointIdentity getEndpoint_ident() {
            return endpoint_ident;
        }

        public void setEndpoint_ident(EndpointIdentity endpoint_ident) {
            this.endpoint_ident = endpoint_ident;
        }
    }

    private JsonNode id;
    private String method;
    private List<Params> params;

    @JsonIgnore
    private String name;

    @Override
    public JsonNode getId() {
        return id;
    }

    @Override
    public void setId(JsonNode id) {
        this.id = id;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    public List<Params> getParams() {
        return this.params;
    }

    public void setParams(List<Params> params) {
        this.params = params;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public EndpointUnresolveRequest(String name) {
        this.name = name;
    }

    public EndpointUnresolveRequest() {
        this.name = EP_UNRESOLVE_REQUEST_MESSAGE;
    }

    /**
     * Minimal check on validity of message
     * 
     * @return true if message has passed validity check
     */
    @JsonIgnore
    @Override
    public boolean valid() {
        if (params == null)
            return false;
        if (params.get(0) == null)
            return false;
        return true;
    }

}
