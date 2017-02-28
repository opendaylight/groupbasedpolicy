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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.AllocationPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NatUtil {

    public void setInboundInterface(Interface iface, WriteTransaction wTx) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat = new NatBuilder().setInbound(new InboundBuilder().build()).build();
        wTx.put(LogicalDatastoreType.CONFIGURATION, natIid, nat);
    }

    public static void setOutboundInterface(Interface iface, WriteTransaction wTx) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        Nat nat = new NatBuilder().setOutbound(new OutboundBuilder().build()).build();
        wTx.put(LogicalDatastoreType.CONFIGURATION, natIid, nat);
    }

    public static void unsetOutboundInterface(Interface iface, WriteTransaction wTx) {
        InstanceIdentifier<Nat> natIid = buildNatIid(VppIidFactory.getInterfaceIID(iface.getKey()));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, natIid);
    }

    public static InstanceIdentifier<Nat> buildNatIid(InstanceIdentifier<Interface> ifaceIid) {
        return ifaceIid.builder().augmentation(NatInterfaceAugmentation.class).child(Nat.class).build();
    }

    public static List<ExternalIpAddressPool> resolveDynamicNat(@Nonnull PolicyContext policyCtx) {
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
                                    pool.getFirst(), pool.getLast()));
                        }
                    }
                }
            }
        }
        List<ExternalIpAddressPool> extIps = new ArrayList<>();
        for (Entry<Long, Ipv4Prefix> entry : hm.entrySet()) {
            new ExternalIpAddressPoolBuilder().setPoolId(entry.getKey()).setExternalIpPool(entry.getValue()).build();
        }
        return extIps;
    }

    private static Map<Long,Ipv4Prefix> resolveIpv4Prefix(@Nonnull Ipv4Prefix prefix, @Nonnull String first,
            @Nullable String last) {
        SubnetUtils subnet = new SubnetUtils(prefix.getValue());
        Map<Long,Ipv4Prefix> ext = new HashMap<>();
        int min = subnet.getInfo().asInteger(first);
        for (String addr : subnet.getInfo().getAllAddresses()) {
            int asInt = subnet.getInfo().asInteger(addr);
            if (asInt < min) {
                continue;
            }
            ext.put(Long.valueOf(asInt), new Ipv4Prefix(addr + "/32"));
            if (last == null || subnet.getInfo().asInteger(addr) > subnet.getInfo().asInteger(last)) {
                break;
            }
        }
        return ext;
    }
}
