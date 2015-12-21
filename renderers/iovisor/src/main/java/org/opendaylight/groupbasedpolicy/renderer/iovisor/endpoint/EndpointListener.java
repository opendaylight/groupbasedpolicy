/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointListener implements DataTreeChangeListener<EndpointL3>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointListener.class);

    private final DataBroker dataProvider;
    private final ListenerRegistration<EndpointListener> registration;
    private final EndpointManager endpointManager;

    public EndpointListener(DataBroker dataProvider, EndpointManager endpointManager) {
        this.dataProvider = checkNotNull(dataProvider);
        registration = dataProvider.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointsIidWildcard()), this);
        this.endpointManager = endpointManager;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<EndpointL3>> changes) {
        for (DataTreeModification<EndpointL3> change : changes) {
            DataObjectModification<EndpointL3> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    LOG.info("Received Endpoint {}", rootNode.getDataAfter().getIpAddress());
                    if (rootNode.getDataAfter().getAugmentation(IovisorModuleAugmentation.class) != null) {
                        endpointManager.processEndpoint(rootNode.getDataAfter());
                    } else {
                        LOG.info("Endpoint did not have IOVisor Module Augmentation (Location URI). Ignoring");
                    }
                    break;
                case DELETE:
                    LOG.warn("Received Endpoint {} but DELETE not implemented yet",
                            rootNode.getDataAfter().getIpAddress());
            }
        }
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
