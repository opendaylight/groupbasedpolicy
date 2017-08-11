/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.NativeForwardPathDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 6/13/17.
 */
public class ConfigureNativeForwardPathCommand extends AbstractLispCommand<NativeForwardPath> {
    private NativeForwardPathDom nativeForwardPathDom;

    public ConfigureNativeForwardPathCommand(NativeForwardPathDom nativeForwardPathDom) {
        this.nativeForwardPathDom = nativeForwardPathDom;
    }

    @Override
    public InstanceIdentifier<NativeForwardPath> getIid() {
        return VppIidFactory.getNativeForwardPathIid(nativeForwardPathDom.getVrfId(),
                                                    new NativeForwardPathKey(nativeForwardPathDom.getNextHopIp()));
    }

    @Override
    public NativeForwardPath getData() {
        return nativeForwardPathDom.getSALObject();
    }
}
