/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapRegisterDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.register.grouping.MapRegister;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureMapRegisterStatusCommand extends AbstractLispCommand<MapRegister>{

    private MapRegisterDom mapRegisterDom;

    public ConfigureMapRegisterStatusCommand(MapRegisterDom mapRegisterDom) {
        this.mapRegisterDom = mapRegisterDom;
    }

    @Override
    public InstanceIdentifier<MapRegister> getIid() {
        return VppIidFactory.getMapRegisterIid();
    }

    @Override
    public MapRegister getData() {
        return mapRegisterDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + mapRegisterDom.toString();
    }
}
