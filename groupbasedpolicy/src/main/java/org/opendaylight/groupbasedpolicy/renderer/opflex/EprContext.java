/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;


/**
 * A context for managing operations to the Endpoint Registry's
 * list of Endpoints.
 *
 * @author tbachman
 *
 */
public interface EprContext {
    /**
     * Provides a callback that is invoked on response
     * to a query of the EPR. This allows the caller
     * to do any behaviors.
     *
     * @author tbachman
     *
     */
    public static interface Callback {
        public void callback(EprContext ctx);
    }
    public void setCallback(Callback callback);
    public RpcMessage getRequest();

    /**
     * Create an Endpoint in the Endopint Registry. This can only
     * be called in response to an {@link EndpointDeclarationRequest}
     * message with a status of "modify" or "detach"
     *
     */
    public void createEp();

    /**
     * Delete an Endpoint in the Endopint Registry. This can only
     * be called in response to an {@link EndpointDeclarationRequest}
     * message with a status of "detach"
     *
     */
    public void deleteEp();

    /**
     * Look up an endpoint in the Endpoint Registry. This can only
     * be called in response to an {@link EndpointRequestRequest}
     * message.
     *
     */
    public void lookupEndpoint();

    /**
     * Send a response to the message that generated the Endpoint
     * Registry interaction.
     *
     */
    public void sendResponse();
}
