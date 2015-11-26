/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.utils;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class IovisorIidFactory {

    private IovisorIidFactory() {
    }

    private static final InstanceIdentifier<IovisorModuleInstances> IOVISOR_MODULE_INSTANCES_IID = InstanceIdentifier.builder(IovisorModuleInstances.class).build();

    /**
     * @return The {@link InstanceIdentifier} of the {@link IovisorModuleInstances}
     */
    public static InstanceIdentifier<IovisorModuleInstances> iovisorModuleInstancesIid() {
        return IOVISOR_MODULE_INSTANCES_IID;
    }

    /**
     * Return the InstanceIdentifier for a specific IovisorModuleInstance.
     * @param iovisorModuleInstanceKey The key of the {@link IovisorModuleInstance} we want to retrieve.
     * @return The {@link InstanceIdentifier} of the {@link IovisorModuleInstance}
     */
    public static InstanceIdentifier<IovisorModuleInstance> iovisorModuleInstanceIid(IovisorModuleInstanceKey iovisorModuleInstanceKey) {
        return IOVISOR_MODULE_INSTANCES_IID.child(IovisorModuleInstance.class, iovisorModuleInstanceKey);
    }
}
