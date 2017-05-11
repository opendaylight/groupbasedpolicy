/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalEndpointKey;

import com.google.common.base.Function;

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

    public static AddressEndpointKey fromParentEndpointKey(ParentEndpointKey key) {
        return new AddressEndpointKey(key.getAddress(), key.getAddressType(),
                key.getContextId(), key.getContextType());
    }

    public static boolean sameExternalLocationCase(AddressEndpointWithLocation ref, AddressEndpointWithLocation ae1) {
        if (ref.getRelativeLocations() != null || ref.getAbsoluteLocation() == null) {
            return false;
        }
        Function<AbsoluteLocation, Optional<ExternalLocation>> absoluteToExternal =
                new Function<AbsoluteLocation, Optional<ExternalLocation>>() {

                    @Override
                    public Optional<ExternalLocation> apply(AbsoluteLocation absoluteLocation) {
                        if (absoluteLocation == null || absoluteLocation.getLocationType() == null) {
                            return Optional.empty();
                        }
                        Optional<LocationType> locationType = Optional.ofNullable(absoluteLocation.getLocationType());
                        if (locationType.isPresent() && locationType.get() instanceof ExternalLocationCase) {
                            ExternalLocationCase extLocCase = (ExternalLocationCase) absoluteLocation.getLocationType();
                            return Optional.of(new ExternalLocationBuilder()
                                .setExternalNodeMountPoint(extLocCase.getExternalNodeMountPoint())
                                .setExternalNodeConnector(extLocCase.getExternalNodeConnector())
                                .build());
                        }
                        return Optional.empty();
                    }
                };
        Optional<ExternalLocation> refLoc0 = absoluteToExternal.apply(ref.getAbsoluteLocation());
        if (!refLoc0.isPresent()) {
            return false;
        }
        Predicate<ExternalLocation> sameLocation = new Predicate<ExternalLocation>() {

            @Override
            public boolean test(ExternalLocation loc1) {
                boolean valuesPresent = refLoc0.get().getExternalNodeMountPoint() == null
                        || loc1.getExternalNodeMountPoint() == null
                        || refLoc0.get().getExternalNodeConnector() == null
                        || loc1.getExternalNodeConnector() == null;
                return (valuesPresent) ? false : refLoc0.get()
                    .getExternalNodeMountPoint()
                    .toString()
                    .equals(loc1.getExternalNodeMountPoint().toString())
                        && refLoc0.get().getExternalNodeConnector().equals(loc1.getExternalNodeConnector());
            }
        };
        List<ExternalLocation> extLocs = new ArrayList<>();
        Optional<ExternalLocation> refLoc1 = absoluteToExternal.apply(ae1.getAbsoluteLocation());
        if (refLoc1.isPresent()) {
            extLocs.add(refLoc1.get());
        } else if (ae1.getRelativeLocations() != null && ae1.getRelativeLocations().getExternalLocation() != null) {
            extLocs.addAll(ae1.getRelativeLocations().getExternalLocation());
        }
        if (extLocs.stream().filter(sameLocation).findAny().isPresent()) {
            return true;
        }
        return false;
    }
}
