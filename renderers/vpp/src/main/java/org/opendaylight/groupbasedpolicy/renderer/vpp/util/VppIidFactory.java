/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.RoutingCommand;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.BridgeDomainsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.bridge.domains.state.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppIidFactory {

    private static final TopologyKey TOPOLOGY_NETCONF = new TopologyKey(new TopologyId("topology-netconf"));

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

    public static InstanceIdentifier<Node> getNetconfNodeIid(NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, TOPOLOGY_NETCONF)
            .child(Node.class, new NodeKey(nodeId))
            .build();
    }

    public static InstanceIdentifier<Topology> getTopologyIid(TopologyKey bridgeDomainKey) {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, bridgeDomainKey).build();
    }

    public static InstanceIdentifier<BridgeDomain> getBridgeDomainStateIid(final BridgeDomainKey bridgeDomainStateKey) {
        return InstanceIdentifier.builder(BridgeDomainsState.class)
            .child(BridgeDomain.class, bridgeDomainStateKey)
            .build();
    }

    public static InstanceIdentifier<Acl> getVppAcl(String aclName) {
        return InstanceIdentifier.builder(AccessLists.class)
            .child(Acl.class, new AclKey(aclName, VppAcl.class))
            .build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl> getAclInterfaceRef(
            InstanceIdentifier<Interface> ifaceIid) {
        return ifaceIid.augmentation(VppAclInterfaceAugmentation.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl.class);
    }

    public static InstanceIdentifier<NatInstance> getNatInstanceIid(Long natInstance) {
        return InstanceIdentifier.builder(NatConfig.class)
                .child(NatInstances.class)
                .child(NatInstance.class, new NatInstanceKey(natInstance))
                .build();
    }

    public static InstanceIdentifier<RoutingProtocol> getRoutingInstanceIid(
        final RoutingProtocolKey routingProtocolKey) {
        return InstanceIdentifier.builder(Routing.class)
            .child(RoutingInstance.class, new RoutingInstanceKey(RoutingCommand.ROUTER_INSTANCE_NAME))
            .child(RoutingProtocols.class)
            .child(RoutingProtocol.class, routingProtocolKey)
            .build();
    }
}
