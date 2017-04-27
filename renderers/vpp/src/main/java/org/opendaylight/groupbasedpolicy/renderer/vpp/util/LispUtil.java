/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.EidDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.HmacKeyDom;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.hmac.key.grouping.HmacKey;

import java.security.InvalidParameterException;

/**
 * Created by Shakib Ahmed on 3/21/17.
 */
public class LispUtil {
    private LispUtil() {

    }

    public static Eid toEid(Address address, long vni, Class<? extends LispAddressFamily> addressType) {
        EidDom eidDom = new EidDom();
        eidDom.setAddress(address);
        eidDom.setVni(vni);
        eidDom.setAddressFamily(addressType);
        return eidDom.getSALObject();
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
}
