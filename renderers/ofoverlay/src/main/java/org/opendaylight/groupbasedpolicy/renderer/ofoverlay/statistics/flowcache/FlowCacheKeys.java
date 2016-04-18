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

public final class FlowCacheKeys {

    private static final String SEPARATOR = ",";

    private String value;

    private FlowCacheKeys(FlowCacheKeysBuilder builder) {
        this.value = Joiner.on(SEPARATOR).join(builder.getValues());
    }

    public String getValue() {
        return value;
    }

    public static FlowCacheKeysBuilder builder(){
        return new FlowCacheKeysBuilder();
    }

    public static class FlowCacheKeysBuilder {

        private List<String> values = new ArrayList<>();

        public List<String> getValues() {
            return values;
        }

        /**
         * Sets FlowCache's "key" values by copying {@code String}s from {@code values},
         * to avoid immutable list put as a parameter.
         * {@code null}s are omitted.
         *
         * @param values List of String
         * @return FlowCacheKeysBuilder
         */
        public FlowCacheKeysBuilder setValues(List<String> values) {
            Preconditions.checkNotNull(values);
            for (String value : values) {
                if (value != null) {
                    this.values.add(value);
                }
            }
            return this;
        }

        public FlowCacheKeysBuilder addValue(String value) {
            values.add(Preconditions.checkNotNull(value));
            return this;
        }

        public FlowCacheKeys build() {
            return new FlowCacheKeys(this);
        }
    }
}
