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

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.endpoints.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Maps;

public class PolicyContext {

    private final RendererPolicy policy;
    private final ImmutableTable<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> policyTable;
    private final ImmutableMap<RuleGroupKey, ResolvedRuleGroup> ruleGroupByKey;
    private final ImmutableMap<AddressEndpointKey, AddressEndpointWithLocation> addrEpByKey;

    public PolicyContext(@Nonnull RendererPolicy policy) {
        this.policy = Preconditions.checkNotNull(policy);
        Optional<Configuration> optConfig = resolveValidConfig(policy);
        if (optConfig.isPresent()) {
            Configuration config = optConfig.get();
            this.ruleGroupByKey = resolveRuleGroups(config);
            List<RendererEndpoint> rendererEps = resolveRendererEndpoints(config);
            this.policyTable = resolvePolicy(rendererEps, ruleGroupByKey);
            addrEpByKey = resolveAddrEpWithLoc(config);
        } else {
            this.ruleGroupByKey = ImmutableMap.of();
            this.policyTable = ImmutableTable.of();
            this.addrEpByKey = ImmutableMap.of();
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
            List<RendererEndpoint> rendererEps, Map<RuleGroupKey, ResolvedRuleGroup> ruleGroupInfoByRuleGroupKey) {
        Builder<RendererEndpointKey, PeerEndpointKey, ImmutableSortedSet<RendererResolvedPolicy>> resultBuilder =
                new Builder<>();
        Supplier<TreeSet<RendererResolvedPolicy>> rendererPolicySupplier = () -> new TreeSet<>();
        rendererEps.stream().forEach(rEp -> {
            rEp.getPeerEndpointWithPolicy().stream().filter(Objects::nonNull).forEach(peer -> {
                ImmutableSortedSet<RendererResolvedPolicy> rPolicy =
                        peer.getRuleGroupWithRendererEndpointParticipation()
                            .stream()
                            .map(ruleGroup -> new RendererResolvedPolicy(ruleGroup.getRendererEndpointParticipation(),
                                    ruleGroupInfoByRuleGroupKey.get(KeyFactory.ruleGroupKey(ruleGroup.getKey()))))
                            .collect(Collectors.collectingAndThen(Collectors.toCollection(rendererPolicySupplier),
                                    ImmutableSortedSet::copyOfSorted));
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

}
