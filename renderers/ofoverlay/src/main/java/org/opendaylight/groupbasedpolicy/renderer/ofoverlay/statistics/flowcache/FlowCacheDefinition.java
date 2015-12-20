/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheFilter.FlowCacheFilterBuilder;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheKeys.FlowCacheKeysBuilder;

import com.google.common.base.Preconditions;

/**
 * An object to handle flow-cache parameters for JSON conversion
 */
public final class FlowCacheDefinition {

    private String keys;
    private String value;
    private String filter;
    private boolean log;

    private FlowCacheDefinition() {
    }

    private FlowCacheDefinition(FlowCacheDefinitionBuilder builder) {
        this.keys = builder.getKeysBuilder().build().getValue();
        this.value = builder.getValue();
        this.filter = builder.getFilterBuilder().build().getValue();
        this.log = builder.isLog();
    }

    public String getKeys() {
        return keys;
    }

    public String getValue() {
        return value;
    }

    public String getFilter() {
        return filter;
    }

    public boolean getLog() {
        return log;
    }

    public static FlowCacheDefinitionBuilder builder(){
        return new FlowCacheDefinitionBuilder();
    }

    @Override
    public String toString() {
        return "FlowCacheDefinition [keys=" + keys + ", value=" + value + ", filter=" + filter + ", log=" + log + "]";
    }

    public static class FlowCacheDefinitionBuilder {

        private String value;
        private boolean log = false;
        private final FlowCacheKeysBuilder keysBuilder = new FlowCacheKeysBuilder();
        private final FlowCacheFilterBuilder filterBuilder = new FlowCacheFilterBuilder();

        public FlowCacheKeysBuilder getKeysBuilder() {
            return keysBuilder;
        }

        public String getValue() {
            return value;
        }

        public FlowCacheDefinitionBuilder setValue(String value) {
            Preconditions.checkNotNull(value);
            this.value = value;
            return this;
        }

        public FlowCacheFilterBuilder getFilterBuilder() {
            return filterBuilder;
        }

        public boolean isLog() {
            return log;
        }

        public FlowCacheDefinitionBuilder setLog(boolean log) {
            this.log = log;
            return this;
        }

        public FlowCacheDefinition build() {
            return new FlowCacheDefinition(this);
        }
    }
}
