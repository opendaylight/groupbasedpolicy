/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainBuilder;

public class DtoEquivalenceUtilsTest {

    @Test
    public void testEqualsAddrEpLocByAddrEpKey() {
        Map<AddressEndpointKey, AddressEndpointLocation> o1 = new HashMap<>();
        Map<AddressEndpointKey, AddressEndpointLocation> o1_equal = new HashMap<>();
        AddressEndpoint ep1_1 = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx1"))
            .setAddressType(AddressType.class)
            .setAddress("adr1_1")
            .build();
        AddressEndpointLocation loc1_1 = new AddressEndpointLocationBuilder().setContextType(ep1_1.getContextType())
            .setContextId(ep1_1.getContextId())
            .setAddressType(ep1_1.getAddressType())
            .setAddress(ep1_1.getAddress())
            .build();
        o1.put(ep1_1.getKey(), loc1_1);
        o1_equal.put(ep1_1.getKey(), loc1_1);

        Map<AddressEndpointKey, AddressEndpointLocation> o2 = new HashMap<>();
        AddressEndpoint ep2_1 = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx2"))
            .setAddressType(AddressType.class)
            .setAddress("adr2_1")
            .build();
        AddressEndpointLocation loc2_1 = new AddressEndpointLocationBuilder().setContextType(ep2_1.getContextType())
            .setContextId(ep2_1.getContextId())
            .setAddressType(ep2_1.getAddressType())
            .setAddress(ep2_1.getAddress())
            .build();
        AddressEndpoint ep2_2 = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx2"))
            .setAddressType(AddressType.class)
            .setAddress("adr2_2")
            .build();
        AddressEndpointLocation loc2_2 = new AddressEndpointLocationBuilder().setContextType(ep2_2.getContextType())
            .setContextId(ep2_2.getContextId())
            .setAddressType(ep2_2.getAddressType())
            .setAddress(ep2_2.getAddress())
            .build();
        o2.put(ep2_1.getKey(), loc2_1);
        o2.put(ep2_2.getKey(), loc2_2);

        Map<AddressEndpointKey, AddressEndpointLocation> o3 = new HashMap<>();
        AddressEndpoint ep3_1 = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx3"))
            .setAddressType(AddressType.class)
            .setAddress("adr3_1")
            .build();
        AddressEndpointLocation loc3_1 = new AddressEndpointLocationBuilder().setContextType(ep3_1.getContextType())
            .setContextId(ep3_1.getContextId())
            .setAddressType(ep3_1.getAddressType())
            .setAddress(ep3_1.getAddress())
            .build();
        o3.put(ep3_1.getKey(), loc3_1);

        assertFalse(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(null, o2));
        assertFalse(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(o1, null));
        assertTrue(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(null, null));
        assertTrue(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(o1, o1));
        assertTrue(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(o1, o1_equal));
        assertFalse(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(o1, o2));
        assertFalse(DtoEquivalenceUtils.equalsAddrEpLocByAddrEpKey(o1, o3));
    }

    @Test
    public void test_ADDR_EP_EQ() {
        AddressEndpointBuilder epA = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx1"))
            .setAddressType(AddressType.class)
            .setAddress("adr1_1");
        AddressEndpointBuilder epA_parent = new AddressEndpointBuilder(epA.build())
            .setParentEndpointChoice(new ParentEndpointCaseBuilder().build());
        AddressEndpointBuilder epA_child = new AddressEndpointBuilder(epA_parent.build())
            .setChildEndpoint(ImmutableList.of(new ChildEndpointBuilder().build()));
        AddressEndpointBuilder epA_networkContainment = new AddressEndpointBuilder(epA_child.build())
            .setNetworkContainment(new NetworkContainmentBuilder().build());
        AddressEndpointBuilder epA_tenant =
                new AddressEndpointBuilder(epA_networkContainment.build()).setTenant(new TenantId("tenantId"));
        AddressEndpointBuilder epA_timestamp = new AddressEndpointBuilder(epA_tenant.build()).setTimestamp(1L);
        AddressEndpointBuilder epA_condition = new AddressEndpointBuilder(epA_timestamp.build())
            .setCondition(ImmutableList.of(new ConditionName("conditionName")));
        AddressEndpointBuilder epA_epg = new AddressEndpointBuilder(epA_condition.build())
            .setEndpointGroup(ImmutableList.of(new EndpointGroupId("epgId")));
        AddressEndpointBuilder epB = new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx2"))
            .setAddressType(AddressType.class)
            .setAddress("adr2_1");

        assertTrue(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA.build(),
                new AddressEndpointBuilder(epA.build()).build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA.build(), epA_parent.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_parent.build(), epA_child.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_child.build(), epA_networkContainment.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_networkContainment.build(), epA_tenant.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_tenant.build(), epA_timestamp.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_timestamp.build(), epA_condition.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA_condition.build(), epA_epg.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_EQ.equivalent(epA.build(), epB.build()));
    }

    @Test
    public void test_CONT_EP_EQ() {
        ContainmentEndpointBuilder epA =
                new ContainmentEndpointBuilder().setContextType(ContextType.class).setContextId(new ContextId("ctx1"));
        ContainmentEndpointBuilder epA_child = new ContainmentEndpointBuilder(epA.build())
            .setChildEndpoint(ImmutableList.of(new ChildEndpointBuilder().build()));
        ContainmentEndpointBuilder epA_networkContainment = new ContainmentEndpointBuilder(epA_child.build())
            .setNetworkContainment(new NetworkContainmentBuilder().build());
        ContainmentEndpointBuilder epA_tenant =
                new ContainmentEndpointBuilder(epA_networkContainment.build()).setTenant(new TenantId("tenantId"));
        ContainmentEndpointBuilder epA_timestamp = new ContainmentEndpointBuilder(epA_tenant.build()).setTimestamp(1L);
        ContainmentEndpointBuilder epA_condition = new ContainmentEndpointBuilder(epA_timestamp.build())
            .setCondition(ImmutableList.of(new ConditionName("conditionName")));
        ContainmentEndpointBuilder epA_epg = new ContainmentEndpointBuilder(epA_condition.build())
            .setEndpointGroup(ImmutableList.of(new EndpointGroupId("epgId")));
        ContainmentEndpointBuilder epB =
                new ContainmentEndpointBuilder().setContextType(ContextType.class).setContextId(new ContextId("ctx2"));

        assertTrue(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA.build(),
                new ContainmentEndpointBuilder(epA.build()).build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA.build(), epA_child.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA_child.build(), epA_networkContainment.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA_networkContainment.build(), epA_tenant.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA_tenant.build(), epA_timestamp.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA_timestamp.build(), epA_condition.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA_condition.build(), epA_epg.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_EQ.equivalent(epA.build(), epB.build()));
    }

    @Test
    public void test_ADDR_EP_LOC_EQ() {
        List<ExternalLocation> externalLocations =
                ImmutableList.of(new ExternalLocationBuilder().setExternalNode("extNode").build());
        List<InternalLocation> internalLocations =
                ImmutableList.of(new InternalLocationBuilder().setInternalNode(null).build());
        RelativeLocationsBuilder relativeLocationsBuilder_ext =
                new RelativeLocationsBuilder().setExternalLocation(externalLocations);
        RelativeLocationsBuilder relativeLocationsBuilder_int =
                new RelativeLocationsBuilder().setInternalLocation(internalLocations);
        RelativeLocationsBuilder relativeLocationsBuilder_both =
                new RelativeLocationsBuilder(relativeLocationsBuilder_ext.build())
                    .setInternalLocation(internalLocations);

        AddressEndpointLocationBuilder locA = new AddressEndpointLocationBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx1"))
            .setAddressType(AddressType.class)
            .setAddress("adr1_1");
        AddressEndpointLocationBuilder locA_absLoc = new AddressEndpointLocationBuilder(locA.build())
            .setAbsoluteLocation(new AbsoluteLocationBuilder().build());
        AddressEndpointLocationBuilder locA_extLoc = new AddressEndpointLocationBuilder(locA_absLoc.build())
            .setRelativeLocations(relativeLocationsBuilder_ext.build());
        AddressEndpointLocationBuilder locA_intLoc = new AddressEndpointLocationBuilder(locA_absLoc.build())
            .setRelativeLocations(relativeLocationsBuilder_int.build());
        AddressEndpointLocationBuilder locA_bothLoc = new AddressEndpointLocationBuilder(locA_absLoc.build())
            .setRelativeLocations(relativeLocationsBuilder_both.build());
        AddressEndpointLocationBuilder locB = new AddressEndpointLocationBuilder().setContextType(ContextType.class)
            .setContextId(new ContextId("ctx2"))
            .setAddressType(AddressType.class)
            .setAddress("adr2_1");

        assertTrue(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA.build(),
                new AddressEndpointLocationBuilder(locA.build()).build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA.build(), locA_absLoc.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA_absLoc.build(), locA_extLoc.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA_extLoc.build(), locA_intLoc.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA_extLoc.build(), locA_bothLoc.build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA_intLoc.build(), locA_bothLoc.build()));
        assertTrue(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA_bothLoc.build(),
                new AddressEndpointLocationBuilder(locA_bothLoc.build()).build()));
        assertFalse(DtoEquivalenceUtils.ADDR_EP_LOC_EQ.equivalent(locA.build(), locB.build()));
    }

    @Test
    public void test_CONT_EP_LOC_EQ() {
        List<ExternalLocation> externalLocations =
                ImmutableList.of(new ExternalLocationBuilder().setExternalNode("extNode").build());
        List<InternalLocation> internalLocations =
                ImmutableList.of(new InternalLocationBuilder().setInternalNode(null).build());
        RelativeLocationsBuilder relativeLocationsBuilder_ext =
                new RelativeLocationsBuilder().setExternalLocation(externalLocations);
        RelativeLocationsBuilder relativeLocationsBuilder_int =
                new RelativeLocationsBuilder().setInternalLocation(internalLocations);
        RelativeLocationsBuilder relativeLocationsBuilder_both =
                new RelativeLocationsBuilder(relativeLocationsBuilder_ext.build())
                    .setInternalLocation(internalLocations);

        ContainmentEndpointLocationBuilder locA = new ContainmentEndpointLocationBuilder()
            .setContextType(ContextType.class).setContextId(new ContextId("ctx1"));
        ContainmentEndpointLocationBuilder locA_extLoc = new ContainmentEndpointLocationBuilder(locA.build())
            .setRelativeLocations(relativeLocationsBuilder_ext.build());
        ContainmentEndpointLocationBuilder locA_intLoc = new ContainmentEndpointLocationBuilder(locA.build())
            .setRelativeLocations(relativeLocationsBuilder_int.build());
        ContainmentEndpointLocationBuilder locA_bothLoc = new ContainmentEndpointLocationBuilder(locA.build())
            .setRelativeLocations(relativeLocationsBuilder_both.build());
        ContainmentEndpointLocationBuilder locB = new ContainmentEndpointLocationBuilder()
            .setContextType(ContextType.class).setContextId(new ContextId("ctx2"));

        assertTrue(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA.build(),
                new ContainmentEndpointLocationBuilder(locA.build()).build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA.build(), locA_extLoc.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA_extLoc.build(), locA_intLoc.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA_extLoc.build(), locA_bothLoc.build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA_intLoc.build(), locA_bothLoc.build()));
        assertTrue(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA_bothLoc.build(),
                new ContainmentEndpointLocationBuilder(locA_bothLoc.build()).build()));
        assertFalse(DtoEquivalenceUtils.CONT_EP_LOC_EQ.equivalent(locA.build(), locB.build()));
    }

    @Test
    public void test_FWD_BY_TENANT_EQ() {
        ForwardingContext fctx = new ForwardingContextBuilder().setContextId(new ContextId("contextId")).build();
        NetworkDomain networkDomain =
                new NetworkDomainBuilder().setNetworkDomainId(new NetworkDomainId("networkDomainId")).build();

        ForwardingByTenantBuilder fwA = new ForwardingByTenantBuilder().setTenantId(new TenantId("tenantA"));
        ForwardingByTenantBuilder fwA_fctx =
                new ForwardingByTenantBuilder(fwA.build()).setForwardingContext(ImmutableList.of(fctx));
        ForwardingByTenantBuilder fwA_network =
                new ForwardingByTenantBuilder(fwA_fctx.build()).setNetworkDomain(ImmutableList.of(networkDomain));
        ForwardingByTenantBuilder fwB = new ForwardingByTenantBuilder().setTenantId(new TenantId("tenantB"));

        assertTrue(DtoEquivalenceUtils.FWD_BY_TENANT_EQ.equivalent(fwA.build(),
                new ForwardingByTenantBuilder(fwA.build()).build()));
        assertFalse(DtoEquivalenceUtils.FWD_BY_TENANT_EQ.equivalent(fwA.build(), fwA_fctx.build()));
        assertFalse(DtoEquivalenceUtils.FWD_BY_TENANT_EQ.equivalent(fwA_fctx.build(), fwA_network.build()));
        assertFalse(DtoEquivalenceUtils.FWD_BY_TENANT_EQ.equivalent(fwA.build(), fwB.build()));
    }

}
