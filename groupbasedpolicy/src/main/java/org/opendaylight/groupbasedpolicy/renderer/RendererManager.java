/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.listener.EndpointLocationsListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.EndpointsListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.ForwardingListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.RenderersListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.ResolvedPoliciesListener;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RenderersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.endpoints.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.external.containment.endpoints.PeerExternalContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.peer.external.endpoints.PeerExternalEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

public class RendererManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RendererManager.class);

    private static long version = 0;

    private final DataBroker dataProvider;
    private final NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry;
    private final Set<RendererName> processingRenderers = new HashSet<>();
    private Map<InstanceIdentifier<?>, RendererName> rendererByNode = new HashMap<>();
    private ResolvedPolicyInfo policyInfo;
    private EndpointInfo epInfo;
    private EndpointLocationInfo epLocInfo;
    private Forwarding forwarding;
    private boolean changesWaitingToProcess = false;
    private boolean currentVersionHasConfig = false;

    private final EndpointsListener endpointsListener;
    private final EndpointLocationsListener endpointLocationsListener;
    private final ResolvedPoliciesListener resolvedPoliciesListener;
    private final ForwardingListener forwardingListener;
    private final RenderersListener renderersListener;

    public RendererManager(DataBroker dataProvider, NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry) {
        this.dataProvider = checkNotNull(dataProvider);
        this.netDomainAugmentorRegistry = checkNotNull(netDomainAugmentorRegistry);
        endpointsListener = new EndpointsListener(this, dataProvider);
        endpointLocationsListener = new EndpointLocationsListener(this, dataProvider);
        resolvedPoliciesListener = new ResolvedPoliciesListener(this, dataProvider);
        forwardingListener = new ForwardingListener(this, dataProvider);
        renderersListener = new RenderersListener(this, dataProvider);
    }

    public synchronized void endpointsUpdated(final Endpoints endpoints) {
        epInfo = new EndpointInfo(endpoints);
        changesWaitingToProcess = true;
        processState();
    }

    public synchronized void endpointLocationsUpdated(final EndpointLocations epLocations) {
        epLocInfo = new EndpointLocationInfo(epLocations);
        changesWaitingToProcess = true;
        processState();
    }

    public synchronized void resolvedPoliciesUpdated(final ResolvedPolicies resolvedPolicies) {
        policyInfo = new ResolvedPolicyInfo(resolvedPolicies);
        changesWaitingToProcess = true;
        processState();
    }

    public synchronized void forwardingUpdated(final Forwarding forwarding) {
        this.forwarding = forwarding;
        changesWaitingToProcess = true;
        processState();
    }

    public synchronized void renderersUpdated(final Renderers renderersCont) {
        ImmutableMultimap<InstanceIdentifier<?>, RendererName> renderersByNode =
                RendererUtils.resolveRenderersByNodes(renderersCont.getRenderer());
        rendererByNode = new HashMap<>();
        for (InstanceIdentifier<?> nodePath : renderersByNode.keySet()) {
            ImmutableCollection<RendererName> renderers = renderersByNode.get(nodePath);
            // only first renderer is used
            rendererByNode.put(nodePath, renderers.asList().get(0));
        }
        if (processingRenderers.isEmpty()) {
            changesWaitingToProcess = true;
        } else {
            LOG.debug("Waiting for renderers. Version {} needs to be processed by renderers: {}", version,
                    processingRenderers);
            ImmutableMap<RendererName, Renderer> rendererByName =
                    RendererUtils.resolveRendererByName(renderersCont.getRenderer());
            for (RendererName configuredRenderer : processingRenderers) {
                Renderer renderer = rendererByName.get(configuredRenderer);
                RendererPolicy rendererPolicy = renderer.getRendererPolicy();
                if (rendererPolicy != null && rendererPolicy.getVersion() != null
                        && renderer.getRendererPolicy().getVersion().equals(version)) {
                    processingRenderers.remove(configuredRenderer);
                    Status status = rendererPolicy.getStatus();
                    if (status != null && status.getUnconfiguredRule() != null) {
                        LOG.warn("Renderer {} did not configure policy with version {} successfully. \n{}",
                                configuredRenderer.getValue(), version, status);
                    } else {
                        LOG.debug("Renderer {} configured policy with version {} successfully.",
                                configuredRenderer.getValue(), version);
                    }
                }
            }
        }
        processState();
    }

    private void processState() {
        if (!processingRenderers.isEmpty()) {
            LOG.debug("Waiting for renderers. Version {} needs to be processed by renderers: {}", version,
                    processingRenderers);
            return;
        }
        if (rendererByNode.values().isEmpty()) {
            return;
        }
        if (!changesWaitingToProcess) {
            return;
        }
        Map<RendererName, RendererConfigurationBuilder> rendererConfigBuilderByRendererName = createRendererConfigBuilders();
        Set<RendererName> rendererNames = new HashSet<>(rendererByNode.values());
        boolean newVersionHasConfig = false;
        Map<RendererName, Optional<Configuration>> configsByRendererName = new HashMap<>();
        for (RendererName rendererName : rendererNames) {
            RendererConfigurationBuilder rendererPolicyBuilder = rendererConfigBuilderByRendererName.get(rendererName);
            Optional<Configuration> potentialConfig = createConfiguration(rendererPolicyBuilder);
            if (potentialConfig.isPresent()) {
                newVersionHasConfig = true;
            }
            configsByRendererName.put(rendererName, potentialConfig);
        }
        if (newVersionHasConfig || currentVersionHasConfig) {
            version++;
            if (!writeRenderersConfigs(configsByRendererName)) {
                LOG.warn("Version {} was not dispatched successfully. Previous version is valid until next update.",
                        version);
                for (RendererName rendererName : rendererConfigBuilderByRendererName.keySet()) {
                    processingRenderers.remove(rendererName);
                }
                version--;
                changesWaitingToProcess = true;
                return;
            } else {
                currentVersionHasConfig = newVersionHasConfig;
            }
        }
        changesWaitingToProcess = false;
    }

    private boolean writeRenderersConfigs(Map<RendererName, Optional<Configuration>> configsByRendererName) {
        List<Renderer> renderers = new ArrayList<>();
        for (RendererName rendererName : configsByRendererName.keySet()) {
            RendererPolicy rendererPolicy = null;
            if (configsByRendererName.get(rendererName).isPresent()) {
                rendererPolicy = new RendererPolicyBuilder().setVersion(version)
                    .setConfiguration(configsByRendererName.get(rendererName).get())
                    .build();

            } else {
                rendererPolicy = new RendererPolicyBuilder().setVersion(version).build();
            }
            renderers.add(new RendererBuilder().setName(rendererName).setRendererPolicy(rendererPolicy).build());
            processingRenderers.add(rendererName);
            LOG.debug("Created configuration for renderer {} with version {}", rendererName.getValue(), version);
        }
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Renderers.class),
                new RenderersBuilder().setRenderer(renderers).build());
        return DataStoreHelper.submitToDs(wTx);
    }

    /**
     * Entry is added to the result map only if:<br>
     * 1. There is at least one Address EP with absolute location
     * 2. There is a renderer responsible for that EP
     *
     * @return
     */
    private Map<RendererName, RendererConfigurationBuilder> createRendererConfigBuilders() {
        if (!isStateValid()) {
            return Collections.emptyMap();
        }
        Map<RendererName, RendererConfigurationBuilder> rendererConfigBuilderByRendererName = new HashMap<>();
        for (InstanceIdentifier<?> absEpLocation : epLocInfo.getAllAbsoluteNodeLocations()) {
            RendererName rendererName = rendererByNode.get(absEpLocation);
            if (rendererName == null) {
                LOG.trace("Renderer does not exist for EP with location: {}", absEpLocation);
                continue;
            }
            RendererConfigurationBuilder rendererConfigBuilder = rendererConfigBuilderByRendererName.get(rendererName);
            if (rendererConfigBuilder == null) {
                rendererConfigBuilder = new RendererConfigurationBuilder();
                rendererConfigBuilderByRendererName.put(rendererName, rendererConfigBuilder);
            }
            for (AddressEndpointKey rendererAdrEpKey : epLocInfo.getAddressEpsWithAbsoluteNodeLocation(absEpLocation)) {
                Optional<AddressEndpoint> potentialAddressEp = epInfo.getEndpoint(rendererAdrEpKey);
                if (!potentialAddressEp.isPresent()) {
                    LOG.trace("Endpoint does not exist but has location: {}", rendererAdrEpKey);
                    continue;
                }
                AddressEndpoint rendererAdrEp = potentialAddressEp.get();
                resolveRendererConfigForEndpoint(rendererAdrEp, rendererConfigBuilder);
            }
        }
        return rendererConfigBuilderByRendererName;
    }

    private boolean isStateValid() {
        if (rendererByNode.isEmpty() || policyInfo == null || epInfo == null || epLocInfo == null
                || forwarding == null) {
            return false;
        }
        return true;
    }

    private Optional<Configuration> createConfiguration(@Nullable RendererConfigurationBuilder rendererPolicyBuilder) {
        if (rendererPolicyBuilder == null) {
            return Optional.absent();
        }
        ConfigurationBuilder configBuilder = new ConfigurationBuilder();
        RendererEndpoints rendererEndpoints = rendererPolicyBuilder.buildRendererEndpoints();
        if (isRendererEndpointsEmpty(rendererEndpoints)) {
            return Optional.absent();
        }
        configBuilder.setRendererEndpoints(rendererEndpoints);

        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints endpoints =
                rendererPolicyBuilder.buildEndoints(epInfo, epLocInfo, rendererByNode);
        configBuilder.setEndpoints(endpoints);

        RuleGroups ruleGroups = rendererPolicyBuilder.buildRuluGroups(policyInfo);
        configBuilder.setRuleGroups(ruleGroups);

        RendererForwarding rendererForwarding = rendererPolicyBuilder.buildRendererForwarding(forwarding,
                netDomainAugmentorRegistry.getEndpointAugmentors());
        configBuilder.setRendererForwarding(rendererForwarding);

        return Optional.of(configBuilder.build());
    }

    private boolean isRendererEndpointsEmpty(RendererEndpoints rendererEndpoints) {
        if (rendererEndpoints == null || rendererEndpoints.getRendererEndpoint() == null
                || rendererEndpoints.getRendererEndpoint().isEmpty()) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    void resolveRendererConfigForEndpoint(AddressEndpoint rendererAdrEp,
            RendererConfigurationBuilder rendererPolicyBuilder) {
        Set<EpgKeyDto> rendererEpgs = toEpgKeys(rendererAdrEp.getEndpointGroup(), rendererAdrEp.getTenant());
        RendererEndpointKey rendererEpKey = AddressEndpointUtils.toRendererEpKey(rendererAdrEp.getKey());
        for (EpgKeyDto rendererEpg : rendererEpgs) {
            ImmutableSet<ConsEpgKey> consPeerEpgs = policyInfo.findConsumerPeers(rendererEpg);
            for (ConsEpgKey consPeerEpg : consPeerEpgs) {
                Optional<ResolvedPolicy> potentialPolicy = policyInfo.findPolicy(consPeerEpg, rendererEpg);
                ResolvedPolicy policy = potentialPolicy.get();
                ImmutableSet<AddressEndpointKey> consPeerAdrEps = epInfo.findAddressEpsWithEpg(consPeerEpg);
                resolveRendererPolicyBetweenEpAndPeers(rendererEpKey, consPeerAdrEps, policy,
                        EndpointPolicyParticipation.PROVIDER, rendererPolicyBuilder);
                ImmutableSet<ContainmentEndpointKey> consPeerContEps = epInfo.findContainmentEpsWithEpg(consPeerEpg);
                resolveRendererPolicyBetweenEpAndContPeers(rendererEpKey, consPeerContEps, policy,
                        EndpointPolicyParticipation.PROVIDER, rendererPolicyBuilder);
            }
            ImmutableSet<ProvEpgKey> provPeerEpgs = policyInfo.findProviderPeers(rendererEpg);
            for (ProvEpgKey provPeerEpg : provPeerEpgs) {
                Optional<ResolvedPolicy> potentialPolicy = policyInfo.findPolicy(rendererEpg, provPeerEpg);
                ResolvedPolicy policy = potentialPolicy.get();
                ImmutableSet<AddressEndpointKey> provPeerAdrEps = epInfo.findAddressEpsWithEpg(provPeerEpg);
                resolveRendererPolicyBetweenEpAndPeers(rendererEpKey, provPeerAdrEps, policy,
                        EndpointPolicyParticipation.CONSUMER, rendererPolicyBuilder);
                ImmutableSet<ContainmentEndpointKey> provPeerContEps = epInfo.findContainmentEpsWithEpg(provPeerEpg);
                resolveRendererPolicyBetweenEpAndContPeers(rendererEpKey, provPeerContEps, policy,
                        EndpointPolicyParticipation.CONSUMER, rendererPolicyBuilder);
            }
        }
    }

    private void resolveRendererPolicyBetweenEpAndContPeers(RendererEndpointKey rendererEpKey,
            Set<ContainmentEndpointKey> peerContEps, ResolvedPolicy policy,
            EndpointPolicyParticipation rendererEpParticipation, RendererConfigurationBuilder rendererPolicyBuilder) {
        if (isRendererEpInEig(policy, rendererEpParticipation)) {
            LOG.info("Renderer EP cannot be in EIG therefore it is ignored: {}. \nPolicy: {}", rendererEpKey);
            LOG.debug("Renderer EP participation: {}, Policy: {}", rendererEpParticipation, policy);
            return;
        }
        for (ContainmentEndpointKey peerContEpKey : peerContEps) {
            ExternalImplicitGroup eig = policy.getExternalImplicitGroup();
            if (eig != null) { // peers are in EIG
                if (!epLocInfo.hasRelativeLocation(peerContEpKey)) {
                    LOG.debug("EIG Containment Peer does not have relative location therefore it is ignored: {}",
                            peerContEpKey);
                    continue;
                }
                PeerExternalContainmentEndpointKey peerExtContEpKey =
                        ContainmentEndpointUtils.toPeerExtContEpKey(peerContEpKey);
                for (PolicyRuleGroupWithEndpointConstraints ruleGrpsWithEpConstraints : policy
                    .getPolicyRuleGroupWithEndpointConstraints()) {
                    // TODO filter based on endpoint constraints
                    for (PolicyRuleGroup ruleGrp : ruleGrpsWithEpConstraints.getPolicyRuleGroup()) {
                        rendererPolicyBuilder.add(rendererEpKey, peerExtContEpKey, ruleGrp.getKey(),
                                rendererEpParticipation);
                    }
                }
            } else {
                LOG.info("Peer Containment EP cannot be in other EPG than EIG therefore it is ignored: {}",
                        peerContEpKey);
            }
        }
    }

    private void resolveRendererPolicyBetweenEpAndPeers(RendererEndpointKey rendererEpKey,
            Set<AddressEndpointKey> peerAdrEps, ResolvedPolicy policy,
            EndpointPolicyParticipation rendererEpParticipation, RendererConfigurationBuilder rendererPolicyBuilder) {
        if (isRendererEpInEig(policy, rendererEpParticipation)) {
            LOG.info("Renderer EP cannot be in EIG therefore it is ignored: {}. \nPolicy: {}", rendererEpKey);
            LOG.debug("Renderer EP participation: {}, Policy: {}", rendererEpParticipation, policy);
            return;
        }
        for (AddressEndpointKey peerAdrEpKey : peerAdrEps) {
            if (isSameKeys(rendererEpKey, peerAdrEpKey)) {
                continue;
            }
            ExternalImplicitGroup eig = policy.getExternalImplicitGroup();
            if (eig != null) {
                if (!epLocInfo.hasRelativeLocation(peerAdrEpKey)) {
                    LOG.debug("EIG Peer does not have relative location therefore it is ignored: {}", peerAdrEpKey);
                    continue;
                }
                PeerExternalEndpointKey peerExtEpKey = AddressEndpointUtils.toPeerExtEpKey(peerAdrEpKey);
                for (PolicyRuleGroupWithEndpointConstraints ruleGrpsWithEpConstraints : policy
                    .getPolicyRuleGroupWithEndpointConstraints()) {
                    // TODO filter based on endpoint constraints
                    for (PolicyRuleGroup ruleGrp : ruleGrpsWithEpConstraints.getPolicyRuleGroup()) {
                        rendererPolicyBuilder.add(rendererEpKey, peerExtEpKey, ruleGrp.getKey(),
                                rendererEpParticipation);
                    }
                }
            } else {
                if (!epLocInfo.hasRealLocation(peerAdrEpKey)) {
                    LOG.debug("Peer does not have real location therefore it is ignored: {}", peerAdrEpKey);
                    continue;
                }
                PeerEndpointKey peerEpKey = AddressEndpointUtils.toPeerEpKey(peerAdrEpKey);
                for (PolicyRuleGroupWithEndpointConstraints ruleGrpsWithEpConstraints : policy
                    .getPolicyRuleGroupWithEndpointConstraints()) {
                    // TODO filter based on endpoint constraints
                    for (PolicyRuleGroup ruleGrp : ruleGrpsWithEpConstraints.getPolicyRuleGroup()) {
                        rendererPolicyBuilder.add(rendererEpKey, peerEpKey, ruleGrp.getKey(), rendererEpParticipation);
                    }
                }
            }
        }
    }

    private boolean isRendererEpInEig(ResolvedPolicy policy, EndpointPolicyParticipation rendererEpParticipation) {
        ExternalImplicitGroup eig = policy.getExternalImplicitGroup();
        if (rendererEpParticipation == EndpointPolicyParticipation.PROVIDER
                && ExternalImplicitGroup.ProviderEpg == eig) {
            return true;
        } else if (rendererEpParticipation == EndpointPolicyParticipation.CONSUMER
                && ExternalImplicitGroup.ConsumerEpg == eig) {
            return true;
        }
        return false;
    }

    private boolean isSameKeys(RendererEndpointKey rendererEpKey, AddressEndpointKey peerAdrEpKey) {
        if (rendererEpKey.getAddress().equals(peerAdrEpKey.getAddress())
                && rendererEpKey.getAddressType().equals(peerAdrEpKey.getAddressType())
                && rendererEpKey.getContextId().equals(peerAdrEpKey.getContextId())
                && rendererEpKey.getContextType().equals(peerAdrEpKey.getContextType())) {
            return true;
        }
        return false;
    }

    private Set<EpgKeyDto> toEpgKeys(List<EndpointGroupId> epgIds, TenantId tenantId) {
        return FluentIterable.from(epgIds).transform(new Function<EndpointGroupId, EpgKeyDto>() {

            @Override
            public EpgKeyDto apply(EndpointGroupId input) {
                return new EpgKeyDto(input, tenantId);
            }
        }).toSet();
    }

    @VisibleForTesting
    Set<RendererName> getProcessingRenderers() {
        return processingRenderers;
    }

    @VisibleForTesting
    static void resetVersion() {
        version = 0;
    }

    @Override
    public void close() throws Exception {
        endpointsListener.close();
        endpointLocationsListener.close();
        resolvedPoliciesListener.close();
        forwardingListener.close();
        renderersListener.close();
    }

}
