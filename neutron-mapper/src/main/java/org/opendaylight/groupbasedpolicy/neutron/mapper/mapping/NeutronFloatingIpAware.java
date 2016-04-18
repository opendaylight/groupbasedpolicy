/*
 * Copyright (c) 2015 Intel, Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class NeutronFloatingIpAware implements MappingProcessor<Floatingip, NeutronFloatingIP> {

    public static final Logger LOG = LoggerFactory.getLogger(NeutronFloatingIpAware.class);
    private final DataBroker dataProvider;

    public NeutronFloatingIpAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    @Override
    public NeutronFloatingIP convertToNeutron(Floatingip fip) {
        NeutronFloatingIP result = new NeutronFloatingIP();
        result.setID(fip.getUuid().getValue());
        if (fip.getFloatingNetworkId() != null) {
            result.setFloatingNetworkUUID(fip.getFloatingNetworkId().getValue());
        }
        if (fip.getPortId() != null) {
            result.setPortUUID(fip.getPortId().getValue());
        }
        if (fip.getFixedIpAddress() != null ) {
            result.setFixedIPAddress(String.valueOf(fip.getFixedIpAddress().getValue()));
        }
        if (fip.getFloatingIpAddress() != null) {
            result.setFloatingIPAddress(String.valueOf(fip.getFloatingIpAddress().getValue()));
        }
        if (fip.getTenantId() != null) {
            result.setTenantID(fip.getTenantId());
        }
        if (fip.getRouterId() != null) {
            result.setRouterUUID(fip.getRouterId().getValue());
        }
        result.setStatus(fip.getStatus());
        return result;
    }

    @Override
    public int canCreate(NeutronFloatingIP floatingIP) {
        LOG.trace("canCreate floatingIP - {}", floatingIP);
        return StatusCode.OK;
    }

    @Override
    public void created(NeutronFloatingIP floatingIP) {
        LOG.trace("created floatinIp - {}", floatingIP);
    }

    @Override
    public int canUpdate(NeutronFloatingIP delta, NeutronFloatingIP original) {
        LOG.trace("canUpdate floatingIP - delta: {} original: {}", delta, original);
        String oldFixedIPAddress = Strings.nullToEmpty(original.getFixedIPAddress());
        String newFixedIPAddress = Strings.nullToEmpty(delta.getFixedIPAddress());
        if (oldFixedIPAddress.equals(newFixedIPAddress)) {
            // interesting fields were not changed
            return StatusCode.OK;
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        L3ContextId routerL3Context = new L3ContextId(delta.getRouterUUID());
        String newFloatingIp = Strings.nullToEmpty(delta.getFloatingIPAddress());
        if (!newFixedIPAddress.isEmpty() && !newFloatingIp.isEmpty()) {
            IpAddress epIp = Utils.createIpAddress(newFixedIPAddress);
            IpAddress epNatIp = Utils.createIpAddress(newFloatingIp);
            NatAddress nat = new NatAddressBuilder().setNatAddress(epNatIp).build();
            rwTx.put(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.l3EndpointIid(routerL3Context, epIp).augmentation(NatAddress.class), nat, true);
        }
        if (!oldFixedIPAddress.isEmpty()) {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.l3EndpointIid(routerL3Context, Utils.createIpAddress(oldFixedIPAddress))
                        .augmentation(NatAddress.class),
                    rwTx);
        }

        boolean isSubmitToDsSuccessful = DataStoreHelper.submitToDs(rwTx);
        if (!isSubmitToDsSuccessful) {
            return StatusCode.INTERNAL_SERVER_ERROR;
        }

        return StatusCode.OK;
    }

    @Override
    public void updated(NeutronFloatingIP floatingIP) {
        LOG.trace("updated floatingIP - {}", floatingIP);
    }

    @Override
    public int canDelete(NeutronFloatingIP floatingIP) {
        LOG.trace("canDelete floatingIP - {}", floatingIP);
        return StatusCode.OK;
    }

    @Override
    public void deleted(NeutronFloatingIP floatingIP) {
        LOG.trace("deleted floatingIP - {}", floatingIP);
    }
}
