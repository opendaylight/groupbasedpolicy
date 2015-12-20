/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public final class FlowCacheFilter {

    private static final String AND = "&";
    private static final String LB = "(";
    private static final String RB = ")";

    private String value;

    private FlowCacheFilter(FlowCacheFilterBuilder builder) {
        this.value = Joiner.on(AND).join(builder.getValues());
    }

    public String getValue() {
        return value;
    }

    public static FlowCacheFilterBuilder builder(){
        return new FlowCacheFilterBuilder();
    }

    public static class FlowCacheFilterBuilder {

        private List<String> values = new ArrayList<>();

        public List<String> getValues() {
            return values;
        }

        public FlowCacheFilterBuilder setValues(List<String> values) {
            this.values = Preconditions.checkNotNull(values);
            return this;
        }

        public FlowCacheFilterBuilder addValue(String value) {
            values.add(LB + Preconditions.checkNotNull(value)  + RB);
            return this;
        }

        public FlowCacheFilter build() {
            return new FlowCacheFilter(this);
        }
    }
}
