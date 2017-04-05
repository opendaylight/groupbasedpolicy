/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.util;

import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
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

    public static AddressEndpointKey fromAddressEndpointWithLocationKey(AddressEndpointWithLocationKey key) {
        return new AddressEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    public static AddressEndpointKey fromPeerExtEpKey(PeerExternalEndpointKey peerExtEpKey) {
        return new AddressEndpointKey(peerExtEpKey.getAddress(), peerExtEpKey.getAddressType(),
                peerExtEpKey.getContextId(), peerExtEpKey.getContextType());
    }

    /**
     * Compares absolute external locations of address end-points in the arguments.
     */
    public static boolean sameExternalLocationCase(AddressEndpointWithLocation ae0, AddressEndpointWithLocation ae1) {
        if (ae0.getAbsoluteLocation() == null || ae1.getAbsoluteLocation() == null) {
            return false;
        }
        Optional<LocationType> loc0Type = Optional.ofNullable(ae0.getAbsoluteLocation().getLocationType());
        Optional<LocationType> loc1Type = Optional.ofNullable(ae1.getAbsoluteLocation().getLocationType());
        if (!(loc0Type.isPresent() && loc0Type.get() instanceof ExternalLocationCase)
                || !(loc1Type.isPresent() && loc1Type.get() instanceof ExternalLocationCase)) {
            return false;
        }
        ExternalLocationCase loc0 = (ExternalLocationCase) loc0Type.get();
        ExternalLocationCase loc1 = (ExternalLocationCase) loc1Type.get();
        return (loc0.getExternalNodeMountPoint() == null || loc1.getExternalNodeMountPoint() == null
                || loc0.getExternalNodeConnector() == null || loc1.getExternalNodeConnector() == null) ? false : loc0
                    .getExternalNodeMountPoint().toString().equals(loc1.getExternalNodeMountPoint().toString())
                        && loc0.getExternalNodeConnector().equals(loc1.getExternalNodeConnector());
    }
}
