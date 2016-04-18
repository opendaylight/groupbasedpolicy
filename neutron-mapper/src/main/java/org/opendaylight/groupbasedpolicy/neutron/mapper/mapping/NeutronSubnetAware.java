/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils.ForwardingCtx;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.NeutronSubnetIPAllocationPool;
import org.opendaylight.neutron.spi.NeutronSubnet_HostRoute;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.Dhcpv6Base;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.Dhcpv6Off;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.Dhcpv6Slaac;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.Dhcpv6Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.Dhcpv6Stateless;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.IpVersionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.IpVersionV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.IpVersionV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnet.attributes.AllocationPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnet.attributes.HostRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;

public class NeutronSubnetAware implements MappingProcessor<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet, NeutronSubnet> {

    private final static Logger LOG = LoggerFactory.getLogger(NeutronSubnetAware.class);
    private final DataBroker dataProvider;
    private final NeutronNetworkDao networkDao;

    public NeutronSubnetAware(DataBroker dataProvider, NeutronNetworkDao networkDao) {
        this.dataProvider = checkNotNull(dataProvider);
        this.networkDao = checkNotNull(networkDao);
    }

    // copied from Neutron's NeutronSubnetInterface
    static final ImmutableBiMap<Class<? extends IpVersionBase>,Integer> IPV_MAP
    = new ImmutableBiMap.Builder<Class<? extends IpVersionBase>,Integer>()
    .put(IpVersionV4.class, 4)
    .put(IpVersionV6.class, 6)
    .build();

    // copied from Neutron's NeutronSubnetInterface
    private static final ImmutableBiMap<Class<? extends Dhcpv6Base>,String> DHCPV6_MAP
    = new ImmutableBiMap.Builder<Class<? extends Dhcpv6Base>,String>()
    .put(Dhcpv6Off.class,"off")
    .put(Dhcpv6Stateful.class,"dhcpv6-stateful")
    .put(Dhcpv6Slaac.class,"slaac")
    .put(Dhcpv6Stateless.class,"dhcpv6-stateless")
    .build();

    @Override
    public NeutronSubnet convertToNeutron(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet subnet) {
        return toNeutron(subnet);
    }

    @SuppressWarnings("deprecation")
    static NeutronSubnet toNeutron(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet subnet) {
        NeutronSubnet result = new NeutronSubnet();
        result.setName(subnet.getName());
        result.setTenantID(subnet.getTenantId());
        result.setNetworkUUID(subnet.getNetworkId().getValue());
        result.setIpVersion(IPV_MAP.get(subnet.getIpVersion()));
        result.setCidr(subnet.getCidr());
        if (subnet.getGatewayIp() != null) {
            result.setGatewayIP(String.valueOf(subnet.getGatewayIp().getValue()));
        }
        if (subnet.getIpv6RaMode() != null) {
            result.setIpV6RaMode(DHCPV6_MAP.get(subnet.getIpv6RaMode()));
        }
        if (subnet.getIpv6AddressMode() != null) {
            result.setIpV6AddressMode(DHCPV6_MAP.get(subnet.getIpv6AddressMode()));
        }
        result.setEnableDHCP(subnet.isEnableDhcp());
        if (subnet.getAllocationPools() != null) {
            List<NeutronSubnetIPAllocationPool> allocationPools = new ArrayList<NeutronSubnetIPAllocationPool>();
            for (AllocationPools allocationPool : subnet.getAllocationPools()) {
                NeutronSubnetIPAllocationPool pool = new NeutronSubnetIPAllocationPool();
                pool.setPoolStart(allocationPool.getStart());
                pool.setPoolEnd(allocationPool.getEnd());
                allocationPools.add(pool);
            }
            result.setAllocationPools(allocationPools);
        }
        if (subnet.getDnsNameservers() != null) {
            List<String> dnsNameServers = new ArrayList<String>();
            for (IpAddress dnsNameServer : subnet.getDnsNameservers()) {
                dnsNameServers.add(String.valueOf(dnsNameServer.getValue()));
            }
            result.setDnsNameservers(dnsNameServers);
        }
        if (subnet.getHostRoutes() != null) {
            List<NeutronSubnet_HostRoute> hostRoutes = new ArrayList<NeutronSubnet_HostRoute>();
            for (HostRoutes hostRoute : subnet.getHostRoutes()) {
                NeutronSubnet_HostRoute nsHostRoute = new NeutronSubnet_HostRoute();
                nsHostRoute.setDestination(String.valueOf(hostRoute.getDestination().getValue()));
                nsHostRoute.setNextHop(String.valueOf(hostRoute.getNexthop().getValue()));
                hostRoutes.add(nsHostRoute);
            }
            result.setHostRoutes(hostRoutes);
        }
        result.setID(subnet.getUuid().getValue());
        return result;
    }

    @Override
    public int canCreate(NeutronSubnet subnet) {
        LOG.trace("canCreate subnet - {}", subnet);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void created(NeutronSubnet neutronSubnet) {
        LOG.trace("created subnet - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        SubnetId subnetId = new SubnetId(Utils.normalizeUuid(neutronSubnet.getID()));
        TenantId tenantId = new TenantId(Utils.normalizeUuid(neutronSubnet.getTenantID()));
        Subnet subnet = createSubnet(neutronSubnet);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetId), subnet, true);
        DataStoreHelper.submitToDs(rwTx);

        rwTx = dataProvider.newReadWriteTransaction();
        if (networkDao.isExternalNetwork(neutronSubnet.getNetworkUUID())) {
            LOG.trace("created - adding L3 Endpoint");
            L2FloodDomainId l2FdId = new L2FloodDomainId(subnet.getParent().getValue());
            ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);
            IpAddress defaultGateway = Utils.createIpAddress(neutronSubnet.getGatewayIP());
            //Create L3Endpoint for defaultGateway
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

    @Override
    public int canUpdate(NeutronSubnet delta, NeutronSubnet original) {
        LOG.trace("canUpdate subnet - delta: {} original: {}", delta, original);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void updated(NeutronSubnet subnet) {
        LOG.trace("updated subnet - {}", subnet);
        created(subnet);
    }

    @Override
    public int canDelete(NeutronSubnet subnet) {
        LOG.trace("canDelete subnet - {}", subnet);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void deleted(NeutronSubnet neutronSubnet) {
        LOG.trace("deleted subnet - {}", neutronSubnet);
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
