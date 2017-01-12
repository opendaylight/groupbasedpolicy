/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointKey;

public class AddressEndpointUtils {

    public static RendererEndpointKey toRendererEpKey(AddressEndpointKey rendererAdrEpKey) {
        return new RendererEndpointKey(rendererAdrEpKey.getAddress(), rendererAdrEpKey.getAddressType(),
                rendererAdrEpKey.getContextId(), rendererAdrEpKey.getContextType());
    }

    public static PeerEndpointKey toPeerEpKey(AddressEndpointKey peerAdrEpKey) {
        return new PeerEndpointKey(peerAdrEpKey.getAddress(), peerAdrEpKey.getAddressType(),
                peerAdrEpKey.getContextId(), peerAdrEpKey.getContextType());
    }

    public static PeerExternalEndpointKey toPeerExtEpKey(AddressEndpointKey peerAdrEpKey) {
        return new PeerExternalEndpointKey(peerAdrEpKey.getAddress(), peerAdrEpKey.getAddressType(),
                peerAdrEpKey.getContextId(), peerAdrEpKey.getContextType());
    }

    public static AddressEndpointKey fromRendererEpKey(RendererEndpointKey rendererEpKey) {
        return new AddressEndpointKey(rendererEpKey.getAddress(), rendererEpKey.getAddressType(),
                rendererEpKey.getContextId(), rendererEpKey.getContextType());
    }

    public static AddressEndpointKey fromPeerEpKey(PeerEndpointKey peerEpKey) {
        return new AddressEndpointKey(peerEpKey.getAddress(), peerEpKey.getAddressType(), peerEpKey.getContextId(),
                peerEpKey.getContextType());
    }

    public static AddressEndpointKey fromPeerExtEpKey(PeerExternalEndpointKey peerExtEpKey) {
        return new AddressEndpointKey(peerExtEpKey.getAddress(), peerExtEpKey.getAddressType(),
                peerExtEpKey.getContextId(), peerExtEpKey.getContextType());
    }
}
