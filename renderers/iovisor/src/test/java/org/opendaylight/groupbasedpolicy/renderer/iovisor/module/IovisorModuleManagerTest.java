/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.module;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;

public class IovisorModuleManagerTest extends GbpIovisorDataBrokerTest {

    private DataBroker dataBroker;
    private IovisorModuleManager iovisorModuleManager;
    IovisorModuleInstance iovisorModuleInstance1;
    IovisorModuleInstance iovisorModuleInstance2;
    IovisorModuleInstance badIovisorModuleInstance1;

    @Before
    public void initialisation() throws Exception {
        dataBroker = getDataBroker();
        iovisorModuleManager = new IovisorModuleManager(dataBroker);

        String iom1 = "10.10.10.10:10000";
        IovisorModuleId iom1Id = new IovisorModuleId(iom1);
        String iom2 = "iom1.groupbasedpolicy.org:10000";
        IovisorModuleId iom2Id = new IovisorModuleId(iom2);

        String badIom1 = "10.10.10.10";
        IovisorModuleId badIom1Id = new IovisorModuleId(badIom1);

        iovisorModuleInstance1 = new IovisorModuleInstanceBuilder().setId(iom1Id)
            .setKey(new IovisorModuleInstanceKey(iom1Id))
            .setUri(new Uri(iom1))
            .build();

        iovisorModuleInstance2 = new IovisorModuleInstanceBuilder().setId(iom2Id)
            .setKey(new IovisorModuleInstanceKey(iom2Id))
            .setUri(new Uri(iom2))
            .build();

        badIovisorModuleInstance1 = new IovisorModuleInstanceBuilder().setId(badIom1Id)
            .setKey(new IovisorModuleInstanceKey(badIom1Id))
            .setUri(new Uri(badIom1))
            .build();
    }

    @Test
    public void addActiveIovisorModuleTest() {
        Assert.assertFalse(iovisorModuleManager.addActiveIovisorModule(iovisorModuleInstance1.getId()));
        Assert.assertTrue(iovisorModuleManager.addProvisionedIovisorModule(iovisorModuleInstance1));
        Assert.assertTrue(iovisorModuleManager.addActiveIovisorModule(iovisorModuleInstance1.getId()));
    }

    @Test
    public void addProvisionedIovisorModuleTest() {
        Assert.assertTrue(iovisorModuleManager.addProvisionedIovisorModule(iovisorModuleInstance1));
        Assert.assertFalse(iovisorModuleManager.addProvisionedIovisorModule(badIovisorModuleInstance1));
    }

    @Test
    public void addIovisorInstanceTest() {
        Assert.assertTrue(
                iovisorModuleManager.addIovisorModule(iovisorModuleInstance1, LogicalDatastoreType.CONFIGURATION));
        Assert.assertTrue(
                iovisorModuleManager.addIovisorModule(iovisorModuleInstance1, LogicalDatastoreType.OPERATIONAL));

        Assert.assertFalse(
                iovisorModuleManager.addIovisorModule(badIovisorModuleInstance1, LogicalDatastoreType.CONFIGURATION));
        Assert.assertFalse(
                iovisorModuleManager.addIovisorModule(badIovisorModuleInstance1, LogicalDatastoreType.OPERATIONAL));
    }

    @Test
    public void getActiveIovisorModuleTest() {
        // Prepare a provisioned IOM
        Assert.assertTrue(iovisorModuleManager.addProvisionedIovisorModule(iovisorModuleInstance1));
        IovisorModuleInstance fetchedIom =
                iovisorModuleManager.getProvisionedIovisorModule(iovisorModuleInstance1.getId());
        Assert.assertEquals(iovisorModuleInstance1, fetchedIom);
        // Its not Active yet
        Assert.assertNull(iovisorModuleManager.getActiveIovisorModule(iovisorModuleInstance1.getId()));
        Assert.assertTrue(iovisorModuleManager.addActiveIovisorModule(iovisorModuleInstance1.getId()));
        Assert.assertEquals(iovisorModuleInstance1,
                iovisorModuleManager.getActiveIovisorModule(iovisorModuleInstance1.getId()));

    }

    @Test
    public void getProvisionedIovisorModuleTest() {
        Assert.assertTrue(iovisorModuleManager.addProvisionedIovisorModule(iovisorModuleInstance1));
        Assert.assertEquals(iovisorModuleInstance1,
                iovisorModuleManager.getProvisionedIovisorModule(iovisorModuleInstance1.getId()));
    }
}
