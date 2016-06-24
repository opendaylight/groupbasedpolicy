/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.UnconfiguredRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.UnconfiguredRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.UnconfiguredRendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.UnconfiguredRendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.unconfigured.renderer.endpoint.UnconfiguredPeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.unconfigured.renderer.endpoint.UnconfiguredPeerEndpointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: assembly methods for {@link Status}.
 */
public class StatusUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StatusUtil.class);

    private StatusUtil() {
        throw new IllegalAccessError("instance of util class not supported");
    }

    /**
     * @param context holder of actual configuration state
     * @param info    detailed message for not configurable item
     * @return ful collection of not configurable items under given endpoint
     */
    public static UnconfiguredRendererEndpoint assembleFullyNotConfigurableRendererEP(final PolicyConfigurationContext context,
                                                                                      final String info) {
        final RendererEndpoint rendererEndpoint = context.getCurrentRendererEP();
        LOG.trace("fully not configurable EP: {}", info);
        return new UnconfiguredRendererEndpointBuilder(rendererEndpoint)
                .setUnconfiguredPeerEndpoint(assemblePeerEndpoint(rendererEndpoint.getPeerEndpoint().stream()))
                .setInfo(info)
                .build();
    }

    /**
     * @param context holder of actual configuration state
     * @param info    detailed message for not configurable item
     * @return filtered collection of not configurable items under given endpoint and peer
     */
    public static UnconfiguredRendererEndpoint assembleNotConfigurableRendererEPForPeer(final PolicyConfigurationContext context,
                                                                                        final PeerEndpoint peerEndpoint,
                                                                                        final String info) {
        final RendererEndpoint rendererEndpoint = context.getCurrentRendererEP();
        LOG.trace("not configurable EP for peer: {}", info);
        return new UnconfiguredRendererEndpointBuilder(rendererEndpoint)
                .setUnconfiguredPeerEndpoint(assemblePeerEndpoint(Stream.of(peerEndpoint)))
                .setInfo(info)
                .build();
    }

    @VisibleForTesting
    static List<UnconfiguredPeerEndpoint> assemblePeerEndpoint(final Stream<PeerEndpoint> peerEndpoint) {
        return peerEndpoint
                .map((peerEP) -> new UnconfiguredPeerEndpointBuilder(peerEP)
                        .setUnconfiguredRuleGroup(
                                assembleRuleGroups(peerEP.getRuleGroupWithRendererEndpointParticipation().stream())
                        ).build())
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    static List<UnconfiguredRuleGroup> assembleRuleGroups(final Stream<RuleGroupWithRendererEndpointParticipation> stream) {
        return stream
                .filter(Objects::nonNull)
                .map((ruleGroup) -> new UnconfiguredRuleGroupBuilder(ruleGroup)
                        .setRendererEndpointParticipation(ruleGroup.getRendererEndpointParticipation())
                        // TODO: find rule-group and append names of resolved rules ...setUnconfiguredResolvedRule()
                        .build())
                .collect(Collectors.toList());
    }
}
