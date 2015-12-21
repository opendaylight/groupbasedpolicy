/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class EndpointManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);

    private EndpointListener endpointListener;
    private final IovisorEndpointAug iovisorEndpointAug;

    public EndpointManager(DataBroker dataBroker, EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        Preconditions.checkNotNull(dataBroker, "DataBroker instance must not be null");
        iovisorEndpointAug = new IovisorEndpointAug(epRendererAugmentationRegistry);
        endpointListener = new EndpointListener(dataBroker, this);
        LOG.info("Initialized IOVisor EndpointManager");
    }

    public void processEndpoint(EndpointL3 endpoint) {
        /*
         * Is Augmentation valid?
         * - IPAddress or a name that can be resolved to an IPAddress
         */

        /*
         * IOVisorModule registered?
         * - In this rev, we will register the IOVisorModule (agent)
         */

        /*
         * Validate there is a resolved policy for tenant/EPGs.
         * - If so, add to resolved policy endpoint map thing
         * - If not, wait until there is (listener)
         * .... not real sure on this one... chicken and egg.
         */

        /*
         * Validate there is a valid forwarding model
         *
         */

        LOG.info("Processed Endpoint {}", endpoint);
    }

    @Override
    public void close() throws Exception {
        if (iovisorEndpointAug != null)
            iovisorEndpointAug.close();
        if (endpointListener != null)
            endpointListener.close();

    }
}
