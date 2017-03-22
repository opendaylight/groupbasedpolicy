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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.AllocationPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

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

    public static List<ExternalIpAddressPool> resolveDynamicNat(@Nonnull PolicyContext policyCtx,
        List<MappingEntryBuilder> natEntries) {
        List<RendererForwardingByTenant> rendererForwardingByTenant =
                policyCtx.getPolicy().getConfiguration().getRendererForwarding().getRendererForwardingByTenant();
        Map<Long, Ipv4Prefix> hm = new HashMap<>();
        for (RendererForwardingByTenant rft : rendererForwardingByTenant) {
            for (RendererNetworkDomain rnd : rft.getRendererNetworkDomain()) {
                SubnetAugmentRenderer subnet = rnd.getAugmentation(SubnetAugmentRenderer.class);
                if (subnet.getSubnet() != null && !subnet.getSubnet().isIsTenant()
                        && subnet.getSubnet().getAllocationPool() != null) {
                    for (AllocationPool pool : subnet.getSubnet().getAllocationPool()) {
                        if (subnet.getSubnet().getIpPrefix().getIpv4Prefix() != null) {
                            hm.putAll(resolveIpv4Prefix(subnet.getSubnet().getIpPrefix().getIpv4Prefix(),
                                    pool.getFirst(), pool.getLast(), natEntries));
                        }
                    }
                }
            }
        }
        List<ExternalIpAddressPool> extIps = new ArrayList<>();
        for (Entry<Long, Ipv4Prefix> entry : hm.entrySet()) {
            extIps.add(new ExternalIpAddressPoolBuilder().setPoolId(entry.getKey())
                .setExternalIpPool(entry.getValue())
                .build());
        }
        return extIps;
    }

    @VisibleForTesting
    private static Map<Long,Ipv4Prefix> resolveIpv4Prefix(@Nonnull Ipv4Prefix prefix, @Nonnull String first,
            @Nullable String last, @Nullable List<MappingEntryBuilder> natEntries) {
        LOG.trace("Resolving Ipv4Prefix. prefix: {}, first: {}, last: {}", prefix.getValue(), first, last);
        SubnetUtils subnet = new SubnetUtils(prefix.getValue());
        Map<Long,Ipv4Prefix> ext = new HashMap<>();
        int min = subnet.getInfo().asInteger(first);
        for (String addr : subnet.getInfo().getAllAddresses()) {
            int asInt = subnet.getInfo().asInteger(addr);
            if (asInt < min) {
                continue;
            }
            ext.put(Integer.toUnsignedLong(asInt), new Ipv4Prefix(addr + "/32"));
            if (last == null || subnet.getInfo().asInteger(addr) >= subnet.getInfo().asInteger(last)) {
                break;
            }
        }
        if (natEntries != null) {
            for (MappingEntryBuilder natEntry : natEntries) {
                Ipv4Address externalSrcAddress = natEntry.getExternalSrcAddress();
                ext.remove(Integer.toUnsignedLong(subnet.getInfo().asInteger(externalSrcAddress.getValue())));
            }
        }

        return ext;
    }
}
