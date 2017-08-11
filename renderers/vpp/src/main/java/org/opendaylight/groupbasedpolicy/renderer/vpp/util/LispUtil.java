/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import java.security.InvalidParameterException;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.EidDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.HmacKeyDom;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Function;

/**
 * Created by Shakib Ahmed on 3/21/17.
 */
public class LispUtil {
    private LispUtil() {

    }

    public static Function<String, InstanceIdentifier<Node>> HOSTNAME_TO_IID = host -> {
        return VppIidFactory.getNetconfNodeIid(new NodeId(host));
    };

    public static Eid toEid(Address address, long vni, Class<? extends LispAddressFamily> addressType) {
        EidDom eidDom = new EidDom();
        eidDom.setAddress(address);
        eidDom.setVni(vni);
        eidDom.setAddressFamily(addressType);
        return eidDom.getSALObject();
    }

    public static RemoteEid toRemoteEid(Address address, long vni, Class<? extends LispAddressFamily> addressType) {
        RemoteEidBuilder remoteEidBuilder = new RemoteEidBuilder();
        remoteEidBuilder.setAddress(address);
        remoteEidBuilder.setVirtualNetworkId(new InstanceIdType(vni));
        remoteEidBuilder.setAddressType(addressType);
        return remoteEidBuilder.build();
    }

    public static LocalEid toLocalEid(Address address, long vni, Class<? extends LispAddressFamily> addressType) {
        LocalEidBuilder localEidBuilder = new LocalEidBuilder();
        localEidBuilder.setAddress(address);
        localEidBuilder.setVirtualNetworkId(new InstanceIdType(vni));
        localEidBuilder.setAddressType(addressType);
        return localEidBuilder.build();
    }

    public static HmacKey toHmacKey(HmacKeyType keyType, String key) {
        HmacKeyDom hmacKeyDom = new HmacKeyDom();
        hmacKeyDom.setKey(key);
        hmacKeyDom.setKeyType(keyType);
        return hmacKeyDom.getSALObject();
    }

    public static Ipv4 toIpv4(String ipStr) throws InvalidParameterException {
        String[] strArray = ipStr.split("/");
        if (strArray.length == 0 || strArray.length > 2) {
            throw new InvalidParameterException("Parameter " + ipStr + " is invalid for IPv4");
        }

        if(strArray.length == 2) {
            int mask = Integer.valueOf(strArray[1]);
            if(mask != 32) {
                throw new InvalidParameterException("Parameter " + ipStr + " is invalid for IPv4");
            }
        }

        return new Ipv4Builder().setIpv4(new Ipv4Address(strArray[0])).build();
    }

    public static Ipv4Prefix toLispIpv4Prefix(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                  .inet.types.rev130715.Ipv4Prefix ipv4Prefix) {
        return new Ipv4PrefixBuilder().setIpv4Prefix(ipv4Prefix).build();
    }
}
