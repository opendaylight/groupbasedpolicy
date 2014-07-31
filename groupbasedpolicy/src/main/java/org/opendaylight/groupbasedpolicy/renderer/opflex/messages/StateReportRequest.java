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
public class StateReportRequest extends RpcMessage {

    public static final String STATE_MESSAGE = "report_state";

    static public class Params {
        private String subject;
        private String context;
        private ManagedObject object;
        private List<ManagedObject> fault;
        private List<ManagedObject> event;
        private List<ManagedObject> statistics;
        private List<ManagedObject> health;
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
        public ManagedObject getObject() {
            return object;
        }
        public void setObject(ManagedObject object) {
            this.object = object;
        }
        public List<ManagedObject> getFault() {
            return fault;
        }
        public void setFault(List<ManagedObject> fault) {
            this.fault = fault;
        }
        public List<ManagedObject> getEvent() {
            return event;
        }
        public void setEvent(List<ManagedObject> event) {
            this.event = event;
        }
        public List<ManagedObject> getStatistics() {
            return statistics;
        }
        public void setStatistics(List<ManagedObject> statistics) {
            this.statistics = statistics;
        }
        public List<ManagedObject> getHealth() {
            return health;
        }
        public void setHealth(List<ManagedObject> health) {
            this.health = health;
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

    public StateReportRequest(String name) {
        this.name = name;
    }

    public StateReportRequest() {
        this.name = STATE_MESSAGE;
        this.method = STATE_MESSAGE;
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
