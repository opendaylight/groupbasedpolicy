/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ne.location.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.NetworkElements;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.NetworkElementsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.NetworkElement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.NetworkElementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.NetworkElementKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element._interface.EndpointNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element._interface.EndpointNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element._interface.EndpointNetworkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;

public class NeLocationProviderTest extends CustomDataBrokerTest {

    private DataBroker dataBroker;
    private NeLocationProvider neProvider;
    private String L3_CONTEXT_ID = "l3Context";
    private String IPv4_HOST_ADDRESS_1 = "192.168.50.71";
    private String IPv4_HOST_ADDRESS_2 = "192.168.50.72";
    private String IPv4_NETWORK_ADDRESS_1 = "192.168.50.0/24";
    private String IPv4_NETWORK_ADDRESS_2 = "192.168.51.0/24";
    private String NODE_ID_1 = "node1";
    private String NODE_ID_2 = "node2";
    private String CONNECTOR_ID_1 = "connector:1";
    private String CONNECTOR_ID_2 = "connector:2";

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(NetworkElements.class, EndpointLocations.class, Endpoints.class, L3Context.class, Nodes.class);
    }

    @Before
    public void init() {
        dataBroker = getDataBroker();
        neProvider = new NeLocationProvider(dataBroker);
    }

    @Test
    public void test_NetworkElementsListenerRegistration() {
        DataBroker dataBroker = mock(DataBroker.class);
        NeLocationProvider provider = new NeLocationProvider(dataBroker);
        verify(dataBroker).registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(NetworkElements.class).build()), provider);
    }

    @Test
    public void test_EndpointsListenerRegistration() {
        DataBroker dataBroker = mock(DataBroker.class);
        new NeLocationProvider(dataBroker);
        verify(dataBroker)
            .registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                    InstanceIdentifier.builder(Endpoints.class).child(AddressEndpoints.class)
                    .child(AddressEndpoint.class).build())), isA(EndpointsListener.class));
    }

    @Test
    public void test_ListenersUnregistration() {
        DataBroker dataBroker = mock(DataBroker.class);
        ListenerRegistration<EndpointsListener> endpointsListenerRegistration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataTreeChangeListener(any(), isA(EndpointsListener.class)))
            .thenReturn(endpointsListenerRegistration);
        ListenerRegistration<NeLocationProvider> NEListenerRegistration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataTreeChangeListener(any(), isA(NeLocationProvider.class)))
            .thenReturn(NEListenerRegistration);
        NeLocationProvider provider = new NeLocationProvider(dataBroker);
        provider.close();
        verify(endpointsListenerRegistration).close();
        verify(NEListenerRegistration).close();
    }

    @Test
    public void test_AddressEndpointWrite_NoNE_NoOverwrite() throws Exception {
        AddressEndpoint endpoint =
                createAddressEndpoint(IPv4_HOST_ADDRESS_1, IpPrefixType.class, L3_CONTEXT_ID, L3Context.class);
        InstanceIdentifier<AddressEndpoint> iid = IidFactory.addressEndpointIid(endpoint.getKey());
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, iid, endpoint, true);
        wtx.submit().get();
        List<AddressEndpoint> endpoints = neProvider.getEndpoints();
        assertEquals(1, endpoints.size());
        assertEquals(endpoint, endpoints.get(0));
        verifyEmptyLocations();
    }

    @Test
    public void test_AddressEndpointWrite_NoNE_Overwrite() throws Exception {
        test_AddressEndpointWrite_NoNE_NoOverwrite();
        NetworkContainment nc = new NetworkContainmentBuilder()
                .setNetworkDomainId(new NetworkDomainId(L3_CONTEXT_ID))
                .build();
        InstanceIdentifier<NetworkContainment> iid = InstanceIdentifier
                .builder(Endpoints.class)
                .child(AddressEndpoints.class)
                .child(AddressEndpoint.class, new AddressEndpointKey(IPv4_HOST_ADDRESS_1, IpPrefixType.class,
                        new ContextId(L3_CONTEXT_ID), L3Context.class))
                .child(NetworkContainment.class)
                .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, iid, nc);
        wtx.submit().get();
        List<AddressEndpoint> l3Endpoint = neProvider.getEndpoints();
        assertEquals(1, l3Endpoint.size());
        assertNotNull(l3Endpoint.get(0).getNetworkContainment());
        assertEquals(nc, l3Endpoint.get(0).getNetworkContainment());
        verifyEmptyLocations();
    }

    @Test
    public void test_EndpointsDelete_NoNE() throws Exception {
        test_AddressEndpointWrite_NoNE_NoOverwrite();
        InstanceIdentifier<Endpoints> iid = InstanceIdentifier.builder(Endpoints.class)
                .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
        wtx.submit().get();
        List<AddressEndpoint> l3Endpoint = neProvider.getEndpoints();
        assertEquals(0, l3Endpoint.size());
        verifyEmptyLocations();
    }

    @Test
    public void test_EndpointsModify_NoNE() throws Exception {
        test_AddressEndpointWrite_NoNE_NoOverwrite();
        InstanceIdentifier<Endpoints> iid = InstanceIdentifier.builder(Endpoints.class)
                .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
        wtx.submit().get();
        List<AddressEndpoint> l3Endpoint = neProvider.getEndpoints();
        assertEquals(0, l3Endpoint.size());
        verifyEmptyLocations();
    }

    @Test
    public void test_NetworkElementsWrite_NoEP_NoOverwrite() throws Exception {
        NetworkElements nes = createNetworkElements(NODE_ID_1, CONNECTOR_ID_1, L3_CONTEXT_ID, IPv4_NETWORK_ADDRESS_1);
        InstanceIdentifier<NetworkElements> iid = InstanceIdentifier.builder(NetworkElements.class).build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, nes);
        wtx.submit().get();
        NetworkElements networkElements = neProvider.getNetworkElements();
        assertEquals(nes, networkElements);
        verifyEmptyLocations();
    }

    @Test
    public void test_NetworkElementsWrite_NoEP_Overwrite() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        NetworkElements nes = createNetworkElements(NODE_ID_2, CONNECTOR_ID_2, L3_CONTEXT_ID, IPv4_NETWORK_ADDRESS_1);
        InstanceIdentifier<NetworkElements> iid = InstanceIdentifier.builder(NetworkElements.class).build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, nes);
        wtx.submit().get();
        NetworkElements networkElements = neProvider.getNetworkElements();
        assertEquals(nes, networkElements);
        verifyEmptyLocations();
    }

    @Test
    public void test_NetworkElementWrite_NoEP_Overwrite() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        NetworkElement ne = createNetworkElement(NODE_ID_1, CONNECTOR_ID_1, L3_CONTEXT_ID, IPv4_NETWORK_ADDRESS_2);
        InstanceIdentifier<NetworkElement> iid = InstanceIdentifier.builder(NetworkElements.class)
            .child(NetworkElement.class, new NetworkElementKey(ne.getKey()))
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, ne);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertNotNull(nes.getNetworkElement());
        assertEquals(1, nes.getNetworkElement().size());
        assertEquals(ne, nes.getNetworkElement().get(0));
        verifyEmptyLocations();
    }

    @Test
    public void test_InterfaceWrite_NoEP_Overwrite() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        Interface iface = createInterface(NODE_ID_1, CONNECTOR_ID_1, L3_CONTEXT_ID, IPv4_NETWORK_ADDRESS_2);
        InstanceIdentifier<Interface> iid = InstanceIdentifier.builder(NetworkElements.class)
            .child(NetworkElement.class, new NetworkElementKey(createNetworkElementIid(NODE_ID_1)))
            .child(Interface.class, new InterfaceKey(iface.getKey()))
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, iface);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertNotNull(nes.getNetworkElement());
        assertEquals(1, nes.getNetworkElement().size());
        assertNotNull(nes.getNetworkElement().get(0).getInterface());
        assertEquals(1, nes.getNetworkElement().get(0).getInterface().size());
        assertEquals(iface, nes.getNetworkElement().get(0).getInterface().get(0));
        verifyEmptyLocations();
    }

    @Test
    public void test_EndpointNetworkChange_NoEP() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        EndpointNetwork en = createEndpointNetwork(L3_CONTEXT_ID, IPv4_NETWORK_ADDRESS_2);
        InstanceIdentifier<EndpointNetwork> iid = InstanceIdentifier.builder(NetworkElements.class)
            .child(NetworkElement.class, new NetworkElementKey(createNetworkElementIid(NODE_ID_1)))
            .child(Interface.class, new InterfaceKey(createInterfaceIid(NODE_ID_1, CONNECTOR_ID_1)))
            .child(EndpointNetwork.class, new EndpointNetworkKey(en.getKey()))
            .build();
        InstanceIdentifier<EndpointNetwork> removeIid =
                InstanceIdentifier.builder(NetworkElements.class)
                    .child(NetworkElement.class, new NetworkElementKey(createNetworkElementIid(NODE_ID_1)))
                    .child(Interface.class, new InterfaceKey(createInterfaceIid(NODE_ID_1, CONNECTOR_ID_1)))
                    .child(EndpointNetwork.class, new EndpointNetworkKey(
                            new IpPrefix(new Ipv4Prefix(IPv4_NETWORK_ADDRESS_1)),new ContextId(L3_CONTEXT_ID)))
                    .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, removeIid);
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, en);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertNotNull(nes.getNetworkElement());
        assertEquals(1, nes.getNetworkElement().size());
        assertNotNull(nes.getNetworkElement().get(0).getInterface());
        assertEquals(1, nes.getNetworkElement().get(0).getInterface().size());
        assertNotNull(nes.getNetworkElement().get(0).getInterface().get(0).getEndpointNetwork());
        assertEquals(1, nes.getNetworkElement().get(0).getInterface().get(0).getEndpointNetwork().size());
        assertEquals(en, nes.getNetworkElement().get(0).getInterface().get(0).getEndpointNetwork().get(0));
        verifyEmptyLocations();
    }

    @Test
    public void test_NetworkElementsDelete() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        InstanceIdentifier<NetworkElements> iid = InstanceIdentifier.builder(NetworkElements.class).build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertEquals(new NetworkElementsBuilder().build(), nes);
        verifyEmptyLocations();
    }

    @Test
    public void test_NetworkElementDelete() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        InstanceIdentifier<NetworkElement> iid = InstanceIdentifier.builder(NetworkElements.class)
            .child(NetworkElement.class, new NetworkElementKey(createNetworkElementIid(NODE_ID_1)))
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertNotNull(nes.getNetworkElement());
        assertTrue(nes.getNetworkElement().isEmpty());
        verifyEmptyLocations();
    }

    @Test
    public void test_InterfaceDelete() throws Exception {
        test_NetworkElementsWrite_NoEP_NoOverwrite();
        InstanceIdentifier<Interface> iid = InstanceIdentifier.builder(NetworkElements.class)
            .child(NetworkElement.class, new NetworkElementKey(createNetworkElementIid(NODE_ID_1)))
            .child(Interface.class, new InterfaceKey(createInterfaceIid(NODE_ID_1, CONNECTOR_ID_1)))
            .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        wtx.submit().get();
        NetworkElements nes = neProvider.getNetworkElements();
        assertNotNull(nes.getNetworkElement());
        assertEquals(1, nes.getNetworkElement().size());
        assertNotNull(nes.getNetworkElement().get(0).getInterface());
        assertTrue(nes.getNetworkElement().get(0).getInterface().isEmpty());
        verifyEmptyLocations();
    }

    private AddressEndpoint createAddressEndpoint(String ipAddr, Class<? extends AddressType> addrType,
            String context, Class<? extends ContextType> cType) {
        return new AddressEndpointBuilder().setAddress(ipAddr).setAddressType(addrType)
                .setContextId(new ContextId(context)).setContextType(cType).build();
    }

    private NetworkElements createNetworkElements(String node, String iface, String l3c, String prefix) {
        return new NetworkElementsBuilder()
            .setNetworkElement(Collections.singletonList(createNetworkElement(node, iface, l3c, prefix))).build();
    }

    private NetworkElement createNetworkElement(String node, String iface, String l3c, String prefix) {
        return new NetworkElementBuilder().setIid(createNetworkElementIid(node))
            .setInterface(Collections.singletonList(createInterface(node, iface, l3c, prefix)))
            .build();
    }

    private Interface createInterface(String node, String iface, String l3c, String prefix) {
        return new InterfaceBuilder().setIid(createInterfaceIid(node, iface))
            .setEndpointNetwork(Collections.singletonList(createEndpointNetwork(l3c, prefix)))
            .build();
    }

    private EndpointNetwork createEndpointNetwork(String l3c, String prefix) {
        return new EndpointNetworkBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix(prefix)))
            .setL3ContextId(new ContextId(l3c))
            .build();
    }

    private InstanceIdentifier<?> createNetworkElementIid(String node) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(node))).build();
    }

    private InstanceIdentifier<?> createInterfaceIid(String node, String connector) {
        return InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(node)))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(connector)))
            .build();
    }

    private void verifyEmptyLocations() throws Exception {
        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<EndpointLocations> locationIid = InstanceIdentifier.builder(EndpointLocations.class).build();
        CheckedFuture<Optional<EndpointLocations>, ReadFailedException> read =
                rtx.read(LogicalDatastoreType.OPERATIONAL, locationIid);
        assertFalse(read.get().isPresent());
        rtx.close();
    }
}
