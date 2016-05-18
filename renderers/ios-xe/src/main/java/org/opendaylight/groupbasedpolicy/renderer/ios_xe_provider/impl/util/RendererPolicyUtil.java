/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import java.util.List;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;

/**
 * Purpose: provide util methods handling {@link RendererPolicy}
 */
public final class RendererPolicyUtil {

    private static final AddressEndpointKeyEquivalence ADDRESS_EP_KEY_EQUIVALENCE = new AddressEndpointKeyEquivalence();
    private static final Comparable EMPTY_COMPARABLE = "";

    private RendererPolicyUtil() {
        throw new IllegalAccessError("Shall not instantiate util class.");
    }


    /**
     * @param rendererEp                  lightweight endpoint key
     * @param addressEndpointWithLocation collection of heavyweight endpoint definitions
     * @return full address endpoint found by given key
     */
    public static AddressEndpointWithLocation lookupEndpoint(final AddressEndpointKey rendererEp, final List<AddressEndpointWithLocation> addressEndpointWithLocation) {
        final Predicate<AddressEndpointKey> addressEndpointKeyPredicate = ADDRESS_EP_KEY_EQUIVALENCE.equivalentTo(rendererEp);
        AddressEndpointWithLocation needle = null;

        for (AddressEndpointWithLocation ep : addressEndpointWithLocation) {
            if (addressEndpointKeyPredicate.apply(ep)) {
                needle = ep;
                break;
            }
        }
        return needle;
    }

    public static Ordering<EndpointGroupId> createEndpointGroupIdOrdering() {
        return Ordering.natural().onResultOf(new Function<EndpointGroupId, Comparable>() {
            @Nullable
            @Override
            public Comparable apply(@Nullable final EndpointGroupId input) {
                if (input == null) {
                    return EMPTY_COMPARABLE;
                }
                return MoreObjects.firstNonNull(input.getValue(), EMPTY_COMPARABLE);
            }
        });
    }

    public static Ordering<ConditionName> createConditionNameOrdering() {
        return Ordering.natural().onResultOf(new Function<ConditionName, Comparable>() {
            @Nullable
            @Override
            public Comparable apply(@Nullable final ConditionName input) {
                if (input == null) {
                    return EMPTY_COMPARABLE;
                }
                return MoreObjects.firstNonNull(input.getValue(), EMPTY_COMPARABLE);
            }
        });
    }
}
