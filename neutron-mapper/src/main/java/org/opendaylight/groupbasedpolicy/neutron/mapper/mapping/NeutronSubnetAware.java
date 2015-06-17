/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils.ForwardingCtx;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.networks.by.l2.flood.domains.ExternalNetworkByL2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class NeutronSubnetAware implements INeutronSubnetAware {

    private final static Logger LOG = LoggerFactory.getLogger(NeutronSubnetAware.class);
    private final DataBroker dataProvider;

    public NeutronSubnetAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#canCreateSubnet(org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public int canCreateSubnet(NeutronSubnet subnet) {
        LOG.trace("canCreateSubnet - {}", subnet);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#neutronSubnetCreated(org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public void neutronSubnetCreated(NeutronSubnet neutronSubnet) {
        LOG.trace("neutronSubnetCreated - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        SubnetId subnetId = new SubnetId(Utils.normalizeUuid(neutronSubnet.getID()));
        TenantId tenantId = new TenantId(Utils.normalizeUuid(neutronSubnet.getTenantID()));
        Subnet subnet = createSubnet(neutronSubnet);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetId), subnet, true);
        DataStoreHelper.submitToDs(rwTx);
        rwTx = dataProvider.newReadWriteTransaction();

        L2FloodDomainId l2FdId = new L2FloodDomainId(subnet.getParent().getValue());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);

        if (isExternalNetwork(subnet.getParent(), rwTx)) {
            LOG.trace("neutronSubnetCreated - adding L3 Endpoint");
            IpAddress defaultGateway = Utils.createIpAddress(neutronSubnet.getGatewayIP());
            //Create L3Endpoint for defaultGateway and write to externalGateways to L3Endpoints in neutron-gbp datastore
            NetworkDomainId containment = new NetworkDomainId(neutronSubnet.getID());
            NeutronPortAware.addL3EndpointForExternalGateway(tenantId, fwCtx.getL3Context().getId(), defaultGateway, containment ,rwTx);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    private Subnet createSubnet(NeutronSubnet neutronSubnet) {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(neutronSubnet.getID()));
        subnetBuilder.setParent(new ContextId(neutronSubnet.getNetworkUUID()));
        if (!Strings.isNullOrEmpty(neutronSubnet.getName())) {
            subnetBuilder.setName(new Name(neutronSubnet.getName()));
        }
        subnetBuilder.setIpPrefix(Utils.createIpPrefix(neutronSubnet.getCidr()));
        return subnetBuilder.build();
    }


    private boolean isExternalNetwork(ContextId context, ReadWriteTransaction rwTx) {
        L2FloodDomainId l2FdId = new L2FloodDomainId(context.getValue());
        LOG.trace("neutronSubnetCreated - Looking up L2FD: {}", l2FdId);
        InstanceIdentifier<ExternalNetworkByL2FloodDomain> iid = NeutronGbpIidFactory.externalNetworkByL2FloodDomain(l2FdId);
        Optional<ExternalNetworkByL2FloodDomain> external = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                iid, rwTx);
        if (external.isPresent()) {
            return true;
        }
        return false;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#canUpdateSubnet(org.opendaylight.neutron.spi.NeutronSubnet,
     *      org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
        LOG.trace("canUpdateSubnet - delta: {} original: {}", delta, original);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#neutronSubnetUpdated(org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public void neutronSubnetUpdated(NeutronSubnet subnet) {
        LOG.trace("neutronSubnetUpdated - {}", subnet);
        neutronSubnetCreated(subnet);
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#canDeleteSubnet(org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public int canDeleteSubnet(NeutronSubnet subnet) {
        LOG.trace("canDeleteSubnet - {}", subnet);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSubnetAware#neutronSubnetDeleted(org.opendaylight.neutron.spi.NeutronSubnet)
     */
    @Override
    public void neutronSubnetDeleted(NeutronSubnet neutronSubnet) {
        LOG.trace("neutronSubnetDeleted - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        SubnetId subnetId = new SubnetId(Utils.normalizeUuid(neutronSubnet.getID()));
        TenantId tenantId = new TenantId(Utils.normalizeUuid(neutronSubnet.getTenantID()));
        Optional<Subnet> potentialSubnet = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, subnetId), rwTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
            rwTx.cancel();
            return;
        }
        DataStoreHelper.submitToDs(rwTx);
    }
}
