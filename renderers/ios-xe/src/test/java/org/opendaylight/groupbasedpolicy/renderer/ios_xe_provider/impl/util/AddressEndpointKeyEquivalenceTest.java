/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;

/**
 * Test for {@link AddressEndpointKeyEquivalence}.
 */
public class AddressEndpointKeyEquivalenceTest {

    private AddressEndpointKeyEquivalence addressEndpointKeyEquivalence;
    private AddressEndpointLocation addressEndpointLocation;
    private RendererEndpoint rendererEndpoint;

    @Before
    public void setUp() throws Exception {
        addressEndpointKeyEquivalence = new AddressEndpointKeyEquivalence();
        final String address = "1.2.3.4/32";
        final Class<? extends AddressType> addressType = IpPrefixType.class;
        final ContextId contextId = new ContextId("l3-context-id-01");
        final Class<? extends ContextType> contextType = L3Context.class;

        addressEndpointLocation = new AddressEndpointLocationBuilder()
                .setAddress(address)
                .setAddressType(addressType)
                .setContextId(contextId)
                .setContextType(contextType)
                .build();

        rendererEndpoint = new RendererEndpointBuilder()
                .setAddress(address)
                .setAddressType(addressType)
                .setContextId(contextId)
                .setContextType(contextType)
                .build();
    }

    @Test
    public void testDoHash() throws Exception {
        Assert.assertEquals(addressEndpointKeyEquivalence.doHash(addressEndpointLocation),
                addressEndpointKeyEquivalence.doHash(rendererEndpoint));
    }


    @Test
    public void testDoEquivalent() throws Exception {
        Assert.assertTrue(addressEndpointKeyEquivalence.equivalent(addressEndpointLocation, rendererEndpoint));

        final Predicate<AddressEndpointKey> addressPredicate =
                addressEndpointKeyEquivalence.equivalentTo(addressEndpointLocation);
        Assert.assertTrue(addressPredicate.apply(rendererEndpoint));
    }

    @Test
    public void testInHashStructure_plain() throws Exception {
        final HashSet<AddressEndpointKey> bag = new HashSet<>();

        Assert.assertTrue(bag.add(addressEndpointLocation));
        Assert.assertTrue(bag.add(rendererEndpoint));
        Assert.assertEquals(2, bag.size());
    }

    @Test
    public void testInHashStructure_wrapped() throws Exception {
        final HashSet<Equivalence.Wrapper<AddressEndpointKey>> bagOfWrappers = new HashSet<>();
        final Equivalence.Wrapper<AddressEndpointKey> wrap1 = addressEndpointKeyEquivalence.wrap(addressEndpointLocation);
        final Equivalence.Wrapper<AddressEndpointKey> wrap2 = addressEndpointKeyEquivalence.wrap(rendererEndpoint);

        Assert.assertTrue(bagOfWrappers.add(wrap1));
        Assert.assertFalse(bagOfWrappers.add(wrap2));
        Assert.assertEquals(1, bagOfWrappers.size());
    }
}