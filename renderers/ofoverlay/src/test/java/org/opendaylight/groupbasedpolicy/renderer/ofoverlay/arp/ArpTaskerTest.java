/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.OfOverlayDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.TenantsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class ArpTaskerTest extends OfOverlayDataBrokerTest {

    private ArpTasker arpTasker;
    private DataBroker broker;
    private PacketProcessingService packetService;
    private SalFlowService flowService;

    @Before
    public void init() {

        packetService = mock(PacketProcessingService.class);
        flowService = mock(SalFlowService.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addMacForL3EpAndCreateEp_onPacketReceivedTest() throws Exception {

        IpAddress ipAddr = new IpAddress(new Ipv4Address("192.168.0.1"));
        L3ContextId l3conId = new L3ContextId("l3context");
        EndpointL3Key key = new EndpointL3Key(ipAddr, l3conId);
        EndpointL3Builder epL3 = new EndpointL3Builder();
        // node conector
        NodeConnectorId connectorId = new NodeConnectorId("nodeConnector");
        MacAddress macAddr = new MacAddress("00:00:00:00:00:01");
        FlowCapableNodeConnectorBuilder fcnConnector =
                new FlowCapableNodeConnectorBuilder().setHardwareAddress(macAddr);
        NodeConnectorBuilder connector = new NodeConnectorBuilder().setKey(new NodeConnectorKey(connectorId))
            .addAugmentation(FlowCapableNodeConnector.class, fcnConnector.build());
        // node
        NodeId nodeId = new NodeId("node");
        ExternalInterfacesBuilder extIface = new ExternalInterfacesBuilder().setNodeConnectorId(connectorId);
        OfOverlayNodeConfigBuilder ofOverNodeCfg =
                new OfOverlayNodeConfigBuilder().setExternalInterfaces(Collections.singletonList(extIface.build()));
        NodeBuilder node = new NodeBuilder().addAugmentation(OfOverlayNodeConfig.class, ofOverNodeCfg.build())
                .setKey(new NodeKey(nodeId))
                .setId(nodeId)
                .setNodeConnector(Collections.singletonList(connector.build()));
        // subnet
        NetworkDomainId domainId = new NetworkDomainId("domainId");
        TenantId tenantId = new TenantId("tenant");
        L2ContextId l2conId = new L2ContextId("l2context");
        SubnetBuilder subnet = new SubnetBuilder().setId(new SubnetId(domainId))
            .setIpPrefix(new IpPrefix(new Ipv4Prefix(ipAddr.getIpv4Address().getValue() + "/24")))
            .setParent(l2conId);
        TenantsBuilder tenants =
                new TenantsBuilder().setTenant(Collections.singletonList(new TenantBuilder().setId(tenantId)
                    .setForwardingContext(
                            new ForwardingContextBuilder().setSubnet(Collections.singletonList(subnet.build())).build())
                    .build()));

        // test without key
        ReadOnlyTransaction rtx = mock(ReadOnlyTransaction.class);
        broker = mock(DataBroker.class);
        arpTasker = new ArpTasker(broker, packetService, flowService);

        epL3.setKey(new EndpointL3Key(mock(IpAddress.class), null));
        arpTasker.addMacForL3EpAndCreateEp(epL3.build());
        verify(broker, never()).newReadOnlyTransaction();

        // test without node with external interface
        epL3.setKey(key);
        when(broker.newReadOnlyTransaction()).thenReturn(rtx);
        CheckedFuture<Optional<DataObject>, ReadFailedException> future =
                Futures.immediateCheckedFuture(Optional.<DataObject>absent());
        when(rtx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class)))
            .thenReturn(future);
        arpTasker.addMacForL3EpAndCreateEp(epL3.build());
        verify(broker).newReadOnlyTransaction();
        verify(rtx).close();

        // test correct
        broker = getDataBroker();
        arpTasker = new ArpTasker(broker, packetService, flowService);
        WriteTransaction wtx = broker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Nodes.class).build(),
                new NodesBuilder().setNode(Collections.singletonList(node.build())).build(), true);
        wtx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, node.getKey())
                .child(NodeConnector.class, new NodeConnectorKey(connectorId))
                .build(),
                connector.build(), true);
        // ignoring a Windows-specific bug
        try {
            wtx.put(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(Tenants.class).build(), tenants.build(), true);
        } catch (UncheckedExecutionException e) {
            assumeNoException(e);
        }
        wtx.submit().get();

        Future<RpcResult<AddFlowOutput>> flowFuture = mock(Future.class);
        when(flowService.addFlow(any(AddFlowInput.class))).thenReturn(flowFuture);

        epL3.setNetworkContainment(domainId).setTenant(tenantId);
        arpTasker.addMacForL3EpAndCreateEp(epL3.build());
        ArgumentCaptor<AddFlowInput> argument = ArgumentCaptor.forClass(AddFlowInput.class);
        verify(flowService).addFlow(argument.capture());
        AddFlowInput result = argument.getValue();
        assertEquals(EtherTypes.ARP.intValue(), result.getMatch()
                .getEthernetMatch()
                .getEthernetType()
                .getType()
                .getValue()
                .intValue());
        ArpMatch match = (ArpMatch)result.getMatch().getLayer3Match();
        assertEquals(ArpOperation.REPLY.intValue(), match.getArpOp().intValue());
        assertEquals("192.168.0.254/32", match.getArpTargetTransportAddress().getValue());
        assertEquals("192.168.0.1/32", match.getArpSourceTransportAddress().getValue());
        assertEquals(connectorId, result.getMatch().getInPort());
        assertEquals(new NodeRef(
                InstanceIdentifier.builder(Nodes.class).child(Node.class, node.getKey()).build()),
                result.getNode());

        // onPacketReceived
        Arp arp = new Arp();
        byte[] sha = HexEncode.bytesFromHexString("00:00:00:00:00:01");
        byte[] spa = InetAddress.getByName("192.168.0.1").getAddress();
        byte[] tha = HexEncode.bytesFromHexString("00:00:00:00:00:02");
        byte[] tpa = InetAddress.getByName("192.168.0.2").getAddress();
        int htype = 1;
        int ptype = EtherTypes.IPv4.intValue();
        short hlen = 6;
        short plen = 4;
        int operation = ArpOperation.REPLY.intValue();

        arp.setSenderHardwareAddress(sha);
        arp.setSenderProtocolAddress(spa);
        arp.setTargetHardwareAddress(tha);
        arp.setTargetProtocolAddress(tpa);
        arp.setOperation(operation);
        arp.setHardwareLength(hlen);
        arp.setProtocolLength(plen);
        arp.setHardwareType(htype);
        arp.setProtocolType(ptype);

        Ethernet arpFrame = new Ethernet().setSourceMACAddress(sha)
            .setDestinationMACAddress(tha)
            .setEtherType(EtherTypes.ARP.shortValue());
        arpFrame.setPayload(arp);

        L2BridgeDomainId l2domainId = new L2BridgeDomainId(l2conId);
        L2BridgeDomainBuilder l2domain = new L2BridgeDomainBuilder().setId(l2domainId);

        InstanceIdentifier<NodeConnector> ncIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("node")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("connector")))
            .build();
        PacketReceived packet = new PacketReceivedBuilder().setPayload(arpFrame.serialize())
            .setIngress(new NodeConnectorRef(ncIid))
            .build();

        wtx = broker.newWriteOnlyTransaction();
        OfOverlayL3Context augment = new OfOverlayL3ContextBuilder().build();
        epL3.addAugmentation(OfOverlayL3Context.class, augment);
        InstanceIdentifier<EndpointL3> epL3Iid =
                InstanceIdentifier.builder(Endpoints.class).child(EndpointL3.class, key).build();
        wtx.put(LogicalDatastoreType.OPERATIONAL, epL3Iid, epL3.build(), true);

        InstanceIdentifier<L2BridgeDomain> l2domainIid = InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(ForwardingContext.class)
            .child(L2BridgeDomain.class, new L2BridgeDomainKey(l2domainId))
            .build();
        wtx.put(LogicalDatastoreType.OPERATIONAL, l2domainIid, l2domain.build() ,true);
        wtx.submit();

        arpTasker.onPacketReceived(packet);
        rtx = broker.newReadOnlyTransaction();
        Optional<EndpointL3> optional = rtx.read(LogicalDatastoreType.OPERATIONAL, epL3Iid).get();
        assertTrue(optional.isPresent());
        EndpointL3 epl3 = optional.get();
        assertArrayEquals(sha, HexEncode.bytesFromHexString(epl3.getMacAddress().getValue()));
        assertEquals(l2domain.getId(), epl3.getL2Context());
        Optional<Endpoint> optionalEp = rtx.read(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointIid(l2domainId, new MacAddress("00:00:00:00:00:01"))).get();
        assertTrue(optionalEp.isPresent());
        assertEquals(new OfOverlayContextBuilder(augment).build(),
                optionalEp.get().getAugmentation(OfOverlayContext.class));
    }
}
