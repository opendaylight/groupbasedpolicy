/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.endpoints.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.external.containment.endpoints.PeerExternalContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.external.endpoints.PeerExternalEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.ContainmentEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.ContainmentEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalContainmentEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalContainmentEndpointWithPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalContainmentEndpointWithPolicyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointWithPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointWithPolicyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class RendererConfigurationBuilder {

    private final Table<RendererEndpointKey, PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerEp =
            HashBasedTable.create();
    private final Table<RendererEndpointKey, PeerExternalEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerExtEp =
            HashBasedTable.create();
    private final Table<RendererEndpointKey, PeerExternalContainmentEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerExtCtxEp =
            HashBasedTable.create();
    private final Set<AddressEndpointKey> adrEpKeys = new HashSet<>();
    private final Set<ContainmentEndpointKey> contEpKeys = new HashSet<>();
    private final Set<PolicyRuleGroupKey> policyRuleGrpKeys = new HashSet<>();

    public void add(RendererEndpointKey rendererEpKey, PeerEndpointKey peerEpKey, PolicyRuleGroupKey ruleGrpKey,
            EndpointPolicyParticipation rendererEpParticipation) {
        Set<RuleGroupWithRendererEndpointParticipation> ruleGrpWithRendererEpParticipation =
                policiesByEpAndPeerEp.get(rendererEpKey, peerEpKey);
        if (ruleGrpWithRendererEpParticipation == null) {
            ruleGrpWithRendererEpParticipation = new HashSet<>();
            policiesByEpAndPeerEp.put(rendererEpKey, peerEpKey, ruleGrpWithRendererEpParticipation);
            adrEpKeys.add(AddressEndpointUtils.fromRendererEpKey(rendererEpKey));
            adrEpKeys.add(AddressEndpointUtils.fromPeerEpKey(peerEpKey));
        }
        policyRuleGrpKeys.add(ruleGrpKey);
        ruleGrpWithRendererEpParticipation
            .add(toRuleGroupWithRendererEndpointParticipation(ruleGrpKey, rendererEpParticipation));
    }

    public void add(RendererEndpointKey rendererEpKey, PeerExternalEndpointKey peerExtEpKey,
            PolicyRuleGroupKey ruleGrpKey, EndpointPolicyParticipation rendererEpParticipation) {
        Set<RuleGroupWithRendererEndpointParticipation> ruleGrpWithRendererEpParticipation =
                policiesByEpAndPeerExtEp.get(rendererEpKey, peerExtEpKey);
        if (ruleGrpWithRendererEpParticipation == null) {
            ruleGrpWithRendererEpParticipation = new HashSet<>();
            policiesByEpAndPeerExtEp.put(rendererEpKey, peerExtEpKey, ruleGrpWithRendererEpParticipation);
            adrEpKeys.add(AddressEndpointUtils.fromRendererEpKey(rendererEpKey));
            adrEpKeys.add(AddressEndpointUtils.fromPeerExtEpKey(peerExtEpKey));
        }
        policyRuleGrpKeys.add(ruleGrpKey);
        ruleGrpWithRendererEpParticipation
            .add(toRuleGroupWithRendererEndpointParticipation(ruleGrpKey, rendererEpParticipation));
    }

    public void add(RendererEndpointKey rendererEpKey, PeerExternalContainmentEndpointKey peerExtContainmentEpKey,
            PolicyRuleGroupKey ruleGrpKey, EndpointPolicyParticipation rendererEpParticipation) {
        Set<RuleGroupWithRendererEndpointParticipation> ruleGrpWithRendererEpParticipation =
                policiesByEpAndPeerExtCtxEp.get(rendererEpKey, peerExtContainmentEpKey);
        if (ruleGrpWithRendererEpParticipation == null) {
            ruleGrpWithRendererEpParticipation = new HashSet<>();
            policiesByEpAndPeerExtCtxEp.put(rendererEpKey, peerExtContainmentEpKey, ruleGrpWithRendererEpParticipation);
            adrEpKeys.add(AddressEndpointUtils.fromRendererEpKey(rendererEpKey));
            contEpKeys.add(ContainmentEndpointUtils.fromPeerExtContEpKey(peerExtContainmentEpKey));
        }
        policyRuleGrpKeys.add(ruleGrpKey);
        ruleGrpWithRendererEpParticipation
            .add(toRuleGroupWithRendererEndpointParticipation(ruleGrpKey, rendererEpParticipation));
    }

    public static RuleGroupWithRendererEndpointParticipation toRuleGroupWithRendererEndpointParticipation(
            PolicyRuleGroupKey ruleGrpKey, EndpointPolicyParticipation rendererEpParticipation) {
        return new RuleGroupWithRendererEndpointParticipationBuilder().setTenantId(ruleGrpKey.getTenantId())
            .setContractId(ruleGrpKey.getContractId())
            .setSubjectName(ruleGrpKey.getSubjectName())
            .setRendererEndpointParticipation(rendererEpParticipation)
            .build();
    }

    public ImmutableTable<RendererEndpointKey, PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> getPoliciesByEpAndPeerEp() {
        return ImmutableTable.copyOf(policiesByEpAndPeerEp);
    }

    public ImmutableTable<RendererEndpointKey, PeerExternalEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> getPoliciesByEpAndPeerExtEp() {
        return ImmutableTable.copyOf(policiesByEpAndPeerExtEp);
    }

    public ImmutableTable<RendererEndpointKey, PeerExternalContainmentEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> getPoliciesByEpAndPeerExtConEp() {
        return ImmutableTable.copyOf(policiesByEpAndPeerExtCtxEp);
    }

    public ImmutableSet<AddressEndpointKey> getAddressEndpointKeys() {
        return ImmutableSet.copyOf(adrEpKeys);
    }

    public ImmutableSet<ContainmentEndpointKey> getContainmentEndpointKeys() {
        return ImmutableSet.copyOf(contEpKeys);
    }

    public ImmutableSet<PolicyRuleGroupKey> getPolicyRuleGroupKeys() {
        return ImmutableSet.copyOf(policyRuleGrpKeys);
    }

    public @Nonnull RendererEndpoints buildRendererEndpoints() {
        Map<RendererEndpointKey, RendererEndpointBuilder> rendererEpBuilderByKey = new HashMap<>();
        for (RendererEndpointKey rendererEpKey : policiesByEpAndPeerEp.rowKeySet()) {
            RendererEndpointBuilder rendererEpBuilder =
                    resolveRendererEndpointBuilder(rendererEpKey, rendererEpBuilderByKey);
            List<PeerEndpointWithPolicy> peerEpsWithPolicy =
                    toListPeerEndpointWithPolicy(policiesByEpAndPeerEp.row(rendererEpKey));
            rendererEpBuilder.setPeerEndpointWithPolicy(peerEpsWithPolicy);
            rendererEpBuilderByKey.put(rendererEpKey, rendererEpBuilder);
        }
        for (RendererEndpointKey rendererEpKey : policiesByEpAndPeerExtEp.rowKeySet()) {
            RendererEndpointBuilder rendererEpBuilder =
                    resolveRendererEndpointBuilder(rendererEpKey, rendererEpBuilderByKey);
            List<PeerExternalEndpointWithPolicy> peerExtEpsWithPolicy =
                    toListPeerExternalEndpointWithPolicy(policiesByEpAndPeerExtEp.row(rendererEpKey));
            rendererEpBuilder.setPeerExternalEndpointWithPolicy(peerExtEpsWithPolicy);
            rendererEpBuilderByKey.put(rendererEpKey, rendererEpBuilder);
        }
        for (RendererEndpointKey rendererEpKey : policiesByEpAndPeerExtCtxEp.rowKeySet()) {
            RendererEndpointBuilder rendererEpBuilder =
                    resolveRendererEndpointBuilder(rendererEpKey, rendererEpBuilderByKey);
            List<PeerExternalContainmentEndpointWithPolicy> peerExtContEpsWithPolicy =
                    toListPeerExternalContainmentEndpointWithPolicy(policiesByEpAndPeerExtCtxEp.row(rendererEpKey));
            rendererEpBuilder.setPeerExternalContainmentEndpointWithPolicy(peerExtContEpsWithPolicy);
            rendererEpBuilderByKey.put(rendererEpKey, rendererEpBuilder);
        }
        List<RendererEndpoint> rendererEps = new ArrayList<>();
        for (RendererEndpointBuilder builder : rendererEpBuilderByKey.values()) {
            rendererEps.add(builder.build());
        }
        return new RendererEndpointsBuilder().setRendererEndpoint(rendererEps).build();
    }

    private static RendererEndpointBuilder resolveRendererEndpointBuilder(RendererEndpointKey rendererEpKey,
            Map<RendererEndpointKey, RendererEndpointBuilder> rendererEpBuilderByKey) {
        RendererEndpointBuilder rendererEpBuilder = rendererEpBuilderByKey.get(rendererEpKey);
        if (rendererEpBuilder == null) {
            rendererEpBuilder = new RendererEndpointBuilder();
            rendererEpBuilder.setKey(rendererEpKey);
        }
        return rendererEpBuilder;
    }

    private static List<PeerEndpointWithPolicy> toListPeerEndpointWithPolicy(
            Map<PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByPeerEp) {
        List<PeerEndpointWithPolicy> peerEpsWithPolicy = new ArrayList<>();
        for (Entry<PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> entry : policiesByPeerEp
            .entrySet()) {
            PeerEndpointKey peerEpKey = entry.getKey();
            PeerEndpointWithPolicyKey peerEndpointWithPolicyKey = new PeerEndpointWithPolicyKey(peerEpKey.getAddress(),
                    peerEpKey.getAddressType(), peerEpKey.getContextId(), peerEpKey.getContextType());
            PeerEndpointWithPolicy peerEndpointWithPolicy =
                    new PeerEndpointWithPolicyBuilder().setKey(peerEndpointWithPolicyKey)
                        .setRuleGroupWithRendererEndpointParticipation(new ArrayList<>(entry.getValue()))
                        .build();
            peerEpsWithPolicy.add(peerEndpointWithPolicy);
        }
        return peerEpsWithPolicy;
    }

    private static List<PeerExternalEndpointWithPolicy> toListPeerExternalEndpointWithPolicy(
            Map<PeerExternalEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByPeerExtEp) {
        List<PeerExternalEndpointWithPolicy> peerExtEpsWithPolicy = new ArrayList<>();
        for (Entry<PeerExternalEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> entry : policiesByPeerExtEp
            .entrySet()) {
            PeerExternalEndpointKey peerEpKey = entry.getKey();
            PeerExternalEndpointWithPolicyKey peerExternalEpWithPolicyKey =
                    new PeerExternalEndpointWithPolicyKey(peerEpKey.getAddress(), peerEpKey.getAddressType(),
                            peerEpKey.getContextId(), peerEpKey.getContextType());
            PeerExternalEndpointWithPolicy peerExternalEpWithPolicy =
                    new PeerExternalEndpointWithPolicyBuilder().setKey(peerExternalEpWithPolicyKey)
                        .setRuleGroupWithRendererEndpointParticipation(new ArrayList<>(entry.getValue()))
                        .build();
            peerExtEpsWithPolicy.add(peerExternalEpWithPolicy);
        }
        return peerExtEpsWithPolicy;
    }

    private static List<PeerExternalContainmentEndpointWithPolicy> toListPeerExternalContainmentEndpointWithPolicy(
            Map<PeerExternalContainmentEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByPeerExtContEp) {
        List<PeerExternalContainmentEndpointWithPolicy> peerExtContEpsWithPolicy = new ArrayList<>();
        for (Entry<PeerExternalContainmentEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> entry : policiesByPeerExtContEp
            .entrySet()) {
            PeerExternalContainmentEndpointKey peerEpKey = entry.getKey();
            PeerExternalContainmentEndpointWithPolicyKey peerExternalContEpWithPolicyKey =
                    new PeerExternalContainmentEndpointWithPolicyKey(peerEpKey.getContextId(),
                            peerEpKey.getContextType());
            PeerExternalContainmentEndpointWithPolicy peerExternalContEpWithPolicy =
                    new PeerExternalContainmentEndpointWithPolicyBuilder().setKey(peerExternalContEpWithPolicyKey)
                        .setRuleGroupWithRendererEndpointParticipation(new ArrayList<>(entry.getValue()))
                        .build();
            peerExtContEpsWithPolicy.add(peerExternalContEpWithPolicy);
        }
        return peerExtContEpsWithPolicy;
    }

    public Endpoints buildEndoints(EndpointInfo epInfo, EndpointLocationInfo epLocInfo,
            Map<InstanceIdentifier<?>, RendererName> rendererByNode) {
        List<AddressEndpointWithLocation> epsWithLoc =
                resolveEpsWithLoc(getAddressEndpointKeys(), epInfo, epLocInfo, rendererByNode);
        List<ContainmentEndpointWithLocation> contEpsWithLoc =
                resolveContEpsWithLoc(getContainmentEndpointKeys(), epInfo, epLocInfo);
        return new EndpointsBuilder().setAddressEndpointWithLocation(epsWithLoc)
            .setContainmentEndpointWithLocation(contEpsWithLoc)
            .build();
    }

    private static List<AddressEndpointWithLocation> resolveEpsWithLoc(Set<AddressEndpointKey> epKeys,
            EndpointInfo epInfo, EndpointLocationInfo epLocInfo,
            Map<InstanceIdentifier<?>, RendererName> rendererByNode) {
        List<AddressEndpointWithLocation> result = new ArrayList<>();
        for (AddressEndpointKey epKey : epKeys) {
            Optional<AddressEndpoint> potentialEp = epInfo.getEndpoint(epKey);
            Preconditions.checkArgument(potentialEp.isPresent());
            Optional<AddressEndpointLocation> potentionalEpLoc = epLocInfo.getAdressEndpointLocation(epKey);
            Preconditions.checkArgument(potentionalEpLoc.isPresent());
            RendererName rendererName = resolveRendererName(potentionalEpLoc.get(), rendererByNode);
            result.add(createEpWithLoc(potentialEp.get(), potentionalEpLoc.get(), rendererName));
        }
        return result;
    }

    private static RendererName resolveRendererName(AddressEndpointLocation epLoc,
            Map<InstanceIdentifier<?>, RendererName> rendererByNode) {
        Optional<InstanceIdentifier<?>> potentialAbsNodeLoc = EndpointLocationUtils.resolveAbsoluteNodeLocation(epLoc);
        if (potentialAbsNodeLoc.isPresent()) {
            return rendererByNode.get(potentialAbsNodeLoc.get());
        }
        return null;
    }

    private static AddressEndpointWithLocation createEpWithLoc(AddressEndpoint ep, AddressEndpointLocation epLoc,
            RendererName rendererName) {
        return new AddressEndpointWithLocationBuilder().setAddress(ep.getAddress())
            .setAddressType(ep.getAddressType())
            .setContextId(ep.getContextId())
            .setContextType(ep.getContextType())
            .setTenant(ep.getTenant())
            .setChildEndpoint(ep.getChildEndpoint())
            .setParentEndpointChoice(ep.getParentEndpointChoice())
            .setEndpointGroup(ep.getEndpointGroup())
            .setCondition(ep.getCondition())
            .setNetworkContainment(ep.getNetworkContainment())
            .setTimestamp(ep.getTimestamp())
            .setAbsoluteLocation(epLoc.getAbsoluteLocation())
            .setRelativeLocations(epLoc.getRelativeLocations())
            .setRendererName(rendererName)
            .build();
    }

    private static List<ContainmentEndpointWithLocation> resolveContEpsWithLoc(Set<ContainmentEndpointKey> contEpKeys,
            EndpointInfo epInfo, EndpointLocationInfo epLocInfo) {
        List<ContainmentEndpointWithLocation> result = new ArrayList<>();
        for (ContainmentEndpointKey contEpKey : contEpKeys) {
            Optional<ContainmentEndpoint> potentialContEp = epInfo.getContainmentEndpoint(contEpKey);
            Preconditions.checkArgument(potentialContEp.isPresent());
            Optional<ContainmentEndpointLocation> potentialContEpLoc =
                    epLocInfo.getContainmentEndpointLocation(contEpKey);
            Preconditions.checkArgument(potentialContEpLoc.isPresent());
            result.add(createContEpWithLoc(potentialContEp.get(), potentialContEpLoc.get()));
        }
        return result;
    }

    private static ContainmentEndpointWithLocation createContEpWithLoc(ContainmentEndpoint contEp,
            ContainmentEndpointLocation contEpLoc) {
        return new ContainmentEndpointWithLocationBuilder().setContextId(contEp.getContextId())
            .setContextType(contEp.getContextType())
            .setTenant(contEp.getTenant())
            .setChildEndpoint(contEp.getChildEndpoint())
            .setEndpointGroup(contEp.getEndpointGroup())
            .setCondition(contEp.getCondition())
            .setNetworkContainment(contEp.getNetworkContainment())
            .setTimestamp(contEp.getTimestamp())
            .setRelativeLocations(contEpLoc.getRelativeLocations())
            .build();
    }

    public RuleGroups buildRuluGroups(ResolvedPolicyInfo policyInfo) {
        List<RuleGroup> ruleGroups = resolveRuleGroups(getPolicyRuleGroupKeys(), policyInfo);
        return new RuleGroupsBuilder().setRuleGroup(ruleGroups).build();
    }

    private List<RuleGroup> resolveRuleGroups(Set<PolicyRuleGroupKey> policyRuleGrpKeys,
            ResolvedPolicyInfo policyInfo) {
        List<RuleGroup> result = new ArrayList<>();
        for (PolicyRuleGroupKey policyRuleGrpKey : policyRuleGrpKeys) {
            Optional<PolicyRuleGroup> potentialPolicyRuleGrp = policyInfo.getPolicyRuleGroup(policyRuleGrpKey);
            Preconditions.checkArgument(potentialPolicyRuleGrp.isPresent());
            result.add(createRuleGroup(potentialPolicyRuleGrp.get()));
        }
        return result;
    }

    private RuleGroup createRuleGroup(PolicyRuleGroup policyRuleGrp) {
        return new RuleGroupBuilder().setTenantId(policyRuleGrp.getTenantId())
            .setContractId(policyRuleGrp.getContractId())
            .setSubjectName(policyRuleGrp.getSubjectName())
            .setResolvedRule(policyRuleGrp.getResolvedRule())
            .setOrder(policyRuleGrp.getOrder())
            .build();
    }

    // TODO this copies entire Forwarding to RendererForwarding - it could copy only forwarding used
    // in EPs (renderer EPs + peers)
    public RendererForwarding buildRendererForwarding(Forwarding forwarding, Set<NetworkDomainAugmentor> augmentors) {
        List<RendererForwardingByTenant> forwardingContextByTenant =
                resolveForwardingContextByTenant(forwarding.getForwardingByTenant(), augmentors);
        return new RendererForwardingBuilder().setRendererForwardingByTenant(forwardingContextByTenant).build();
    }

    private static List<RendererForwardingByTenant> resolveForwardingContextByTenant(
            List<ForwardingByTenant> forwardingByTenant, Set<NetworkDomainAugmentor> augmentors) {
        List<RendererForwardingByTenant> result = new ArrayList<>();
        for (ForwardingByTenant fwdByTenant : forwardingByTenant) {
            result.add(resolveRendererForwardingByTenant(fwdByTenant, augmentors));
        }
        return result;
    }

    private static RendererForwardingByTenant resolveRendererForwardingByTenant(ForwardingByTenant fwdByTenant,
            Set<NetworkDomainAugmentor> augmentors) {
        List<RendererForwardingContext> rendererForwardingContexts =
                resolveRendererForwardingContexts(fwdByTenant.getForwardingContext());
        List<RendererNetworkDomain> rendererNetworkDomains =
                resolveRendererNetworkDomains(fwdByTenant.getNetworkDomain(), augmentors);
        return new RendererForwardingByTenantBuilder().setTenantId(fwdByTenant.getTenantId())
            .setRendererForwardingContext(rendererForwardingContexts)
            .setRendererNetworkDomain(rendererNetworkDomains)
            .build();
    }

    private static List<RendererForwardingContext> resolveRendererForwardingContexts(
            @Nullable List<ForwardingContext> fwdCtx) {
        if (fwdCtx == null) {
            return Collections.emptyList();
        }
        return FluentIterable.from(fwdCtx).transform(new Function<ForwardingContext, RendererForwardingContext>() {

            @Override
            public RendererForwardingContext apply(ForwardingContext input) {
                return new RendererForwardingContextBuilder().setContextId(input.getContextId())
                    .setContextType(input.getContextType())
                    .setName(input.getName())
                    .setParent(input.getParent())
                    .build();
            }
        }).toList();
    }

    private static List<RendererNetworkDomain> resolveRendererNetworkDomains(@Nullable List<NetworkDomain> netDomains,
            Set<NetworkDomainAugmentor> augmentors) {
        if (netDomains == null) {
            return Collections.emptyList();
        }
        return FluentIterable.from(netDomains).transform(new Function<NetworkDomain, RendererNetworkDomain>() {

            @Override
            public RendererNetworkDomain apply(NetworkDomain input) {
                RendererNetworkDomainBuilder rendererNetworkDomainBuilder =
                        new RendererNetworkDomainBuilder().setNetworkDomainId(input.getNetworkDomainId())
                            .setNetworkDomainType(input.getNetworkDomainType())
                            .setName(input.getName())
                            .setParent(input.getParent());
                for (NetworkDomainAugmentor augmentor : augmentors) {
                    Entry<Class<? extends Augmentation<RendererNetworkDomain>>, Augmentation<RendererNetworkDomain>> networkDomainAugmentation =
                            augmentor.buildRendererNetworkDomainAugmentation(input);
                    if (networkDomainAugmentation != null) {
                        rendererNetworkDomainBuilder.addAugmentation(networkDomainAugmentation.getKey(),
                                networkDomainAugmentation.getValue());
                    }
                }
                return rendererNetworkDomainBuilder.build();
            }
        }).toList();
    }

}
