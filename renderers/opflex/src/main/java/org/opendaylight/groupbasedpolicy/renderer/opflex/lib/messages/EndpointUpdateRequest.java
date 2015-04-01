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
public class EndpointUpdateRequest extends RpcMessage {

    public static final String EP_UPDATE_MESSAGE = "endpoint_update";

    static public class Params {

        private List<ManagedObject> replace;
        private List<Uri> delete_uri;

        public List<ManagedObject> getReplace() {
            return replace;
        }

        public void setReplace(List<ManagedObject> replace) {
            this.replace = replace;
        }

        public List<Uri> getDelete_uri() {
            return delete_uri;
        }

        public void setDelete_uri(List<Uri> delete_uri) {
            this.delete_uri = delete_uri;
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

    public EndpointUpdateRequest(String name) {
        this.name = name;
    }

    public EndpointUpdateRequest() {
        this.name = EP_UPDATE_MESSAGE;
        this.method = EP_UPDATE_MESSAGE;
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
