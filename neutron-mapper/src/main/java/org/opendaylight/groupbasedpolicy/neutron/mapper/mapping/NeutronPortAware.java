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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkClient;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils.ForwardingCtx;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class NeutronPortAware implements INeutronPortAware {

    public static final Logger LOG = LoggerFactory.getLogger(NeutronPortAware.class);
    private static final String DEVICE_OWNER_DHCP = "network:dhcp";
    private static final String DEVICE_OWNER_ROUTER_IFACE = "network:router_interface";
    private static final String DEVICE_OWNER_ROUTER_GATEWAY = "network:router_gateway";
    private static final String DEVICE_OWNER_FLOATING_IP = "network:floatingip";
    private final DataBroker dataProvider;
    private final EndpointService epService;
    private final NeutronSecurityRuleAware secRuleAware;
    private final NeutronSecurityGroupAware secGrpAware;

    public NeutronPortAware(DataBroker dataProvider, EndpointService epService, NeutronSecurityRuleAware secRuleAware, NeutronSecurityGroupAware secGrpAware) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epService = checkNotNull(epService);
        this.secRuleAware = checkNotNull(secRuleAware);
        this.secGrpAware = secGrpAware;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#canCreatePort(org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public int canCreatePort(NeutronPort port) {
        LOG.trace("canCreatePort - {}", port);
        // TODO Li msunal this has to be rewrite when OFOverlay renderer will support l3-endpoints.
        List<Neutron_IPs> fixedIPs = port.getFixedIPs();
        if (fixedIPs != null && fixedIPs.size() > 1) {
            LOG.warn("Neutron mapper does not support multiple IPs on the same port.");
            return StatusCode.BAD_REQUEST;
        }
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#neutronPortCreated(org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public void neutronPortCreated(NeutronPort port) {
        LOG.trace("neutronPortCreated - {}", port);
        if (isRouterInterfacePort(port)) {
            LOG.trace("Port is router interface - {} does nothing. {} handles router iface.",
                    NeutronPortAware.class.getSimpleName(), NeutronRouterAware.class.getSimpleName());
            return;
        }
        if (isRouterGatewayPort(port)) {
            LOG.trace("Port is router gateway - {} does nothing. {} handles router iface.",
                    NeutronPortAware.class.getSimpleName(), NeutronRouterAware.class.getSimpleName());
            return;
        }
        if (isFloatingIpPort(port)) {
            LOG.trace("Port is floating ip - {} device id - {}", port.getID(), port.getDeviceID());
            return;
        }
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        if (isDhcpPort(port)) {
            LOG.trace("Port is DHCP port. - {}", port.getID());
            Neutron_IPs firstIp = MappingUtils.getFirstIp(port.getFixedIPs());
            if (firstIp == null) {
                LOG.warn("Illegal state - DHCP port does not have an IP address.");
                rwTx.cancel();
                return;
            }
        } else {
            // this is here b/c stable/kilo sends sec-groups only with port
            List<NeutronSecurityGroup> secGroups = port.getSecurityGroups();
            if (secGroups != null) {
                for (NeutronSecurityGroup secGroup : secGroups) {
                    EndpointGroupId epgId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
                    Optional<EndpointGroup> potentialEpg = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                            IidFactory.endpointGroupIid(tenantId, epgId), rwTx);
                    if (!potentialEpg.isPresent()) {
                        boolean isSecGroupCreated = secGrpAware.addNeutronSecurityGroup(secGroup, rwTx);
                        if (!isSecGroupCreated) {
                            rwTx.cancel();
                            return;
                        }
                    } else {
                        List<NeutronSecurityRule> secRules = secGroup.getSecurityRules();
                        if (secRules != null) {
                            for (NeutronSecurityRule secRule : secRules) {
                                secRuleAware.addNeutronSecurityRule(secRule, rwTx);
                            }
                        }
                    }
                }
            }
        }
        boolean isNeutronPortCreated = addNeutronPort(port, rwTx, epService);
        if (!isNeutronPortCreated) {
            rwTx.cancel();
            return;
        }

        DataStoreHelper.submitToDs(rwTx);
    }

    public static boolean addNeutronPort(NeutronPort port, ReadWriteTransaction rwTx, EndpointService epService) {
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        L2FloodDomainId l2FdId = new L2FloodDomainId(port.getNetworkUUID());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);
        boolean isFwCtxValid = validateForwardingCtx(fwCtx);
        if (!isFwCtxValid) {
            return false;
        }
        EndpointKey epKey = new EndpointKey(fwCtx.getL2BridgeDomain().getId(), new MacAddress(port.getMacAddress()));
        addNeutronGbpMapping(port, epKey, rwTx);
        try {
            RegisterEndpointInput registerEpRpcInput = createRegisterEndpointInput(port, fwCtx);
            RpcResult<Void> rpcResult = epService.registerEndpoint(registerEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC registerEndpoint failed. Input of RPC: {}", registerEpRpcInput);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("addNeutronPort failed. {}", port, e);
            return false;
        }
        return true;
    }

    private static void addNeutronGbpMapping(NeutronPort port, EndpointKey epKey, ReadWriteTransaction rwTx) {
        UniqueId portId = new UniqueId(port.getID());
        if (!isRouterInterfacePort(port) && !isRouterGatewayPort(port) && !isFloatingIpPort(port)) {
            LOG.trace("Adding Port-Endpoint mapping for port {} (device owner {}) and endpoint {}", port.getID(),
                    port.getDeviceOwner(), epKey);
            EndpointByPort endpointByPort = MappingFactory.createEndpointByPort(epKey, portId);
            rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronGbpIidFactory.endpointByPortIid(portId), endpointByPort,
                    true);
            PortByEndpoint portByEndpoint = MappingFactory.createPortByEndpoint(portId, epKey);
            rwTx.put(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), portByEndpoint,
                    true);
        }
    }

    public static boolean addL3EndpointForExternalGateway(TenantId tenantId, L3ContextId l3ContextId,
            IpAddress ipAddress, NetworkDomainId networkContainment, ReadWriteTransaction rwTx) {

        EndpointL3Key epL3Key = new EndpointL3Key(ipAddress, l3ContextId);
        addNeutronExtGwGbpMapping(epL3Key, rwTx);
        List<EndpointGroupId> epgIds = new ArrayList<>();
        epgIds.add(MappingUtils.EPG_EXTERNAL_ID);
        EndpointL3 epL3 = createL3Endpoint(tenantId, epL3Key, epgIds, networkContainment);
        InstanceIdentifier<EndpointL3> iid_l3 = IidFactory.l3EndpointIid(l3ContextId, ipAddress);
        rwTx.put(LogicalDatastoreType.OPERATIONAL, iid_l3, epL3, true);
        return true;
    }

    private static void addNeutronExtGwGbpMapping(EndpointL3Key epL3Key, ReadWriteTransaction rwTx) {
        ExternalGatewayAsL3Endpoint externalGatewayL3Endpoint = MappingFactory.createExternalGatewayByL3Endpoint(epL3Key);
        rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronGbpIidFactory.externalGatewayAsL3Endpoint(epL3Key.getL3Context(), epL3Key.getIpAddress()),
                externalGatewayL3Endpoint, true);
    }

    public static boolean addL3PrefixEndpoint(L3ContextId l3ContextId, IpPrefix ipPrefix, IpAddress ipAddress, TenantId tenantId,
            EndpointService epService) {
        EndpointL3PrefixKey epL3PrefixKey = new EndpointL3PrefixKey( ipPrefix, l3ContextId);
        EndpointL3Key epL3Key = null;
        List<EndpointL3Key> l3Gateways = new ArrayList<>();
        if (ipAddress != null) {
            epL3Key = new EndpointL3Key(ipAddress, l3ContextId);
            l3Gateways.add(epL3Key);
        }
        try {
            RegisterL3PrefixEndpointInput registerL3PrefixEpRpcInput = createRegisterL3PrefixEndpointInput(epL3PrefixKey, l3Gateways,tenantId);

            RpcResult<Void> rpcResult = epService.registerL3PrefixEndpoint(registerL3PrefixEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC registerEndpoint failed. Input of RPC: {}", registerL3PrefixEpRpcInput);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("addPort - RPC invocation failed.", e);
            return false;
        }
        return true;

    }

    private static boolean validateForwardingCtx(ForwardingCtx fwCtx) {
        if (fwCtx.getL2FloodDomain() == null) {
            LOG.warn("Illegal state - l2-flood-domain does not exist.");
            return false;
        }
        if (fwCtx.getL2BridgeDomain() == null) {
            LOG.warn("Illegal state - l2-bridge-domain does not exist.");
            return false;
        }
        if (fwCtx.getL3Context() == null) {
            LOG.warn("Illegal state - l3-context does not exist.");
            return false;
        }
        return true;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#canUpdatePort(org.opendaylight.neutron.spi.NeutronPort,
     *      org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public int canUpdatePort(NeutronPort delta, NeutronPort original) {
        LOG.trace("canUpdatePort - delta: {} original: {}", delta, original);
        if (delta.getFixedIPs() == null || delta.getFixedIPs().isEmpty()) {
            return StatusCode.OK;
        }
        // TODO Li msunal this has to be rewrite when OFOverlay renderer will support l3-endpoints.
        List<Neutron_IPs> fixedIPs = delta.getFixedIPs();
        if (fixedIPs != null && fixedIPs.size() > 1) {
            LOG.warn("Neutron mapper does not support multiple IPs on the same port.");
            return StatusCode.BAD_REQUEST;
        }
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#neutronPortUpdated(org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public void neutronPortUpdated(NeutronPort port) {
        LOG.trace("neutronPortUpdated - {}", port);
        if (isRouterInterfacePort(port)) {
            LOG.trace("Port is router interface - {} does nothing. {} handles router iface.",
                    NeutronPortAware.class.getSimpleName(), NeutronRouterAware.class.getSimpleName());
            return;
        }
        if (isRouterGatewayPort(port)) {
            LOG.trace("Port is router gateway - {}", port.getID());
            return;
        }
        if (isFloatingIpPort(port)) {
            LOG.trace("Port is floating ip - {}", port.getID());
            return;
        }
        if (Strings.isNullOrEmpty(port.getTenantID())) {
            LOG.trace("REMOVE ME: Tenant is null - {}", port.getID());
            return;
        }

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        MacAddress macAddress = new MacAddress(port.getMacAddress());
        L2FloodDomainId l2FdId = new L2FloodDomainId(port.getNetworkUUID());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rTx);
        boolean isFwCtxValid = validateForwardingCtx(fwCtx);
        if (!isFwCtxValid) {
            rTx.close();
            return;
        }

        Optional<Endpoint> potentionalEp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointIid(fwCtx.getL2BridgeDomain().getId(), macAddress), rTx);
        if (!potentionalEp.isPresent()) {
            LOG.warn("Illegal state - endpoint {} does not exist.", new EndpointKey(fwCtx.getL2BridgeDomain().getId(),
                    macAddress));
            rTx.close();
            return;
        }

        Endpoint ep = potentionalEp.get();
        if (isEpIpDifferentThanPortFixedIp(ep, port) || isEpgDifferentThanSecGrp(ep, port)) {
            UnregisterEndpointInput unregisterEpRpcInput = createUnregisterEndpointInput(ep);
            RegisterEndpointInput registerEpRpcInput = createRegisterEndpointInput(port, fwCtx);
            try {
                RpcResult<Void> rpcResult = epService.unregisterEndpoint(unregisterEpRpcInput).get();
                if (!rpcResult.isSuccessful()) {
                    LOG.warn("Illegal state - RPC unregisterEndpoint failed. Input of RPC: {}", unregisterEpRpcInput);
                    rTx.close();
                    return;
                }
                rpcResult = epService.registerEndpoint(registerEpRpcInput).get();
                if (!rpcResult.isSuccessful()) {
                    LOG.warn("Illegal state - RPC registerEndpoint failed. Input of RPC: {}", registerEpRpcInput);
                    rTx.close();
                    return;
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("addPort - RPC invocation failed.", e);
                rTx.close();
                return;
            }
        }
        rTx.close();
    }

    private boolean isEpIpDifferentThanPortFixedIp(Endpoint ep, NeutronPort port) {
        List<L3Address> l3Addresses = ep.getL3Address();
        List<Neutron_IPs> fixedIPs = port.getFixedIPs();
        if ((l3Addresses == null || l3Addresses.isEmpty()) && (fixedIPs == null || fixedIPs.isEmpty())) {
            return false;
        }
        if (l3Addresses != null && !l3Addresses.isEmpty() && fixedIPs != null && !fixedIPs.isEmpty()) {
            if (fixedIPs.get(0).getIpAddress().equals(Utils.getStringIpAddress(l3Addresses.get(0).getIpAddress()))) {
                return false;
            }
        }
        return true;
    }

    private boolean isEpgDifferentThanSecGrp(Endpoint ep, NeutronPort port) {
        List<EndpointGroupId> epgIds = ep.getEndpointGroups();
        List<NeutronSecurityGroup> secGroups = port.getSecurityGroups();
        if ((epgIds == null || epgIds.isEmpty()) && (secGroups == null || secGroups.isEmpty())) {
            return false;
        }
        if (epgIds != null && !epgIds.isEmpty() && secGroups != null && !secGroups.isEmpty()) {
            if (epgIds.size() != secGroups.size()) {
                return true;
            }
            Collection<EndpointGroupId> epgIdsFromSecGroups = Collections2.transform(secGroups,
                    new Function<NeutronSecurityGroup, EndpointGroupId>() {

                        @Override
                        public EndpointGroupId apply(NeutronSecurityGroup input) {
                            return new EndpointGroupId(input.getSecurityGroupUUID());
                        }
                    });
            // order independent equals
            Set<EndpointGroupId> one = new HashSet<>(epgIds);
            Set<EndpointGroupId> two = new HashSet<>(epgIdsFromSecGroups);
            if (one.equals(two)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#canDeletePort(org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public int canDeletePort(NeutronPort port) {
        LOG.trace("canDeletePort - {}", port);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronPortAware#neutronPortDeleted(org.opendaylight.neutron.spi.NeutronPort)
     */
    @Override
    public void neutronPortDeleted(NeutronPort port) {
        LOG.trace("neutronPortDeleted - {}", port);
        if (isRouterInterfacePort(port)) {
            LOG.trace("Port is router interface - {} does nothing. {} handles router iface.",
                    NeutronPortAware.class.getSimpleName(), NeutronRouterAware.class.getSimpleName());
            return;
        }
        if (isRouterGatewayPort(port)) {
            LOG.trace("Port is router gateway - {} does nothing. {} handles router iface.",
                    NeutronPortAware.class.getSimpleName(), NeutronRouterAware.class.getSimpleName());
            return;
        }
        if (isFloatingIpPort(port)) {
            LOG.trace("Port is floating ip - {} device id - {}", port.getID(), port.getDeviceID());
            return;
        }
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        L2FloodDomainId l2FdId = new L2FloodDomainId(port.getNetworkUUID());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rwTx);
        boolean isFwCtxValid = validateForwardingCtx(fwCtx);
        if (!isFwCtxValid) {
            rwTx.cancel();
            return;
        }

        UnregisterEndpointInput unregisterEpRpcInput = createUnregisterEndpointInput(port, fwCtx);
        boolean isEndpointUnregistered = false;
        try {
            isEndpointUnregistered = epService.unregisterEndpoint(unregisterEpRpcInput).get().isSuccessful();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("unregisterEndpoint - RPC invocation failed.", e);
        }
        if (isEndpointUnregistered) {
            EndpointKey epKey = new EndpointKey(fwCtx.getL2BridgeDomain().getId(), new MacAddress(port.getMacAddress()));
            deleteNeutronGbpMapping(port, epKey, rwTx);
            DataStoreHelper.submitToDs(rwTx);
        } else {
            LOG.warn("Illegal state - RPC unregisterEndpoint failed. Input of RPC: {}", unregisterEpRpcInput);
            rwTx.cancel();
        }
    }

    private static void deleteNeutronGbpMapping(NeutronPort port, EndpointKey epKey, ReadWriteTransaction rwTx) {
        UniqueId portId = new UniqueId(port.getID());
        if (!isRouterInterfacePort(port) && !isRouterGatewayPort(port) && !isFloatingIpPort(port)) {
            LOG.trace("Adding Port-Endpoint mapping for port {} (device owner {}) and endpoint {}", port.getID(),
                    port.getDeviceOwner(), epKey);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.endpointByPortIid(portId), rwTx);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), rwTx);
        }
    }

    private static RegisterL3PrefixEndpointInput createRegisterL3PrefixEndpointInput(EndpointL3PrefixKey key, List<EndpointL3Key> endpointL3Keys, TenantId tenantId) {
        List<EndpointL3Gateways> l3Gateways = new ArrayList<EndpointL3Gateways>();
        for (EndpointL3Key epL3Key : endpointL3Keys) {
            EndpointL3Gateways l3Gateway = new EndpointL3GatewaysBuilder().setIpAddress(epL3Key.getIpAddress())
                .setL3Context(epL3Key.getL3Context())
                .build();
            l3Gateways.add(l3Gateway);
        }
        RegisterL3PrefixEndpointInputBuilder inputBuilder = new RegisterL3PrefixEndpointInputBuilder()
                                                .setL3Context(key.getL3Context())
                                                .setIpPrefix(key.getIpPrefix())
                                                .setEndpointGroup(MappingUtils.EPG_EXTERNAL_ID)
                                                .setTenant(tenantId)
                                                .setEndpointL3Gateways(l3Gateways)
                                                .setTimestamp(System.currentTimeMillis());
        return inputBuilder.build();
    }

    private static EndpointL3 createL3Endpoint(TenantId tenantId, EndpointL3Key epL3Key,
            List<EndpointGroupId> epgIds, NetworkDomainId containment) {

        EndpointL3Builder epL3Builder = new EndpointL3Builder()
        .setTenant(tenantId)
        .setNetworkContainment(containment)
        .setIpAddress(epL3Key.getIpAddress())
        .setL3Context(epL3Key.getL3Context())
        .setEndpointGroups(epgIds)
        .setTimestamp(System.currentTimeMillis());

        return epL3Builder.build();
    }

    private static RegisterEndpointInput createRegisterEndpointInput(NeutronPort port, ForwardingCtx fwCtx) {
        List<EndpointGroupId> epgIds = new ArrayList<>();
        if (isDhcpPort(port)) {
            epgIds.add(NetworkService.EPG_ID);
        }

        List<NeutronSecurityGroup> securityGroups = port.getSecurityGroups();
        if ((securityGroups == null || securityGroups.isEmpty())) {
            if (!isDhcpPort(port)) {
                LOG.warn(
                        "Port {} does not contain any security group. The port should belong to 'default' security group at least.",
                        port.getID());
            }
        } else {
            for (NeutronSecurityGroup secGrp : securityGroups) {
                epgIds.add(new EndpointGroupId(secGrp.getSecurityGroupUUID()));
            }
            epgIds.add(NetworkClient.EPG_ID);
        }
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder().setL2Context(
                fwCtx.getL2BridgeDomain().getId())
            .setMacAddress(new MacAddress(port.getMacAddress()))
            .setTenant(new TenantId(Utils.normalizeUuid(port.getTenantID())))
            .setEndpointGroups(epgIds)
            .setTimestamp(System.currentTimeMillis());
        List<Neutron_IPs> fixedIPs = port.getFixedIPs();
        // TODO Li msunal this getting of just first IP has to be rewrite when OFOverlay renderer
        // will support l3-endpoints. Then we will register L2 and L3 endpoints separately.
        Neutron_IPs firstIp = MappingUtils.getFirstIp(fixedIPs);
        if (firstIp != null) {
            inputBuilder.setNetworkContainment(new SubnetId(firstIp.getSubnetUUID()));
            L3Address l3Address = new L3AddressBuilder().setIpAddress(Utils.createIpAddress(firstIp.getIpAddress()))
                .setL3Context(fwCtx.getL3Context().getId())
                .build();
            inputBuilder.setL3Address(ImmutableList.of(l3Address));
        }
        if (!Strings.isNullOrEmpty(port.getName())) {

        }
        return inputBuilder.build();
    }

    public static boolean isDhcpPort(NeutronPort port) {
        return DEVICE_OWNER_DHCP.equals(port.getDeviceOwner());
    }

    public static boolean isRouterInterfacePort(NeutronPort port) {
        return DEVICE_OWNER_ROUTER_IFACE.equals(port.getDeviceOwner());
    }

    public static boolean isRouterGatewayPort(NeutronPort port) {
        return DEVICE_OWNER_ROUTER_GATEWAY.equals(port.getDeviceOwner());
    }

    public static boolean isFloatingIpPort(NeutronPort port) {
        return DEVICE_OWNER_FLOATING_IP.equals(port.getDeviceOwner());
    }

    private UnregisterEndpointInput createUnregisterEndpointInput(Endpoint ep) {
        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        L2 l2Ep = new L2Builder().setL2Context(ep.getL2Context()).setMacAddress(ep.getMacAddress()).build();
        inputBuilder.setL2(ImmutableList.of(l2Ep));
        // TODO Li msunal this has to be rewrite when OFOverlay renderer will support l3-endpoints.
        // Endpoint probably will not have l3-addresses anymore, because L2 and L3 endpoints should
        // be registered separately.
        if (ep.getL3Address() != null && !ep.getL3Address().isEmpty()) {
            List<L3> l3Eps = new ArrayList<>();
            for (L3Address ip : ep.getL3Address()) {
                l3Eps.add(new L3Builder().setL3Context(ip.getL3Context()).setIpAddress(ip.getIpAddress()).build());
            }
            inputBuilder.setL3(l3Eps);
        }
        return inputBuilder.build();
    }

    private UnregisterEndpointInput createUnregisterEndpointInput(NeutronPort port, ForwardingCtx fwCtx) {
        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        L2 l2Ep = new L2Builder().setL2Context(fwCtx.getL2BridgeDomain().getId())
            .setMacAddress(new MacAddress(port.getMacAddress()))
            .build();
        inputBuilder.setL2(ImmutableList.of(l2Ep));
        // TODO Li msunal this has to be rewrite when OFOverlay renderer will support l3-endpoints.
        // Endpoint probably will not have l3-addresses anymore, because L2 and L3 endpoints should
        // be registered separately.
        if (port.getFixedIPs() != null && !port.getFixedIPs().isEmpty()) {
            inputBuilder.setL3(createL3s(port.getFixedIPs(), fwCtx.getL3Context().getId()));
        }
        return inputBuilder.build();
    }

    private List<L3> createL3s(List<Neutron_IPs> neutronIps, L3ContextId l3ContextId) {
        List<L3> l3s = new ArrayList<>();
        for (Neutron_IPs fixedIp : neutronIps) {
            String ip = fixedIp.getIpAddress();
            L3 l3 = new L3Builder().setIpAddress(Utils.createIpAddress(ip)).setL3Context(l3ContextId).build();
            l3s.add(l3);
        }
        return l3s;
    }

}
