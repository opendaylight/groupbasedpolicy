/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class ItrRemoteLocatorSetDom implements CommandModel {
    private String locatorSetName;

    public String getLocatorSetName() {
        return locatorSetName;
    }

    public void setLocatorSetName(String locatorSetName) {
        this.locatorSetName = locatorSetName;
    }

    @Override
    public ItrRemoteLocatorSet getSALObject() {
        return new ItrRemoteLocatorSetBuilder()
                .setRemoteLocatorSetName(locatorSetName).build();
    }

    @Override public String toString() {
        return "ItrRemoteLocatorSet{" + "locatorSetName='" + locatorSetName  + '}';
    }
}
