/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

import java.util.Map;

import com.google.common.collect.Maps;

public class JsonRpcMessageMap {
    private Map<String, JsonRpcMessage> messageMap;
    
    public JsonRpcMessageMap() {
        messageMap = Maps.newHashMap();
    }
    
    public JsonRpcMessage get(String messageName) {
        return messageMap.get(messageName);
    }

    public void add(JsonRpcMessage message) {
        messageMap.put(message.getName(), message);
    }
}
