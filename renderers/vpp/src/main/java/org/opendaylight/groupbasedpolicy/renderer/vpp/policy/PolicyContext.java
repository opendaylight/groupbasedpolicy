/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

public class PolicyContext {

    private final RendererPolicy policy;
    private final ImmutableTable<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> policyTable;
    private final ImmutableMap<RuleGroupKey, ResolvedRuleGroup> ruleGroupByKey;
    private final ImmutableMap<AddressEndpointKey, AddressEndpointWithLocation> addrEpByKey;
    private final ImmutableTable<TenantId, RendererForwardingContextKey, RendererForwardingContext> forwardingCtxTable;
    private final ImmutableTable<TenantId, RendererNetworkDomainKey, RendererNetworkDomain> networkDomainTable;
    private final ImmutableSetMultimap<RuleGroupKey, AddressEndpointWithLocationKey> endpointsByRuleGroups;

    public PolicyContext(@Nonnull RendererPolicy policy) {
        this.policy = Preconditions.checkNotNull(policy);
        Optional<Configuration> optConfig = resolveValidConfig(policy);
        if (optConfig.isPresent()) {
            Configuration config = optConfig.get();
            this.ruleGroupByKey = resolveRuleGroups(config);
            List<RendererEndpoint> rendererEps = resolveRendererEndpoints(config);
            SetMultimap<RuleGroupKey, AddressEndpointWithLocationKey> epsByRules = HashMultimap.create();
            this.policyTable = resolvePolicy(rendererEps, ruleGroupByKey, epsByRules);
            this.endpointsByRuleGroups =
                    ImmutableSetMultimap.<RuleGroupKey, AddressEndpointWithLocationKey>copyOf(epsByRules);
            addrEpByKey = resolveAddrEpWithLoc(config);
            this.forwardingCtxTable = resolveForwardingCtxTable(config);
            this.networkDomainTable = resolveNetworkDomainTable(config);
        } else {
            this.ruleGroupByKey = ImmutableMap.of();
            this.policyTable = ImmutableTable.of();
            this.addrEpByKey = ImmutableMap.of();
            this.forwardingCtxTable = ImmutableTable.of();
            this.networkDomainTable = ImmutableTable.of();
            this.endpointsByRuleGroups = ImmutableSetMultimap.<RuleGroupKey, AddressEndpointWithLocationKey>of();
        }
    }

    private static List<RendererEndpoint> resolveRendererEndpoints(Configuration config) {
        if (config.getRendererEndpoints() == null) {
            return Collections.emptyList();
        }
        List<RendererEndpoint> rendererEndpoints = config.getRendererEndpoints().getRendererEndpoint();
        if (rendererEndpoints == null) {
            return Collections.emptyList();
        }
        return rendererEndpoints;
    }

    private static ImmutableTable<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> resolvePolicy(
            List<RendererEndpoint> rendererEps, Map<RuleGroupKey, ResolvedRuleGroup> ruleGroupInfoByRuleGroupKey,
            SetMultimap<RuleGroupKey, AddressEndpointWithLocationKey> epsByRules) {
        Builder<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> resultBuilder =
                new Builder<>();
        Supplier<TreeSet<RendererResolvedPolicy>> rendererPolicySupplier = () -> new TreeSet<>();
        rendererEps.stream().forEach(rEp -> {
            rEp.getPeerEndpoint()
                .stream()
                .filter(Objects::nonNull)
                .filter(peer -> peer.getRuleGroupWithRendererEndpointParticipation() != null)
                .forEach(peer -> {
                    ImmutableSortedSet<RendererResolvedPolicy> rPolicy = peer
                        .getRuleGroupWithRendererEndpointParticipation()
                        .stream()
                        .map(ruleGroup -> new RendererResolvedPolicy(ruleGroup.getRendererEndpointParticipation(),
                                ruleGroupInfoByRuleGroupKey.get(KeyFactory.ruleGroupKey(ruleGroup.getKey()))))
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(rendererPolicySupplier),
                                ImmutableSortedSet::copyOfSorted));
                    peer.getRuleGroupWithRendererEndpointParticipation()
                        .forEach(ruleGroup -> epsByRules.put(KeyFactory.ruleGroupKey(ruleGroup.getKey()),
                                AddressEndpointUtils.addrEpWithLocationKey(rEp.getKey())));
                    resultBuilder.put(rEp.getKey(), KeyFactory.peerEndpointKey(peer.getKey()),
                            ImmutableSortedSet.copyOfSorted(rPolicy));
                });
        });
        return resultBuilder.build();
    }

    private static ImmutableMap<RuleGroupKey, ResolvedRuleGroup> resolveRuleGroups(Configuration config) {
        return config.getRuleGroups().getRuleGroup().stream().collect(Collectors
            .collectingAndThen(Collectors.toMap(RuleGroup::getKey, ResolvedRuleGroup::new), ImmutableMap::copyOf));
    }

    private static ImmutableTable<TenantId, RendererForwardingContextKey, RendererForwardingContext> resolveForwardingCtxTable(
            Configuration config) {
        Builder<TenantId, RendererForwardingContextKey, RendererForwardingContext> resultBuilder = new Builder<>();
        Optional.ofNullable(config.getRendererForwarding()).map(x -> x.getRendererForwardingByTenant()).ifPresent(x -> {
            x.forEach(fwdByTenant -> {
                Optional.ofNullable(fwdByTenant.getRendererForwardingContext()).ifPresent(fwdCtxs -> {
                    fwdCtxs.forEach(fwdCtx -> {
                        resultBuilder.put(fwdByTenant.getTenantId(), fwdCtx.getKey(), fwdCtx);
                    });
                });
            });
        });
        return resultBuilder.build();
    }

    private static ImmutableTable<TenantId, RendererNetworkDomainKey, RendererNetworkDomain> resolveNetworkDomainTable(
            Configuration config) {
        Builder<TenantId, RendererNetworkDomainKey, RendererNetworkDomain> resultBuilder = new Builder<>();
        Optional.ofNullable(config.getRendererForwarding()).map(x -> x.getRendererForwardingByTenant()).ifPresent(x -> {
            x.forEach(fwdByTenant -> {
                Optional.ofNullable(fwdByTenant.getRendererNetworkDomain()).ifPresent(netDomains -> {
                    netDomains.forEach(netDomain -> {
                        resultBuilder.put(fwdByTenant.getTenantId(), netDomain.getKey(), netDomain);
                    });
                });
            });
        });
        return resultBuilder.build();
    }

    private static ImmutableMap<AddressEndpointKey, AddressEndpointWithLocation> resolveAddrEpWithLoc(
            Configuration config) {
        return Maps.uniqueIndex(config.getEndpoints().getAddressEndpointWithLocation(),
                new com.google.common.base.Function<AddressEndpointWithLocation, AddressEndpointKey>() {

                    @Override
                    public AddressEndpointKey apply(AddressEndpointWithLocation input) {
                        return new AddressEndpointKey(input.getAddress(), input.getAddressType(), input.getContextId(),
                                input.getContextType());
                    }
                });
    }

    private static Optional<Configuration> resolveValidConfig(RendererPolicy policy) {
        Configuration config = policy.getConfiguration();
        if (config == null) {
            return Optional.empty();
        }
        if (config.getRendererEndpoints() == null) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    public @Nonnull RendererPolicy getPolicy() {
        return policy;
    }

    public @Nonnull ImmutableTable<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> getPolicyTable() {
        return policyTable;
    }

    public @Nonnull ImmutableMap<RuleGroupKey, ResolvedRuleGroup> getRuleGroupByKey() {
        return ruleGroupByKey;
    }

    public @Nonnull ImmutableMap<AddressEndpointKey, AddressEndpointWithLocation> getAddrEpByKey() {
        return addrEpByKey;
    }

    public @Nonnull ImmutableTable<TenantId, RendererForwardingContextKey, RendererForwardingContext> getForwardingCtxTable() {
        return forwardingCtxTable;
    }

    public @Nonnull ImmutableTable<TenantId, RendererNetworkDomainKey, RendererNetworkDomain> getNetworkDomainTable() {
        return networkDomainTable;
    }

    public @Nonnull ImmutableSetMultimap<RuleGroupKey, AddressEndpointWithLocationKey> getEndpointsByRuleGroups() {
        return endpointsByRuleGroups;
    }

    public Optional<RendererEndpoint> getRendererEndpoint(RendererEndpointKey key) {
        return getPolicy().getConfiguration()
            .getRendererEndpoints()
            .getRendererEndpoint()
            .stream()
            .filter(rendEp -> rendEp.getKey().equals(key))
            .findAny();
    }

    @Override
    public String toString() {
        return "PolicyContext [policyTable=" + policyTable + ", ruleGroupByKey=" + ruleGroupByKey + ", addrEpByKey="
                + addrEpByKey + ", forwardingCtxTable=" + forwardingCtxTable + ", networkDomainTable="
                + networkDomainTable + "]";
    }

}
