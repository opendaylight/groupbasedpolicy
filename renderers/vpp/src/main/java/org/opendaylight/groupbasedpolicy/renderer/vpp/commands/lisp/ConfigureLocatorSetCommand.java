/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocatorSetDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureLocatorSetCommand extends AbstractLispCommand<LocatorSet> {

    private LocatorSetDom locatorSetDom;

    public ConfigureLocatorSetCommand(LocatorSetDom locatorSetDom) {
        this.locatorSetDom = locatorSetDom;
    }

    @Override
    public InstanceIdentifier<LocatorSet> getIid() {
        return VppIidFactory.getLocatorSetIid(new LocatorSetKey(locatorSetDom.getLocatorName()));
    }

    @Override
    public LocatorSet getData() {
        return locatorSetDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + locatorSetDom.toString();
    }
}
