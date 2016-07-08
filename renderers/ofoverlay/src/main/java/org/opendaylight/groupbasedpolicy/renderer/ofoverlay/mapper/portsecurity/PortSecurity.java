/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <h1>Manage the table that enforces port security. Initial flows in group-based policy pipeline (table=0)</h1>
 *
 * Lower-priority flows are leading flows for all traffic incoming from endpoints associated to gbp classifier.<br>
 * Created when an {@link Endpoint} is internal and contains {@link OfOverlayContext} augmentation. Several flows of
 * this kind are produced.
 *<p>
 * <i>L2 flow:</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - in_port, {@link NodeConnectorId}
 *      - dl_src {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress}<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 *<p>
 * <i>L3 flow:</i><br>
 * Priority = 120<br>
 * Matches:<br>
 *      - ip, (ethertype)<br>
 *      - in_port, {@link NodeConnectorId}<br>
 *      - dl_src {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress}<br>
 *      - nw_src (source ip address)<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 *<p>
 * <i>L3 Arp flow:</i><br>
 * Priority = 121<br>
 * Matches:<br>
 *      - arp, (ethertype)<br>
 *      - in_port, {@link NodeConnectorId}<br>
 *      - dl_src {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress}<br>
 *      - arp_spa (arp source transport address)<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 *<p>
 * <i>L3 Dhcp dora flow:</i><br>
 * Priority = 115<br>
 * Matches:<br>
 *      - ip, (ethertype)<br>
 *      - in_port, {@link NodeConnectorId}<br>
 *      - dl_src {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress}<br>
 *      - nw_dst (destination ip address)<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 *<p>
 * Higher-priority flows providing VLAN support for external networks. Created when node contains external ports
 *<p>
 * <i>Allow from external:</i><br>
 * Priority = 200<br>
 * Matches:<br>
 *      - in_port, {@link NodeConnectorId}<br>
 * Actions:<br>
 *      - {@link GoToTable} INGRESS NAT table
 *<p>
 * <i>Flow that pops VLAN tag for inbound traffic:</i><br>
 * Priority = 210<br>
 * See {@link PortSecurityFlows#popVlanTagsOnExternalPortFlows(short, NodeConnectorId, List, int, OfWriter)}
 *<p>
 * Highest priority flows used to direct traffic coming from tunnel (SFC). These flows are created always
 *<p>
 * <i>Allow from tunnel:</i><br>
 * Priority = 300<br>
 * Matches:<br>
 *      - in_port (has to be tunnel port), {@link NodeConnectorId}<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 *
 */
public class PortSecurity extends FlowTable {
    private static final Logger LOG =
            LoggerFactory.getLogger(PortSecurity.class);
    // Priorities
    private static final Integer DROP = 1;
    private static final Integer L2FLOW = 100;
    private static final Integer DROP_ARP = 110;
    private static final Integer DROP_IPV4 = 111;
    private static final Integer DROP_IPV6 = 112;
    private static final Integer DHCP_DORA = 115;
    private static final Integer L3IP_FLOW = 120;
    private static final Integer L3ARP_FLOW = 121;
    private static final Integer ALLOW_EXTERNAL = 200;
    private static final Integer POP_VLAN_TAG_EXTERNAL = 210;
    private static final Integer ALLOW_FROM_TUNNEL = 300;
    private final short tableId;

    public PortSecurity(OfContext ctx, short tableId) {
        super(ctx);
        this.tableId = tableId;
    }

    @Override
    public short getTableId() {
        return tableId;
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) {
        NodeId endpointNodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        if (endpointNodeId == null) {
            LOG.warn("Endpoint {} has no location specified, skipped", endpoint);
            return;
        }
        PortSecurityFlows flows = new PortSecurityFlows(endpointNodeId, tableId);

        // Do sync
        syncFlows(flows, endpointNodeId, endpoint, ofWriter);
    }

    @VisibleForTesting
    void syncFlows(PortSecurityFlows flows, NodeId nodeId, Endpoint endpoint, OfWriter ofWriter) {

        // TODO all "dropFlow" and "allowFlowFromTunnelFlow" flows should be called only once
        // Drop all
        flows.dropFlow(DROP, null, ofWriter);

        // Drop IP traffic that doesn't match a source IP rule
        flows.dropFlow(DROP_ARP, FlowUtils.ARP, ofWriter);
        flows.dropFlow(DROP_IPV4, FlowUtils.IPv4, ofWriter);
        flows.dropFlow(DROP_IPV6, FlowUtils.IPv6, ofWriter);

        // Allow traffic from tunnel ports
        short sourceMapperId = ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER();
        NodeConnectorId vxLanPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (vxLanPort != null) {
            flows.allowFromTunnelFlow(sourceMapperId, ALLOW_FROM_TUNNEL, vxLanPort, ofWriter);
        }
        NodeConnectorId vxLanGpePort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlanGpe.class);
        if (vxLanGpePort != null) {
            flows.allowFromTunnelFlow(sourceMapperId, ALLOW_FROM_TUNNEL, vxLanGpePort, ofWriter);
        }

        // L3/L2 flows
        TenantId tenantId = endpoint.getTenant();
        // Internal endpoint
        if (EndpointManager.isInternal(endpoint, getExternalImplicitGroupsForTenant(tenantId))) {
            NodeConnectorId nodeConnectorId = ctx.getEndpointManager().getEndpointNodeConnectorId(endpoint);
            MacAddress macAddress = endpoint.getMacAddress();
            if (nodeConnectorId != null && macAddress != null) {
                // Allow layer 3 traffic (ARP and IP) with the correct source IP, MAC, and source port
                flows.l3Flow(sourceMapperId, endpoint, nodeConnectorId, macAddress, L3IP_FLOW, false, ofWriter);
                flows.l3Flow(sourceMapperId, endpoint, nodeConnectorId, macAddress, L3ARP_FLOW, true, ofWriter);
                flows.l3DhcpDoraFlow(sourceMapperId, nodeConnectorId, macAddress, DHCP_DORA, ofWriter);
                // Allow layer 2 traffic with the correct source MAC and port (lower priority than drop IP rules)
                flows.l2flow(sourceMapperId, nodeConnectorId, macAddress, L2FLOW, ofWriter);
            }
            // External endpoint
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("External Endpoint is ignored in PortSecurity: {}", endpoint);
            }
        }
        // External ports
        short ingressNatId = ctx.getPolicyManager().getTABLEID_INGRESS_NAT();
        for (NodeConnectorId connectorId : ctx.getSwitchManager().getExternalPorts(nodeId)) {
            // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..
            if (tenantId != null && ctx.getTenant(tenantId) != null) {
                Tenant tenant = ctx.getTenant(tenantId).getTenant();
                if (tenant != null && tenant.getForwardingContext() != null &&
                        tenant.getForwardingContext().getL2FloodDomain() != null) {
                    List<L2FloodDomain> floodDomains = tenant.getForwardingContext().getL2FloodDomain();
                    flows.popVlanTagsOnExternalPortFlows(ingressNatId, connectorId, floodDomains,
                            POP_VLAN_TAG_EXTERNAL, ofWriter);
                }
            }
            // Allowing untagged frames entering policy domain
            flows.allowFromExternalPortFlow(ingressNatId, connectorId, ALLOW_EXTERNAL, ofWriter);
        }
    }

    private Set<ExternalImplicitGroup> getExternalImplicitGroupsForTenant(TenantId tenantId) {
        IndexedTenant tenant = ctx.getTenant(tenantId);
        if (tenant == null) {
            return Collections.emptySet();
        }
        return tenant.getExternalImplicitGroups();
    }
}
