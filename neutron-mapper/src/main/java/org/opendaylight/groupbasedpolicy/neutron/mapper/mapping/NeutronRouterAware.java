/*
 * Copyright (c) 2015 Intel, Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.PortUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class NeutronRouterAware implements NeutronAware<Router> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronRouterAware.class);
    public static final InstanceIdentifier<Router> ROUTER_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Routers.class).child(Router.class).build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;

    public NeutronRouterAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(Router router, Neutron neutron) {
        LOG.trace("created router - {}", router);
    }

    @Override
    public void onUpdated(Router oldRouter, Router newRouter, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated router - OLD: {}\nNEW: {}", oldRouter, newRouter);

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(newRouter.getTenantId().getValue());
        L3ContextId l3ContextIdFromRouterId = new L3ContextId(newRouter.getUuid().getValue());
        InstanceIdentifier<L3Context> l3ContextIidForRouterId =
                IidFactory.l3ContextIid(tenantId, l3ContextIdFromRouterId);
        Optional<L3Context> potentialL3ContextForRouter =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, rwTx);
        L3Context l3Context = null;
        if (potentialL3ContextForRouter.isPresent()) {
            l3Context = potentialL3ContextForRouter.get();
        } else { // add L3 context if missing
            l3Context = createL3ContextFromRouter(newRouter);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, l3Context);
        }

        if (newRouter.getGatewayPortId() != null && oldRouter.getGatewayPortId() == null) {
            // external network is attached to router
            Uuid gatewayPortId = newRouter.getGatewayPortId();
            Optional<Port> potentialGwPort = PortUtils.findPort(gatewayPortId, newNeutron.getPorts());
            if (!potentialGwPort.isPresent()) {
                LOG.warn("Illegal state - router gateway port {} does not exist for router {}.",
                        gatewayPortId.getValue(), newRouter);
                rwTx.cancel();
                return;
            }

            Port gwPort = potentialGwPort.get();
            List<FixedIps> fixedIpsFromGwPort = gwPort.getFixedIps();
            if (fixedIpsFromGwPort == null || fixedIpsFromGwPort.isEmpty()) {
                LOG.warn("Illegal state - router gateway port {} does not contain fixed IPs {}",
                        gatewayPortId.getValue(), gwPort);
                rwTx.cancel();
                return;
            }

            // router can have only one external network
            FixedIps ipWithSubnetFromGwPort = fixedIpsFromGwPort.get(0);
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> potentialSubnet = SubnetUtils.findSubnet(ipWithSubnetFromGwPort.getSubnetId(), newNeutron.getSubnets());
            if (!potentialSubnet.isPresent()) {
                LOG.warn("Illegal state - Subnet {} does not exist for router {}.",
                        ipWithSubnetFromGwPort.getSubnetId(), newRouter);
                rwTx.cancel();
                return;
            }
            IpAddress gatewayIp =  potentialSubnet.get().getGatewayIp();
            boolean registeredExternalGateway = epRegistrator.registerL3EndpointAsExternalGateway(tenantId, gatewayIp,
                    l3ContextIdFromRouterId, new NetworkDomainId(ipWithSubnetFromGwPort.getSubnetId().getValue()));
            if (!registeredExternalGateway) {
                LOG.warn("Could not add L3Prefix as gateway of default route. Gateway port {}", gwPort);
                rwTx.cancel();
                return;
            }
            EndpointL3Key epL3Key = new EndpointL3Key(gatewayIp, l3ContextIdFromRouterId);
            addNeutronExtGwGbpMapping(epL3Key, rwTx);

            boolean registeredDefaultRoute = epRegistrator.registerExternalL3PrefixEndpoint(MappingUtils.DEFAULT_ROUTE,
                    l3ContextIdFromRouterId, gatewayIp, tenantId);
            if (!registeredDefaultRoute) {
                LOG.warn("Could not add EndpointL3Prefix as default route. Gateway port {}", gwPort);
                rwTx.cancel();
                return;
            }
            Subnet subnetWithGw =
                    new SubnetBuilder().setId(new SubnetId(ipWithSubnetFromGwPort.getSubnetId().getValue()))
                        .setVirtualRouterIp(gatewayIp)
                .build();
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetWithGw.getId()),
                    subnetWithGw);
            L2BridgeDomainId l2BdId = new L2BridgeDomainId(potentialSubnet.get().getNetworkId().getValue());
            Optional<L2BridgeDomain> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
            if (!optBd.isPresent()) {
                LOG.warn(
                        "Could not read L2-Bridge-Domain {} Modifiaction of it's parent to L3-Context of router {} aborted.",
                        l2BdId, newRouter.getUuid());
                rwTx.cancel();
                return;
            }
            L2BridgeDomain l2BdWithGw = new L2BridgeDomainBuilder(optBd.get())
                .setParent(new L3ContextId(l3ContextIdFromRouterId.getValue()))
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId),
                    l2BdWithGw);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    private static @Nonnull L3Context createL3ContextFromRouter(Router router) {
        Name l3ContextName = null;
        if (!Strings.isNullOrEmpty(router.getName())) {
            l3ContextName = new Name(router.getName());
        }
        return new L3ContextBuilder().setId(new L3ContextId(router.getUuid().getValue()))
            .setName(l3ContextName)
            .setDescription(new Description(MappingUtils.NEUTRON_ROUTER + router.getUuid().getValue()))
            .build();
    }

    private static void addNeutronExtGwGbpMapping(EndpointL3Key epL3Key, ReadWriteTransaction rwTx) {
        ExternalGatewayAsL3Endpoint externalGatewayL3Endpoint =
                MappingFactory.createExternalGatewayByL3Endpoint(epL3Key);
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.externalGatewayAsL3Endpoint(epL3Key.getL3Context(), epL3Key.getIpAddress()),
                externalGatewayL3Endpoint, true);
    }

    @Override
    public void onDeleted(Router router, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("deleted router - {}", router);
    }

}
