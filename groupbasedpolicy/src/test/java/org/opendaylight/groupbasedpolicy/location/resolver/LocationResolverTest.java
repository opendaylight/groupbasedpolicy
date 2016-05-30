/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.location.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.ProviderName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProviderKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderContainmentEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class LocationResolverTest extends CustomDataBrokerTest {

    private final String PROVIDER_NAME = "location-provider";
    private final String ADDRESS = "192.168.50.20/24";
    private final String NODE_1 = "node1";
    private final String NODE_2 = "node2";
    private final String NODE_CONNNECTOR = "connector";
    private final ContextId contextId = new ContextId("context");

    private InstanceIdentifier<Node> nodeIid1 =
            InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(NODE_1))).build();
    private InstanceIdentifier<Node> nodeIid2 =
            InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(NODE_2))).build();
    private InstanceIdentifier<NodeConnector> connectorIid = InstanceIdentifier.builder(Nodes.class)
        .child(Node.class, new NodeKey(new NodeId(NODE_1)))
        .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(NODE_CONNNECTOR)))
        .build();
    private DataBroker dataBroker;
    private LocationResolver resolver;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        resolver = new LocationResolver(dataBroker);
    }

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(LocationProvider.class, Nodes.class, EndpointLocations.class);
    }

    @Test
    public void testOnDataTreeChanged_write() throws Exception {
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(new InternalLocationCaseBuilder()
                    .setInternalNode(nodeIid1).setInternalNodeConnector(connectorIid).build()).build();
        RelativeLocations relativeLocations = new RelativeLocationsBuilder()
            .setInternalLocation(Collections.singletonList(new InternalLocationBuilder().setInternalNode(nodeIid1)
                .setInternalNodeConnector(connectorIid)
                .build()))
            .build();
        LocationProvider provider = new LocationProviderBuilder().setProvider(new ProviderName(PROVIDER_NAME))
            .setProviderAddressEndpointLocation(
                    Collections.singletonList(new ProviderAddressEndpointLocationBuilder()
                        .setKey(new ProviderAddressEndpointLocationKey(ADDRESS, AddressType.class, contextId,
                                ContextType.class))
                        .setAbsoluteLocation(absoluteLocation)
                        .setRelativeLocations(relativeLocations)
                        .build()))
            .setProviderContainmentEndpointLocation(
                    Collections.singletonList(new ProviderContainmentEndpointLocationBuilder().setContextId(contextId)
                        .setContextType(ContextType.class)
                        .setRelativeLocations(relativeLocations)
                        .build()))
            .build();
        InstanceIdentifier<LocationProvider> iid = InstanceIdentifier.builder(LocationProviders.class)
            .child(LocationProvider.class, provider.getKey())
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, iid, provider);
        wtx.submit().get();

        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<EndpointLocations> readIid = InstanceIdentifier.builder(EndpointLocations.class).build();
        Optional<EndpointLocations> read = rtx.read(LogicalDatastoreType.OPERATIONAL, readIid).get();
        assertTrue(read.isPresent());
        EndpointLocations readLocations = read.get();
        assertNotNull(readLocations.getAddressEndpointLocation());
        assertEquals(1, readLocations.getAddressEndpointLocation().size());
        assertEquals(new AddressEndpointLocationKey(ADDRESS, AddressType.class, contextId, ContextType.class),
                readLocations.getAddressEndpointLocation().get(0).getKey());
        assertEquals(absoluteLocation, readLocations.getAddressEndpointLocation().get(0).getAbsoluteLocation());
        assertNotNull(readLocations.getContainmentEndpointLocation());
        assertEquals(1, readLocations.getContainmentEndpointLocation().size());
        assertEquals(new ContainmentEndpointLocationKey(contextId, ContextType.class),
                readLocations.getContainmentEndpointLocation().get(0).getKey());
        assertEquals(relativeLocations, readLocations.getContainmentEndpointLocation().get(0).getRelativeLocations());
    }

    @Test
    public void testOnDataTreeChanged_overWrite() throws Exception {
        testOnDataTreeChanged_write();
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(new InternalLocationCaseBuilder()
                    .setInternalNode(nodeIid2).setInternalNodeConnector(connectorIid).build()).build();
        RelativeLocations relativeLocations = new RelativeLocationsBuilder()
            .setInternalLocation(Collections.singletonList(new InternalLocationBuilder().setInternalNode(nodeIid2)
                .setInternalNodeConnector(connectorIid)
                .build()))
            .build();
        LocationProvider provider = new LocationProviderBuilder().setProvider(new ProviderName(PROVIDER_NAME))
            .setProviderAddressEndpointLocation(
                    Collections.singletonList(new ProviderAddressEndpointLocationBuilder()
                        .setKey(new ProviderAddressEndpointLocationKey(ADDRESS, AddressType.class, contextId,
                                ContextType.class))
                        .setAbsoluteLocation(absoluteLocation)
                        .setRelativeLocations(relativeLocations)
                        .build()))
            .setProviderContainmentEndpointLocation(
                    Collections.singletonList(new ProviderContainmentEndpointLocationBuilder().setContextId(contextId)
                        .setContextType(ContextType.class)
                        .setRelativeLocations(relativeLocations)
                        .build()))
            .build();
        InstanceIdentifier<LocationProvider> iid = InstanceIdentifier.builder(LocationProviders.class)
            .child(LocationProvider.class, provider.getKey())
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, iid, provider);
        wtx.submit().get();

        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<EndpointLocations> readIid = InstanceIdentifier.builder(EndpointLocations.class).build();
        Optional<EndpointLocations> read = rtx.read(LogicalDatastoreType.OPERATIONAL, readIid).get();
        assertTrue(read.isPresent());
        EndpointLocations readLocations = read.get();
        assertNotNull(readLocations.getAddressEndpointLocation());
        assertEquals(1, readLocations.getAddressEndpointLocation().size());
        assertEquals(new AddressEndpointLocationKey(ADDRESS, AddressType.class, contextId, ContextType.class),
                readLocations.getAddressEndpointLocation().get(0).getKey());
        assertEquals(absoluteLocation, readLocations.getAddressEndpointLocation().get(0).getAbsoluteLocation());
        assertNotNull(readLocations.getContainmentEndpointLocation());
        assertEquals(1, readLocations.getContainmentEndpointLocation().size());
        assertEquals(new ContainmentEndpointLocationKey(contextId, ContextType.class),
                readLocations.getContainmentEndpointLocation().get(0).getKey());
        assertEquals(relativeLocations, readLocations.getContainmentEndpointLocation().get(0).getRelativeLocations());
    }

    @Test
    public void testOnDataTreeChanged_delete() throws Exception {
        testOnDataTreeChanged_write();
        InstanceIdentifier<LocationProvider> iid = InstanceIdentifier.builder(LocationProviders.class)
            .child(LocationProvider.class, new LocationProviderKey(new ProviderName(PROVIDER_NAME)))
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
        wtx.submit().get();

        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<EndpointLocations> readIid = InstanceIdentifier.builder(EndpointLocations.class).build();
        Optional<EndpointLocations> read = rtx.read(LogicalDatastoreType.OPERATIONAL, readIid).get();
        assertTrue(read.isPresent());
        EndpointLocations readLocations = read.get();
        assertEquals(1, readLocations.getAddressEndpointLocation().size());
        assertNull(readLocations.getAddressEndpointLocation().get(0).getAbsoluteLocation());
        assertTrue(readLocations.getAddressEndpointLocation()
            .get(0)
            .getRelativeLocations()
            .getInternalLocation()
            .isEmpty());
        assertNull(readLocations.getAddressEndpointLocation().get(0).getRelativeLocations().getExternalLocation());
        assertEquals(1, readLocations.getContainmentEndpointLocation().size());
        assertTrue(readLocations.getAddressEndpointLocation()
            .get(0)
            .getRelativeLocations()
            .getInternalLocation()
            .isEmpty());
        assertNull(readLocations.getAddressEndpointLocation().get(0).getRelativeLocations().getExternalLocation());
    }

    @Test
    public void testOnDataTreeChanged_modify() throws Exception {
        testOnDataTreeChanged_write();
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(new InternalLocationCaseBuilder()
                    .setInternalNode(nodeIid2).setInternalNodeConnector(connectorIid).build()).build();
        InstanceIdentifier<AbsoluteLocation> iid = InstanceIdentifier.builder(LocationProviders.class)
            .child(LocationProvider.class, new LocationProviderKey(new ProviderName(PROVIDER_NAME)))
            .child(ProviderAddressEndpointLocation.class,
                    new ProviderAddressEndpointLocationKey(ADDRESS, AddressType.class, contextId, ContextType.class))
            .child(AbsoluteLocation.class)
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, iid, absoluteLocation);
        wtx.submit().get();

        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<EndpointLocations> readIid = InstanceIdentifier.builder(EndpointLocations.class).build();
        Optional<EndpointLocations> read = rtx.read(LogicalDatastoreType.OPERATIONAL, readIid).get();
        assertTrue(read.isPresent());
        EndpointLocations readLocations = read.get();
        assertNotNull(readLocations.getAddressEndpointLocation());
        assertEquals(1, readLocations.getAddressEndpointLocation().size());
        assertEquals(new AddressEndpointLocationKey(ADDRESS, AddressType.class, contextId, ContextType.class),
                readLocations.getAddressEndpointLocation().get(0).getKey());
        assertEquals(absoluteLocation, readLocations.getAddressEndpointLocation().get(0).getAbsoluteLocation());
    }
}
