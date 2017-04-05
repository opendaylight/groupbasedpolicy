/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.AllocationPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class NatUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NatUtil.class);

    public void setInboundInterface(Interface iface, WriteTransaction wTx) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat = new NatBuilder().setInbound(new InboundBuilder().build()).build();
        wTx.put(LogicalDatastoreType.CONFIGURATION, natIid, nat);
    }

    public static void setOutboundInterface(Interface iface, DataBroker dataBroker) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat = new NatBuilder().setOutbound(new OutboundBuilder().build()).build();
        GbpNetconfTransaction.netconfSyncedWrite(dataBroker, natIid, nat, GbpNetconfTransaction.RETRY_COUNT);

    }

    public static void unsetOutboundInterface(Interface iface, DataBroker dataBroker) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        GbpNetconfTransaction.netconfSyncedDelete(dataBroker, natIid, GbpNetconfTransaction.RETRY_COUNT);
    }

    public static InstanceIdentifier<Nat> buildNatIid(InstanceIdentifier<Interface> ifaceIid) {
        return ifaceIid.builder().augmentation(NatInterfaceAugmentation.class).child(Nat.class).build();
    }

    public static Optional<InstanceIdentifier<PhysicalInterface>> resolvePhysicalInterface(IpPrefix extSubnetPrefix,
        ReadOnlyTransaction rTx) {
        Optional<RendererNodes> readFromDs =
            DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodesIid(), rTx);
        rTx.close();
        if (!readFromDs.isPresent() || readFromDs.get().getRendererNode() == null) {
            return Optional.absent();
        }
        RendererNodes rendererNodes = readFromDs.get();
        List<RendererNode>
            vppNodes =
            rendererNodes.getRendererNode()
                .stream()
                .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class) != null)
                .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class).getPhysicalInterface() != null)
                .collect(Collectors.toList());
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
                return Optional.of(VppIidFactory.getRendererNodeIid(rn)
                    .builder()
                    .augmentation(VppInterfaceAugmentation.class)
                    .child(PhysicalInterface.class, new PhysicalInterfaceKey(optResolvedIface.get().getKey()))
                    .build());
            }
        }
        return Optional.absent();
    }

    static List<ExternalIpAddressPool> resolveDynamicNat(@Nonnull PolicyContext policyCtx,
        @Nullable List<MappingEntryBuilder> sNatEntries) {
        List<RendererForwardingByTenant> forwardingByTenantList =
                policyCtx.getPolicy().getConfiguration().getRendererForwarding().getRendererForwardingByTenant();
        Map<Long, Ipv4Prefix> extCache = new HashMap<>();
        // loop through forwarding by tenant
        for (RendererForwardingByTenant rft : forwardingByTenantList) {
            // loop through renderer network domain
            for (RendererNetworkDomain domain : rft.getRendererNetworkDomain()) {
                final SubnetAugmentRenderer subnetAugmentation = domain.getAugmentation(SubnetAugmentRenderer.class);
                final Subnet subnet = subnetAugmentation.getSubnet();
                if (subnet != null && !subnet.isIsTenant() && subnet.getAllocationPool() != null) {
                    // loop through allocation pool
                    for (AllocationPool pool : subnet.getAllocationPool()) {
                        final IpPrefix subnetIpPrefix = subnet.getIpPrefix();
                        if (subnetIpPrefix.getIpv4Prefix() != null) {
                            final String firstEntry = pool.getFirst();
                            final String lastEntry = pool.getLast();
                            extCache.putAll(resolveDynamicNatPrefix(subnetIpPrefix.getIpv4Prefix(), firstEntry, lastEntry,
                                    sNatEntries));
                        }
                    }
                }
            }
        }
        final List<ExternalIpAddressPool> extIps = new ArrayList<>();
        for (Entry<Long, Ipv4Prefix> entry : extCache.entrySet()) {
            extIps.add(new ExternalIpAddressPoolBuilder().setPoolId(entry.getKey())
                .setExternalIpPool(entry.getValue())
                .build());
        }
        return extIps;
    }

    @VisibleForTesting
    private static Map<Long, Ipv4Prefix> resolveDynamicNatPrefix(@Nonnull final Ipv4Prefix prefix,
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

    public static void resolveOutboundNatInterface(DataBroker mountpoint, InstanceIdentifier<Node> mountPointIid,
        NodeId nodeId, Map<NodeId, PhysicalInterfaceKey> extInterfaces) {
        if (extInterfaces.containsKey(nodeId)){
            PhysicalInterfaceKey physicalInterfaceKey = extInterfaces.get(nodeId);
            Optional<Interfaces> readIfaces = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Interfaces.class), mountpoint.newReadOnlyTransaction());
            if(readIfaces.isPresent() ) {
                for (Interface nodeInterface : readIfaces.get().getInterface()) {
                    if (nodeInterface.getName().equals(physicalInterfaceKey.getInterfaceName())) {
                        LOG.trace("Setting outbound NAT on interface {} on node: {}", nodeInterface.getName(), mountPointIid);
                        NatUtil.setOutboundInterface(nodeInterface, mountpoint);
                    }
                }

            }

        }

    }
}
