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
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils.ForwardingCtx;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class NeutronPortAware implements INeutronPortAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortAware.class);
    private static final String DEVICE_OWNER_DHCP = "network:dhcp";
    private static final String DEVICE_OWNER_ROUTER_IFACE = "network:router_interface";
    private static final int DHCP_CLIENT_PORT = 68;
    private static final int DHCP_SERVER_PORT = 67;
    private final DataBroker dataProvider;
    private final EndpointService epService;

    public NeutronPortAware(DataBroker dataProvider, EndpointService epService) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epService = checkNotNull(epService);
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
            LOG.trace("Port is router interface - do nothing - NeutronRouterAware handles router iface");
            return;
        }
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));

        if (isDhcpPort(port)) {
            List<NeutronSecurityRule> dhcpSecRules = createDhcpSecRules(port, null, rwTx);
            if (dhcpSecRules == null) {
                rwTx.cancel();
                return;
            }

            for (NeutronSecurityRule dhcpSecRule : dhcpSecRules) {
                boolean isDhcpSecRuleAdded = NeutronSecurityRuleAware.addNeutronSecurityRule(dhcpSecRule, rwTx);
                if (!isDhcpSecRuleAdded) {
                    rwTx.cancel();
                    return;
                }
            }
        } else {
            List<NeutronSecurityGroup> secGroups = port.getSecurityGroups();
            if (secGroups != null) {
                for (NeutronSecurityGroup secGroup : secGroups) {
                    EndpointGroupId epgId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
                    Optional<EndpointGroup> potentialEpg = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                            IidFactory.endpointGroupIid(tenantId, epgId), rwTx);
                    if (!potentialEpg.isPresent()) {
                        boolean isSecGroupCreated = NeutronSecurityGroupAware.addNeutronSecurityGroup(secGroup, rwTx);
                        if (!isSecGroupCreated) {
                            rwTx.cancel();
                            return;
                        }
                        if (containsSecRuleWithRemoteSecGroup(secGroup)) {
                            List<NeutronSecurityRule> dhcpSecRules = createDhcpSecRules(port, epgId, rwTx);
                            if (dhcpSecRules == null) {
                                rwTx.cancel();
                                return;
                            }
                            List<NeutronSecurityRule> routerSecRules = NeutronRouterAware.createRouterSecRules(port, epgId, rwTx);
                            if (routerSecRules == null) {
                                rwTx.cancel();
                                return;
                            }
                        }
                    } else {
                        List<NeutronSecurityRule> secRules = secGroup.getSecurityRules();
                        if (secRules != null) {
                            for (NeutronSecurityRule secRule : secRules) {
                                NeutronSecurityRuleAware.addNeutronSecurityRule(secRule, rwTx);
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

        try {
            RegisterEndpointInput registerEpRpcInput = createRegisterEndpointInput(port, fwCtx);
            RpcResult<Void> rpcResult = epService.registerEndpoint(registerEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC registerEndpoint failed. Input of RPC: {}", registerEpRpcInput);
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

    private List<NeutronSecurityRule> createDhcpSecRules(NeutronPort port, EndpointGroupId consumerEpgId, ReadTransaction rTx) {
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        Neutron_IPs firstIp = getFirstIp(port.getFixedIPs());
        if (firstIp == null) {
            LOG.warn("Illegal state - DHCP port does not have an IP address.");
            return null;
        }
        SubnetId dhcpSubnetId = new SubnetId(firstIp.getSubnetUUID());
        Optional<Subnet> potentialSubnet = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, dhcpSubnetId), rTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - Subnet {} where is DHCP port does not exist.", dhcpSubnetId.getValue());
            return null;
        }
        IpPrefix ipSubnet = potentialSubnet.get().getIpPrefix();
        NeutronSecurityRule dhcpRuleEgress = createDhcpSecRule(port.getID(), tenantId, ipSubnet, consumerEpgId, true);
        NeutronSecurityRule dhcpRuleIngress = createDhcpSecRule(port.getID(), tenantId, ipSubnet, consumerEpgId, false);
        return ImmutableList.of(dhcpRuleEgress, dhcpRuleIngress);
    }

    private NeutronSecurityRule createDhcpSecRule(String ruleUuid, TenantId tenantId, IpPrefix ipSubnet, EndpointGroupId consumerEpgId,
            boolean isEgress) {
        NeutronSecurityRule dhcpSecRule = new NeutronSecurityRule();
        dhcpSecRule.setSecurityRuleGroupID(MappingUtils.EPG_DHCP_ID.getValue());
        dhcpSecRule.setSecurityRuleTenantID(tenantId.getValue());
        dhcpSecRule.setSecurityRuleRemoteIpPrefix(Utils.getStringIpPrefix(ipSubnet));
        if (consumerEpgId != null) {
            dhcpSecRule.setSecurityRemoteGroupID(consumerEpgId.getValue());
        }
        if (isEgress) {
            dhcpSecRule.setSecurityRuleUUID(NeutronUtils.EGRESS + "__" + ruleUuid);
            dhcpSecRule.setSecurityRuleDirection(NeutronUtils.EGRESS);
            dhcpSecRule.setSecurityRulePortMin(DHCP_CLIENT_PORT);
            dhcpSecRule.setSecurityRulePortMax(DHCP_CLIENT_PORT);
        } else {
            dhcpSecRule.setSecurityRuleUUID(NeutronUtils.INGRESS + "__" + ruleUuid);
            dhcpSecRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
            dhcpSecRule.setSecurityRulePortMin(DHCP_SERVER_PORT);
            dhcpSecRule.setSecurityRulePortMax(DHCP_SERVER_PORT);
        }
        dhcpSecRule.setSecurityRuleProtocol(NeutronUtils.UDP);
        if (ipSubnet.getIpv4Prefix() != null) {
            dhcpSecRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        } else {
            dhcpSecRule.setSecurityRuleEthertype(NeutronUtils.IPv6);
        }
        return dhcpSecRule;
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
            LOG.trace("Port is router interface - do nothing - NeutronRouterAware handles router iface");
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
            LOG.trace("Port is router interface - do nothing - NeutronRouterAware handles router iface");
            return;
        }
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(port.getTenantID()));
        L2FloodDomainId l2FdId = new L2FloodDomainId(port.getNetworkUUID());
        ForwardingCtx fwCtx = MappingUtils.createForwardingContext(tenantId, l2FdId, rTx);
        boolean isFwCtxValid = validateForwardingCtx(fwCtx);
        if (!isFwCtxValid) {
            rTx.close();
            return;
        }

        UnregisterEndpointInput unregisterEpRpcInput = createUnregisterEndpointInput(port, fwCtx);
        try {
            RpcResult<Void> rpcResult = epService.unregisterEndpoint(unregisterEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC unregisterEndpoint failed. Input of RPC: {}", unregisterEpRpcInput);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("addPort - RPC invocation failed.", e);
        } finally {
            rTx.close();
        }
    }

    private static RegisterEndpointInput createRegisterEndpointInput(NeutronPort port, ForwardingCtx fwCtx) {
        List<EndpointGroupId> epgIds = new ArrayList<>();
        // each EP has to be in EPG ANY, except dhcp and router
        if (isDhcpPort(port)) {
            epgIds.add(MappingUtils.EPG_DHCP_ID);
        } else if (isRouterInterfacePort(port)) {
            epgIds.add(MappingUtils.EPG_ROUTER_ID);
        } else if (!containsSecRuleWithRemoteSecGroup(port.getSecurityGroups())) {
            epgIds.add(MappingUtils.EPG_ANY_ID);
        }

        List<NeutronSecurityGroup> securityGroups = port.getSecurityGroups();
        if ((securityGroups == null || securityGroups.isEmpty())) {
            if (!isDhcpPort(port) && !isRouterInterfacePort(port)) {
                LOG.warn(
                        "Port {} does not contain any security group. The port should belong to 'default' security group at least.",
                        port.getPortUUID());
            }
        } else {
            for (NeutronSecurityGroup secGrp : securityGroups) {
                epgIds.add(new EndpointGroupId(secGrp.getSecurityGroupUUID()));
            }
        }
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder().setL2Context(
                fwCtx.getL2BridgeDomain().getId())
            .setMacAddress(new MacAddress(port.getMacAddress()))
            .setTenant(new TenantId(Utils.normalizeUuid(port.getTenantID())))
            .setEndpointGroups(epgIds)
            .addAugmentation(OfOverlayContextInput.class,
                    new OfOverlayContextInputBuilder().setPortName(createTapPortName(port)).build())
            .setTimestamp(System.currentTimeMillis());
        List<Neutron_IPs> fixedIPs = port.getFixedIPs();
        // TODO Li msunal this getting of just first IP has to be rewrite when OFOverlay renderer
        // will support l3-endpoints. Then we will register L2 and L3 endpoints separately.
        Neutron_IPs firstIp = getFirstIp(fixedIPs);
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

    private static boolean containsSecRuleWithRemoteSecGroup(List<NeutronSecurityGroup> secGroups) {
        if (secGroups == null) {
            return false;
        }
        for (NeutronSecurityGroup secGroup : secGroups) {
            boolean containsSecRuleWithRemoteSecGroup = containsSecRuleWithRemoteSecGroup(secGroup);
            if (containsSecRuleWithRemoteSecGroup) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSecRuleWithRemoteSecGroup(NeutronSecurityGroup secGroup) {
        List<NeutronSecurityRule> secRules = secGroup.getSecurityRules();
        if (secRules == null) {
            return false;
        }
        for (NeutronSecurityRule secRule : secRules) {
            if (!Strings.isNullOrEmpty(secRule.getSecurityRemoteGroupID())) {
                return true;
            }
        }
        return false;
    }

    private static Name createTapPortName(NeutronPort port) {
        return new Name("tap" + port.getID().substring(0, 11));
    }

    private static Neutron_IPs getFirstIp(List<Neutron_IPs> fixedIPs) {
        if (fixedIPs == null || fixedIPs.isEmpty()) {
            return null;
        }
        Neutron_IPs neutron_Ip = fixedIPs.get(0);
        if (fixedIPs.size() > 1) {
            LOG.warn("Neutron mapper does not support multiple IPs on the same port. Only first IP is selected {}",
                    neutron_Ip);
        }
        return neutron_Ip;
    }

    private static boolean isDhcpPort(NeutronPort port) {
        return DEVICE_OWNER_DHCP.equals(port.getDeviceOwner());
    }

    private static boolean isRouterInterfacePort(NeutronPort port) {
        return DEVICE_OWNER_ROUTER_IFACE.equals(port.getDeviceOwner());
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
