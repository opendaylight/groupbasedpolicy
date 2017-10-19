/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;

public class EidDom<T extends LispAddressFamily> implements CommandModel {

    private long vni;
    private Address address;
    private Class<T> addressFamily;

    public long getVni() {
        return vni;
    }

    public void setVni(long vni) {
        this.vni = vni;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Class<T> getAddressFamily() {
        return addressFamily;
    }

    public void setAddressFamily(Class<T> addressFamily) {
        this.addressFamily = addressFamily;
    }

    @Override
    public Eid getSALObject() {
        return new EidBuilder()
                    .setVirtualNetworkId(new InstanceIdType(vni))
                    .setAddressType(getAddressFamily())
                    .setAddress(address).build();
    }

    @Override public String toString() {
        return "Eid{" + "vni=" + vni + ", address=" + address + ", addressFamily=" + addressFamily + '}';
    }
}
