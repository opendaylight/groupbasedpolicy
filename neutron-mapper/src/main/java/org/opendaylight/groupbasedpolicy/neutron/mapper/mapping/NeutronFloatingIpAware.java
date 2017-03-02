/*
 * Copyright (c) 2015 Intel, Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.Floatingips;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class NeutronFloatingIpAware implements NeutronAware<Floatingip> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronFloatingIpAware.class);
    public static final InstanceIdentifier<Floatingip> FLOATING_IP_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Floatingips.class).child(Floatingip.class).build();
    private final DataBroker dataProvider;

    public NeutronFloatingIpAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    @Override
    public void onCreated(Floatingip floatingIP, Neutron neutron) {
        LOG.trace("created floatinIp - {}", floatingIP);
    }

    @Override
    public void onUpdated(Floatingip oldFloatingIp, Floatingip newFloatingIp, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated floatingIP - OLD: {}\nNEW: {}", oldFloatingIp, newFloatingIp);
        IpAddress oldEpIp = oldFloatingIp.getFixedIpAddress();
        IpAddress newEpIp = newFloatingIp.getFixedIpAddress();
        if (Objects.equal(oldEpIp, newEpIp)) {
            // floating IP was not moved from one port to the other
            return;
        }
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        try {
            Utils.syncNat(rwTx, oldFloatingIp, newFloatingIp);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        syncNatForEndpoint(rwTx, oldFloatingIp, newFloatingIp);
        boolean isSubmitToDsSuccessful = DataStoreHelper.submitToDs(rwTx);
        if (!isSubmitToDsSuccessful) {
            LOG.warn("Nat address {} was not added to endpoint {}", newFloatingIp.getFloatingIpAddress(), newEpIp);
        }
    }

    @Deprecated
    private void syncNatForEndpoint(ReadWriteTransaction rwTx, Floatingip newFloatingIp, Floatingip oldFloatingIp) {
        IpAddress oldEpIp = oldFloatingIp.getFixedIpAddress();
        IpAddress newEpIp = newFloatingIp.getFixedIpAddress();
        IpAddress epNatIp = newFloatingIp.getFloatingIpAddress();
        L3ContextId routerL3ContextId = new L3ContextId(newFloatingIp.getRouterId().getValue());
        if (epNatIp != null && newEpIp != null) {
            NatAddress nat = new NatAddressBuilder().setNatAddress(epNatIp).build();
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointIid(routerL3ContextId, newEpIp)
                .augmentation(NatAddress.class), nat, true);
        }
        if (oldEpIp != null) {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.l3EndpointIid(routerL3ContextId, oldEpIp).augmentation(NatAddress.class), rwTx);
        }
    }

    @Override
    public void onDeleted(Floatingip floatingIP, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("deleted floatingIP - {}", floatingIP);
    }
}
