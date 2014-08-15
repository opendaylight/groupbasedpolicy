/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;



/**
 * Factory for creating contexts for the correct type of Endpoint.
 *
 * @author tbachman
 *
 */
public class EprContextFactory { 

    /**
     * Create an Endpoint Registry Context from an OpFlex
     * Endpoint Declaration Request messasge.
     *
     */
    public static EprContext create(JsonRpcEndpoint agent,
            EndpointDeclarationRequest request,
            DataBroker dataProvider, ScheduledExecutorService executor) {
        EndpointDeclarationRequest.Params params = request.getParams().get(0);

        /*
         * Use the first identifier to determine the type of
         * identifier being passed to us, so we can install the
         * EP into the appropriate EPR list
         */
        Identity id = 
                new Identity(params.getIdentifier().get(0));
        if (id.isL2()) {
            L2EprContext ctx =
                    new L2EprContext(agent, id, request,
                    params.getIdentifier().size(),
                    dataProvider, executor);
            return ctx;
        }
        else if (id.isL3()) {
            L3EprContext ctx =
                    new L3EprContext(agent, id, request,
                    params.getIdentifier().size(),
                    dataProvider, executor);
                return ctx;
        }

        return null;
    }
    

    public static EprContext create(JsonRpcEndpoint agent,
            EndpointRequestRequest request,
            DataBroker dataProvider, ScheduledExecutorService executor) {
        EndpointRequestRequest.Params params = request.getParams().get(0);

        /*
         * Use the first identifier to determine the type of
         * identifier being passed to us, so we can install the
         * EP into the appropriate EPR list
         */
        Identity id =
                new Identity(params.getIdentifier().get(0));
        if (id.isL2()) {
            L2EprContext ctx =
                    new L2EprContext(agent, id, request,
                    params.getIdentifier().size(),
                    dataProvider, executor);
            return ctx;
        }
        else if (id.isL3()) {
            L3EprContext ctx =
                    new L3EprContext(agent, id, request,
                    params.getIdentifier().size(),
                    dataProvider, executor);
                return ctx;
        }

        System.out.println("Couldn't create context");
        return null;
    }

}
