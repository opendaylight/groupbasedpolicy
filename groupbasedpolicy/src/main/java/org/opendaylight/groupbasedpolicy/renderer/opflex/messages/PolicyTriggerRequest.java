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
public class PolicyTriggerRequest extends RpcMessage {

    public static final String TRIGGER_MESSAGE = "trigger_policy";

    static public class Params {
        private String policy_type;
        private String context;
        private String policy_name;
        private int prr;
        public String getPolicy_type() {
            return policy_type;
        }
        public void setPolicy_type(String policy_type) {
            this.policy_type = policy_type;
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
        public int getPrr() {
            return prr;
        }
        public void setPrr(int prr) {
            this.prr = prr;
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

    public PolicyTriggerRequest(String name) {
        this.name = name;
    }

    public PolicyTriggerRequest() {
        this.name = TRIGGER_MESSAGE;
        this.method = TRIGGER_MESSAGE;
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
