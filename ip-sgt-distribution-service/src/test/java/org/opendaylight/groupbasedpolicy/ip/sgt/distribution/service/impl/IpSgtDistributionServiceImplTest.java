/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ip.sgt.distribution.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.RemoveIpSgtBindingFromPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.RemoveIpSgtBindingFromPeerInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SendIpSgtBindingToPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SendIpSgtBindingToPeerInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.binding.PeerNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(IpSgtDistributionServiceImpl.class)
public class IpSgtDistributionServiceImplTest {

    private static final PeerSequence EMPTY_PEER_SEQUENCE = new PeerSequenceBuilder().build();
    private static final DateAndTime DUMMY_DT = TimeConv.toDt(123456L);
    private final IpAddress ADDR = new IpAddress(new Ipv4Address("10.0.0.1"));
    private final IpPrefix BINDING_ADDR = new IpPrefix(new Ipv4Prefix("192.168.50.1/32"));
    private final Sgt BINDING_SGT = new Sgt(1010);
    private final String NODE_ID = "node1";
    private final String TOPOLOGY_ID = "test-topology";
    private final String DOMAIN_ID = TOPOLOGY_ID + "/" + NODE_ID;
    private final InstanceIdentifier<Node> PEER_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("test-topology")))
        .child(Node.class, new NodeKey(
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(NODE_ID)))
        .build();
    private final InstanceIdentifier<Node> SXP_NODE_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("sxp")))
        .child(Node.class,
                new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                ADDR.getIpv4Address().getValue())))
        .build();
    private DataBroker dataBroker;
    private IpSgtDistributionServiceImpl impl;
    private SxpCapableNodeListener nodeListener;

    @Before
    public void init() throws Exception {
        SxpControllerService sxpService = mock(SxpControllerService.class);
        when(sxpService.addNode(any())).thenReturn(Futures.immediateFuture(
                RpcResultBuilder.<AddNodeOutput>success(new AddNodeOutputBuilder().setResult(true).build()).build()));
        nodeListener = PowerMockito.mock(SxpCapableNodeListener.class);
        PowerMockito.whenNew(SxpCapableNodeListener.class).withAnyArguments().thenReturn(nodeListener);
        dataBroker = mock(DataBroker.class);
        impl = new IpSgtDistributionServiceImpl(dataBroker, sxpService, ADDR);
    }

    @Test
    public void testInit() throws Exception {
        RpcProviderRegistry rpcProvider = mock(RpcProviderRegistry.class);
        SxpControllerService sxpService = mock(SxpControllerService.class);
        when(rpcProvider.getRpcService(SxpControllerService.class)).thenReturn(sxpService);
        when(sxpService.addNode(any())).thenReturn(Futures.immediateFuture(
                RpcResultBuilder.<AddNodeOutput>success(new AddNodeOutputBuilder().setResult(true).build()).build()));
        SxpCapableNodeListener nodeListener = PowerMockito.mock(SxpCapableNodeListener.class);
        PowerMockito.whenNew(SxpCapableNodeListener.class).withAnyArguments().thenReturn(nodeListener);
        impl = new IpSgtDistributionServiceImpl(dataBroker, sxpService, ADDR);
        AddNodeInput addNodeInput = new AddNodeInputBuilder().setNodeId(new NodeId(ADDR.getIpv4Address().getValue()))
            .setSourceIp(ADDR)
            .setDescription(IpSgtDistributionServiceImpl.SXP_NODE_DESCRIPTION)
            .build();
        verify(sxpService).addNode(eq(addNodeInput));
        PowerMockito.verifyNew(SxpCapableNodeListener.class);
    }

    @Test
    public void testSendIpSgtBindingToPeer_successfullWrite() throws Exception {
        SendIpSgtBindingToPeerInput input =
                new SendIpSgtBindingToPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        WriteTransaction wtx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        when(wtx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(DOMAIN_ID);
        Future<RpcResult<Void>> response = impl.sendIpSgtBindingToPeer(input);
        final MasterDatabaseBinding expectedMasterDBBinding = new MasterDatabaseBindingBuilder().setIpPrefix(BINDING_ADDR)
                .setSecurityGroupTag(BINDING_SGT)
                .setPeerSequence(EMPTY_PEER_SEQUENCE)
                .setTimestamp(DUMMY_DT)
                .build();
        verify(wtx).put(eq(LogicalDatastoreType.CONFIGURATION),
                eq(InstanceIdentifier.builder(SXP_NODE_IID)
                        .augmentation(SxpNodeIdentity.class)
                        .child(SxpDomains.class)
                        .child(SxpDomain.class, new SxpDomainKey(DOMAIN_ID))
                        .child(MasterDatabase.class)
                        .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(BINDING_ADDR))
                        .build()),
                Matchers.argThat(createMasterBDBindingMatcher(expectedMasterDBBinding)));
        assertTrue(response.get().isSuccessful());
    }

    private static ArgumentMatcher<MasterDatabaseBinding> createMasterBDBindingMatcher(final MasterDatabaseBinding expectedMasterDBBinding) {
        return new ArgumentMatcher<MasterDatabaseBinding>() {
            @Override
            public boolean matches(final Object o) {
                boolean verdict = false;
                if (o instanceof MasterDatabaseBinding) {
                    final MasterDatabaseBinding otherMasterDBBinding =
                            new MasterDatabaseBindingBuilder((MasterDatabaseBinding) o).setTimestamp(DUMMY_DT).build();
                    verdict = expectedMasterDBBinding.equals(otherMasterDBBinding);
                }
                return verdict;
            }
        };
    }

    @Test
    public void testSendIpSgtBindingToPeer_failedWrite() throws Exception {
        SendIpSgtBindingToPeerInput input =
                new SendIpSgtBindingToPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        WriteTransaction wtx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        when(wtx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(new TransactionCommitFailedException("")));
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(DOMAIN_ID);
        Future<RpcResult<Void>> response = impl.sendIpSgtBindingToPeer(input);
        assertFalse(response.get().isSuccessful());
    }

    @Test
    public void testSendIpSgtBindingToPeer_noSxpCapableNode() throws Exception {
        SendIpSgtBindingToPeerInput input =
                new SendIpSgtBindingToPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(null);
        Future<RpcResult<Void>> response = impl.sendIpSgtBindingToPeer(input);
        assertFalse(response.get().isSuccessful());
    }

    @Test
    public void testRemoveIpSgtBindingFromPeer_successfullWrite() throws Exception {
        RemoveIpSgtBindingFromPeerInput input =
                new RemoveIpSgtBindingFromPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        WriteTransaction wtx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        when(wtx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(DOMAIN_ID);
        Future<RpcResult<Void>> response = impl.removeIpSgtBindingFromPeer(input);
        new MasterDatabaseBindingBuilder().setIpPrefix(BINDING_ADDR).setSecurityGroupTag(BINDING_SGT).build();
        verify(wtx).delete(eq(LogicalDatastoreType.CONFIGURATION),
                eq(InstanceIdentifier.builder(SXP_NODE_IID)
                    .augmentation(SxpNodeIdentity.class)
                    .child(SxpDomains.class)
                    .child(SxpDomain.class, new SxpDomainKey(DOMAIN_ID))
                    .child(MasterDatabase.class)
                    .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(BINDING_ADDR))
                    .build()));
        assertTrue(response.get().isSuccessful());
    }

    @Test
    public void testRemoveIpSgtBindingFromPeer_failedWrite() throws Exception {
        RemoveIpSgtBindingFromPeerInput input =
                new RemoveIpSgtBindingFromPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        WriteTransaction wtx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        when(wtx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(new TransactionCommitFailedException("")));
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(DOMAIN_ID);
        Future<RpcResult<Void>> response = impl.removeIpSgtBindingFromPeer(input);
        assertFalse(response.get().isSuccessful());
    }

    @Test
    public void testRemoveIpSgtBindingFromPeer_noSxpCapableNode() throws Exception {
        RemoveIpSgtBindingFromPeerInput input =
                new RemoveIpSgtBindingFromPeerInputBuilder()
                    .setBinding(
                            Collections
                                .singletonList(new BindingBuilder().setIpPrefix(BINDING_ADDR)
                                    .setSgt(BINDING_SGT)
                                    .setPeerNode(Collections
                                        .singletonList(new PeerNodeBuilder().setNodeIid(PEER_IID).build()))
                                    .build()))
                    .build();
        when(nodeListener.getDomainIdForPeer(PEER_IID)).thenReturn(null);
        Future<RpcResult<Void>> response = impl.removeIpSgtBindingFromPeer(input);
        assertFalse(response.get().isSuccessful());
    }
}
