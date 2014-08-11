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

/**
 * The abstract {@link RpcMessage} is used for creating application
 * specific RPC messages that can be used by the RPC library. The RPC
 * library uses these for serialization and deserialization of messages.
 *
 * <p>The class provides notifiers for request and response messages,
 * and provides for sending new requests.
 *
 * <p>The class should be used to store the
 *
 * @author tbachman
 */
public abstract class RpcMessage {

    public abstract String getName();
    public abstract void setName(String name);
    public abstract String getId();
    public abstract void setId(String id);
    public abstract String getMethod();
    public abstract void setMethod(String method);
    public abstract boolean valid();
}
