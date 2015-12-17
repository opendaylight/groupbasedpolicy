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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronOvsdbIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NeutronGbpFloatingIpListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronGbpFloatingIpListener.class);
    private final ListenerRegistration<DataChangeListener> gbpFloatingIpListener;
    private final DataBroker dataBroker;

    public NeutronGbpFloatingIpListener(DataBroker dataBroker) {
        this.dataBroker = checkNotNull(dataBroker);
         gbpFloatingIpListener = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                NeutronOvsdbIidFactory.neutronGbpInternalPortByFloatingIpIidWildcard(), this, DataChangeScope.BASE);
        LOG.trace("NeutronGbpFloatingIpListener started");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        /*
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getValue() instanceof InternalPortByFloatingIpPort) {
                InternalPortByFloatingIpPort internalPortByFloatingIp = (InternalPortByFloatingIpPort) entry.getValue();
                processInternalPortByFloatingIp(internalPortByFloatingIp);
            }
        }

        /*
         * Updates
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof InternalPortByFloatingIpPort) {
                InternalPortByFloatingIpPort internalPortByFloatingIp = (InternalPortByFloatingIpPort) entry.getValue();
                processInternalPortByFloatingIp(internalPortByFloatingIp);
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

    private void processInternalPortByFloatingIp(InternalPortByFloatingIpPort internalPortByFloatingIp) {
        IpAddress natAddress = internalPortByFloatingIp.getFloatingIpPortIpAddress();
        IpAddress ipAddress = internalPortByFloatingIp.getInternalPortIpAddress();

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<EndpointByPort> optEndpointByPort = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL,
                NeutronOvsdbIidFactory.endpointByPortIid(internalPortByFloatingIp.getInternalPortId()), rTx);
        if (optEndpointByPort.isPresent()) {
            EpKey l2EpKey = new EpKey(optEndpointByPort.get().getL2Context(), optEndpointByPort.get().getMacAddress());
            updateEndpointNat(l2EpKey, ipAddress, natAddress);
        } else {
            LOG.error("processEpByFloatingIp: Couldn't find EP associated with {}.", internalPortByFloatingIp);
        }
    }

    private void updateEndpointNat(EpKey l2EpKey, IpAddress ipAddress, IpAddress natAddress) {
        Endpoint l2Ep;
        EndpointL3 l3Ep;
        EndpointL3Key l3EpKey = null;

        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        Optional<Endpoint> optL2Ep = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointIid((L2BridgeDomainId) l2EpKey.getL2Context(), l2EpKey.getMacAddress()), rwTx);
        if (optL2Ep.isPresent()) {
            l2Ep = optL2Ep.get();
        } else {
            LOG.error("updateEndpointNat: No Endpoint {} ", l2EpKey);
            return;
        }

        if (l2Ep.getL3Address() == null) {
            LOG.error("updateEndpointNat: L2Ep {} had no IP address to translate to.", l2Ep);
            return;
        }

        for (L3Address l3Address : l2Ep.getL3Address()) {
            if (l3Address.getIpAddress().equals(ipAddress)) {
                l3EpKey = new EndpointL3Key(l3Address.getIpAddress(), l3Address.getL3Context());
                break;
            }
        }
        if (l3EpKey == null) {
            return;
        }
        Optional<EndpointL3> optL3Ep = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.l3EndpointIid(l3EpKey.getL3Context(), l3EpKey.getIpAddress()), rwTx);
        if (optL3Ep.isPresent()) {
            l3Ep = optL3Ep.get();
            NatAddress nat = new NatAddressBuilder().setNatAddress(natAddress).build();

            EndpointL3 updatedEpL3 = new EndpointL3Builder(l3Ep).addAugmentation(NatAddress.class, nat).build();

            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointIid(l3EpKey.getL3Context(), l3EpKey.getIpAddress()), updatedEpL3,true);
            boolean writeResult = DataStoreHelper.submitToDs(rwTx);
            if(!writeResult) {
                LOG.trace("updateEndpointNat: Could not write {} to datastore.",updatedEpL3.getKey());
            }
        }
    }

    @Override
    public void close() throws Exception {
        gbpFloatingIpListener.close();
    }

}
