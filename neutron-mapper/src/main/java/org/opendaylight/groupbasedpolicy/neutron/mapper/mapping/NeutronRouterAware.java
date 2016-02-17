/*
 * Copyright (c) 2015 Intel, Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils.ForwardingCtx;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronMapperIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronCRUDInterfaces;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronRoute;
import org.opendaylight.neutron.spi.NeutronRouter;
import org.opendaylight.neutron.spi.NeutronRouter_Interface;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class NeutronRouterAware implements INeutronRouterAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronRouterAware.class);
    private static final String DEFAULT_ROUTE = "0.0.0.0/0";
    private final DataBroker dataProvider;
    private final  EndpointService epService;

    public NeutronRouterAware(DataBroker dataProvider, EndpointService epService) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epService = checkNotNull(epService);
    }

    @Override
    public int canCreateRouter(NeutronRouter router) {
        LOG.trace("canCreateRouter - {}", router);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void neutronRouterCreated(NeutronRouter router) {
        LOG.trace("neutronRouterCreated - {}", router);
        // TODO Li msunal external gateway
    }

    @Override
    public int canUpdateRouter(NeutronRouter delta, NeutronRouter original) {
        LOG.trace("canUpdateRouter - delta: {} original: {}", delta, original);
        // TODO Li msunal external gateway
        return StatusCode.OK;
    }

    @Override
    public void neutronRouterUpdated(NeutronRouter router) {
        LOG.trace("neutronRouterUpdated - {}", router);
        if (router.getExternalGatewayInfo() == null || router.getExternalGatewayInfo().getExternalFixedIPs() == null) {
            LOG.trace("neutronRouterUpdated - not an external Gateway");
            return;
        }

        NeutronCRUDInterfaces neutronCRUDInterface = new NeutronCRUDInterfaces().fetchINeutronPortCRUD(this);
        INeutronPortCRUD portInterface = neutronCRUDInterface.getPortInterface();
        if (portInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronPortCRUD.class.getName());
            return;
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(router.getTenantID()));
        L3ContextId l3ContextIdFromRouterId = new L3ContextId(router.getID());
        InstanceIdentifier<L3Context> l3ContextIidForRouterId = IidFactory.l3ContextIid(tenantId,
                l3ContextIdFromRouterId);
        Optional<L3Context> potentialL3ContextForRouter = DataStoreHelper.readFromDs(
                LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, rwTx);
        L3Context l3Context = null;
        if (potentialL3ContextForRouter.isPresent()) {
            l3Context = potentialL3ContextForRouter.get();
        } else { // add L3 context if missing
            l3Context = createL3ContextFromRouter(router);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, l3Context);
        }

        neutronCRUDInterface = neutronCRUDInterface.fetchINeutronSubnetCRUD(this);
        INeutronSubnetCRUD subnetInterface = neutronCRUDInterface.getSubnetInterface();
        if (subnetInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronSubnetCRUD.class.getName());
            return;
        }
        NeutronSubnet defaultSubnet = subnetInterface.getSubnet(router.getExternalGatewayInfo()
            .getExternalFixedIPs()
            .get(0)
            .getSubnetUUID());
        IpAddress defaultGateway = null;
        if (defaultSubnet != null) {
            defaultGateway = Utils.createIpAddress(defaultSubnet.getGatewayIP());
            //Create L3Endpoint for defaultGateway and write to externalGateways to L3Endpoints in neutron-gbp datastore
            NetworkDomainId containment = new NetworkDomainId(defaultSubnet.getID());
            NeutronPortAware.addL3EndpointForExternalGateway(tenantId, l3Context.getId(), defaultGateway, containment ,rwTx);
        }
        // Create L3Prefix Endpoints for all routes
        if (router.getRoutes().isEmpty()) {
            NeutronRoute defaultRoute = new NeutronRoute();
            defaultRoute.setDestination(DEFAULT_ROUTE);
            defaultRoute.setNextHop(Utils.getStringIpAddress(defaultGateway));
            router.setRoutes(ImmutableList.of(defaultRoute));

        }
        if (defaultGateway != null) {
            for (NeutronRoute route : router.getRoutes()) {
                IpPrefix ipPrefix = Utils.createIpPrefix(route.getDestination());
                boolean addedL3Prefix = NeutronPortAware.addL3PrefixEndpoint(l3ContextIdFromRouterId, ipPrefix,
                        defaultGateway, tenantId, epService);
                if (!addedL3Prefix) {
                    LOG.warn("Could not add EndpointL3Prefix for Neutron route {} for router {}", route, router.getID());
                    rwTx.cancel();
                    return;
                }
            }
        }
        for (Neutron_IPs externalFixedIp : router.getExternalGatewayInfo().getExternalFixedIPs()) {
            NeutronPort routerPort = portInterface.getPort(router.getGatewayPortId());
            IpAddress ipAddress = Utils.createIpAddress(routerPort.getFixedIPs().get(0).getIpAddress());
            // External subnet associated with gateway port should use the gateway IP not router IP.
            NeutronSubnet neutronSubnet = subnetInterface.getSubnet(externalFixedIp.getSubnetUUID());
            ipAddress = Utils.createIpAddress(neutronSubnet.getGatewayIP());
            SubnetId subnetId = new SubnetId(externalFixedIp.getSubnetUUID());
            Subnet subnet = resolveSubnetWithVirtualRouterIp(tenantId, subnetId, ipAddress, rwTx);
            if (subnet == null) {
                rwTx.cancel();
                return;
            }
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet);

            if (Strings.isNullOrEmpty(routerPort.getTenantID())) {
                routerPort.setTenantID(router.getTenantID());
            }

            boolean isSuccessful = setNewL3ContextToEpsFromSubnet(tenantId, l3Context, subnet, rwTx);
            if (!isSuccessful) {
                rwTx.cancel();
                return;
            }
        }

        DataStoreHelper.submitToDs(rwTx);
    }

    @Override
    public int canDeleteRouter(NeutronRouter router) {
        LOG.trace("canDeleteRouter - {}", router);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void neutronRouterDeleted(NeutronRouter router) {
        LOG.trace("neutronRouterDeleted - {}", router);
    }

    @Override
    public int canAttachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        LOG.trace("canAttachInterface - router: {} interface: {}", router, routerInterface);
        try (ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction()) {
            L3ContextId l3ContextIdFromRouterId = new L3ContextId(router.getID());
            TenantId tenantId = new TenantId(Utils.normalizeUuid(router.getTenantID()));
            SubnetId subnetId = new SubnetId(routerInterface.getSubnetUUID());
            Optional<Subnet> potentialSubnet = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.subnetIid(tenantId, subnetId), rTx);
            if (!potentialSubnet.isPresent()) {
                LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
                return StatusCode.NOT_FOUND;
            }
            Subnet subnet = potentialSubnet.get();
            L2FloodDomainId l2FdId = new L2FloodDomainId(subnet.getParent().getValue());
            ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rTx);
            if (fwCtx.getL3Context() != null && fwCtx.getL3Context().getId().equals(l3ContextIdFromRouterId)) {
                // TODO Be msunal
                LOG.warn("Illegal state - Neutron mapper does not support multiple router interfaces in the same subnet yet.");
                return StatusCode.FORBIDDEN;
            }
            return StatusCode.OK;
        }
    }

    @Override
    public void neutronRouterInterfaceAttached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        LOG.trace("neutronRouterInterfaceAttached - router: {} interface: {}", router, routerInterface);
        NeutronCRUDInterfaces neutronCRUDInterface = new NeutronCRUDInterfaces().fetchINeutronPortCRUD(this);
        INeutronPortCRUD portInterface = neutronCRUDInterface.getPortInterface();
        if (portInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronPortCRUD.class.getName());
            return;
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(router.getTenantID()));
        L3ContextId l3ContextIdFromRouterId = new L3ContextId(router.getID());
        InstanceIdentifier<L3Context> l3ContextIidForRouterId = IidFactory.l3ContextIid(tenantId,
                l3ContextIdFromRouterId);
        Optional<L3Context> potentialL3ContextForRouter = DataStoreHelper.readFromDs(
                LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, rwTx);
        L3Context l3Context = null;
        if (potentialL3ContextForRouter.isPresent()) {
            l3Context = potentialL3ContextForRouter.get();
        } else { // add L3 context if missing
            l3Context = createL3ContextFromRouter(router);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, l3ContextIidForRouterId, l3Context);
        }

        // Based on Neutron Northbound - Port representing router interface
        // contains exactly on fixed IP
        NeutronPort routerPort = portInterface.getPort(routerInterface.getPortUUID());
        SubnetId subnetId = new SubnetId(routerInterface.getSubnetUUID());
        IpAddress ipAddress = Utils.createIpAddress(routerPort.getFixedIPs().get(0).getIpAddress());
        Subnet subnet = resolveSubnetWithVirtualRouterIp(tenantId, subnetId, ipAddress, rwTx);
        if (subnet == null) {
            rwTx.cancel();
            return;
        }
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet);

        boolean isSuccessful = setNewL3ContextToEpsFromSubnet(tenantId, l3Context, subnet, rwTx);
        if (!isSuccessful) {
            rwTx.cancel();
            return;
        }

        DataStoreHelper.submitToDs(rwTx);
    }

    private static @Nonnull L3Context createL3ContextFromRouter(NeutronRouter router) {
        Name l3ContextName = null;
        if (!Strings.isNullOrEmpty(router.getName())) {
            l3ContextName = new Name(router.getName());
        }
        return new L3ContextBuilder().setId(new L3ContextId(router.getID()))
            .setName(l3ContextName)
            .setDescription(new Description(MappingUtils.NEUTRON_ROUTER + router.getID()))
            .build();
    }

    private @Nullable Subnet resolveSubnetWithVirtualRouterIp(TenantId tenantId, SubnetId subnetId,
            IpAddress ipAddress, ReadTransaction rTx) {
        Optional<Subnet> potentialSubnet = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, subnetId), rTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
            return null;
        }

        // TODO: Li alagalah: Add gateways and prefixes instead of
        // VirtualRouterID
        return new SubnetBuilder(potentialSubnet.get()).setVirtualRouterIp(ipAddress).build();
    }

    public boolean setNewL3ContextToEpsFromSubnet(TenantId tenantId, L3Context l3Context, Subnet subnet,
            ReadWriteTransaction rwTx) {
        if (subnet.getParent() == null) {
            LOG.warn("Illegal state - subnet {} does not have a parent.", subnet.getId().getValue());
            return false;
        }

        L2FloodDomainId l2FdId = new L2FloodDomainId(subnet.getParent().getValue());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);
        if (fwCtx.getL2BridgeDomain() == null) {
            LOG.warn("Illegal state - l2-flood-domain {} does not have a parent.", l2FdId.getValue());
            return false;
        }

        L2BridgeDomain l2BridgeDomain = new L2BridgeDomainBuilder(fwCtx.getL2BridgeDomain()).setParent(
                l3Context.getId()).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BridgeDomain.getId()),
                l2BridgeDomain);

        NeutronCRUDInterfaces neutronCRUDInterface = new NeutronCRUDInterfaces().fetchINeutronSubnetCRUD(this);
        INeutronSubnetCRUD subnetInterface = neutronCRUDInterface.getSubnetInterface();
        if (subnetInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronSubnetCRUD.class.getName());
            return false;
        }

        List<L3> l3Eps = new ArrayList<>();
        L3ContextId oldL3ContextId = fwCtx.getL3Context().getId();
        NeutronSubnet neutronSubnet = subnetInterface.getSubnet(subnet.getId().getValue());
        List<NeutronPort> portsInNeutronSubnet = neutronSubnet.getPortsInSubnet();
        for (NeutronPort port : portsInNeutronSubnet) {
            if (NeutronPortAware.isRouterGatewayPort(port) || NeutronPortAware.isRouterInterfacePort(port)) {
                continue;
            }
            boolean isPortAdded = NeutronPortAware.addNeutronPort(port, rwTx, epService);
            if (!isPortAdded) {
                return false;
            }
            // TODO Li msunal this has to be rewrite when OFOverlay renderer
            // will support l3-endpoints.
            Neutron_IPs firstIp = MappingUtils.getFirstIp(port.getFixedIPs());
            if (firstIp != null) {
                l3Eps.add(new L3Builder().setL3Context(oldL3ContextId)
                    .setIpAddress(Utils.createIpAddress(firstIp.getIpAddress()))
                    .build());
            }
        }
        if (neutronSubnet.getGatewayIP() != null) {
            l3Eps.add(new L3Builder().setL3Context(oldL3ContextId)
                    .setIpAddress(Utils.createIpAddress(neutronSubnet.getGatewayIP()))
                    .build());
        }

        if (!l3Eps.isEmpty()) {
            epService.unregisterEndpoint(new UnregisterEndpointInputBuilder().setL3(l3Eps).build());
        }
        return true;
    }

    @Override
    public int canDetachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        LOG.trace("canDetachInterface - router: {} interface: {}", router, routerInterface);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void neutronRouterInterfaceDetached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        LOG.trace("neutronRouterInterfaceDetached - router: {} interface: {}", router, routerInterface);
        NeutronCRUDInterfaces neutronCRUDInterface = new NeutronCRUDInterfaces().fetchINeutronSubnetCRUD(this);
        INeutronSubnetCRUD subnetInterface = neutronCRUDInterface.getSubnetInterface();
        if (subnetInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronSubnetCRUD.class.getName());
            return;
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(router.getTenantID()));
        L3ContextId l3ContextId = new L3ContextId(router.getID());
        SubnetId subnetId = new SubnetId(routerInterface.getSubnetUUID());
        InstanceIdentifier<L3Context> l3ContextIid = IidFactory.l3ContextIid(tenantId, l3ContextId);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l3ContextIid(tenantId, l3ContextId), rwTx);

        Optional<Subnet> potentialSubnet = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, subnetId), rwTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
            rwTx.cancel();
            return;
        }

        Subnet subnet = new SubnetBuilder(potentialSubnet.get()).setVirtualRouterIp(null).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetId), subnet);

        L2FloodDomainId l2FdId = new L2FloodDomainId(subnet.getParent().getValue());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);
        if (fwCtx.getL2BridgeDomain() == null) {
            LOG.warn("Illegal state - l2-flood-domain {} does not have a parent.", l2FdId.getValue());
            rwTx.cancel();
            return;
        }

        Optional<NetworkMapping> potentialNetworkMapping = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                NeutronMapperIidFactory.networkMappingIid(l2FdId), rwTx);
        if (!potentialNetworkMapping.isPresent()) {
            LOG.warn("Illegal state - network-mapping {} does not exist.", l2FdId.getValue());
            rwTx.cancel();
            return;
        }

        L2BridgeDomain l2BridgeDomain = new L2BridgeDomainBuilder(fwCtx.getL2BridgeDomain()).setParent(
                potentialNetworkMapping.get().getL3ContextId()).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BridgeDomain.getId()),
                l2BridgeDomain);

        NeutronSubnet neutronSubnet = subnetInterface.getSubnet(subnetId.getValue());
        List<NeutronPort> portsInNeutronSubnet = neutronSubnet.getPortsInSubnet();
        for (NeutronPort port : portsInNeutronSubnet) {
            if (NeutronPortAware.isRouterGatewayPort(port) || NeutronPortAware.isRouterInterfacePort(port)) {
                continue;
            }
            boolean isPortAdded = NeutronPortAware.addNeutronPort(port, rwTx, epService);
            if (!isPortAdded) {
                rwTx.cancel();
                return;
            }
        }
    }

}
