/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.hostconfigs;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util.HostconfigUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.Hostconfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.Hostconfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.HostconfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class TestResources extends AbstractDataBrokerTest {

    protected DataBroker dataBroker;

    void setDataBroker() {
        this.dataBroker = getDataBroker();
    }

    void writeTopologyNode(TopologyId topologyId, NodeId nodeId) throws InterruptedException, ExecutionException {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, createNodeIid(topologyId, nodeId), new NodeBuilder().setNodeId(nodeId).build(), true);
        wTx.submit().get();
    }

    void writeRendererNode(InstanceIdentifier<Node> nodeIid) throws InterruptedException,
            ExecutionException {
        InstanceIdentifier<RendererNode> rendererNodeIid = createRendererNodeIid(nodeIid);
        RendererNode rendererNode = new RendererNodeBuilder().setNodePath(nodeIid).build();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, rendererNodeIid, rendererNode, true);
        wTx.submit().get();
    }

    void deleteRendererNode(InstanceIdentifier<Node> nodeIid) throws InterruptedException, ExecutionException {
        InstanceIdentifier<RendererNode> rendererNodeIid = createRendererNodeIid(nodeIid);
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, rendererNodeIid);
        wTx.submit().get();
    }

    Optional<Hostconfig> readHostconfig(NodeId nodeId) throws InterruptedException, ExecutionException {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Hostconfig> opt = rTx.read(LogicalDatastoreType.OPERATIONAL, createHostconfigWildcardNodeIid(nodeId))
            .get();
        rTx.close();
        return opt;
    }

    static InstanceIdentifier<Node> createNodeIid(TopologyId topologyId, NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(topologyId))
            .child(Node.class, new NodeKey(nodeId))
            .build();
    }

    InstanceIdentifier<RendererNode> createRendererNodeIid(InstanceIdentifier<Node> nodeIid) {
        return InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, new RendererKey(VppNodeListener.VPP_RENDERER_NAME))
            .child(RendererNodes.class)
            .child(RendererNode.class, new RendererNodeKey(nodeIid))
            .build();
    }

    InstanceIdentifier<Hostconfig> createHostconfigWildcardNodeIid(NodeId nodeId) {
        return InstanceIdentifier.builder(Neutron.class)
            .child(Hostconfigs.class)
            .child(Hostconfig.class, new HostconfigKey(nodeId.getValue(), HostconfigUtil.L2_HOST_TYPE))
            .build();
    }
}
