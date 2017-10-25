/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ne.location.provider;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsListener implements ClusteredDataTreeChangeListener<AddressEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointsListener.class);

    private NeLocationProvider listener;
    private ListenerRegistration<EndpointsListener> listenerRegistration;

    public EndpointsListener(DataBroker dataBroker, NeLocationProvider listener) {
        this.listener = listener;
        this.listenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
            LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Endpoints.class)
                .child(AddressEndpoints.class).child(AddressEndpoint.class).build()), this);
        LOG.info("NE location provider registered for Endpoints listening");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<AddressEndpoint>> changes) {
        listener.onEndpointsChange(changes);
    }

    public void close() {
        this.listenerRegistration.close();
    }
}
