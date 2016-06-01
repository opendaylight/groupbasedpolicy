/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppIidFactory {

    public static InstanceIdentifier<Interface> getInterfaceIID(InterfaceKey interfaceKey) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, interfaceKey);
    }

    public static InstanceIdentifier<Renderer> getRendererIID(RendererKey rendererKey) {
        return InstanceIdentifier.create(Renderers.class).child(Renderer.class, rendererKey);
    }

    public static InstanceIdentifier<RendererNodes> getRendererNodesIid() {
        return InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, new RendererKey(VppNodeManager.vppRenderer))
            .child(RendererNodes.class)
            .build();
    }

    public static InstanceIdentifier<RendererNode> getRendererNodeIid(RendererNode rendererNode) {
        return InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, new RendererKey(VppNodeManager.vppRenderer))
            .child(RendererNodes.class)
            .child(RendererNode.class, new RendererNodeKey(rendererNode.getNodePath()))
            .build();
    }

    public static InstanceIdentifier<Node> getNodeIid(NodeKey key) {
        TopologyKey topologyKey = new TopologyKey(new TopologyId("topology-netconf"));
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class,topologyKey)
                .child(Node.class, key).build();
    }
}
