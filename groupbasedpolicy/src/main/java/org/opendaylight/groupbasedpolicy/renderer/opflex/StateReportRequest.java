/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

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
        private String object;   // TODO: change to MOs
        private List<String> fault;
        private List<String> event;
        private List<String> statistics;
        private List<String> health;
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
        public String getObject() {
            return object;
        }
        public void setObject(String object) {
            this.object = object;
        }
        public List<String> getFault() {
            return fault;
        }
        public void setFault(List<String> fault) {
            this.fault = fault;
        }
        public List<String> getEvent() {
            return event;
        }
        public void setEvent(List<String> event) {
            this.event = event;
        }
        public List<String> getStatistics() {
            return statistics;
        }
        public void setStatistics(List<String> statistics) {
            this.statistics = statistics;
        }
        public List<String> getHealth() {
            return health;
        }
        public void setHealth(List<String> health) {
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
    }
}
