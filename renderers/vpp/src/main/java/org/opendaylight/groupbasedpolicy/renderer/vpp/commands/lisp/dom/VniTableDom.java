/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;

public class VniTableDom implements CommandModel {
    private long virtualNetworkIdentifier;
    private VrfSubtable vrfSubtable;

    public long getVirtualNetworkIdentifier() {
        return virtualNetworkIdentifier;
    }

    public void setVirtualNetworkIdentifier(long virtualNetworkIdentifier) {
        this.virtualNetworkIdentifier = virtualNetworkIdentifier;
    }

    public VrfSubtable getVrfSubtable() {
        return vrfSubtable;
    }

    public void setVrfSubtable(VrfSubtable vrfSubtable) {
        this.vrfSubtable = vrfSubtable;
    }

    @Override
    public VniTable getSALObject() {
        Preconditions.checkNotNull(virtualNetworkIdentifier, "Virtual Network Identifier" +
                " needs to be set!");

        VniTableKey vniTableKey = new VniTableKey(virtualNetworkIdentifier);
        return new VniTableBuilder()
                        .setKey(vniTableKey)
                        .setVirtualNetworkIdentifier(vniTableKey.getVirtualNetworkIdentifier())
                        .setVrfSubtable(vrfSubtable).build();
    }

    @Override public String toString() {
        return "VniTable{" + "virtualNetworkIdentifier=" + virtualNetworkIdentifier + ", vrfSubtable=" + vrfSubtable
            + '}';
    }
}
