/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocalMappingDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureLocalMappingInEidTableCommand extends AbstractLispCommand<LocalMapping> {

    private LocalMappingDom localMappingDom;

    public ConfigureLocalMappingInEidTableCommand(LocalMappingDom localMappingDom) {
        this.localMappingDom = localMappingDom;
    }

    @Override
    public InstanceIdentifier<LocalMapping> getIid() {
        return VppIidFactory.getLocalMappingIid(new VniTableKey(localMappingDom.getVni()),
                                                new LocalMappingKey(localMappingDom.getMappingId()));
    }

    @Override
    public LocalMapping getData() {
        return localMappingDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + localMappingDom.toString();
    }
}
