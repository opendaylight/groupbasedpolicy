/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * <h1>Manage the table that maps the destination address to the next hop for the
 * path as well as applies any relevant routing transformations (table=3)</h1>
 *
 * Sync Ep flows, every endpoint pair creates L2 and L3 flow<br>
 * <ul><li>Flow is external, when any {@link Endpoint} is external</li>
 * <li>Flow is local, when src and dst endpoint {@link EndpointFwdCtxOrdinals} are the same</li>
 * <li>Flow is local, when src and dst endpoint ordinals are not the same and {@link OfOverlayContext} is missing</li></ul>
 * Also applies to L3
 * <p>
 * L2 Flows:
 * <p>
 * <i>External, local and remote L2 flows</i><br>
 * Priority = 50<br>
 * Matches:<br>
 *      - dl_dst mac address {@link MacAddress}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 * Actions:<br>
 *      - load tunnel Ipv4 (local and remote only)<br>
 *      - loadReg2 {@link NxmNxReg2}<br>
 *      - loadReg3 {@link NxmNxReg3}<br>
 *      - loadReg7 (next hop) {@link NxmNxReg7}<br>
 *      - {@link GoToTable} POLICY ENFORCER table<br>
 * <p>
 * L3 flows:
 * <p>
 * <i>External, local and remote L3 routed flows:</i><br>
 * Priority = 132<br>
 * Matches:<br>
 *      - ip (ethertype)
 *      - dl_dst mac address {@link MacAddress}<br>
 *      - nw_dst ip address {@link IpAddress}<br>
 *      - setReg6 {@link NxmNxReg6}<br>
 * Actions:<br>
 *      - loadReg2 {@link NxmNxReg2}<br>
 *      - loadReg3 {@link NxmNxReg3}<br>
 *      - loadReg4 (tunnel destination) {@link NxmNxReg4} (remote only)<br>
 *      - loadReg7 (next hop) {@link NxmNxReg7}<br>
 *      - set dst mac to eth_dst {@link MacAddress}<br>
 *      - dec_ttl {@link DecNwTtl} (local only)<br>
 *      - {@link GoToTable} POLICY ENFORCER table
 * <p>
 * If virtual router ip is present in subnet, and subnet contains L3 context, arp flow is created<br>
 * <p>
 * <i>Router Arp flow</i><br>
 * Priority = 150<br>
 * Matches:<br>
 *      - arp (ethertype)<br>
 *      - arp target transport address<br>
 *      - setReg6 {@link NxmNxReg6}<br>
 * Actions:<br>
 *      - move eth_src = eth_dst<br>
 *      - set dl_src {@link MacAddress}<br>
 *      - load arp_op<br>
 *      - move arp_sha = arp_tha<br>
 *      - load arp_sha<br>
 *      - move arp_spa = arp_tpa<br>
 *      - load arp_spa<br>
 *      - output:port {@link NodeConnectorId}<br>
 * <p>
 * <i>Broadcast flow (per flood domain)</i>
 * Priority = 140<br>
 * Matches:<br>
 *      - ethernet destination {@link MacAddress}
 *      - setReg5 {@link NxmNxReg5}<br>
 * Actions:<br>
 *      - load tunnel ID<br>
 *      - group action<br>
 * <p>
 * <i>L3 Prefix flow</i><br>
 * Priority = 140<br>
 * Matches:<br>
 *      - ethernet destination {@link MacAddress}
 *      - setReg5 {@link NxmNxReg5}<br>
 * Actions:<br>
 *      - dl_dst {@link MacAddress}<br>
 *      - dec_ttl<br>
 *      - loadReg2 {@link NxmNxReg2}<br>
 *      - loadReg3 {@link NxmNxReg3}<br>
 *      - loadReg4 (next hop) {@link NxmNxReg4}<br>
 *      - loadReg7 (if internal, port_num == {@link NodeConnectorId of L2 EP} ) {@link NxmNxReg7}<br>
 *      - loadReg7 (if external, port_num = external port) {@link NxmNxReg7}<br>
 *      - {@link GoToTable} POLICY ENFORCER table
 */
public class DestinationMapper extends FlowTable {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationMapper.class);

    private final DestinationMapperUtils utils;
    private final short tableId;

    final Map<TenantId, HashSet<Subnet>> subnetsByTenant = new HashMap<>();

    // This is the MAC address of the magical router in the sky
    public static final MacAddress ROUTER_MAC = new MacAddress("88:f0:31:b5:12:b5");
    private static final MacAddress MULTICAST_MAC = new MacAddress("01:00:00:00:00:00");
    private static final int EXTERNAL_L2 = 50;
    private static final int LOCAL_L2 = 50;
    private static final int L3_PREFIX = 100;
    private static final int L3_LOCAL = 132;
    private static final int L3_EXTERNAL = 132;
    private static final int ROUTER_ARP = 150;
    private static final int REMOTE_L2 = 50;
    private static final int REMOTE_L3 = 132;
    // Priorities
    private static final int DROP_FLOW = 1;
    private static final int BROADCAST = 140;

    public DestinationMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.tableId = tableId;
        utils = new DestinationMapperUtils(ctx);
    }

    @Override
    public short getTableId() {
        return tableId;
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {
        NodeId endpointNodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        if (endpointNodeId == null) {
            LOG.warn("Endpoint {} has no location specified, skipped", endpoint);
            return;
        }
        DestinationMapperFlows flows = new DestinationMapperFlows(utils, endpointNodeId, tableId);

        // Do sync
        syncFlows(flows, endpoint, endpointNodeId, ofWriter);
    }

    @VisibleForTesting
    void syncFlows(DestinationMapperFlows flows, Endpoint endpoint, NodeId nodeId, OfWriter ofWriter) {

        // Create basic drop flow
        flows.dropFlow(DROP_FLOW, null, ofWriter);

        // Sync flows related to endpoints
        List<Subnet> localSubnets = utils.getLocalSubnets(nodeId);
        if (localSubnets != null) {
            // Local
            syncLocalFlows(flows, endpoint, localSubnets, ofWriter);
            // Remote & external
            syncEndpointFlows(flows, nodeId, endpoint, ofWriter);
        }

        // Sync router ARP flow
        TenantId tenantId = endpoint.getTenant();
        syncArpFlow(flows, tenantId, ofWriter);

        // Create broadcast flow
        EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        if (ordinals != null) {
            flows.createBroadcastFlow(BROADCAST, ordinals, MULTICAST_MAC, ofWriter);
        }

        // L3 Prefix Endpoint handling
        Collection<EndpointL3Prefix> prefixes = ctx.getEndpointManager().getEndpointsL3PrefixForTenant(tenantId);
        if (prefixes != null) {
            LOG.trace("DestinationMapper - Processing L3PrefixEndpoint");
            syncL3PrefixFlow(flows, prefixes, tenantId, nodeId, ofWriter);
        }
    }

    @VisibleForTesting
    void syncEndpointFlows(DestinationMapperFlows flows, NodeId nodeId, Endpoint endpoint, OfWriter ofWriter) {
        SetMultimap<EpKey, EpKey> visited = HashMultimap.create();
        Set<EndpointGroupId> groupIds = utils.getAllEndpointGroups(endpoint);
        for (EndpointGroupId groupId : groupIds) {
            EgKey groupKey = new EgKey(endpoint.getTenant(), groupId);
            Set<EgKey> peers = Sets.union(Collections.singleton(groupKey),
                    ctx.getCurrentPolicy().getPeers(groupKey));
            for (EgKey peer : peers) {
                Collection<Endpoint> endpointsForGroup = new HashSet<>();
                endpointsForGroup.addAll(ctx.getEndpointManager().getEndpointsForGroup(peer));
                endpointsForGroup.addAll(ctx.getEndpointManager().getExtEpsNoLocForGroup(peer));
                for (Endpoint peerEndpoint : endpointsForGroup) {
                    subnetsByTenant.put(peerEndpoint.getTenant(), utils.getSubnets(endpoint.getTenant()));
                    EpKey epKey = new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
                    EpKey peerEpKey = new EpKey(peerEndpoint.getL2Context(), peerEndpoint.getMacAddress());
                    if (visited.get(epKey) != null && visited.get(epKey).contains(peerEpKey)) {
                        continue;
                    }
                    // Basic checks
                    IndexedTenant endpointTenant = utils.getIndexedTenant(endpoint.getTenant());
                    IndexedTenant peerTenant = utils.getIndexedTenant(peerEndpoint.getTenant());
                    if (endpointTenant == null || peerTenant == null) {
                        LOG.debug("Source or destination endpoint references empty tenant. SrcEp: {} DestEp: {}",
                                endpointTenant, peerTenant);
                        continue;
                    }
                    EndpointFwdCtxOrdinals endpointOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
                    EndpointFwdCtxOrdinals peerOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, peerEndpoint);
                    if (endpointOrdinals == null || peerOrdinals == null) {
                        LOG.debug("Source od destination endpoint ordinals are null. SrcOrdinals: {} DestOrdinals: {}",
                                endpointOrdinals, peerOrdinals);
                        continue;
                    }

                    if (peerEndpoint.getEndpointGroup() == null && peerEndpoint.getEndpointGroups() == null) {
                        LOG.debug("Didn't process endpoint {} due to EPG(s) being null", peerEndpoint.getKey());
                        continue;
                    }

                    List<Subnet> localSubnets = utils.getLocalSubnets(nodeId);
                    if (localSubnets == null) {
                        LOG.error("No subnets could be found locally for node: {}", nodeId);
                        continue;
                    }
                    OfOverlayContext peerContext = peerEndpoint.getAugmentation(OfOverlayContext.class);
                    Subnet epSubnet = endpointTenant.resolveSubnet(new SubnetId(endpoint.getNetworkContainment()));
                    Endpoint l2GatewayEp = utils.getL2EpOfSubnetGateway(endpoint.getTenant(), epSubnet);
                    boolean peerEpIsExternal = peerEndpoint.getNetworkContainment() != null
                            && EndpointManager.isExternal(peerEndpoint, peerTenant.getExternalImplicitGroups());
                    boolean subnetGwIsExternal = l2GatewayEp != null && EndpointManager.isExternal(l2GatewayEp,
                            utils.getIndexedTenant(endpoint.getTenant()).getExternalImplicitGroups());
                    // Sync external
                    if (peerEpIsExternal || subnetGwIsExternal) {
                        Set<NodeConnectorId> externalPorts = ctx.getSwitchManager().getExternalPorts(nodeId);
                        syncExternalFlows(flows, endpoint, peerEndpoint, l2GatewayEp, externalPorts, ofWriter);
                    // Sync remote
                    } else if (peerContext != null && !Objects.equals(peerContext.getNodeId(), nodeId)) {
                        syncRemoteFlows(flows, endpoint, peerEndpoint, peerContext, nodeId, endpointOrdinals, peerOrdinals, localSubnets, ofWriter);
                    }

                    visited.put(epKey, peerEpKey);
                }
            }
        }
    }

    @VisibleForTesting
    void syncArpFlow(DestinationMapperFlows flows, TenantId tenantId, OfWriter ofWriter) {
        for (Entry<TenantId, HashSet<Subnet>> subnetEntry : subnetsByTenant.entrySet()) {
            for (Subnet subnet : subnetEntry.getValue()) {
                IndexedTenant tenant = ctx.getTenant(tenantId);
                L3Context l3Context = utils.getL3ContextForSubnet(tenant, subnet);
                if (subnet == null || subnet.getVirtualRouterIp() == null) {
                    LOG.trace("Arp flow not created, subnet or its virtual router is null. Subnet Id: {}", subnet);
                    continue;
                }
                try {
                    if (l3Context != null && l3Context.getId() != null && tenant != null) {
                        flows.createRouterArpFlow(ROUTER_ARP, tenant, subnet, ofWriter);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to get context ordinal from tenant Id {} and L3 Context Id {}", tenantId,
                            l3Context.getId());
                }
            }
        }
    }

    @VisibleForTesting
    void syncL3PrefixFlow(DestinationMapperFlows flows, Collection<EndpointL3Prefix> l3Prefixes,
                                  TenantId tenantId, NodeId nodeId, OfWriter ofWriter) {
        short policyEnforcerTableId = ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER();
        for (EndpointL3Prefix l3Prefix : l3Prefixes) {
            List<Subnet> localSubnets = utils.getLocalSubnets(nodeId);
            if (localSubnets != null) {
                for (Subnet localSubnet : localSubnets) {
                    for (EndpointL3Gateways l3Gateway : l3Prefix.getEndpointL3Gateways()) {
                        if (l3Gateway != null && l3Gateway.getL3Context() != null && l3Gateway.getIpAddress() != null) {
                            EndpointL3 endpointL3 = ctx.getEndpointManager().getL3Endpoint(l3Gateway.getL3Context(),
                                    l3Gateway.getIpAddress(), tenantId);
                            Endpoint endpointL2 = ctx.getEndpointManager().getL2EndpointFromL3(endpointL3);
                            IndexedTenant tenant = ctx.getTenant(l3Prefix.getTenant());
                            Set<NodeConnectorId> externalPorts = ctx.getSwitchManager().getExternalPorts(nodeId);
                            if (endpointL3 != null && endpointL2 != null && tenant != null && externalPorts != null) {
                                L3Context l3Context = utils.getL3ContextForSubnet(tenant, localSubnet);
                                if (l3Context == null || l3Context.getId() == null) {
                                    LOG.error("No L3 Context found associated with subnet {}", localSubnet.getId());
                                    continue;
                                }
                                EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpointL2);
                                if (ordinals == null) {
                                    LOG.error("No Fwd Ctx ordinals found in endpoint ", endpointL2);
                                    continue;
                                }
                                flows.createL3PrefixFlow(policyEnforcerTableId, L3_PREFIX, endpointL2, l3Prefix, tenant,
                                        localSubnet, externalPorts, ofWriter);
                            }
                        }
                    }
                }
            }
        }
    }

    private void syncExternalFlows(DestinationMapperFlows flows, Endpoint endpoint, Endpoint peerEndpoint,
                                   Endpoint gatewayEndpoint, Set<NodeConnectorId> externalPorts, OfWriter ofWriter) {
        EndpointFwdCtxOrdinals peerOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, peerEndpoint);
        if (peerOrdinals == null) {
            return;
        }
        short goToTable = ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER();
        if (endpoint.getNetworkContainment().equals(peerEndpoint.getNetworkContainment())) {
            if (externalPorts.iterator().hasNext()) {
                // L2 flow
                flows.createExternalL2Flow(goToTable, EXTERNAL_L2, peerEndpoint, externalPorts, ofWriter);
            }
        } else if (gatewayEndpoint != null) {
            HashSet<Subnet> subnets = utils.getSubnets(peerEndpoint.getTenant());
            if (subnets == null) {
                LOG.trace("No subnets in tenant {}", peerEndpoint.getTenant());
                return;
            }
            for (L3Address l3Address : endpoint.getL3Address()) {
                if (l3Address.getIpAddress() == null || l3Address.getL3Context() == null) {
                    LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",
                            endpoint.getL3Address());
                    continue;
                }
                // L3 flow
                flows.createExternalL3RoutedFlow(goToTable, L3_EXTERNAL, peerEndpoint, gatewayEndpoint, l3Address,
                            externalPorts, ofWriter);
            }
        }
    }

    private void syncLocalFlows(DestinationMapperFlows flows, Endpoint endpoint, List<Subnet> localSubnets,
                                OfWriter ofWriter) {
        short goToTable = ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER();
        // L2 Flow
        flows.createLocalL2Flow(goToTable, LOCAL_L2, endpoint, ofWriter);
        if (endpoint.getL3Address() == null) {
            LOG.trace("Endpoint {} didn't have L3 Address so was not processed for L3 flows.", endpoint.getKey());
            return;
        }
        for (L3Address l3Address : endpoint.getL3Address()) {
            if (l3Address.getIpAddress() == null || l3Address.getL3Context() == null) {
                LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",
                        endpoint.getL3Address());
            } else {
                for (Subnet localSubnet : localSubnets) {
                    HashSet<Subnet> subnets = utils.getSubnets(endpoint.getTenant());
                    if (subnets == null) {
                        LOG.trace("No subnets in tenant {}", utils.getIndexedTenant(endpoint.getTenant()));
                        continue;
                    }
                    Subnet remoteSubnet = ctx.getTenant(endpoint.getTenant())
                            .resolveSubnet(new SubnetId(endpoint.getNetworkContainment()));
                    // Do check
                    if(checked(localSubnet, remoteSubnet, l3Address, endpoint)) {
                        // L3 flow
                        flows.createLocalL3RoutedFlow(goToTable, L3_LOCAL, endpoint, l3Address, localSubnet, remoteSubnet,
                                ofWriter);
                    }
                }
            }
        }
    }

    private void syncRemoteFlows(DestinationMapperFlows flows, Endpoint endpoint, Endpoint peerEndpoint, OfOverlayContext ofc,
                                 NodeId nodeId, EndpointFwdCtxOrdinals endpointOrdinals,
                                 EndpointFwdCtxOrdinals peerOrdinals, List<Subnet> localSubnets, OfWriter ofWriter) {
        short goToTable = ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER();
        if (endpointOrdinals.getBdId() == peerOrdinals.getBdId()) {
            IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(ofc.getNodeId(), TunnelTypeVxlan.class);
            NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
            // L2 flow
            if (tunDst != null && tunPort != null) {
                flows.createRemoteL2Flow(goToTable, REMOTE_L2, endpoint, peerEndpoint, tunDst, tunPort, ofWriter);
            }
        } else {
            LOG.trace("DestinationMapper: RemoteL2Flow: not created, in different BDs src: {} dst: {}",
                    endpointOrdinals.getBdId(), peerOrdinals.getBdId());
        }
        if (peerEndpoint.getL3Address() == null) {
            LOG.trace("Endpoint {} didn't have L3 Address so was not processed for L3 flows.", peerEndpoint.getKey());
            return;
        }
        for (L3Address l3Address : peerEndpoint.getL3Address()) {
            if (l3Address.getIpAddress() == null || l3Address.getL3Context() == null) {
                LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",
                        peerEndpoint.getL3Address());
            } else {
                for (Subnet localSubnet : localSubnets) {
                    HashSet<Subnet> subnets = utils.getSubnets(peerEndpoint.getTenant());
                    if (subnets == null) {
                        LOG.trace("No subnets in tenant {}", peerEndpoint.getTenant());
                        return;
                    }
                    Subnet remoteSubnet = ctx.getTenant(peerEndpoint.getTenant())
                            .resolveSubnet(new SubnetId(peerEndpoint.getNetworkContainment()));
                    // Do check
                    if(checked(localSubnet, remoteSubnet, l3Address, peerEndpoint)) {
                        IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(ofc.getNodeId(), TunnelTypeVxlan.class);
                        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
                        // L3 flow
                        if (tunDst != null && tunPort != null) {
                            flows.createRemoteL3RoutedFlow(goToTable, REMOTE_L3, peerEndpoint, l3Address, remoteSubnet, tunDst,
                                    tunPort, localSubnet, ofWriter);
                        }
                    }
                }
            }
        }
    }

    private boolean checked(Subnet localSubnet, Subnet remoteSubnet, L3Address l3Address, Endpoint peerEndpoint) {
        if(peerEndpoint.getTenant() == null) {
            LOG.trace("Endpoint {} does not contain info about tenant", peerEndpoint.getKey());
            return false;
        }
        if (remoteSubnet == null) {
            LOG.trace("Destination IP address does not match any subnet in tenant {}",
                    l3Address.getIpAddress());
            return false;
        }
        if (remoteSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Destination subnet {} for Endpoint {}.{} has no gateway IP",
                    remoteSubnet.getIpPrefix(), peerEndpoint, l3Address.getKey());
            return false;
        }
        if (localSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Local subnet {} has no gateway IP", localSubnet.getIpPrefix());
            return false;
        }
        L3Context destL3c = utils.getL3ContextForSubnet(ctx.getTenant(peerEndpoint.getTenant()), remoteSubnet);
        if (destL3c == null || destL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", remoteSubnet.getId());
            return false;
        }
        L3Context srcL3c = utils.getL3ContextForSubnet(ctx.getTenant(peerEndpoint.getTenant()), localSubnet);
        if (srcL3c == null || srcL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", localSubnet.getId());
            return false;
        }
        if (!(srcL3c.getId().getValue().equals(destL3c.getId().getValue()))) {
            LOG.trace("Trying to route between two L3Contexts {} and {}. Not currently supported.", srcL3c.getId()
                    .getValue(), destL3c.getId().getValue());
            return false;
        }
        return true;
    }
}
