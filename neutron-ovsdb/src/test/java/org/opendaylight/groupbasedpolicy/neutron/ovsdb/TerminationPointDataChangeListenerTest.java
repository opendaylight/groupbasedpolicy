/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TerminationPointDataChangeListenerTest {

    private TerminationPointDataChangeListener listener;

    private DataBroker dataBroker;
    private EndpointService epService;
    private ListenerRegistration<DataChangeListener> registration;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;

    private Map<InstanceIdentifier<?>, DataObject> dataMap;
    private Set<InstanceIdentifier<?>> dataSet;
    private Node node;

    private CheckedFuture<Optional<OvsdbBridgeAugmentation>, ReadFailedException> ovsdbBridgeFuture;
    private CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture;
    private CheckedFuture<Optional<EndpointByPort>, ReadFailedException> endpointByPortFuture;
    private CheckedFuture<Optional<Endpoint>, ReadFailedException> endpointFuture;
    private CheckedFuture<Optional<OfOverlayNodeConfig>, ReadFailedException> ofOverlayNodeConfigFuture;
    private ReadOnlyTransaction readTransaction;
    private ReadWriteTransaction readWriteTransaction;

    private OvsdbTerminationPointAugmentation ovsdbTp;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void init() throws Exception {
        dataBroker = mock(DataBroker.class);
        epService = mock(EndpointService.class);
        registration = mock(ListenerRegistration.class);
        when(
                dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(registration);
        change = mock(AsyncDataChangeEvent.class);

        InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid = InstanceIdentifier.create(
                NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .child(TerminationPoint.class)
            .augmentation(OvsdbTerminationPointAugmentation.class);
        ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getInterfaceType()).thenReturn((Class) Object.class);
        InterfaceExternalIds externalId = mock(InterfaceExternalIds.class);
        when(ovsdbTp.getInterfaceExternalIds()).thenReturn(Collections.singletonList(externalId));
        when(externalId.getExternalIdKey()).thenReturn("iface-id");
        when(externalId.getExternalIdValue()).thenReturn(UUID.randomUUID().toString());

        dataMap = new HashMap<>();
        dataMap.put(ovsdbTpIid, ovsdbTp);
        dataSet = new HashSet<InstanceIdentifier<?>>(Collections.singletonList(ovsdbTpIid));

        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.submit()).thenReturn(submitFuture);

        // OvsdbBridgeAugmentation
        ovsdbBridgeFuture = mock(CheckedFuture.class);
        Optional<OvsdbBridgeAugmentation> ovsdbOptional = mock(Optional.class);
        when(ovsdbBridgeFuture.checkedGet()).thenReturn(ovsdbOptional);
        when(ovsdbOptional.isPresent()).thenReturn(true);
        OvsdbBridgeAugmentation ovsdbBridge = mock(OvsdbBridgeAugmentation.class);
        when(ovsdbOptional.get()).thenReturn(ovsdbBridge);

        DatapathId dpid = mock(DatapathId.class);
        when(ovsdbBridge.getDatapathId()).thenReturn(dpid);
        when(dpid.getValue()).thenReturn("FF:FF:FF:FF:FF:FF:FF:FF");

        OvsdbBridgeName bridgeName = mock(OvsdbBridgeName.class);
        when(ovsdbBridge.getBridgeName()).thenReturn(bridgeName);
        when(bridgeName.getValue()).thenReturn("bridgeName");

        // Node
        nodeFuture = mock(CheckedFuture.class);
        Optional<Node> nodeOptional = mock(Optional.class);
        when(nodeFuture.checkedGet()).thenReturn(nodeOptional);
        when(nodeOptional.isPresent()).thenReturn(true);
        node = mock(Node.class);
        when(nodeOptional.get()).thenReturn(node);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNode);

        // EndpointByPort
        endpointByPortFuture = mock(CheckedFuture.class);
        Optional<EndpointByPort> endpointByPortOptional = mock(Optional.class);
        when(endpointByPortFuture.checkedGet()).thenReturn(endpointByPortOptional);
        when(endpointByPortOptional.isPresent()).thenReturn(true);
        EndpointByPort endpointByPort = mock(EndpointByPort.class);
        when(endpointByPortOptional.get()).thenReturn(endpointByPort);
        L2BridgeDomainId l2BridgeDomainId = mock(L2BridgeDomainId.class);
        MacAddress macAddress = mock(MacAddress.class);
        when(endpointByPort.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpointByPort.getMacAddress()).thenReturn(macAddress);

        // Endpoint
        endpointFuture = mock(CheckedFuture.class);
        Optional<Endpoint> endpointOptional = mock(Optional.class);
        when(endpointFuture.checkedGet()).thenReturn(endpointOptional);
        when(endpointOptional.isPresent()).thenReturn(true);
        OfOverlayContext ofc = mock(OfOverlayContext.class);
        Endpoint endpoint = new EndpointBuilder().setL2Context(new L2BridgeDomainId("foo"))
            .setMacAddress(new MacAddress("01:23:45:67:89:AB"))
            .setTenant(new TenantId("fooTenant"))
            .addAugmentation(OfOverlayContext.class, ofc)
            .build();
        when(endpointOptional.get()).thenReturn(endpoint);

        // OfOverlayNodeConfig
        ofOverlayNodeConfigFuture = mock(CheckedFuture.class);
        Optional<OfOverlayNodeConfig> ofOverlayNodeConfigOptional = mock(Optional.class);
        when(ofOverlayNodeConfigFuture.checkedGet()).thenReturn(ofOverlayNodeConfigOptional);
        when(ofOverlayNodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig ofOverlayNodeConfig = mock(OfOverlayNodeConfig.class);
        when(ofOverlayNodeConfigOptional.get()).thenReturn(ofOverlayNodeConfig);

        listener = new TerminationPointDataChangeListener(dataBroker, epService);
    }

    @Test
    public void testConstructor() throws Exception {
        listener.close();
        verify(registration).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_Creation() {
        when(change.getCreatedData()).thenReturn(dataMap);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                ovsdbBridgeFuture)
            .thenReturn(endpointFuture)
            .thenReturn(nodeFuture);

        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                endpointByPortFuture).thenReturn(ofOverlayNodeConfigFuture);

        listener.onDataChanged(change);
        verify(readWriteTransaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_CreationExternalIdNull() {
        when(change.getCreatedData()).thenReturn(dataMap);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                ovsdbBridgeFuture).thenReturn(nodeFuture);
        when(ovsdbTp.getInterfaceExternalIds()).thenReturn(null);

        listener.onDataChanged(change);
        verify(readWriteTransaction, never()).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_Update() {
        when(change.getUpdatedData()).thenReturn(dataMap);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                ovsdbBridgeFuture)
            .thenReturn(endpointFuture)
            .thenReturn(nodeFuture);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                endpointByPortFuture).thenReturn(ofOverlayNodeConfigFuture);

        listener.onDataChanged(change);
        verify(readWriteTransaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_Removal() {
        when(change.getRemovedPaths()).thenReturn(dataSet);
        when(change.getOriginalData()).thenReturn(dataMap);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                endpointByPortFuture);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                endpointFuture);

        listener.onDataChanged(change);
        verify(readWriteTransaction).submit();
    }
}
