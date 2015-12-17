/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronOvsdbIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NeutronGbpExternalGatewaysListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronGbpExternalGatewaysListener.class);
    private final ListenerRegistration<DataChangeListener> gbpExternalGatewaysListener;
    private final DataBroker dataBroker;

    public NeutronGbpExternalGatewaysListener(DataBroker dataBroker) {
        this.dataBroker = checkNotNull(dataBroker);
        gbpExternalGatewaysListener = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                NeutronOvsdbIidFactory.neutronGbpExternalGatewayIidWildcard(), this, DataChangeScope.BASE);

        LOG.trace("NeutronGbpMapperListener started");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        /*
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getValue() instanceof ExternalGatewayAsL3Endpoint) {
                ExternalGatewayAsL3Endpoint ExternalGatewayAsL3Endpoint = (ExternalGatewayAsL3Endpoint) entry.getValue();
                processExternalGatewayAsL3Endpoint(ExternalGatewayAsL3Endpoint);
            }
        }

        /*
         * Updates
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof ExternalGatewayAsL3Endpoint) {
                ExternalGatewayAsL3Endpoint ExternalGatewayAsL3Endpoint = (ExternalGatewayAsL3Endpoint) entry.getValue();
                processExternalGatewayAsL3Endpoint(ExternalGatewayAsL3Endpoint);
            }
        }

        /*
         * Deletions
         */
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            /*
             * Remove ?
             */
        }
    }

    private void processExternalGatewayAsL3Endpoint(ExternalGatewayAsL3Endpoint ExternalGatewayAsL3Endpoint) {
        LOG.trace("Received ExternalGatewayAsL3Endpoints notification {}", ExternalGatewayAsL3Endpoint);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        EndpointL3Key epL3Key = new EndpointL3Key(ExternalGatewayAsL3Endpoint.getIpAddress(),
                ExternalGatewayAsL3Endpoint.getL3Context());
        InstanceIdentifier<EndpointL3> epL3Iid = IidFactory.l3EndpointIid(epL3Key.getL3Context(),
                epL3Key.getIpAddress());
        Optional<EndpointL3> optEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, epL3Iid, rwTx);

        if (optEndpointL3.isPresent()) {
            EndpointL3 endpointL3 = optEndpointL3.get();

            // Handle Augmentation for location ie Context
            OfOverlayL3Context ofL3ContextAug = endpointL3.getAugmentation(OfOverlayL3Context.class);
            OfOverlayL3ContextBuilder ofL3ContextAugBuilder;
            if (ofL3ContextAug != null) {
                 ofL3ContextAugBuilder = new OfOverlayL3ContextBuilder(ofL3ContextAug);
            } else {
                 ofL3ContextAugBuilder = new OfOverlayL3ContextBuilder();
            }
            EndpointL3Builder epL3Builder = new EndpointL3Builder(endpointL3).addAugmentation(
                    OfOverlayL3Context.class, ofL3ContextAugBuilder.setLocationType(LocationType.External)
                        .build());
            // Handle Augmentation for NAT.
            NatAddress ofL3NatAug = endpointL3.getAugmentation(NatAddress.class);
            if (ofL3NatAug != null) {
                epL3Builder.addAugmentation(NatAddress.class, ofL3NatAug);
            }
            rwTx.put(LogicalDatastoreType.OPERATIONAL, epL3Iid, epL3Builder.build());
            DataStoreHelper.submitToDs(rwTx);
        } else {
            LOG.error("External Gateway {} does not exist in Endpoint repository", epL3Key);
            return;
        }
    }

    @Override
    public void close() throws Exception {
        gbpExternalGatewaysListener.close();
    }

}
