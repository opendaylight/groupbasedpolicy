/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.List;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;

public class EndpointLocationUtils {

    public static ImmutableMultimap<InstanceIdentifier<?>, AddressEndpointLocation> resolveEndpointsByAbsoluteNodeLocation(
            @Nullable List<AddressEndpointLocation> addressEndpointLocations) {
        if (addressEndpointLocations == null) {
            return ImmutableMultimap.of();
        }
        Builder<InstanceIdentifier<?>, AddressEndpointLocation> resultBuilder = ImmutableMultimap.builder();
        for (AddressEndpointLocation epLoc : addressEndpointLocations) {
            Optional<InstanceIdentifier<?>> potentialAbsIntNodeLocation = resolveAbsoluteInternalNodeLocation(epLoc);
            if (potentialAbsIntNodeLocation.isPresent()) {
                resultBuilder.put(potentialAbsIntNodeLocation.get(), epLoc);
            } else {
                Optional<InstanceIdentifier<?>> potentialAbsExtNodeMpLocation =
                        resolveAbsoluteExternalNodeMountPointLocation(epLoc);
                if (potentialAbsExtNodeMpLocation.isPresent()) {
                    resultBuilder.put(potentialAbsExtNodeMpLocation.get(), epLoc);
                }
            }
        }
        return resultBuilder.build();
    }

    public static Optional<InstanceIdentifier<?>> resolveAbsoluteInternalNodeLocation(AddressEndpointLocation epLoc) {
        AbsoluteLocation absLoc = epLoc.getAbsoluteLocation();
        if (absLoc != null) {
            LocationType locType = absLoc.getLocationType();
            if (locType instanceof InternalLocationCase) {
                InternalLocationCase absRegularLoc = (InternalLocationCase) locType;
                if (absRegularLoc.getInternalNode() != null) {
                    return Optional.of(absRegularLoc.getInternalNode());
                }
            }
        }
        return Optional.absent();
    }

    public static Optional<InstanceIdentifier<?>> resolveAbsoluteExternalNodeMountPointLocation(
            AddressEndpointLocation epLoc) {
        AbsoluteLocation absLoc = epLoc.getAbsoluteLocation();
        if (absLoc != null) {
            LocationType locType = absLoc.getLocationType();
            if (locType instanceof ExternalLocationCase) {
                ExternalLocationCase realExtLoc = (ExternalLocationCase) locType;
                if (realExtLoc.getExternalNodeMountPoint() != null) {
                    return Optional.of(realExtLoc.getExternalNodeMountPoint());
                }
            }
        }
        return Optional.absent();
    }

    public static Optional<InstanceIdentifier<?>> resolveAbsoluteNodeLocation(AddressEndpointLocation epLoc) {
        if (epLoc.getAbsoluteLocation() == null) {
            return Optional.absent();
        }
        LocationType locType = epLoc.getAbsoluteLocation().getLocationType();
        if (locType instanceof InternalLocationCase) {
            InternalLocationCase absLoc = (InternalLocationCase) locType;
            if (absLoc.getInternalNode() != null) {
                return Optional.of(absLoc.getInternalNode());
            }
        } else if (locType instanceof ExternalLocationCase) {
            ExternalLocationCase absLoc = (ExternalLocationCase) locType;
            if (absLoc.getExternalNodeMountPoint() != null) {
                return Optional.of(absLoc.getExternalNodeMountPoint());
            }
        }
        return Optional.absent();
    }

}
