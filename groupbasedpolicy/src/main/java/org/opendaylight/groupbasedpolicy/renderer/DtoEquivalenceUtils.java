/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.CommonEndpointFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenant;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

class DtoEquivalenceUtils {

    private static final Function<ForwardingByTenant, TenantId> FORWARDING_BY_TENANT_TO_TENANT_ID =
            new Function<ForwardingByTenant, TenantId>() {

                @Override
                public TenantId apply(ForwardingByTenant input) {
                    return input.getTenantId();
                }
            };

    @VisibleForTesting
    static final Equivalence<AddressEndpoint> ADDR_EP_EQ = new Equivalence<AddressEndpoint>() {

        @Override
        protected boolean doEquivalent(AddressEndpoint a, AddressEndpoint b) {
            if (!Objects.equals(a.getKey(), b.getKey())) {
                return false;
            }
            if (!Objects.equals(a.getParentEndpointChoice(), b.getParentEndpointChoice())) {
                return false;
            }
            if (!equalsDtoLists(a.getChildEndpoint(), b.getChildEndpoint())) {
                return false;
            }
            if (!equalsCommonEndpointFields(a, b)) {
                return false;
            }
            if (!equalsAugmentations(a, b)) {
                return false;
            }
            return true;
        }

        @Override
        protected int doHash(AddressEndpoint t) {
            return t.hashCode();
        }
    };
    @VisibleForTesting
    static final Equivalence<ContainmentEndpoint> CONT_EP_EQ = new Equivalence<ContainmentEndpoint>() {

        @Override
        protected boolean doEquivalent(ContainmentEndpoint a, ContainmentEndpoint b) {
            if (!Objects.equals(a.getKey(), b.getKey())) {
                return false;
            }
            if (!equalsDtoLists(a.getChildEndpoint(), b.getChildEndpoint())) {
                return false;
            }
            if (!equalsCommonEndpointFields(a, b)) {
                return false;
            }
            return true;
        }

        @Override
        protected int doHash(ContainmentEndpoint t) {
            return t.hashCode();
        }
    };
    @VisibleForTesting
    static final Equivalence<AddressEndpointLocation> ADDR_EP_LOC_EQ =
            new Equivalence<AddressEndpointLocation>() {

                @Override
                protected boolean doEquivalent(AddressEndpointLocation a, AddressEndpointLocation b) {
                    if (!Objects.equals(a.getKey(), b.getKey())) {
                        return false;
                    }
                    if (!Objects.equals(a.getAbsoluteLocation(), b.getAbsoluteLocation())) {
                        return false;
                    }
                    if (!equalsRelativeLocations(a.getRelativeLocations(), b.getRelativeLocations())) {
                        return false;
                    }
                    return true;
                }

                @Override
                protected int doHash(AddressEndpointLocation t) {
                    return t.hashCode();
                }
            };
    @VisibleForTesting
    static final Equivalence<ContainmentEndpointLocation> CONT_EP_LOC_EQ =
            new Equivalence<ContainmentEndpointLocation>() {

                @Override
                protected boolean doEquivalent(ContainmentEndpointLocation a, ContainmentEndpointLocation b) {
                    if (!Objects.equals(a.getKey(), b.getKey())) {
                        return false;
                    }
                    if (!equalsRelativeLocations(a.getRelativeLocations(), b.getRelativeLocations())) {
                        return false;
                    }
                    return true;
                }

                @Override
                protected int doHash(ContainmentEndpointLocation t) {
                    return t.hashCode();
                }
            };
    @VisibleForTesting
    static final Equivalence<ForwardingByTenant> FWD_BY_TENANT_EQ = new Equivalence<ForwardingByTenant>() {

        @Override
        protected boolean doEquivalent(ForwardingByTenant x, ForwardingByTenant y) {
            if (!Objects.equals(x.getKey(), y.getKey())) {
                return false;
            }
            if (!equalsDtoLists(x.getForwardingContext(), y.getForwardingContext())) {
                return false;
            }
            if (!equalsDtoLists(x.getNetworkDomain(), y.getNetworkDomain())) {
                return false;
            }
            return true;
        }

        @Override
        protected int doHash(ForwardingByTenant t) {
            return t.hashCode();
        }
    };

    private DtoEquivalenceUtils() {}

    private static boolean equalsAugmentations(AddressEndpoint a, AddressEndpoint b) {
        if (a.getAugmentation(NatAddress.class) != null && b.getAugmentation(NatAddress.class) != null) {
            if (!a.getAugmentation(NatAddress.class)
                .getNatAddress()
                .equals(b.getAugmentation(NatAddress.class).getNatAddress())) {
                return false;
            }
        } else if (a.getAugmentation(NatAddress.class) != null || b.getAugmentation(NatAddress.class) != null) {
            return false;
        }
        return true;
    }

    static boolean equalsAddressEpByKey(@Nullable Map<AddressEndpointKey, AddressEndpoint> o1,
            @Nullable Map<AddressEndpointKey, AddressEndpoint> o2) {
        return equalsDtoMapsByEquivalence(o1, o2, ADDR_EP_EQ);
    }

    static boolean equalsContainmentEpByKey(@Nullable Map<ContainmentEndpointKey, ContainmentEndpoint> o1,
            @Nullable Map<ContainmentEndpointKey, ContainmentEndpoint> o2) {
        return equalsDtoMapsByEquivalence(o1, o2, CONT_EP_EQ);
    }

    static boolean equalsAddrEpLocByAddrEpKey(Map<AddressEndpointKey, AddressEndpointLocation> o1,
            Map<AddressEndpointKey, AddressEndpointLocation> o2) {
        return equalsDtoMapsByEquivalence(o1, o2, ADDR_EP_LOC_EQ);
    }

    static boolean equalsContEpLocByContEpKey(Map<ContainmentEndpointKey, ContainmentEndpointLocation> o1,
            Map<ContainmentEndpointKey, ContainmentEndpointLocation> o2) {
        return equalsDtoMapsByEquivalence(o1, o2, CONT_EP_LOC_EQ);
    }

    static boolean equalsForwarding(@Nullable Forwarding a, @Nullable Forwarding b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        return equalsForwardingByTenantLists(a.getForwardingByTenant(), b.getForwardingByTenant());
    }

    private static boolean equalsCommonEndpointFields(CommonEndpointFields a, CommonEndpointFields b) {
        if (!Objects.equals(a.getNetworkContainment(), b.getNetworkContainment())) {
            return false;
        }
        if (!Objects.equals(a.getTenant(), b.getTenant())) {
            return false;
        }
        if (!Objects.equals(a.getTimestamp(), b.getTimestamp())) {
            return false;
        }
        if (!equalsDtoLists(a.getCondition(), b.getCondition())) {
            return false;
        }
        if (!equalsDtoLists(a.getEndpointGroup(), b.getEndpointGroup())) {
            return false;
        }
        return true;
    }

    private static boolean equalsRelativeLocations(RelativeLocations a, RelativeLocations b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        if (!equalsDtoLists(a.getExternalLocation(), b.getExternalLocation())) {
            return false;
        }
        if (!equalsDtoLists(a.getInternalLocation(), b.getInternalLocation())) {
            return false;
        }
        return true;
    }

    private static boolean equalsForwardingByTenantLists(List<ForwardingByTenant> a, List<ForwardingByTenant> b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        if (a.size() != b.size()) {
            return false;
        }
        ImmutableMap<TenantId, ForwardingByTenant> aMap = Maps.uniqueIndex(a, FORWARDING_BY_TENANT_TO_TENANT_ID);
        ImmutableMap<TenantId, ForwardingByTenant> bMap = Maps.uniqueIndex(b, FORWARDING_BY_TENANT_TO_TENANT_ID);
        return equalsDtoMapsByEquivalence(aMap, bMap, FWD_BY_TENANT_EQ);
    }

    private static <K, V> boolean equalsDtoMapsByEquivalence(@Nullable Map<K, V> o1, @Nullable Map<K, V> o2,
            Equivalence<V> eq) {
        if ((o1 == null && o2 != null) || (o1 != null && o2 == null)) {
            return false;
        }
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1.size() != o2.size()) {
            return false;
        }
        MapDifference<K, V> diff = Maps.difference(o1, o2, eq);
        if (!diff.entriesOnlyOnLeft().isEmpty() || !diff.entriesOnlyOnRight().isEmpty()) {
            return false;
        }
        if (!diff.entriesDiffering().isEmpty()) {
            return false;
        }
        return true;
    }

    private static <T> boolean equalsDtoLists(@Nullable List<T> a, @Nullable List<T> b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        return a.size() == b.size() && a.containsAll(b);
    }
}
