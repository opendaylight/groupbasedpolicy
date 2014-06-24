/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class JsonRpc10Request {

    String id;
    String method;
    JsonRpcMessage params;

    public JsonRpc10Request() {
    }

    public JsonRpc10Request(String id) {
        setId(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonRpcMessage getParams() {
        return this.params;
    }

    public void setParams(JsonRpcMessage params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "JsonRpc10Request [id=" + id + ", method=" + method
                + ", params=" + params + "]";
    }
}
