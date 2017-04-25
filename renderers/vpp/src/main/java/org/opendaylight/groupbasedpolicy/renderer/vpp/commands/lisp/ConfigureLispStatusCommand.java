/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LispDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 3/16/17.
 */
public class ConfigureLispStatusCommand extends AbstractLispCommand<Lisp> {

    private LispDom lispDom;

    public ConfigureLispStatusCommand(LispDom data) {
        lispDom = data;
    }

    @Override
    public InstanceIdentifier<Lisp> getIid() {
        return VppIidFactory.getLispStateIid();
    }

    @Override
    public Lisp getData() {
        return lispDom.getSALObject();
    }

    @Override
    public String toString() {
        return "state: " + lispDom.isEnabled();
    }
}
