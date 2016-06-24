/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;

public class KeyFactory {

    private KeyFactory() {}

    public static AddressEndpointWithLocationKey addressEndpointWithLocationKey(RendererEndpointKey fromKey) {
        return new AddressEndpointWithLocationKey(fromKey.getAddress(), fromKey.getAddressType(),
                fromKey.getContextId(), fromKey.getContextType());
    }

    public static PeerEndpointKey peerEndpointKey(PeerEndpointKey fromKey) {
        return new PeerEndpointKey(fromKey.getAddress(), fromKey.getAddressType(), fromKey.getContextId(),
                fromKey.getContextType());
    }

    public static RuleGroupKey ruleGroupKey(RuleGroupWithRendererEndpointParticipationKey fromKey) {
        return new RuleGroupKey(fromKey.getContractId(), fromKey.getSubjectName(), fromKey.getTenantId());
    }

    public static AddressEndpointKey addressEndpointKey(RendererEndpointKey fromKey) {
        return new AddressEndpointKey(fromKey.getAddress(), fromKey.getAddressType(), fromKey.getContextId(),
                fromKey.getContextType());
    }

    public static ProviderAddressEndpointLocationKey providerAddressEndpointLocationKey(
            AddressEndpointWithLocationKey fromKey) {
        return new ProviderAddressEndpointLocationKey(fromKey.getAddress(), fromKey.getAddressType(),
                fromKey.getContextId(), fromKey.getContextType());
    }

    public static RendererEndpointKey rendererEndpointKey(AddressEndpointWithLocationKey fromKey) {
        return new RendererEndpointKey(fromKey.getAddress(), fromKey.getAddressType(), fromKey.getContextId(),
                fromKey.getContextType());
    }

    public static PeerEndpointKey peerEndpointKey(AddressEndpointWithLocationKey fromKey) {
        return new PeerEndpointKey(fromKey.getAddress(), fromKey.getAddressType(), fromKey.getContextId(),
                fromKey.getContextType());
    }

}
