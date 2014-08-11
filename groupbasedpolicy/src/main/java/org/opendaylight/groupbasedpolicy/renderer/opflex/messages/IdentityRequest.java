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

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class IdentityRequest extends RpcMessage {

    public static final String IDENTITY_MESSAGE = "send_identity";

    static public class Params {
        private String name;
        private String domain;
        private List<String> my_role;
        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Params() {
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


    }
    private String id;
    private String method;
    private List<Params> params;

    @JsonIgnore
    private String name;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
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

    public IdentityRequest(String name) {
        this.name = name;
    }

    public IdentityRequest() {
        this.name = IDENTITY_MESSAGE;
        this.method = IDENTITY_MESSAGE;
    }
    
    /**
     * Minimal check on validity of message
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
