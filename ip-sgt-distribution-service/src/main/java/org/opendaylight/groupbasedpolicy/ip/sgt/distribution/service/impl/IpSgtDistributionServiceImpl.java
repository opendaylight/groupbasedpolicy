/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ip.sgt.distribution.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.IpSgtDistributionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.RemoveIpSgtBindingFromPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SendIpSgtBindingToPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.binding.PeerNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class IpSgtDistributionServiceImpl implements AutoCloseable, IpSgtDistributionService {

    private static final Logger LOG = LoggerFactory.getLogger(IpSgtDistributionServiceImpl.class);
    public static final String SXP_NODE_DESCRIPTION = "ODL-GBP SXP node";
    public static final String SXP_TOPOLOGY_ID = "sxp";
    private final String SXP_NODE_ID;
    private DataBroker dataBroker;
    private IpAddress sourceIp;
    private SxpCapableNodeListener nodeCollector;

    public IpSgtDistributionServiceImpl(DataBroker dataBroker, SxpControllerService sxpService, IpAddress sourceIp) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.sourceIp = Preconditions.checkNotNull(sourceIp);
        Preconditions.checkNotNull(sxpService);

        if (sourceIp.getIpv4Address() != null) {
            SXP_NODE_ID = sourceIp.getIpv4Address().getValue();
        } else {
            SXP_NODE_ID = sourceIp.getIpv6Address().getValue();
        }
        createSxpNode(sxpService);
        nodeCollector = new SxpCapableNodeListener(dataBroker, SXP_NODE_ID);

    }

    private void createSxpNode(SxpControllerService sxpService) {
        AddNodeInput addNodeInput = new AddNodeInputBuilder().setNodeId(new NodeId(SXP_NODE_ID))
            .setSourceIp(sourceIp)
            .setDescription(SXP_NODE_DESCRIPTION)
            .build();
        Future<RpcResult<AddNodeOutput>> addNodeResult = sxpService.addNode(addNodeInput);
        try {
            if (!addNodeResult.get().getResult().isResult()) {
                LOG.error("RPC add-node wasn't successfull");
            }
        } catch (Exception e) {
            LOG.error("RPC add-node wasn't successfull");
        }
    }

    @Override
    public Future<RpcResult<Void>> sendIpSgtBindingToPeer(SendIpSgtBindingToPeerInput input) {
        Map<String, Multimap<Sgt, IpPrefix>> bindingsMap = new HashMap<>();
        boolean success = true;
        for (Binding binding : input.getBinding()) {
            success = transformChanges(binding, bindingsMap);
            if (!success) {
                break;
            }
        }
        if (!success) {
            return Futures.immediateCheckedFuture(RpcResultBuilder.<Void>failed().build());
        }
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        bindingsMap.entrySet().forEach(bindingEntries -> {
            String domainId = bindingEntries.getKey();
            bindingEntries.getValue().entries().forEach(binding -> writeBinding(binding, domainId, wtx));
        });
        ListenableFuture<Void> submit = wtx.submit();
        SettableFuture<RpcResult<Void>> future = SettableFuture.create();
        Futures.addCallback(submit, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                future.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable t) {
                future.set(RpcResultBuilder.<Void>failed().build());

            }

        });
        return future;
    }

    private boolean transformChanges(Binding binding, Map<String, Multimap<Sgt, IpPrefix>> bindingsMap) {
        Sgt sgt = binding.getSgt();
        IpPrefix addr = binding.getIpPrefix();
        for (PeerNode peer : binding.getPeerNode()) {
            String domainId = nodeCollector.getDomainIdForPeer((InstanceIdentifier<Node>) peer.getNodeIid());
            if (domainId == null) {
                LOG.debug("Node {} is not SXP capable", peer.getNodeIid());
                return false;
            }
            Multimap<Sgt, IpPrefix> domainBindingMap = bindingsMap.get(domainId);
            if (domainBindingMap == null) {
                domainBindingMap = ArrayListMultimap.create();
                bindingsMap.put(domainId, domainBindingMap);
            }
            domainBindingMap.get(sgt).add(addr);
        }
        return true;
    }

    private void writeBinding(Entry<Sgt, IpPrefix> binding, String domainId, WriteTransaction wtx) {
        IpPrefix addr = binding.getValue();
        InstanceIdentifier<MasterDatabaseBinding> iid = bindingIid(domainId, addr);
        MasterDatabaseBinding newBinding = createBinding(binding);
        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, newBinding);
    }

    private InstanceIdentifier<MasterDatabaseBinding> bindingIid(String domainId, IpPrefix prefix) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(SXP_TOPOLOGY_ID)))
            .child(Node.class,
                    new NodeKey(
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(
                                    SXP_NODE_ID)))
            .augmentation(SxpNodeIdentity.class)
            .child(SxpDomains.class)
            .child(SxpDomain.class, new SxpDomainKey(domainId))
            .child(MasterDatabase.class)
            .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(prefix))
            .build();
    }

    private MasterDatabaseBinding createBinding(Entry<Sgt, IpPrefix> binding) {
        return new MasterDatabaseBindingBuilder().setIpPrefix(binding.getValue())
            .setSecurityGroupTag(binding.getKey())
            .build();
    }

    @Override
    public Future<RpcResult<Void>> removeIpSgtBindingFromPeer(RemoveIpSgtBindingFromPeerInput input) {
        Map<String, Multimap<Sgt, IpPrefix>> bindingsMap = new HashMap<>();
        boolean success = true;
        for (Binding binding : input.getBinding()) {
            success = transformChanges(binding, bindingsMap);
            if (!success) {
                break;
            }
        }
        if (!success) {
            return Futures.immediateCheckedFuture(RpcResultBuilder.<Void>failed().build());
        }
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        bindingsMap.entrySet().forEach(bindingEntries -> {
            String domainId = bindingEntries.getKey();
            bindingEntries.getValue().entries().forEach(binding -> removeBinding(binding, domainId, wtx));
        });
        ListenableFuture<Void> submit = wtx.submit();
        SettableFuture<RpcResult<Void>> future = SettableFuture.create();
        Futures.addCallback(submit, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                future.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable t) {
                future.set(RpcResultBuilder.<Void>failed().build());

            }

        });
        return future;
    }

    private void removeBinding(Entry<Sgt, IpPrefix> binding, String domainId, WriteTransaction wtx) {
        IpPrefix addr = binding.getValue();
        InstanceIdentifier<MasterDatabaseBinding> iid = bindingIid(domainId, addr);
        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
    }

    @Override
    public void close() throws Exception {
        nodeCollector.close();
    }

}
