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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class ManagedObject {

    public static class Properties {
        private String name;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getData() {
            return data;
        }
        public void setData(String data) {
            this.data = data;
        }
        private String data;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<Properties> getProperties() {
        return properties;
    }
    public void setProperties(List<Properties> properties) {
        this.properties = properties;
    }
    public List<ManagedObject> getChildren() {
        return children;
    }
    public void setChildren(List<ManagedObject> children) {
        this.children = children;
    }
    public List<ManagedObject> getStatistics() {
        return statistics;
    }
    public void setStatistics(List<ManagedObject> statistics) {
        this.statistics = statistics;
    }
    public List<ManagedObject> getFrom_relations() {
        return from_relations;
    }
    public void setFrom_relations(List<ManagedObject> from_relations) {
        this.from_relations = from_relations;
    }
    public List<ManagedObject> getTo_relations() {
        return to_relations;
    }
    public void setTo_relations(List<ManagedObject> to_relations) {
        this.to_relations = to_relations;
    }
    public List<ManagedObject> getFaults() {
        return faults;
    }
    public void setFaults(List<ManagedObject> faults) {
        this.faults = faults;
    }
    public List<ManagedObject> getHealth() {
        return health;
    }
    public void setHealth(List<ManagedObject> health) {
        this.health = health;
    }
    private String name;
    private List<Properties> properties;
    private List<ManagedObject> children;
    private List<ManagedObject> statistics;
    private List<ManagedObject> from_relations;
    private List<ManagedObject> to_relations;
    private List<ManagedObject> faults;
    private List<ManagedObject> health;
    
}
