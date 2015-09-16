/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrdinalFactory {

    private final static Logger LOG = LoggerFactory.getLogger(OrdinalFactory.class);

    /**
     * Counter used to allocate ordinal values for forwarding contexts and VNIDs
     */
    private final static AtomicInteger policyOrdinal = new AtomicInteger(1);

    private final static ConcurrentMap<String, Integer> ordinals = new ConcurrentHashMap<>();

    // XXX - need to garbage collect
    private final static ConcurrentMap<ConditionGroup, Integer> cgOrdinals = new ConcurrentHashMap<>();

    /**
     * Creates an ordinal for the OfOverlay pipeline comparison, based on @TenantId and a
     * uniqueID (UUID) associated with any other attribute.
     *
     * @param tenantId the tenant id
     * @param id a unique id
     * @return the ordinal
     * @throws Exception throws all exception
     */
    public static int getContextOrdinal(final TenantId tenantId, final UniqueId id) throws Exception {
        if (tenantId == null || id == null)
            return 0;
        return getContextOrdinalFromString(tenantId.getValue() + "|" + id.getValue());
    }

    /**
     * Get a unique ordinal for the given condition group, suitable for use in
     * the data plane. This is unique only for this node, and not globally.
     *
     * @param cg
     *        the {@link ConditionGroup}
     * @return the unique ID
     */
    public static int getCondGroupOrdinal(final ConditionGroup cg) {
        if (cg == null)
            return 0;
        Integer ord = cgOrdinals.get(cg);
        if (ord == null) {
            ord = policyOrdinal.getAndIncrement();
            Integer old = cgOrdinals.putIfAbsent(cg, ord);
            if (old != null)
                ord = old;
        }
        return ord.intValue();
    }

    /**
     * Get a 32-bit context ordinal suitable for use in the OF data plane for
     * the given policy item.
     *
     * @param destNode
     *        destination node ID
     * @return the 32-bit ordinal value
     * @throws Exception throws all exception
     */

    public static int getContextOrdinal(NodeId destNode) throws Exception {
        return getContextOrdinalFromString(destNode.getValue());
    }

    public static int getContextOrdinal(Endpoint ep, NetworkDomainId networkContainment) throws Exception {

        Set<String> epgs = new TreeSet<>();

        // Get EPGs and add to ordered Set
        if (ep.getEndpointGroup() != null) {
            epgs.add(ep.getEndpointGroup().getValue());
        }
        if (ep.getEndpointGroups() != null) {
            for (EndpointGroupId epgId : ep.getEndpointGroups()) {
                epgs.add(epgId.getValue());
            }
        }

        StringBuilder key = new StringBuilder(ep.getTenant().getValue());

        for (String epg : epgs) {
            key.append('|');
            key.append(epg);
        }

        key.append("|").append(networkContainment);

        return getContextOrdinalFromString(key.toString());

    }

    public static int getContextOrdinal(Endpoint ep) throws Exception {

        Set<String> epgs = new TreeSet<>();

        // Get EPGs and add to ordered Set
        if (ep.getEndpointGroup() != null) {
            epgs.add(ep.getEndpointGroup().getValue());
        }
        if (ep.getEndpointGroups() != null) {
            for (EndpointGroupId epgId : ep.getEndpointGroups()) {
                epgs.add(epgId.getValue());
            }
        }

        StringBuilder key = new StringBuilder(ep.getTenant().getValue());

        for (String epg : epgs) {
            key.append('|');
            key.append(epg);
        }

        return getContextOrdinalFromString(key.toString());

    }

    /**
     * Get a 32-bit context ordinal suitable for use in the OF data plane for
     * the given policy item.
     *
     * @param id
     *        the unique ID for the element
     * @return the 32-bit ordinal value
     */
    private static int getContextOrdinalFromString(final String id) throws Exception {

        Integer ord = ordinals.get(id);
        if (ord == null) {
            ord = policyOrdinal.getAndIncrement();
            Integer old = ordinals.putIfAbsent(id, ord);
            if (old != null)
                ord = old;
        }
        return ord.intValue();
    }

    public static final EndpointFwdCtxOrdinals getEndpointFwdCtxOrdinals(OfContext ctx, PolicyInfo policyInfo,
            Endpoint ep) throws Exception {
        IndexedTenant tenant = ctx.getPolicyResolver().getTenant(ep.getTenant());
        if (tenant == null) {
            LOG.debug("Tenant {} is null", ep.getTenant());
            return null;
        }
        return new EndpointFwdCtxOrdinals(ep, policyInfo, ctx);
    }

    // TODO alagalah Li: Move to either OrdinalFactory or EndpointManager
    public static class EndpointFwdCtxOrdinals {

        private NetworkDomainId networkContainment;
        private EpKey ep;
        private int epgId = 0, bdId = 0, fdId = 0, l3Id = 0, cgId = 0, tunnelId = 0;

        private EndpointFwdCtxOrdinals(Endpoint ep, PolicyInfo policyInfo, OfContext ctx) throws Exception {
            this.ep = new EpKey(ep.getL2Context(), ep.getMacAddress());

            IndexedTenant tenant = ctx.getPolicyResolver().getTenant(ep.getTenant());

            // Set network containment either from ep, or from primary EPG
            if (ep.getNetworkContainment() != null) {
                this.networkContainment = ep.getNetworkContainment();
            } else {
                EndpointGroup epg = tenant.getEndpointGroup(ep.getEndpointGroup());
                if (epg.getNetworkDomain() != null) {
                    this.networkContainment = epg.getNetworkDomain();
                } else {
                    LOG.info("endPoint ordinals for {} not processed in SourceMapper. Must be able to resolve "
                            + "network containment either directly, or from primary EPG", ep.getKey());
                    return;
                }
            }

            // TODO: alagalah: I have a draft to address structure of mEPG
            // conditions, but
            // out of scope until broader bugs with conditions are fixed.
            List<ConditionName> conds = ctx.getEndpointManager().getCondsForEndpoint(ep);
            ConditionGroup cg = policyInfo.getEgCondGroup(new EgKey(ep.getTenant(), ep.getEndpointGroup()), conds);
            this.cgId = getCondGroupOrdinal(cg);

            // Based on network containment, determine components of
            // forwarding context
            L3Context l3c = tenant.resolveL3Context(networkContainment);
            L2BridgeDomain bd = tenant.resolveL2BridgeDomain(networkContainment);
            L2FloodDomain fd = tenant.resolveL2FloodDomain(networkContainment);

            // Set ordinal id's for use in flows for each forwarding context
            // component

            this.epgId = getContextOrdinal(ep);

            // TODO: alagalah Li/Be: This idea can be extended to include conditions.
            this.tunnelId = getContextOrdinal(ep, networkContainment);
            if (bd != null)
                this.bdId = getContextOrdinal(ep.getTenant(), bd.getId());
            if (fd != null)
                this.fdId = getContextOrdinal(ep.getTenant(), fd.getId());
            if (l3c != null)
                this.l3Id = getContextOrdinal(ep.getTenant(), l3c.getId());

        }

        public int getTunnelId() {
            return tunnelId;
        }

        public int getEpgId() {
            return this.epgId;
        }

        public NetworkDomainId getNetworkContainment() {
            return this.networkContainment;
        }

        public EpKey getEp() {
            return this.ep;
        }

        public int getBdId() {
            return this.bdId;
        }

        public int getFdId() {
            return this.fdId;
        }

        public int getL3Id() {
            return this.l3Id;
        }

        public int getCgId() {
            return this.cgId;
        }
    }
}
