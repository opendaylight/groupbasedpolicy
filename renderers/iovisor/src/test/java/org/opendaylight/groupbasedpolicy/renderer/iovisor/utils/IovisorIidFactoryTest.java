/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.utils;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IovisorIidFactoryTest {

    @Test
    public void iovisorModuleInstanceIidTest() {
        IovisorModuleInstanceKey iovisorModuleInstanceKey = mock(IovisorModuleInstanceKey.class);
        InstanceIdentifier<IovisorModuleInstance> identifier = IovisorIidFactory.iovisorModuleInstanceIid(iovisorModuleInstanceKey);
        Assert.assertEquals(iovisorModuleInstanceKey, identifier.firstKeyOf(IovisorModuleInstance.class));
    }
}
