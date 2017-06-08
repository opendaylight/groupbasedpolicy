/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.base_endpoint.EndpointAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.listener.EndpointLocationsListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.EndpointsListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.ForwardingListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.RenderersListener;
import org.opendaylight.groupbasedpolicy.renderer.listener.ResolvedPoliciesListener;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.util.ContainmentEndpointUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RendererManager.class);

    private static long version = 0;

    private final DataBroker dataProvider;
    private final NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry;
    private final EndpointAugmentorRegistryImpl epAugmentorRegistry;
    private final Set<RendererName> processingRenderers = new HashSet<>();
    private InputState currentState = new InputState();
    private InputState configuredState;
    private boolean currentVersionHasConfig = false;

    private final EndpointsListener endpointsListener;
    private final EndpointLocationsListener endpointLocationsListener;
    private final ResolvedPoliciesListener resolvedPoliciesListener;
    private final ForwardingListener forwardingListener;
    private final RenderersListener renderersListener;

    private static final class InputState {

        private ResolvedPolicyInfo policyInfo;
        private EndpointInfo epInfo;
        private EndpointLocationInfo epLocInfo;
        private Forwarding forwarding;
        private Map<InstanceIdentifier<?>, RendererName> rendererByNode = new HashMap<>();

        private boolean isValid() {
            if (rendererByNode.isEmpty() || policyInfo == null || epInfo == null || epLocInfo == null
                    || forwarding == null) {
                return false;
            }
            return true;
        }

        private InputState createCopy() {
            InputState copy = new InputState();
            copy.policyInfo = this.policyInfo;
            copy.epInfo = this.epInfo;
            copy.epLocInfo = this.epLocInfo;
            copy.forwarding = this.forwarding;
            copy.rendererByNode = ImmutableMap.copyOf(rendererByNode);
            return copy;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((epInfo == null) ? 0 : epInfo.hashCode());
            result = prime * result + ((epLocInfo == null) ? 0 : epLocInfo.hashCode());
            result = prime * result + ((forwarding == null) ? 0 : forwarding.hashCode());
            result = prime * result + ((policyInfo == null) ? 0 : policyInfo.hashCode());
            result = prime * result + ((rendererByNode == null) ? 0 : rendererByNode.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            InputState other = (InputState) obj;
            if (epInfo == null) {
                if (other.epInfo != null)
                    return false;
            } else if (!epInfo.equals(other.epInfo))
                return false;
            if (epLocInfo == null) {
                if (other.epLocInfo != null)
                    return false;
            } else if (!epLocInfo.equals(other.epLocInfo))
                return false;
            if (forwarding == null) {
                if (other.forwarding != null)
                    return false;
            } else if (!DtoEquivalenceUtils.equalsForwarding(forwarding, other.forwarding))
                return false;
            if (policyInfo == null) {
                if (other.policyInfo != null)
                    return false;
            } else if (!policyInfo.equals(other.policyInfo))
                return false;
            if (rendererByNode == null) {
                if (other.rendererByNode != null)
                    return false;
            } else if (!rendererByNode.equals(other.rendererByNode))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "InputState [policyInfo=" + policyInfo + ", epInfo=" + epInfo + ", epLocInfo=" + epLocInfo
                    + ", forwarding=" + forwarding + ", rendererByNode=" + rendererByNode + ", isValid()=" + isValid()
                    + "]";
        }

    }

    public RendererManager(DataBroker dataProvider, NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry,
                           EndpointAugmentorRegistryImpl epAugmentorRegistry) {
        this.dataProvider = checkNotNull(dataProvider);
        this.netDomainAugmentorRegistry = checkNotNull(netDomainAugmentorRegistry);
        this.epAugmentorRegistry = checkNotNull(epAugmentorRegistry);
        endpointsListener = new EndpointsListener(this, dataProvider);
        endpointLocationsListener = new EndpointLocationsListener(this, dataProvider);
        resolvedPoliciesListener = new ResolvedPoliciesListener(this, dataProvider);
        forwardingListener = new ForwardingListener(this, dataProvider);
        renderersListener = new RenderersListener(this, dataProvider);
    }

    public synchronized void endpointsUpdated(final Endpoints endpoints) {
        currentState.epInfo = new EndpointInfo(endpoints);
        processState();
    }

    public synchronized void endpointLocationsUpdated(final EndpointLocations epLocations) {
        currentState.epLocInfo = new EndpointLocationInfo(epLocations);
        processState();
    }

    public synchronized void resolvedPoliciesUpdated(final ResolvedPolicies resolvedPolicies) {
        currentState.policyInfo = new ResolvedPolicyInfo(resolvedPolicies);
        processState();
    }

    public synchronized void forwardingUpdated(final Forwarding forwarding) {
        currentState.forwarding = forwarding;
        processState();
    }

    public synchronized void renderersUpdated(final Renderers renderersCont) {
        ImmutableMultimap<InstanceIdentifier<?>, RendererName> renderersByNode =
                RendererUtils.resolveRenderersByNodes(renderersCont.getRenderer());
        currentState.rendererByNode = new HashMap<>();
        for (InstanceIdentifier<?> nodePath : renderersByNode.keySet()) {
            ImmutableCollection<RendererName> renderers = renderersByNode.get(nodePath);
            // only first renderer is used
            currentState.rendererByNode.put(nodePath, renderers.asList().get(0));
        }
        if (!processingRenderers.isEmpty()) {
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
                    if (status != null && status.getUnconfiguredEndpoints() != null
                            && status.getUnconfiguredEndpoints().getUnconfiguredRendererEndpoint() != null
                            && !status.getUnconfiguredEndpoints().getUnconfiguredRendererEndpoint().isEmpty()) {
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
        if (currentState.equals(configuredState)) {
            LOG.trace("Nothing was changed in config for renderers {}", currentState);
            return;
        }
        Map<RendererName, RendererConfigurationBuilder> rendererConfigBuilderByRendererName =
                createRendererConfigBuilders();
        Set<RendererName> rendererNames = new HashSet<>(currentState.rendererByNode.values());
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
                return;
            } else {
                currentVersionHasConfig = newVersionHasConfig;
                configuredState = currentState.createCopy();
            }
        }
    }

    private boolean writeRenderersConfigs(Map<RendererName, Optional<Configuration>> configsByRendererName) {
        List<Renderer> renderers = new ArrayList<>();
        for (RendererName rendererName : configsByRendererName.keySet()) {
            RendererPolicy rendererPolicy;
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
        if (!currentState.isValid()) {
            return Collections.emptyMap();
        }
        Map<RendererName, RendererConfigurationBuilder> rendererConfigBuilderByRendererName = new HashMap<>();
        for (InstanceIdentifier<?> epLocation : currentState.epLocInfo.getAllExternalNodeLocations()) {
            RendererName rendererName = currentState.rendererByNode.get(epLocation);
            if (rendererName == null) {
                LOG.trace("Renderer does not exist for EP with location: {}", epLocation);
                continue;
            }
            RendererConfigurationBuilder rendererConfigBuilder = rendererConfigBuilderByRendererName.get(rendererName);
            if (rendererConfigBuilder == null) {
                rendererConfigBuilder = new RendererConfigurationBuilder();
                rendererConfigBuilderByRendererName.put(rendererName, rendererConfigBuilder);
            }
            for (AddressEndpointKey rendererAdrEpKey : currentState.epLocInfo
                    .getAddressEpsWithExternalNodeLocation(epLocation)) {
                Optional<AddressEndpoint> potentialAddressEp = currentState.epInfo.getEndpoint(rendererAdrEpKey);
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
                rendererPolicyBuilder.buildEndpoints(currentState.epInfo, currentState.epLocInfo,
                        currentState.rendererByNode, epAugmentorRegistry.getEndpointAugmentors());
        configBuilder.setEndpoints(endpoints);

        RuleGroups ruleGroups = rendererPolicyBuilder.buildRuleGroups(currentState.policyInfo);
        configBuilder.setRuleGroups(ruleGroups);

        RendererForwarding rendererForwarding = rendererPolicyBuilder.buildRendererForwarding(currentState.forwarding,
                netDomainAugmentorRegistry.getNetworkDomainAugmentors());
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
    void resolveRendererConfigForEndpoint(@Nonnull AddressEndpoint rendererAdrEp,
                                          @Nonnull RendererConfigurationBuilder rendererPolicyBuilder) {
        if (rendererAdrEp.getEndpointGroup() == null
                || rendererAdrEp.getEndpointGroup().contains(RendererUtils.EPG_EXTERNAL_ID)) {
            return;
        }
        Set<EpgKeyDto> rendererEpgs = toEpgKeys(rendererAdrEp.getEndpointGroup(), rendererAdrEp.getTenant());
        RendererEndpointKey rendererEpKey = AddressEndpointUtils.toRendererEpKey(rendererAdrEp.getKey());
        for (EpgKeyDto rendererEpg : rendererEpgs) {
            ImmutableSet<ConsEpgKey> consPeerEpgs = currentState.policyInfo.findConsumerPeers(rendererEpg);
            for (ConsEpgKey consPeerEpg : consPeerEpgs) {
                Optional<ResolvedPolicy> potentialPolicy = currentState.policyInfo.findPolicy(consPeerEpg, rendererEpg);
                ResolvedPolicy policy = potentialPolicy.get();
                ImmutableSet<AddressEndpointKey> consPeerAdrEps =
                        currentState.epInfo.findAddressEpsWithEpg(consPeerEpg);
                resolveRendererPolicyBetweenEpAndPeers(rendererEpKey, consPeerAdrEps, policy,
                        EndpointPolicyParticipation.PROVIDER, rendererPolicyBuilder);
                ImmutableSet<ContainmentEndpointKey> consPeerContEps =
                        currentState.epInfo.findContainmentEpsWithEpg(consPeerEpg);
                resolveRendererPolicyBetweenEpAndContPeers(rendererEpKey, consPeerContEps, policy,
                        EndpointPolicyParticipation.PROVIDER, rendererPolicyBuilder);
            }
            ImmutableSet<ProvEpgKey> provPeerEpgs = currentState.policyInfo.findProviderPeers(rendererEpg);
            for (ProvEpgKey provPeerEpg : provPeerEpgs) {
                Optional<ResolvedPolicy> potentialPolicy = currentState.policyInfo.findPolicy(rendererEpg, provPeerEpg);
                ResolvedPolicy policy = potentialPolicy.get();
                ImmutableSet<AddressEndpointKey> provPeerAdrEps =
                        currentState.epInfo.findAddressEpsWithEpg(provPeerEpg);
                resolveRendererPolicyBetweenEpAndPeers(rendererEpKey, provPeerAdrEps, policy,
                        EndpointPolicyParticipation.CONSUMER, rendererPolicyBuilder);
                ImmutableSet<ContainmentEndpointKey> provPeerContEps =
                        currentState.epInfo.findContainmentEpsWithEpg(provPeerEpg);
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
                if (!currentState.epLocInfo.hasRelativeLocation(peerContEpKey)) {
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
            // external peers
            if (policy.getExternalImplicitGroup() != null) {
                if (!currentState.epLocInfo.hasRelativeLocation(peerAdrEpKey)) {
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
                // peers within virtual domain
                if (!currentState.epLocInfo.hasAbsoluteLocation(peerAdrEpKey)
                        && !currentState.epLocInfo.hasRelativeLocation(peerAdrEpKey)) {
                    LOG.debug("Peer does not have absolute nor relative location therefore it is ignored: {}",
                            peerAdrEpKey);
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
    public void close() {
        try {
            endpointsListener.close();
            endpointLocationsListener.close();
            resolvedPoliciesListener.close();
            forwardingListener.close();
            renderersListener.close();
        } catch (Exception e) {
            LOG.warn("Exception while closing", e);
        }
    }

}
