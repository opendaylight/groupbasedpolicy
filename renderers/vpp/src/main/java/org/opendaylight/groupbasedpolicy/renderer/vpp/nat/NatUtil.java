/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
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
}
