/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

public class FlowCache {
    private static final Pattern REPLACE_PATTERN = Pattern.compile("^\\w+:(\\w+):.*");
    static final String API_FLOW = "/flow/";
    static final String SUFFIX_JSON = "/json";

    private transient Direction direction;
    private String name;
    private FlowCacheDefinition definition;

    /** Array containing key names from FlowCacheDefinition */
    private String[] keyNames;
    private int keyNum;

    private FlowCache() {
    }

    private FlowCache(FlowCacheBuilder builder) {
        this.name = builder.getName();
        this.definition = builder.getDefinition();
        this.direction = builder.getDirection();

        this.keyNames = this.definition.getKeys().split(",");
        this.keyNum = this.keyNames.length;
        for (int i = 0; i < this.keyNum; i++) {
            keyNames[i] = parseNullableKeyName(keyNames[i]);
        }
    }

    public String getName() {
        return name;
    }

    public FlowCacheDefinition getDefinition() {
        return definition;
    }

    public Direction getDirection() {
        return direction;
    }

    public String[] getKeyNames() {
        return keyNames;
    }

    public int getKeyNum() {
        return keyNum;
    }

    public String getPath() {
        return API_FLOW + name + SUFFIX_JSON;
    }

    public String getJsonDefinition() {
        Gson gson = new Gson();
        return gson.toJson(definition);
    }

    public static FlowCacheBuilder builder() {
        return new FlowCacheBuilder();
    }

    private String parseNullableKeyName(String nullableKeyName) {
        String res = REPLACE_PATTERN.matcher(nullableKeyName).replaceAll("$1");
        return res.isEmpty() ? nullableKeyName : res;
    }

    @Override
    public String toString() {
        return "FlowCache [name=" + name + ", definition=" + definition + ", keyNames=" + Arrays.toString(keyNames)
                + ", keyNum=" + keyNum + "]";
    }

    public static class FlowCacheBuilder {

        private String name;
        private FlowCacheDefinition definition;
        private Direction direction;

        public String getName() {
            return name;
        }

        public FlowCacheBuilder setName(String name) {
            Preconditions.checkNotNull(name);
            this.name = name;
            return this;
        }

        public FlowCacheDefinition getDefinition() {
            return definition;
        }

        public FlowCacheBuilder setDefinition(FlowCacheDefinition definition) {
            Preconditions.checkNotNull(definition);
            this.definition = definition;
            return this;
        }

        public Direction getDirection() {
            return direction;
        }

        public FlowCacheBuilder setDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        public FlowCache build() {
            return new FlowCache(this);
        }
    }
}
