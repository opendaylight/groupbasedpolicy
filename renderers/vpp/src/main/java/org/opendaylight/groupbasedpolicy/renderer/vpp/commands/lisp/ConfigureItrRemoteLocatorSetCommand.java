/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.ItrRemoteLocatorSetDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureItrRemoteLocatorSetCommand extends AbstractLispCommand<ItrRemoteLocatorSet> {
    private ItrRemoteLocatorSetDom itrRemoteLocatorSetDom;

    public ConfigureItrRemoteLocatorSetCommand(ItrRemoteLocatorSetDom itrRemoteLocatorSetDom) {
        this.itrRemoteLocatorSetDom = itrRemoteLocatorSetDom;
    }

    @Override
    public InstanceIdentifier<ItrRemoteLocatorSet> getIid() {
        return VppIidFactory.getItrRemoteLocatorSetIid();
    }

    @Override
    public ItrRemoteLocatorSet getData() {
        return itrRemoteLocatorSetDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + itrRemoteLocatorSetDom.toString();
    }
}
