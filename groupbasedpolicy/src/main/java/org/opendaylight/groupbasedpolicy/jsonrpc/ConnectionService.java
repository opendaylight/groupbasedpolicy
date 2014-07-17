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
 * An interface to provide notifications when connections are
 * established or closed. The connection notifications
 * use{@link RpcEncpoint} objects; as connections come and go,
 * the {@link RpcEndpoint} objects associated with the connections
 * can be long-lived
 *
 * @author tbachman
 */
public interface ConnectionService {
    /**
     *
     * Indication that a new connections was established with
     * the {@link JsonRpcEndpoint}
     *
     * @param endpoint The endpoint that added the connection.
     */
    public void addConnection(JsonRpcEndpoint endpoint);

    /**
     *
     * Indication that a connections with the {@link JsonRpcEndpoint}
     * was closed.
     *
     * @param endpoint The endpoint that closed the connection.
     */
    public void channelClosed(JsonRpcEndpoint endpoint) throws Exception;
}
