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
 * The {@link RpcBroker} provides a content-based pub/sub per RpcMessage
 * type.  This allows clients to register for the messages they are interested
 * in.
 *
 * @author tbachman
 */
public interface RpcBroker {

    /**
     * The {@link RpcCallback} provides a callback interface for the
     * {@link RpcBroker}. When the broker needs to publish a new
     * {@link RpcMessage}, it invokes the callbacks that were
     * registered for that message.
     *
     * @author tbachman
     */
    public interface RpcCallback {

        /**
         * Callback that's invoked when the {@link RpcMessage}
         * request message is received
         *
         * @param endpoint The endpoint that received the messgae
         * @param message The concrete {@RpcMessage} received
         */
        public void callback(JsonRpcEndpoint endpoint, RpcMessage message);

    }

    /**
     *
     * Subscribe to a concrete {@RpcMessage}
     *
     * @param message The concrete {@link RpcMessage} message to subscribe to
     * @param callback The callback to invoke when the message is published
     *
     */
    public void subscribe(RpcMessage message, RpcCallback callback);

    /**
     * Notification to call when a new {@link RpcMessage} request
     * is received
     *
     * @param endpoint The endpoint that received this message
     * @param message the concrete {@RpcMessage}
     */
    public void publish(JsonRpcEndpoint endpoint, RpcMessage message);


}
