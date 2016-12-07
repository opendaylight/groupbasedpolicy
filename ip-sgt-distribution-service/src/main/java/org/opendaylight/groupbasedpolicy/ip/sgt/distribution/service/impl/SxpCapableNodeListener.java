/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ip.sgt.distribution.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SxpConnectionAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.sxp.connection.fields.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connection.fields.ConnectionTimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpCapableNodeListener implements DataTreeChangeListener<SxpConnection> {

    private String SXP_NODE_ID;
    private ListenerRegistration<SxpCapableNodeListener> listenerRegistration;
    private Map<InstanceIdentifier<Node>, String> sxpCapableNodes = new HashMap<>();
    private DataBroker dataBroker;

    public SxpCapableNodeListener(DataBroker dataBroker, String sxpNodeId) {
        SXP_NODE_ID = sxpNodeId;
        this.dataBroker = dataBroker;
        DataTreeIdentifier<SxpConnection> iid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class)
                    .child(Node.class)
                    .augmentation(SxpConnectionAugmentation.class)
                    .child(SxpConnection.class)
                    .build());
        listenerRegistration = dataBroker.registerDataTreeChangeListener(iid, this);
    }

    @Override
    public synchronized void onDataTreeChanged(Collection<DataTreeModification<SxpConnection>> changes) {
        changes.forEach((change) -> {
            InstanceIdentifier<Node> nodeIid = change.getRootPath().getRootIdentifier().firstIdentifierOf(Node.class);
            DataObjectModification<SxpConnection> rootNode = change.getRootNode();
            String domainId = createDomainId(nodeIid);
            switch (rootNode.getModificationType()) {
                case DELETE: {
                    removeSxpDomain(domainId);
                    sxpCapableNodes.remove(nodeIid);
                    break;
                }
                case WRITE:
                case SUBTREE_MODIFIED: {
                    createSxpDomain(rootNode.getDataAfter(), domainId);
                    sxpCapableNodes.put(nodeIid, domainId);
                    break;
                }
            }
        });
    }

    private String createDomainId(InstanceIdentifier<Node> nodeIid) {
        String topologyId = nodeIid.firstKeyOf(Topology.class).getTopologyId().getValue();
        String nodeId = nodeIid.firstKeyOf(Node.class).getNodeId().getValue();
        return topologyId + "/" + nodeId;
    }

    private void removeSxpDomain(String domainId) {
        InstanceIdentifier<SxpDomain> iid = sxpDomainIid(domainId);
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        wtx.submit();
    }

    private void createSxpDomain(SxpConnection sxpData, String domainId) {
        IpAddress peerAddr = sxpData.getIpAddress();
        PortNumber port = sxpData.getPortNumber();
        String password = sxpData.getPassword();
        InstanceIdentifier<SxpDomain> iid = sxpDomainIid(domainId);
        SxpDomain domain = new SxpDomainBuilder().setDomainName(domainId)
                .setConnections(new ConnectionsBuilder()
                        .setConnection(Collections.singletonList(new ConnectionBuilder()
                                .setPeerAddress(peerAddr)
                                .setTcpPort(port)
                                .setMode(ConnectionMode.Speaker)
                                .setPassword(password)
                                .setConnectionTimers(new ConnectionTimersBuilder().build())
                                .setDescription("Connection to " + domainId)
                                .setVersion(sxpData.getVersion())
                                .build()))
                        .build())
                .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.merge(LogicalDatastoreType.CONFIGURATION, iid, domain);
        wtx.submit();
    }

    private InstanceIdentifier<SxpDomain> sxpDomainIid(String domainId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(IpSgtDistributionServiceImpl.SXP_TOPOLOGY_ID)))
            .child(Node.class, new NodeKey(new NodeId(SXP_NODE_ID)))
            .augmentation(SxpNodeIdentity.class)
            .child(SxpDomains.class)
            .child(SxpDomain.class, new SxpDomainKey(domainId))
            .build();
    }

    synchronized String getDomainIdForPeer(InstanceIdentifier<Node> peer) {
        return sxpCapableNodes.get(peer);
    }

    void close() {
        listenerRegistration.close();
    }
}
