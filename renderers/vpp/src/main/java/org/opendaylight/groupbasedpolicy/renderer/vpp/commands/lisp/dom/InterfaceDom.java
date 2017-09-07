/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;

public class InterfaceDom implements CommandModel {

    private String interfaceName;
    private short priority;
    private short weight;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    @Override
    public Interface getSALObject() {
        Preconditions.checkNotNull(interfaceName, "Interface Name needs to be set!");

        return new InterfaceBuilder()
                    .setKey(new InterfaceKey(interfaceName))
                    .setInterfaceRef(interfaceName)
                    .setPriority(priority)
                    .setWeight(priority).build();
    }

    @Override public String toString() {
        return "Interface{" + "interfaceName='" + interfaceName + ", priority=" + priority + ", weight="
            + weight + '}';
    }
}
