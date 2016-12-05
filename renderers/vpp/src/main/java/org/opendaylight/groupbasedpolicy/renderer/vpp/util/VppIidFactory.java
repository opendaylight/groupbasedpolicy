/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppIidFactory {

    public static InstanceIdentifier<Interface> getInterfaceIID(InterfaceKey interfaceKey) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, interfaceKey);
    }

    public static InstanceIdentifier<L2> getL2ForInterfaceIid(InterfaceKey interfaceKey) {
        return getInterfaceIID(interfaceKey).builder()
            .augmentation(VppInterfaceAugmentation.class)
            .child(L2.class)
            .build();
    }

    public static InstanceIdentifier<Renderer> getRendererIID(RendererKey rendererKey) {
        return InstanceIdentifier.create(Renderers.class).child(Renderer.class, rendererKey);
    }

    public static InstanceIdentifier<RendererNodes> getRendererNodesIid() {
        return InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(VppRenderer.NAME))
                .child(RendererNodes.class)
                .build();
    }

    public static InstanceIdentifier<RendererNode> getRendererNodeIid(RendererNode rendererNode) {
        return InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(VppRenderer.NAME))
                .child(RendererNodes.class)
                .child(RendererNode.class, new RendererNodeKey(rendererNode.getNodePath()))
                .build();
    }

    public static InstanceIdentifier<Node> getNodeIid(TopologyKey topologyKey, NodeKey nodeKey) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topologyKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public static InstanceIdentifier<Topology> getTopologyIid(TopologyKey bridgeDomainKey) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, bridgeDomainKey).build();
    }

    public static InstanceIdentifier<BridgeDomain> getBridgeDomainStateIid(final BridgeDomainKey bridgeDomainStateKey) {
        return InstanceIdentifier.builder(VppState.class)
                .child(BridgeDomains.class)
                .child(BridgeDomain.class, bridgeDomainStateKey)
                .build();
    }
}
