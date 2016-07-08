/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;


import com.google.common.base.Preconditions;

import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DestinationMapperUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationMapperUtils.class);

    private final OfContext ctx;

    DestinationMapperUtils(OfContext ctx) {
        this.ctx = Preconditions.checkNotNull(ctx);
    }

    HashSet<Subnet> getSubnets(TenantId tenantId) {
        IndexedTenant indexedTenant = ctx.getTenant(tenantId);
        if (indexedTenant != null && indexedTenant.getTenant() != null) {
            ForwardingContext forwardingContext = indexedTenant.getTenant().getForwardingContext();
            if (forwardingContext != null && forwardingContext.getSubnet() != null) {
                return new HashSet<>(forwardingContext.getSubnet());
            }
        }

        return new HashSet<>();
    }

    L3Context getL3ContextForSubnet(IndexedTenant indexedTenant, Subnet subnet) {
        if (indexedTenant == null || subnet == null || subnet.getParent() == null) {
            return null;
        }
        return indexedTenant.resolveL3Context(new L2FloodDomainId(subnet.getParent().getValue()));
    }

    NetworkDomainId getEPNetworkContainment(Endpoint endpoint, IndexedTenant tenant) {
        if (endpoint.getNetworkContainment() != null) {
            return endpoint.getNetworkContainment();
        } else if (tenant != null) {
            return tenant.getEndpointGroup(endpoint.getEndpointGroup())
                    .getNetworkDomain();
        } else {
            return null;
        }
    }

    // Need a method to get subnets for EPs attached to the node locally
    // to set the source Mac address for the router interface.
    List<Subnet> getLocalSubnets(NodeId nodeId) {
        Collection<Endpoint> endpointsForNode = ctx.getEndpointManager().getEndpointsForNode(nodeId);

        List<Subnet> localSubnets = new ArrayList<>();

        for (Endpoint endpoint : endpointsForNode) {
            HashSet<Subnet> subnets = getSubnets(endpoint.getTenant());
            if (subnets.isEmpty()) {
                LOG.debug("No local subnets in tenant {} for EP {}.", endpoint.getTenant(), endpoint.getKey());
                continue;
            }
            NetworkDomainId epNetworkContainment = getEPNetworkContainment(endpoint, ctx.getTenant(endpoint.getTenant()));
            for (Subnet subnet : subnets) {
                if (epNetworkContainment.getValue().equals(subnet.getId().getValue())) {
                    localSubnets.add(subnet);
                }
            }
        }
        return localSubnets;
    }

    Endpoint getL2EpOfSubnetGateway(TenantId tenantId, Subnet subnet) {
        if (subnet != null && subnet.getVirtualRouterIp() != null) {
            IpAddress gwIpAddress = subnet.getVirtualRouterIp();
            Collection<EndpointL3Prefix> prefixEps = ctx.getEndpointManager().getEndpointsL3PrefixForTenant(tenantId);
            if (prefixEps != null) {
                for (EndpointL3Prefix prefixEp : prefixEps) {
                    for (EndpointL3Gateways gw : prefixEp.getEndpointL3Gateways()) {
                        EndpointL3 l3Ep = ctx.getEndpointManager().getL3Endpoint(gw.getL3Context(), gwIpAddress,
                                prefixEp.getTenant());
                        if (l3Ep != null && l3Ep.getL2Context() != null && l3Ep.getMacAddress() != null) {
                            return ctx.getEndpointManager().getEndpoint(
                                    new EpKey(l3Ep.getL2Context(), l3Ep.getMacAddress()));
                        }
                    }
                }
            }
        }
        return null;
    }

    MacAddress routerPortMac(L3Context l3c, IpAddress ipAddress, TenantId tenantId) {
        MacAddress defaultMacAddress = DestinationMapper.ROUTER_MAC;
        if (l3c.getId() != null) {
            EndpointL3 endpointL3 = ctx.getEndpointManager().getL3Endpoint(l3c.getId(), ipAddress, tenantId);
            if (endpointL3 == null || endpointL3.getMacAddress() == null) {
                return defaultMacAddress;
            } else {
                return endpointL3.getMacAddress();
            }
        } else {
            return defaultMacAddress;
        }
    }

    IndexedTenant getIndexedTenant(TenantId tenantId) {
        return ctx.getTenant(tenantId);
    }

    Set<EndpointGroupId> getAllEndpointGroups(Endpoint endpoint) {
        Set<EndpointGroupId> groupIds = new HashSet<>();
        if (endpoint.getEndpointGroup() != null) {
            groupIds.add(endpoint.getEndpointGroup());
        }
        if (endpoint.getEndpointGroups() != null) {
            groupIds.addAll(endpoint.getEndpointGroups());
        }
        return groupIds;
    }

    OrdinalFactory.EndpointFwdCtxOrdinals getEndpointOrdinals(Endpoint endpoint) {
        try {
            return OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        } catch (Exception e) {
            LOG.error("Failed to get fwd ctx ordinals for endpoint {}", endpoint);
            return null;
        }
    }

}
