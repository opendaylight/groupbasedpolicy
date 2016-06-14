/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.EpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSetMultimap;

public class EndpointInfo {

    private final ImmutableMap<AddressEndpointKey, AddressEndpoint> addressEpByKey;
    private final ImmutableMap<ContainmentEndpointKey, ContainmentEndpoint> containmentEpByKey;
    private final ImmutableSetMultimap<EpgKey, AddressEndpointKey> addressEpsByEpg;
    private final ImmutableSetMultimap<EpgKey, ContainmentEndpointKey> containmentEpsByEpg;

    public EndpointInfo(Endpoints endpoints) {
        if (endpoints.getAddressEndpoints() == null || endpoints.getAddressEndpoints().getAddressEndpoint() == null) {
            addressEpByKey = ImmutableMap.of();
            addressEpsByEpg = ImmutableSetMultimap.of();
        } else {
            com.google.common.collect.ImmutableMap.Builder<AddressEndpointKey, AddressEndpoint> addressEpsByKeyBuilder =
                    ImmutableMap.builder();
            com.google.common.collect.ImmutableSetMultimap.Builder<EpgKey, AddressEndpointKey> addressEpsByEpgBuilder =
                    ImmutableSetMultimap.builder();
            com.google.common.collect.ImmutableMultimap.Builder<Set<EpgKey>, AddressEndpointKey> addressEpsByEpgsBuilder =
                    ImmutableMultimap.builder();
            for (AddressEndpoint ep : endpoints.getAddressEndpoints().getAddressEndpoint()) {
                addressEpsByKeyBuilder.put(ep.getKey(), ep);
                Builder<EpgKey> epgsBuilder = ImmutableSet.builder();
                for (EndpointGroupId epgId : ep.getEndpointGroup()) {
                    EpgKey epgKey = new EpgKeyDto(epgId, ep.getTenant());
                    addressEpsByEpgBuilder.put(epgKey, ep.getKey());
                    epgsBuilder.add(epgKey);
                }
                addressEpsByEpgsBuilder.put(epgsBuilder.build(), ep.getKey());
            }
            addressEpByKey = addressEpsByKeyBuilder.build();
            addressEpsByEpg = addressEpsByEpgBuilder.build();
        }
        if (endpoints.getContainmentEndpoints() == null
                || endpoints.getContainmentEndpoints().getContainmentEndpoint() == null) {
            containmentEpByKey = ImmutableMap.of();
            containmentEpsByEpg = ImmutableSetMultimap.of();
        } else {
            com.google.common.collect.ImmutableSetMultimap.Builder<EpgKey, ContainmentEndpointKey> containmentEpsByEpgBuilder =
                    ImmutableSetMultimap.builder();
            com.google.common.collect.ImmutableMap.Builder<ContainmentEndpointKey, ContainmentEndpoint> containmentEpsByKeyBuilder =
                    ImmutableMap.builder();
            com.google.common.collect.ImmutableMultimap.Builder<Set<EpgKey>, ContainmentEndpointKey> containmentEpsByEpgsBuilder =
                    ImmutableMultimap.builder();
            for (ContainmentEndpoint ep : endpoints.getContainmentEndpoints().getContainmentEndpoint()) {
                containmentEpsByKeyBuilder.put(ep.getKey(), ep);
                Builder<EpgKey> epgsBuilder = ImmutableSet.builder();
                for (EndpointGroupId epgId : ep.getEndpointGroup()) {
                    EpgKey epgKey = new EpgKeyDto(epgId, ep.getTenant());
                    containmentEpsByEpgBuilder.put(epgKey, ep.getKey());
                    epgsBuilder.add(epgKey);
                }
                containmentEpsByEpgsBuilder.put(epgsBuilder.build(), ep.getKey());
            }
            containmentEpByKey = containmentEpsByKeyBuilder.build();
            containmentEpsByEpg = containmentEpsByEpgBuilder.build();
        }
    }

    public Optional<AddressEndpoint> getEndpoint(AddressEndpointKey key) {
        return Optional.fromNullable(addressEpByKey.get(key));
    }

    public Optional<ContainmentEndpoint> getContainmentEndpoint(ContainmentEndpointKey key) {
        return Optional.fromNullable(containmentEpByKey.get(key));
    }

    public ImmutableSet<AddressEndpointKey> findAddressEpsWithEpg(EpgKey epg) {
        return addressEpsByEpg.get(epg);
    }

    public ImmutableSet<ContainmentEndpointKey> findContainmentEpsWithEpg(EpgKey epg) {
        return containmentEpsByEpg.get(epg);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addressEpByKey == null) ? 0 : addressEpByKey.hashCode());
        result = prime * result + ((containmentEpByKey == null) ? 0 : containmentEpByKey.hashCode());
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
        EndpointInfo other = (EndpointInfo) obj;
        if (addressEpByKey == null) {
            if (other.addressEpByKey != null)
                return false;
        } else if (!DtoEquivalenceUtils.equalsAddressEpByKey(addressEpByKey, other.addressEpByKey))
            return false;
        if (containmentEpByKey == null) {
            if (other.containmentEpByKey != null)
                return false;
        } else if (!DtoEquivalenceUtils.equalsContainmentEpByKey(containmentEpByKey, other.containmentEpByKey))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EndpointInfo [addressEpByKey=" + addressEpByKey + ", containmentEpByKey=" + containmentEpByKey + "]";
    }

}
