/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DataStoreHelper.class})
public class IovisorModuleUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void validateIovisorModuleInstanceTest() {
        DataBroker dataBroker = Mockito.mock(DataBroker.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(mock(ReadOnlyTransaction.class));

        PowerMockito.mockStatic(DataStoreHelper.class);
        Optional<IovisorModuleInstances> result = mock(Optional.class);
        PowerMockito.when(DataStoreHelper.readFromDs(any(LogicalDatastoreType.class),
                                                     any(InstanceIdentifier.class),
                                                     any(ReadOnlyTransaction.class)))
                                                     .thenReturn(result);

        Uri goodUri = mock(Uri.class);

        when(result.isPresent()).thenReturn(false);
        Assert.assertFalse(IovisorModuleUtils.validateIovisorModuleInstance(dataBroker, goodUri));

        when(result.isPresent()).thenReturn(true);
        IovisorModuleInstance iovisorModuleInstance = mock(IovisorModuleInstance.class);
        when(iovisorModuleInstance.getUri()).thenReturn(goodUri);
        List<IovisorModuleInstance> iovisorModuleInstanceList = new ArrayList<>();
        iovisorModuleInstanceList.add(iovisorModuleInstance);
        IovisorModuleInstances iovisorModuleInstances = mock(IovisorModuleInstances.class);
        when(iovisorModuleInstances.getIovisorModuleInstance()).thenReturn(iovisorModuleInstanceList);
        when(result.get()).thenReturn(iovisorModuleInstances);
        Assert.assertTrue(IovisorModuleUtils.validateIovisorModuleInstance(dataBroker, goodUri));

        Uri wrongUri = mock(Uri.class);
        Assert.assertFalse(IovisorModuleUtils.validateIovisorModuleInstance(dataBroker, wrongUri));
    }
}
