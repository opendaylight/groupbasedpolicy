/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class NatUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NatUtil.class);

    @SuppressWarnings("unchecked")
    static Function<InstanceIdentifier<PhysicalInterface>, NodeId> resolveNodeId = (intf) -> {
        return Preconditions
            .checkNotNull(checkIid(intf).firstKeyOf(RendererNode.class).getNodePath().firstKeyOf(Node.class))
            .getNodeId();
    };

    @SuppressWarnings("unchecked")
    static Function<InstanceIdentifier<PhysicalInterface>, InstanceIdentifier<Interface>> resolveInterfaceIid = (intf) -> {
        checkIid(intf);
        return VppIidFactory
            .getInterfaceIID(new InterfaceKey(checkIid(intf).firstKeyOf(PhysicalInterface.class).getInterfaceName()));
    };

    static private InstanceIdentifier<PhysicalInterface> checkIid(InstanceIdentifier<PhysicalInterface> iid) {
        Preconditions.checkNotNull(iid.firstKeyOf(RendererNode.class).getNodePath().firstKeyOf(Node.class));
        return iid;
    }

    public void setInboundInterface(Interface iface, WriteTransaction wTx) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat =
            new NatBuilder().setInbound(new InboundBuilder().setNat44Support(true).setPostRouting(ConfigUtil.getInstance().isL3FlatEnabled()).build())
                .build();
        wTx.put(LogicalDatastoreType.CONFIGURATION, natIid, nat);
    }

    public static void setOutboundInterface(Interface iface, InstanceIdentifier<Node> vppIid) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat =
            new NatBuilder().setOutbound(new OutboundBuilder().setNat44Support(true).setPostRouting(ConfigUtil.getInstance().isL3FlatEnabled()).build())
                .build();
        GbpNetconfTransaction.netconfSyncedWrite(vppIid, natIid, nat, GbpNetconfTransaction.RETRY_COUNT);

    }

    public static void unsetOutboundInterface(Interface iface, InstanceIdentifier<Node> vppIid) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        GbpNetconfTransaction.netconfSyncedDelete(vppIid, natIid, GbpNetconfTransaction.RETRY_COUNT);
    }

    public static InstanceIdentifier<Nat> buildNatIid(InstanceIdentifier<Interface> ifaceIid) {
        return ifaceIid.builder().augmentation(NatInterfaceAugmentation.class).child(Nat.class).build();
    }

    public static @Nonnull List<InstanceIdentifier<PhysicalInterface>> resolvePhysicalInterface(IpPrefix extSubnetPrefix,
        ReadOnlyTransaction rTx) {
        Optional<RendererNodes> readFromDs =
            DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodesIid(), rTx);
        rTx.close();
        if (!readFromDs.isPresent() || readFromDs.get().getRendererNode() == null) {
            return Collections.emptyList();
        }
        RendererNodes rendererNodes = readFromDs.get();
        List<RendererNode>
            vppNodes =
            rendererNodes.getRendererNode()
                .stream()
                .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class) != null)
                .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class).getPhysicalInterface() != null)
                .collect(Collectors.toList());
        List<InstanceIdentifier<PhysicalInterface>> iids = new ArrayList<>();
        for (RendererNode rn : vppNodes) {
            java.util.Optional<PhysicalInterface>
                optResolvedIface =
                rn.getAugmentation(VppInterfaceAugmentation.class)
                    .getPhysicalInterface()
                    .stream()
                    .filter(phIface -> phIface.getAddress() != null)
                    .filter(phIface -> phIface.getAddress()
                        .stream()
                        .anyMatch(ipAddr -> NetUtils.isInRange(extSubnetPrefix, String.valueOf(ipAddr.getValue()))))
                    .findFirst();
            if (optResolvedIface.isPresent()) {
                iids.add(VppIidFactory.getRendererNodeIid(rn)
                    .builder()
                    .augmentation(VppInterfaceAugmentation.class)
                    .child(PhysicalInterface.class, new PhysicalInterfaceKey(optResolvedIface.get().getKey()))
                    .build());
            }
        }
        return iids;
    }

    public static @Nonnull List<InstanceIdentifier<PhysicalInterface>> resolvePhysicalInterface(ReadOnlyTransaction rTx) {
            Optional<RendererNodes> readFromDs =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodesIid(), rTx);
            rTx.close();
            if (!readFromDs.isPresent() || readFromDs.get().getRendererNode() == null) {
                return Collections.emptyList();
            }
            RendererNodes rendererNodes = readFromDs.get();
            List<RendererNode>
                vppNodes =
                rendererNodes.getRendererNode()
                    .stream()
                    .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class) != null)
                    .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class).getPhysicalInterface() != null)
                    .collect(Collectors.toList());
            List<InstanceIdentifier<PhysicalInterface>> iids = new ArrayList<>();
            for (RendererNode rn : vppNodes) {
                java.util.Optional<PhysicalInterface>
                    optResolvedIface =
                    rn.getAugmentation(VppInterfaceAugmentation.class)
                        .getPhysicalInterface()
                        .stream()
                        .filter(phIface -> phIface.getAddress() != null)
                        .filter(phIface -> phIface.isExternal())
                        .findFirst();
                if (optResolvedIface.isPresent()) {
                    iids.add(VppIidFactory.getRendererNodeIid(rn)
                        .builder()
                        .augmentation(VppInterfaceAugmentation.class)
                        .child(PhysicalInterface.class, new PhysicalInterfaceKey(optResolvedIface.get().getKey()))
                        .build());
                }
            }
            return iids;
        }

    public static Optional<MappingEntryBuilder> createStaticEntry(String internal, Ipv4Address external) {
        IpAddress internalIp = null;
        try {
            InetAddress inetAddr = InetAddress.getByName(internal);
            if (inetAddr instanceof Inet4Address) {
                internalIp = new IpAddress(new Ipv4Address(internal));
            } else if (inetAddr instanceof Inet6Address) {
                internalIp = new IpAddress(new Ipv6Address(internal));
            }
        } catch (UnknownHostException e) {
            LOG.error("Cannot resolve host IP {}. {}", internal, e.getMessage());
            return Optional.absent();
        }
        SubnetUtils subnet = new SubnetUtils(internal + "/32");
        Long index = Integer.toUnsignedLong(subnet.getInfo().asInteger(internal));
        MappingEntryBuilder mappingEntryBuilder = new MappingEntryBuilder()
            .setType(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static)
            .setIndex(index)
            .setInternalSrcAddress(internalIp)
            .setExternalSrcAddress(external);
        return Optional.of(mappingEntryBuilder);
    }

    public static List<ExternalIpAddressPool> resolveDynamicNat(@Nonnull PolicyContext policyCtx,
            @Nullable List<MappingEntryBuilder> sNatEntries) {
        Optional<List<RendererForwardingByTenant>> forwardingByTenantLists = Optional.of(policyCtx)
            .transform(x -> x.getPolicy())
            .transform(x -> x.getConfiguration())
            .transform(x -> x.getRendererForwarding())
            .transform(x -> x.getRendererForwardingByTenant());
        if (!forwardingByTenantLists.isPresent()) {
            LOG.warn("No dynamic NAT resolved in cfg version {}.", policyCtx.getPolicy().getVersion());
            return ImmutableList.of();
        }
        Map<Long, Ipv4Prefix> extCache = new HashMap<>();
        forwardingByTenantLists.get()
            .stream()
            .map(rft -> rft.getRendererNetworkDomain())
            .flatMap(Collection::stream)
            .filter(domain -> domain.getAugmentation(SubnetAugmentRenderer.class) != null)
            .map(domain -> domain.getAugmentation(SubnetAugmentRenderer.class).getSubnet())
            .filter(subnet -> !subnet.isIsTenant() && subnet.getAllocationPool() != null)
            .forEach(subnet -> {
                final IpPrefix subnetIpPrefix = subnet.getIpPrefix();
                subnet.getAllocationPool().forEach(pool -> {
                    if (subnetIpPrefix.getIpv4Prefix() != null) {
                        final String firstEntry = pool.getFirst();
                        final String lastEntry = pool.getLast();
                        extCache.putAll(resolveDynamicNatPrefix(subnetIpPrefix.getIpv4Prefix(), firstEntry, lastEntry,
                                sNatEntries));
                    }
                });
            });
        List<ExternalIpAddressPool> extPools = extCache.entrySet()
            .stream()
            .map(entry -> new ExternalIpAddressPoolBuilder().setPoolId(entry.getKey())
                .setExternalIpPool(entry.getValue())
                .build())
            .collect(Collectors.toList());
        LOG.trace("Resolved dynamic NAT pools in cfg version {}: {}", policyCtx.getPolicy().getVersion(), extPools);
        return extPools;
    }


    static Map<Long, Ipv4Prefix> resolveDynamicNatPrefix(@Nonnull final Ipv4Prefix prefix,
                                                                 @Nonnull final String first,
                                                                 @Nullable final String last,
                                                                 @Nullable final List<MappingEntryBuilder> natEntries) {
        LOG.trace("Resolving Ipv4Prefix. prefix: {}, first: {}, last: {}", prefix.getValue(), first, last);
        final SubnetUtils subnet = new SubnetUtils(prefix.getValue());
        final Map<Long, Ipv4Prefix> extCache = new HashMap<>();
        int min = subnet.getInfo().asInteger(first);
        // loop through all addresses
        for (String address : subnet.getInfo().getAllAddresses()) {
            int asInt = subnet.getInfo().asInteger(address);
            if (asInt < min) {
                continue;
            }
            extCache.put(Integer.toUnsignedLong(asInt), new Ipv4Prefix(address + "/32"));
            if (last == null || subnet.getInfo().asInteger(address) >= subnet.getInfo().asInteger(last)) {
                break;
            }
        }
        if (natEntries != null) {
            // remove every static NAT entry from extCache
            for (MappingEntryBuilder natEntry : natEntries) {
                final Ipv4Address externalSrcAddress = natEntry.getExternalSrcAddress();
                final long id = Integer.toUnsignedLong(subnet.getInfo().asInteger(externalSrcAddress.getValue()));
                if (extCache.get(id) != null) {
                    extCache.remove(id);
                }
            }
        }
        return extCache;
    }

    public static void resolveOutboundNatInterface(InstanceIdentifier<Node> mountPointIid,
        NodeId nodeId, Map<NodeId, PhysicalInterfaceKey> extInterfaces) {
        if (extInterfaces.containsKey(nodeId)){
            PhysicalInterfaceKey physicalInterfaceKey = extInterfaces.get(nodeId);
            Optional<Interfaces> readIfaces = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Interfaces.class),
                VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(mountPointIid).getKey().newReadOnlyTransaction());
            if(readIfaces.isPresent() ) {
                for (Interface nodeInterface : readIfaces.get().getInterface()) {
                    if (nodeInterface.getName().equals(physicalInterfaceKey.getInterfaceName())) {
                        LOG.trace("Setting outbound NAT on interface {} on node: {}", nodeInterface.getName(), mountPointIid);
                        NatUtil.setOutboundInterface(nodeInterface, mountPointIid);
                    }
                }

            }

        }

    }
}
