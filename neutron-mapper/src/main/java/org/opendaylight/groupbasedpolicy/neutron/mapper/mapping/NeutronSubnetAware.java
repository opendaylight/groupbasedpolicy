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
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class NeutronSubnetAware implements
        NeutronAware<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> {

    private final static Logger LOG = LoggerFactory.getLogger(NeutronSubnetAware.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> SUBNET_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class)
                .child(Subnets.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet.class)
                .build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;

    public NeutronSubnetAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet,
            Neutron neutron) {
        LOG.trace("created subnet - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(neutronSubnet.getTenantId().getValue());

        Optional<Network> potentialNetwork =
                NetworkUtils.findNetwork(neutronSubnet.getNetworkId(), neutron.getNetworks());
        if (!potentialNetwork.isPresent()) {
            LOG.warn("Illegal state - network {} does not exist for subnet {}.",
                    neutronSubnet.getNetworkId().getValue(), neutronSubnet);
            rwTx.cancel();
            return;
        }

        Network networkOfSubnet = potentialNetwork.get();
        Subnet subnet = null;
        if (NetworkUtils.isProviderPhysicalNetwork(networkOfSubnet)) {
            // add virtual router IP only in case it is provider physical network
            subnet = createSubnet(neutronSubnet, neutronSubnet.getGatewayIp());
            IpAddress gatewayIp = neutronSubnet.getGatewayIp();
            boolean registeredDefaultRoute = epRegistrator.registerExternalL3PrefixEndpoint(MappingUtils.DEFAULT_ROUTE,
                    new L3ContextId(neutronSubnet.getNetworkId().getValue()), gatewayIp, tenantId);
            if (!registeredDefaultRoute) {
                LOG.warn("Could not add EndpointL3Prefix as default route. Subnet within provider physical network {}",
                        neutronSubnet);
                rwTx.cancel();
                return;
            }
        } else if (NetworkUtils.isRouterExternal(networkOfSubnet)) {
            // virtual router IP is not set and it will be set when router gateway port is set
            subnet = createSubnet(neutronSubnet, null);
        } else {
            // virtual router IP is not set and it will be set when router port is attached to
            // network
            subnet = createSubnet(neutronSubnet, null);
        }
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet, true);

        DataStoreHelper.submitToDs(rwTx);
    }

    public static Subnet createSubnet(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet,
            IpAddress virtualRouterIp) {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(neutronSubnet.getUuid().getValue()));
        subnetBuilder.setParent(new ContextId(neutronSubnet.getNetworkId().getValue()));
        if (!Strings.isNullOrEmpty(neutronSubnet.getName())) {
            try {
                subnetBuilder.setName(new Name(neutronSubnet.getName()));
            } catch (Exception e) {
                LOG.info("Name '{}' of Neutron Subnet '{}' is ignored.", neutronSubnet.getName(),
                        neutronSubnet.getUuid().getValue());
                LOG.debug("Name exception", e);
            }
        }
        subnetBuilder.setIpPrefix(Utils.createIpPrefix(neutronSubnet.getCidr()));
        subnetBuilder.setVirtualRouterIp(virtualRouterIp);
        return subnetBuilder.build();
    }

    @Override
    public void onUpdated(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet oldItem,
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet newItem,
            Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated subnet - {}", newItem);
        onCreated(newItem, newNeutron);
    }

    @Override
    public void onDeleted(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet,
            Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("deleted subnet - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        SubnetId subnetId = new SubnetId(neutronSubnet.getUuid().getValue());
        TenantId tenantId = new TenantId(neutronSubnet.getTenantId().getValue());
        Optional<Subnet> potentialSubnet = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, subnetId), rwTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
            rwTx.cancel();
            return;
        }

        // TODO remove default gateway EP in case when subnet is in provider physical network

        DataStoreHelper.submitToDs(rwTx);
    }

}
