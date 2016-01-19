/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Listens on PortByEndpoint created events. port-name is augmented to endpoint and endpoint-l3 (IFF
 * they exist in DS).
 */
public class PortByEndpointListener extends DataTreeChangeHandler<PortByEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(PortByEndpointListener.class);
    private static final String TAP = "tap";

    public PortByEndpointListener(DataBroker dataProvider) {
        super(dataProvider,
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Mappings.class)
                            .child(NeutronByGbpMappings.class)
                            .child(PortsByEndpoints.class)
                            .child(PortByEndpoint.class)
                            .build()));
    }

    @Override
    protected void onWrite(DataObjectModification<PortByEndpoint> rootNode,
            InstanceIdentifier<PortByEndpoint> rootIdentifier) {
        PortByEndpoint portByEp = rootNode.getDataAfter();
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();

        final EndpointKey epKey = new EndpointKey(portByEp.getL2Context(), portByEp.getMacAddress());
        InstanceIdentifier<Endpoint> epIid = IidFactory.endpointIid(epKey);
        Optional<Endpoint> potentialEp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, epIid, rwTx);
        if (!potentialEp.isPresent()) {
            LOG.warn("PortByEndpoint created notification received but endpoint {} does not exist in DS."
                    + " port-name was not added.", epKey);
            rwTx.cancel();
            return;
        }
        Endpoint ep = potentialEp.get();
        Name portName = createTapPortName(portByEp.getPortId());
        OfOverlayContext newOfOverlayCtx = new OfOverlayContextBuilder().setPortName(portName).build();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, epIid.augmentation(OfOverlayContext.class), newOfOverlayCtx);

        List<L3Address> l3Addresses = ep.getL3Address();
        if (l3Addresses == null) {
            LOG.warn("PortByEndpoint created notification received but endpoint {} has no L3 address."
                    + " port-name was not added.", epKey);
            rwTx.cancel();
            return;
        }
        L3Address l3Address = l3Addresses.get(0);

        EndpointL3Key l3EpKey = new EndpointL3Key(l3Address.getIpAddress(), l3Address.getL3Context());
        InstanceIdentifier<EndpointL3> l3EpIid = IidFactory.l3EndpointIid(l3EpKey);
        Optional<EndpointL3> potentialL3Ep =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, l3EpIid, rwTx);
        if (!potentialL3Ep.isPresent()) {
            LOG.warn("PortByEndpoint created notification received but L3 endpoint {} does not exist in DS."
                    + " port-name was not added.", l3EpKey);
            rwTx.cancel();
            return;
        }
        OfOverlayL3Context newOfOverlayL3Ctx = new OfOverlayL3ContextBuilder().setPortName(portName).build();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, l3EpIid.augmentation(OfOverlayL3Context.class), newOfOverlayL3Ctx);

        DataStoreHelper.submitToDs(rwTx);
    }

    @Override
    protected void onDelete(DataObjectModification<PortByEndpoint> rootNode,
            InstanceIdentifier<PortByEndpoint> rootIdentifier) {
        // NOOP
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<PortByEndpoint> rootNode,
            InstanceIdentifier<PortByEndpoint> rootIdentifier) {
        // NOOP
    }

    private static Name createTapPortName(UniqueId portId) {
        return new Name(TAP + portId.getValue().substring(0, 11));
    }

}
