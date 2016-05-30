/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;

public abstract class AbstractInterfaceCommand<T extends AbstractInterfaceCommand<T>> implements ConfigCommand {

    protected General.Operations operation;
    protected String name;
    protected String description;
    protected Boolean enabled;

    protected enum linkUpDownTrap {
        ENABLED, DISABLED
    }

    public General.Operations getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AbstractInterfaceCommand<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public AbstractInterfaceCommand<T> setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

}
