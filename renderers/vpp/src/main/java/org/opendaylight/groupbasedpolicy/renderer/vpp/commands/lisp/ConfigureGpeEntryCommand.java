/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GbpGpeEntryDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureGpeEntryCommand extends AbstractLispCommand<GpeEntry> {

    private GbpGpeEntryDom gpeEntryDom;

    public ConfigureGpeEntryCommand(GbpGpeEntryDom gpeEntryDom) {
        this.gpeEntryDom = gpeEntryDom;
    }

    @Override
    public InstanceIdentifier<GpeEntry> getIid() {
        return VppIidFactory.getGpeEntryIid(new GpeEntryKey(gpeEntryDom.getId()));
    }

    @Override
    public GpeEntry getData() {
        return gpeEntryDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + gpeEntryDom.toString();
    }
}
