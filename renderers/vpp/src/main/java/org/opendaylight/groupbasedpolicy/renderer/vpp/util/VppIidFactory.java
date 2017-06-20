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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.Gpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.BridgeDomainsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.state.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.ProxyRanges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.proxy.ranges.ProxyRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.proxy.ranges.ProxyRangeKey;
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
        return getRendererNodesIid().child(RendererNode.class, new RendererNodeKey(rendererNode.getNodePath()));
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

    public static InstanceIdentifier<Lisp> getLispStateIid() {
        return InstanceIdentifier.create(Lisp.class);
    }

    public static InstanceIdentifier<VniTable> getVniTableIid(VniTableKey vniTableKey) {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(EidTable.class)
                .child(VniTable.class, vniTableKey).build();
    }

    public static InstanceIdentifier<LocatorSet> getLocatorSetIid(LocatorSetKey locatorSetKey) {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, locatorSetKey).build();
    }

    public static InstanceIdentifier<MapRegister> getMapRegisterIid() {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(MapRegister.class).build();
    }

    public static InstanceIdentifier<MapResolver> getMapResolverIid(MapResolverKey mapResolverKey) {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(MapResolvers.class)
                .child(MapResolver.class, mapResolverKey).build();
    }

    public static InstanceIdentifier<MapServer> getMapServerIid(MapServerKey mapServerKey) {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(MapServers.class)
                .child(MapServer.class, mapServerKey).build();
    }

    public static InstanceIdentifier<LocalMapping> getLocalMappingIid(VniTableKey vniTableKey,
                                                                      LocalMappingKey localMappingKey) {
        return getVniTableIid(vniTableKey)
                .child(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class, localMappingKey);
    }

    public static InstanceIdentifier<LispFeatureData> getLispFeatureDataIid() {
        return InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class).build();
    }

    public static InstanceIdentifier<ProxyRange> getProxyRangeIid(Long vrf,
                                                                  Ipv4Address startAddress,
                                                                  Ipv4Address endAddress) {
        return InstanceIdentifier.builder(ProxyRanges.class)
                .child(ProxyRange.class, new ProxyRangeKey(endAddress, startAddress, vrf)).build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607
            .interfaces._interface.Routing> getRoutingIid(InterfaceKey interfaceKey) {
        return getInterfaceIID(interfaceKey)
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607
                        .interfaces._interface.Routing.class);
    }

    public static InstanceIdentifier<Neighbor> getNeighborIid(InterfaceKey interfaceKey, NeighborKey neighborKey) {
        return getInterfaceIID(interfaceKey)
                .augmentation(Interface1.class)
                .child(Ipv4.class)
                .child(Neighbor.class, neighborKey);
    }

    public static InstanceIdentifier<Unnumbered> getUnnumberedIid(InterfaceKey interfaceName) {
        return getInterfaceIID(interfaceName)
                .augmentation(InterfaceUnnumberedAugmentation.class)
                .child(Unnumbered.class);
    }

    public static InstanceIdentifier<GpeFeatureData> getGpeFeatureDataIid() {
        return InstanceIdentifier.builder(Gpe.class)
                .child(GpeFeatureData.class).build();
    }

    public static InstanceIdentifier<GpeEntry> getGpeEntryIid(GpeEntryKey gpeEntryKey) {
        return InstanceIdentifier.builder(Gpe.class)
                .child(GpeFeatureData.class)
                .child(GpeEntryTable.class)
                .child(GpeEntry.class, gpeEntryKey)
                .build();
    }
}
