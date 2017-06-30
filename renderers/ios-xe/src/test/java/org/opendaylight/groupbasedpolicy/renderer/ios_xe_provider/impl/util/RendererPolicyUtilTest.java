/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;

/**
 * Test for {@link RendererPolicyUtil}.
 */
public class RendererPolicyUtilTest {

    @Test
    public void testLookupEndpoint() throws Exception {
        final AddressEndpointKey key1 = new AddressEndpointBuilder()
                .setAddress("address1")
                .setAddressType(IpPrefixType.class)
                .setContextId(new ContextId("context1"))
                .setContextType(L3Context.class)
                .build();

        final AddressEndpointWithLocation addressEPWithLocation1 =
                createAddressEPWithLocation("address1", IpPrefixType.class, "context1", L3Context.class);
        final AddressEndpointWithLocation addressEPWithLocation2 =
                createAddressEPWithLocation("address2", IpPrefixType.class, "context1", L3Context.class);
        final AddressEndpointWithLocation addressEPWithLocation3 =
                createAddressEPWithLocation("address1", MacAddressType.class, "context1", L3Context.class);
        final AddressEndpointWithLocation addressEPWithLocation4 =
                createAddressEPWithLocation("address1", IpPrefixType.class, "context2", L3Context.class);
        final AddressEndpointWithLocation addressEPWithLocation5 =
                createAddressEPWithLocation("address1", IpPrefixType.class, "context1", L2BridgeDomain.class);

        final List<AddressEndpointWithLocation> endpoints = Lists.newArrayList(
                addressEPWithLocation2, addressEPWithLocation3,
                addressEPWithLocation4, addressEPWithLocation5,
                addressEPWithLocation1
                );

        final AddressEndpointWithLocation actualEndpoint = RendererPolicyUtil.lookupEndpoint(key1, endpoints);
        Assert.assertSame(addressEPWithLocation1, actualEndpoint);
    }

    @Test
    public void testCreateEndpointGroupIdOrdering() throws Exception {
        final Ordering<EndpointGroupId> endpointGroupIdOrdering = RendererPolicyUtil.createEndpointGroupIdOrdering();
        final String epg1 = "epg1";
        final ArrayList<EndpointGroupId> list = Lists.newArrayList(
                new EndpointGroupId("epg3"), new EndpointGroupId(epg1), new EndpointGroupId("epg2"));

        Collections.sort(list, endpointGroupIdOrdering);
        Assert.assertEquals(epg1, list.get(0).getValue());

        Collections.sort(list, endpointGroupIdOrdering.reversed());
        Assert.assertEquals(epg1, list.get(2).getValue());
    }

    @Test
    public void testCreateConditionNameOrdering() throws Exception {
        final Ordering<ConditionName> conditionNameOrdering = RendererPolicyUtil.createConditionNameOrdering();
        final String name1 = "name1";
        final ArrayList<ConditionName> list = Lists.newArrayList(
                new ConditionName("name3"), new ConditionName(name1), new ConditionName("name2"));

        Collections.sort(list, conditionNameOrdering);
        Assert.assertEquals(name1, list.get(0).getValue());

        Collections.sort(list, conditionNameOrdering.reversed());
        Assert.assertEquals(name1, list.get(2).getValue());
    }

    private AddressEndpointWithLocation createAddressEPWithLocation(final String address,
                                                                    final Class<? extends AddressType> addressType,
                                                                    final String context,
                                                                    final Class<? extends ContextType> contextType) {
        return new AddressEndpointWithLocationBuilder()
                .setAddress(address)
                .setAddressType(addressType)
                .setContextId(new ContextId(context))
                .setContextType(contextType)
                .build();
    }
}
