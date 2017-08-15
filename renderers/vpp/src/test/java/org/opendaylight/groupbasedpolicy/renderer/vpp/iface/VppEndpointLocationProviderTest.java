/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class VppEndpointLocationProviderTest extends CustomDataBrokerTest {

    private DataBroker dataBroker;
    private VppEndpointLocationProvider locationProvider;

    private final String INTF_NAME = "interface-name";
    private final NodeId NODE_1 = new NodeId("vpp-node-1");
    private final NodeId NODE_2 = new NodeId("vpp-node-2");
    private final NodeId NODE_3 = new NodeId("vpp-node-3");

    private static final ContextId MAC_CTX = new ContextId("mac-context");
    private static final ContextId IP_CTX = new ContextId("ip-context");
    private static final VppEndpointKey MAC_KEY_23_45_VPP =
            new VppEndpointKey("ab:cd:ef:01:23:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final VppEndpointKey MAC_KEY_24_45_VPP =
            new VppEndpointKey("ab:cd:ef:01:24:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final VppEndpointKey MAC_KEY_25_45_VPP =
            new VppEndpointKey("ab:cd:ef:01:25:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final AddressEndpointKey MAC_KEY_23_45 =
            new AddressEndpointKey("ab:cd:ef:01:23:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final AddressEndpointKey MAC_KEY_24_45 =
            new AddressEndpointKey("ab:cd:ef:01:24:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final AddressEndpointKey MAC_KEY_25_45 =
            new AddressEndpointKey("ab:cd:ef:01:25:45", MacAddressType.class, MAC_CTX, L2BridgeDomain.class);
    private static final AddressEndpointKey IP_KEY_1_22 =
            new AddressEndpointKey("192.168.1.22/32", IpPrefixType.class, IP_CTX, L3Context.class);
    private static final AddressEndpointKey IP_KEY_2_22 =
            new AddressEndpointKey("192.168.2.22/32", IpPrefixType.class, IP_CTX, L3Context.class);
    private static final AddressEndpointKey IP_KEY_3_22 =
            new AddressEndpointKey("192.168.3.22/32", IpPrefixType.class, IP_CTX, L3Context.class);
    private static final AddressEndpointKey IP_KEY_9_99 =
            new AddressEndpointKey("192.168.9.99/32", IpPrefixType.class, IP_CTX, L3Context.class);

    Function<ExternalLocation, NodeId> extLoctoNodeId =
            input -> input.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId();

    @Before
    public void init() {
        dataBroker = getDataBroker();
        locationProvider = new VppEndpointLocationProvider(dataBroker);
    }

    @After
    public void finish() {
        locationProvider.close();
    }

    /**
     * Two L3 endpoints use the same L2 endpoint.
     * L3 <-> L2 <-> L3
     * This is an Openstack CI use case, when metadata service and DHCP service
     * can be reached via the same port.
     * This should result in 2 absolute locations which keys are derived from L3
     * endpoints.
     */
    @Test
    public void l2ChildHasTwoL3Parents() {
        l2ChildHasTwoIpParents(true);
    }

    /**
     * There are two data sources from which location is resolved:
     * {@link AddressEndpoint} and {@link VppEndpoint}
     * Here the order of events is swapped, i.e. vpp endpoint is written
     * prior to address endpoint into the datastore.
     */
    @Test
    public void l2ChildHasTwoL3Parents_swapped_order() {
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        l2ChildHasTwoIpParents(false);
    }

    private void l2ChildHasTwoIpParents(boolean vppEndpointEvent) {
        AddressEndpointBuilder child = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(child, IP_KEY_1_22, IP_KEY_2_22);
        AddressEndpointBuilder firstParent = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        AddressEndpointBuilder secondParent = new AddressEndpointBuilder().setKey(IP_KEY_2_22);
        setChildEndpoints(firstParent, MAC_KEY_23_45);
        setChildEndpoints(secondParent, MAC_KEY_23_45);
        submitEndpointsToDatastore(child.build(), firstParent.build(), secondParent.build());
        assertEndpointsInDatastore(child.getKey(), firstParent.getKey(), secondParent.getKey());
        if (vppEndpointEvent) {
            locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        }
        List<ProviderAddressEndpointLocation> locations = readLocations();
        Assert.assertEquals(locations.size(), 2);
        locations.forEach(location -> {
            Assert.assertTrue(location.getKey().equals(getLocationKey(IP_KEY_1_22))
                    || location.getKey().equals(getLocationKey(IP_KEY_2_22)));
            assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
            assertMountPoint(location.getAbsoluteLocation(), NODE_1);
        });
    }

    /**
     * This is a regular use case with one to one mapping between L2 endpoint
     * and L3 endpoint.
     * As a result one location should be created which key is derived from
     * L2 endpoint.
     */
    @Test
    public void l2ChildHasOneL3Parent() {
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        l2ChildHasOneIpParent(false);
    }

    @Test
    public void l2ChildHasOneL3Parent_swapped_order() {
        l2ChildHasOneIpParent(true);
    }

    private void l2ChildHasOneIpParent(boolean vppEndpointEvent) {
        AddressEndpointBuilder child = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(child, IP_KEY_1_22);
        AddressEndpointBuilder firstParent = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        setChildEndpoints(firstParent, MAC_KEY_23_45);
        submitEndpointsToDatastore(child.build(), firstParent.build());
        assertEndpointsInDatastore(child.getKey(), firstParent.getKey());
        if (vppEndpointEvent) {
            locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        }
        List<ProviderAddressEndpointLocation> locations = readLocations();
        Assert.assertEquals(locations.size(), 1);
        ProviderAddressEndpointLocation location = locations.get(0);
        Assert.assertTrue(location.getKey().equals(getLocationKey(IP_KEY_1_22)));
        assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
        assertMountPoint(location.getAbsoluteLocation(), NODE_1);
    }

    /**
     * L3 <-> L2 --> one absolute location is created which key is derived from L2 endpoint
     * L3 <-> L2 <-> L3 --> two abs. loc. are created which key is derived from L3 endpoints
     * L3 <-> L2 <-> --> one absolute location is created which key is derived from L2 endpoint
     * removed --> no location should be present in datastore
     */
    @Test
    public void testLifecycle() {
        AddressEndpointBuilder child = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(child, IP_KEY_1_22);
        AddressEndpointBuilder firstParent = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        setChildEndpoints(firstParent, MAC_KEY_23_45);
        submitEndpointsToDatastore(child.build(), firstParent.build());
        assertEndpointsInDatastore(child.getKey(), firstParent.getKey());
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        List<ProviderAddressEndpointLocation> locations = readLocations();
        Assert.assertEquals(1, locations.size());
        ProviderAddressEndpointLocation location = locations.get(0);
        Assert.assertTrue(location.getKey().equals(getLocationKey(IP_KEY_1_22)));
        assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
        assertMountPoint(location.getAbsoluteLocation(), NODE_1);
        AddressEndpointBuilder secondParent = new AddressEndpointBuilder().setKey(IP_KEY_2_22);
        setChildEndpoints(secondParent, MAC_KEY_23_45);
        setParentEndpoints(child, IP_KEY_1_22, IP_KEY_2_22);
        submitEndpointsToDatastore(child.build(), secondParent.build());
        assertEndpointsInDatastore(child.getKey(), secondParent.getKey());
        locations = readLocations();
        Assert.assertEquals(2, locations.size());
        locations.forEach(loc -> {
            Assert.assertTrue(loc.getKey().equals(getLocationKey(IP_KEY_1_22))
                    || loc.getKey().equals(getLocationKey(IP_KEY_2_22)));
            assertNodeConnector(loc.getAbsoluteLocation(), INTF_NAME);
            assertMountPoint(loc.getAbsoluteLocation(), NODE_1);
        });
        setParentEndpoints(child, IP_KEY_1_22);
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        putEndpoints(wTx, child.build());
        deleteEndpoints(wTx, secondParent.getKey());
        DataStoreHelper.submitToDs(wTx);
        locations = readLocations();
        Assert.assertEquals(1, locations.size());
        locations.forEach(loc -> {
            Assert.assertTrue(loc.getKey().equals(getLocationKey(IP_KEY_1_22)));
            assertNodeConnector(loc.getAbsoluteLocation(), INTF_NAME);
            assertMountPoint(loc.getAbsoluteLocation(), NODE_1);
        });
        deleteEndpointsFromDatastore(child.getKey(), firstParent.getKey());
        locations = readLocations();
        Assert.assertTrue(locations == null || locations.isEmpty());
    }

    /**
     * Multihome use case
     * L2 <-> L3 <-> L2
     * Relative location should be created in DS which key is derived from L3 endpoint.
     * The location has two items pointing to corresponding L2 interfaces.
     */
    @Test
    public void testRelativeLocation_multihome() {
        AddressEndpointBuilder mac1 = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(mac1, IP_KEY_1_22);
        AddressEndpointBuilder mac2 = new AddressEndpointBuilder().setKey(MAC_KEY_24_45);
        setParentEndpoints(mac2, IP_KEY_1_22);
        AddressEndpointBuilder ip1 = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        setChildEndpoints(ip1, MAC_KEY_23_45, MAC_KEY_24_45);
        submitEndpointsToDatastore(mac1.build(), mac2.build(), ip1.build());
        assertEndpointsInDatastore(mac1.getKey(), mac2.getKey(), ip1.getKey());
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_24_45_VPP, NODE_2, INTF_NAME));
        List<ProviderAddressEndpointLocation> locations = readLocations();
        Assert.assertEquals(1, locations.size());
        List<ExternalLocation> extLocs = locations.get(0).getRelativeLocations().getExternalLocation();
        Assert.assertEquals(1, extLocs.stream().filter(extLoc -> extLoctoNodeId.apply(extLoc).equals(NODE_1)).count());
        Assert.assertEquals(1, extLocs.stream().filter(extLoc -> extLoctoNodeId.apply(extLoc).equals(NODE_2)).count());
        Assert.assertEquals(2,
                extLocs.stream().filter(extLoc -> extLoc.getExternalNodeConnector().contains(INTF_NAME)).count());
    }

    /**
     * Metadata use case in Openstack HA scenarios
     * A DHCP pot is created on each of three controller nodes. Metadata service can be reached
     * through any of the ports. This results in GBP into following endpoint configuration:
     * L3(DHCP1) <-> L2(1) <-> L3(METADATA)
     * L3(DHCP2) <-> L2(2) <-> L3(METADATA)
     * L3(DHCP3) <-> L2(3) <-> L3(METADATA)
     * Notice that Metadata endpoint has three L2 childs. Every L2 endpoint has two parents,
     * Metadata endpoint and L3 endpoint.
     * Locations created:
     * Metadata -> relative location with three l2 items, key is derived from L3 address
     * DHCP(x) -> absolute location, key is derived from L3 address
     */
    @Test
    public void testMetadataHaUseCase() {
        AddressEndpointBuilder mac1 = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(mac1, IP_KEY_1_22, IP_KEY_9_99);
        AddressEndpointBuilder mac2 = new AddressEndpointBuilder().setKey(MAC_KEY_24_45);
        setParentEndpoints(mac2, IP_KEY_2_22, IP_KEY_9_99);
        AddressEndpointBuilder mac3 = new AddressEndpointBuilder().setKey(MAC_KEY_25_45);
        setParentEndpoints(mac3, IP_KEY_3_22, IP_KEY_9_99);
        AddressEndpointBuilder ip1 = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        setChildEndpoints(ip1, MAC_KEY_23_45);
        AddressEndpointBuilder ip2 = new AddressEndpointBuilder().setKey(IP_KEY_2_22);
        setChildEndpoints(ip2, MAC_KEY_24_45);
        AddressEndpointBuilder ip3 = new AddressEndpointBuilder().setKey(IP_KEY_3_22);
        setChildEndpoints(ip3, MAC_KEY_25_45);
        AddressEndpointBuilder ip9 = new AddressEndpointBuilder().setKey(IP_KEY_9_99);
        setChildEndpoints(ip9, MAC_KEY_23_45, MAC_KEY_24_45, MAC_KEY_25_45);
        submitEndpointsToDatastore(mac1.build(), mac2.build(), mac3.build(), ip1.build(), ip2.build(), ip3.build(),
                ip9.build());
        assertEndpointsInDatastore(mac1.getKey(), mac2.getKey(), mac3.getKey(), ip1.getKey(), ip2.getKey(),
                ip3.getKey(), ip9.getKey());
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_24_45_VPP, NODE_2, INTF_NAME));
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_25_45_VPP, NODE_3, INTF_NAME));
        assertL2ChildHasTwoIpParentsAndIpParentHasMultipleChilds();
    }

    /**
     * Metadata use case where endpoints are created in more realistic order
     */
    @Test
    public void metadataHaUseCase_swapped_order() {
        AddressEndpointBuilder mac1 = new AddressEndpointBuilder().setKey(MAC_KEY_23_45);
        setParentEndpoints(mac1, IP_KEY_1_22, IP_KEY_9_99);
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_23_45_VPP, NODE_1, INTF_NAME));
        AddressEndpointBuilder ip1 = new AddressEndpointBuilder().setKey(IP_KEY_1_22);
        setChildEndpoints(ip1, MAC_KEY_23_45);
        AddressEndpointBuilder ip9 = new AddressEndpointBuilder().setKey(IP_KEY_9_99);
        setChildEndpoints(ip9, MAC_KEY_23_45);
        submitEndpointsToDatastore(mac1.build(), ip1.build(), ip9.build());
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_24_45_VPP, NODE_2, INTF_NAME));
        AddressEndpointBuilder mac2 = new AddressEndpointBuilder().setKey(MAC_KEY_24_45);
        setParentEndpoints(mac2, IP_KEY_2_22, IP_KEY_9_99);
        AddressEndpointBuilder ip2 = new AddressEndpointBuilder().setKey(IP_KEY_2_22);
        setChildEndpoints(ip2, MAC_KEY_24_45);
        ip9 = new AddressEndpointBuilder().setKey(IP_KEY_9_99);
        setChildEndpoints(ip9, MAC_KEY_23_45, MAC_KEY_24_45);
        submitEndpointsToDatastore(mac2.build(), ip2.build(), ip9.build());
        AddressEndpointBuilder mac3 = new AddressEndpointBuilder().setKey(MAC_KEY_25_45);
        setParentEndpoints(mac3, IP_KEY_3_22, IP_KEY_9_99);
        AddressEndpointBuilder ip3 = new AddressEndpointBuilder().setKey(IP_KEY_3_22);
        setChildEndpoints(ip3, MAC_KEY_25_45);
        ip9 = new AddressEndpointBuilder().setKey(IP_KEY_9_99);
        setChildEndpoints(ip9, MAC_KEY_23_45, MAC_KEY_24_45, MAC_KEY_25_45);
        locationProvider.createLocationForVppEndpoint(vppEndpointBuilder(MAC_KEY_25_45_VPP, NODE_3, INTF_NAME));
        submitEndpointsToDatastore(mac3.build(), ip3.build(), ip9.build());
        assertEndpointsInDatastore(mac1.getKey(), mac2.getKey(), mac3.getKey(), ip1.getKey(), ip2.getKey(),
                ip3.getKey(), ip9.getKey());
        assertL2ChildHasTwoIpParentsAndIpParentHasMultipleChilds();
    }

    private void assertL2ChildHasTwoIpParentsAndIpParentHasMultipleChilds() {
        List<ProviderAddressEndpointLocation> locations = readLocations();
        Assert.assertEquals(locations.size(), 4);
        locations.forEach(location -> {
            if (location.getKey().equals(getLocationKey(IP_KEY_1_22))) {
                assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
                assertMountPoint(location.getAbsoluteLocation(), NODE_1);
            } else if (location.getKey().equals(getLocationKey(IP_KEY_2_22))) {
                assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
                assertMountPoint(location.getAbsoluteLocation(), NODE_2);
            } else if (location.getKey().equals(getLocationKey(IP_KEY_3_22))) {
                assertNodeConnector(location.getAbsoluteLocation(), INTF_NAME);
                assertMountPoint(location.getAbsoluteLocation(), NODE_3);
            } else if (location.getKey().equals(getLocationKey(IP_KEY_9_99))) {
                List<ExternalLocation> extLocs = location.getRelativeLocations().getExternalLocation();
                Assert.assertEquals(1,
                        extLocs.stream().filter(extLoc -> extLoctoNodeId.apply(extLoc).equals(NODE_1)).count());
                Assert.assertEquals(1,
                        extLocs.stream().filter(extLoc -> extLoctoNodeId.apply(extLoc).equals(NODE_2)).count());
                Assert.assertEquals(1,
                        extLocs.stream().filter(extLoc -> extLoctoNodeId.apply(extLoc).equals(NODE_3)).count());
                Assert.assertEquals(3, extLocs.stream()
                    .filter(extLoc -> extLoc.getExternalNodeConnector().contains(INTF_NAME))
                    .count());
            }
        });
    }

    private static void assertNodeConnector(AbsoluteLocation absoluteLocation, String intfName) {
        ExternalLocationCase extLocation = (ExternalLocationCase) absoluteLocation.getLocationType();
        Assert.assertTrue(extLocation.getExternalNodeConnector().contains(intfName));
    }

    private static void assertMountPoint(AbsoluteLocation absoluteLocation, NodeId nodeId) {
        ExternalLocationCase extLocation = (ExternalLocationCase) absoluteLocation.getLocationType();
        NodeId ref = extLocation.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId();
        Assert.assertTrue(ref.equals(nodeId));
    }

    private static void setParentEndpoints(@Nonnull AddressEndpointBuilder builder,
            @Nonnull AddressEndpointKey... parents) {
        List<ParentEndpoint> builtParents = Arrays.stream(parents)
            .map(key -> new ParentEndpointBuilder().setKey(getParentKey(key)).build())
            .collect(Collectors.toList());
        builder.setParentEndpointChoice(new ParentEndpointCaseBuilder().setParentEndpoint(builtParents).build());
    }

    private static void setChildEndpoints(@Nonnull AddressEndpointBuilder builder,
            @Nonnull AddressEndpointKey... childs) {
        List<ChildEndpoint> builtChilds = Arrays.stream(childs)
            .map(key -> new ChildEndpointBuilder().setKey(getChildKey(key)).build())
            .collect(Collectors.toList());
        builder.setChildEndpoint(builtChilds).build();
    }

    private static ParentEndpointKey getParentKey(AddressEndpointKey key) {
        return new ParentEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    private static ChildEndpointKey getChildKey(AddressEndpointKey key) {
        return new ChildEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    private static VppEndpoint vppEndpointBuilder(VppEndpointKey vppEpKey, NodeId nodeId, String interfaceName) {
        final VppEndpointBuilder vppEndpointBuilder = new VppEndpointBuilder();
        vppEndpointBuilder.setKey(vppEpKey).setVppNodeId(nodeId).setVppInterfaceName(interfaceName);
        return vppEndpointBuilder.build();
    }

    private static ProviderAddressEndpointLocationKey getLocationKey(AddressEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    private void assertEndpointsInDatastore(AddressEndpointKey... keys) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        for (AddressEndpointKey key : keys) {
            Assert.assertTrue(DataStoreHelper
                .readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key), rTx).isPresent());
        }
        rTx.close();
    }

    private static void putEndpoints(WriteTransaction wTx, AddressEndpoint... endpoints) {
        for (AddressEndpoint endpoint : endpoints) {
            wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(endpoint.getKey()), endpoint);
        }
    }

    private static void deleteEndpoints(WriteTransaction wTx, AddressEndpointKey... keys) {
        for (AddressEndpointKey key : keys) {
            wTx.delete(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key));
        }
    }

    private void deleteEndpointsFromDatastore(AddressEndpointKey... keys) {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        for (AddressEndpointKey key : keys) {
            wTx.delete(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key));
        }
        DataStoreHelper.submitToDs(wTx);
    }

    private List<ProviderAddressEndpointLocation> readLocations() {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> locations = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER), rTx);
        rTx.close();
        Assert.assertTrue(locations.isPresent());
        return locations.get().getProviderAddressEndpointLocation();
    }

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(Endpoints.class, Config.class, LocationProviders.class,
                AddressEndpoints.class, MacAddressType.class, IpPrefixType.class);
    }

    private void submitEndpointsToDatastore(AddressEndpoint... endpoints) {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        for (AddressEndpoint endpoint : endpoints) {
            wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(endpoint.getKey()), endpoint);
        }
        DataStoreHelper.submitToDs(wTx);
    }
}
