/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.List;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.renderer.util.EndpointLocationUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

public class EndpointLocationInfo {

    private final ImmutableMultimap<InstanceIdentifier<?>, AddressEndpointLocation> endpointsByExternalNodeLocation;
    private final ImmutableMap<AddressEndpointKey, AddressEndpointLocation> addrEpLocByAddrEpKey;
    private final ImmutableMap<ContainmentEndpointKey, ContainmentEndpointLocation> contEpLocByContEpKey;

    public EndpointLocationInfo(EndpointLocations epLocations) {
        List<AddressEndpointLocation> addressEndpointLocations = epLocations.getAddressEndpointLocation();
        endpointsByExternalNodeLocation =
                EndpointLocationUtils.resolveEndpointsByExternalNodeLocation(addressEndpointLocations);
        if (addressEndpointLocations == null) {
            addrEpLocByAddrEpKey = ImmutableMap.of();
        } else {
            com.google.common.collect.ImmutableMap.Builder<AddressEndpointKey, AddressEndpointLocation> adrEpLocByAdrEpKeyBuilder =
                    ImmutableMap.builder();
            for (AddressEndpointLocation adrEpLoc : addressEndpointLocations) {
                adrEpLocByAdrEpKeyBuilder.put(toAdrEpKey(adrEpLoc.getKey()), adrEpLoc);
            }
            addrEpLocByAddrEpKey = adrEpLocByAdrEpKeyBuilder.build();
        }
        List<ContainmentEndpointLocation> containmentEndpointLocations = epLocations.getContainmentEndpointLocation();
        if (containmentEndpointLocations == null) {
            contEpLocByContEpKey = ImmutableMap.of();
        } else {
            com.google.common.collect.ImmutableMap.Builder<ContainmentEndpointKey, ContainmentEndpointLocation> contEpLocBycontEpKeyBuilder =
                    ImmutableMap.builder();
            for (ContainmentEndpointLocation contEpLoc : containmentEndpointLocations) {
                contEpLocBycontEpKeyBuilder.put(toContEpKey(contEpLoc.getKey()), contEpLoc);
            }
            contEpLocByContEpKey = contEpLocBycontEpKeyBuilder.build();
        }
    }

    public Optional<AddressEndpointLocation> getAdressEndpointLocation(AddressEndpointKey epKey) {
        return Optional.fromNullable(addrEpLocByAddrEpKey.get(epKey));
    }

    public Optional<ContainmentEndpointLocation> getContainmentEndpointLocation(ContainmentEndpointKey contEpKey) {
        return Optional.fromNullable(contEpLocByContEpKey.get(contEpKey));
    }

    private AddressEndpointKey toAdrEpKey(AddressEndpointLocationKey adrEpLocKey) {
        return new AddressEndpointKey(adrEpLocKey.getAddress(), adrEpLocKey.getAddressType(),
                adrEpLocKey.getContextId(), adrEpLocKey.getContextType());
    }

    private ContainmentEndpointKey toContEpKey(ContainmentEndpointLocationKey contEpLocKey) {
        return new ContainmentEndpointKey(contEpLocKey.getContextId(), contEpLocKey.getContextType());
    }

    public Set<InstanceIdentifier<?>> getAllExternalNodeLocations() {
        return endpointsByExternalNodeLocation.keySet();
    }

    public ImmutableSet<AddressEndpointKey> getAddressEpsWithExternalNodeLocation(
            InstanceIdentifier<?> realNodeLocation) {
        return FluentIterable.from(endpointsByExternalNodeLocation.get(realNodeLocation))
            .transform(new Function<AddressEndpointLocation, AddressEndpointKey>() {

                @Override
                public AddressEndpointKey apply(AddressEndpointLocation epLoc) {
                    return new AddressEndpointKey(epLoc.getAddress(), epLoc.getAddressType(), epLoc.getContextId(),
                            epLoc.getContextType());
                }
            })
            .toSet();
    }

    public boolean hasAbsoluteLocation(AddressEndpointKey adrEpKey) {
        AddressEndpointLocation adrEpLoc = addrEpLocByAddrEpKey.get(adrEpKey);
        if (adrEpLoc == null) {
            return false;
        }
        AbsoluteLocation absLocation = adrEpLoc.getAbsoluteLocation();
        if (absLocation == null) {
            return false;
        }
        LocationType locationType = absLocation.getLocationType();
        if (locationType == null) {
            return false;
        }
        return true;
    }

    public boolean hasRelativeLocation(AddressEndpointKey adrEpKey) {
        AddressEndpointLocation adrEpLoc = addrEpLocByAddrEpKey.get(adrEpKey);
        if (adrEpLoc == null) {
            return false;
        }
        RelativeLocations relLocations = adrEpLoc.getRelativeLocations();
        if (relLocations == null) {
            return false;
        }
        if (relLocations.getInternalLocation() == null && relLocations.getExternalLocation() == null) {
            return false;
        }
        return true;
    }

    public boolean hasRelativeLocation(ContainmentEndpointKey contEpKey) {
        ContainmentEndpointLocation contEpLoc = contEpLocByContEpKey.get(contEpKey);
        if (contEpLoc == null) {
            return false;
        }
        RelativeLocations relLocations = contEpLoc.getRelativeLocations();
        if (relLocations == null) {
            return false;
        }
        List<InternalLocation> locs = relLocations.getInternalLocation();
        if (locs == null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addrEpLocByAddrEpKey == null) ? 0 : addrEpLocByAddrEpKey.hashCode());
        result = prime * result + ((contEpLocByContEpKey == null) ? 0 : contEpLocByContEpKey.hashCode());
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
        EndpointLocationInfo other = (EndpointLocationInfo) obj;
        if (addrEpLocByAddrEpKey == null) {
            if (other.addrEpLocByAddrEpKey != null)
                return false;
        } else if (!DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(addrEpLocByAddrEpKey, other.addrEpLocByAddrEpKey))
            return false;
        if (contEpLocByContEpKey == null) {
            if (other.contEpLocByContEpKey != null)
                return false;
        } else if (!DtoEquivalenceUtils.equalsContEpLocByContEpKey(contEpLocByContEpKey, other.contEpLocByContEpKey))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EndpointLocationInfo [adrEpLocByAdrEpKey=" + addrEpLocByAddrEpKey + ", contEpLocBycontEpKey="
                + contEpLocByContEpKey + "]";
    }

}
