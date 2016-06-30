/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.ServiceChainingUtil;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.ClassNameType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link PolicyWriterUtil}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PolicyWriterUtil.class, NetconfTransactionCreator.class, ServiceChainingUtil.class})
public class PolicyWriterUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriterUtilTest.class);

    private static final NodeId NODE_ID = new NodeId("unit-node-id-1");
    private static final String POLICY_MAP_NAME = "unit-policy-map-name-1";
    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction wTx;
    private java.util.Optional<WriteTransaction> wTxOptional;
    @Mock
    private ReadOnlyTransaction rTx;
    private java.util.Optional<ReadOnlyTransaction> rTxOptional;

    @Before
    public void setUp() throws Exception {
        wTxOptional = java.util.Optional.of(wTx);
        rTxOptional = java.util.Optional.of(rTx);
    }

    @Test
    public void testWriteClassMaps() throws Exception {
        LOG.debug("scenario: pass through with null classMapEntries collection");
        Assert.assertTrue(PolicyWriterUtil.writeClassMaps(null, NODE_ID, dataBroker));

        LOG.debug("scenario: pass through with empty classMapEntries collection");
        final List<ClassMap> classMapEntries = new ArrayList<>();
        Assert.assertTrue(PolicyWriterUtil.writeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        classMapEntries.add(new ClassMapBuilder().setName("unit-classMapEntry-name").build());
        Assert.assertFalse(PolicyWriterUtil.writeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.writeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(
                        new ClassMapBuilder().setName("asd").build())));
        Assert.assertTrue(PolicyWriterUtil.writeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->null");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "checkWritten")).toReturn(null);
        Assert.assertFalse(PolicyWriterUtil.writeClassMaps(classMapEntries, NODE_ID, dataBroker));
    }

    @Test
    public void testRemoveClassMaps() throws Exception {
        LOG.debug("scenario: pass through with null classMapEntries collection");
        Assert.assertTrue(PolicyWriterUtil.removeClassMaps(null, NODE_ID, dataBroker));

        LOG.debug("scenario: pass through with empty classMapEntries collection");
        final List<ClassMap> classMapEntries = new ArrayList<>();
        Assert.assertTrue(PolicyWriterUtil.removeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        classMapEntries.add(new ClassMapBuilder().setName("unit-classMapEntry-name").build());
        Assert.assertFalse(PolicyWriterUtil.removeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.removeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
        Assert.assertTrue(PolicyWriterUtil.removeClassMaps(classMapEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->false");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "checkRemoved")).toReturn(false);
        Assert.assertFalse(PolicyWriterUtil.removeClassMaps(classMapEntries, NODE_ID, dataBroker));
    }

    @Test
    public void testWritePolicyMap() throws Exception {
        final List<Class> classEntries = new ArrayList<>();

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        classEntries.add(new ClassBuilder().setName(new ClassNameType("unit-classEntry-name")).build());
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with null classEntries collection");
        try {
            PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, null, NODE_ID, dataBroker);
            Assert.fail("expected NPE caused by classEntries parameter");
        } catch (Exception e) {
            // expected
        }

        LOG.debug("scenario: fail with empty classEntries collection");
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(
                        new ClassMapBuilder().setName("asd").build())));
        Assert.assertTrue(PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->null");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "checkWritten")).toReturn(null);
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));
    }

    @Test
    public void testRemovePolicyMapEntries() throws Exception {
        LOG.debug("scenario: pass through with null classEntries collection");
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntries(POLICY_MAP_NAME, null, NODE_ID, dataBroker));

        LOG.debug("scenario: pass through with empty classEntries collection");
        final List<Class> classEntries = new ArrayList<>();
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntries(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        classEntries.add(new ClassBuilder().setName(new ClassNameType("unit-classMapEntry-name")).build());
        Assert.assertFalse(PolicyWriterUtil.removePolicyMapEntries(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntries(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));

        //TODO: fix - failed remove transacion shall render whole operation as failed
        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->false");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "deleteTransaction")).toReturn(false);
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntries(POLICY_MAP_NAME, classEntries, NODE_ID, dataBroker));
    }

    @Test
    public void testWriteInterface() throws Exception {
        LOG.debug("scenario: fail with no writeOnlyTransaction");
        final String interfaceName = "unit-interface-1";
        Assert.assertFalse(PolicyWriterUtil.writeInterface(POLICY_MAP_NAME, interfaceName, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.writeInterface(POLICY_MAP_NAME, interfaceName, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeInterface(POLICY_MAP_NAME, interfaceName, NODE_ID, dataBroker));
    }

    @Test
    public void testWriteLocal() throws Exception {
        LOG.debug("scenario: succeed with null localForwarder");
        Assert.assertTrue(PolicyWriterUtil.writeLocal(null, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with no writeOnlyTransaction");
        final Local localForwarder = new LocalBuilder().build();
        Assert.assertFalse(PolicyWriterUtil.writeLocal(localForwarder, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.writeLocal(localForwarder, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeLocal(localForwarder, NODE_ID, dataBroker));
    }

    @Test
    public void testRemoveLocal() throws Exception {
        LOG.debug("scenario: succeed with no service path present");
        Assert.assertTrue(PolicyWriterUtil.removeLocal(NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, no writeOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(ServiceChainingUtil.class, "checkServicePathPresence")).toReturn(true);
        Assert.assertFalse(PolicyWriterUtil.removeLocal(NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.removeLocal(NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.removeLocal(NODE_ID, dataBroker));
    }

    @Test
    public void testWriteRemote() throws Exception {
        LOG.debug("scenario: succeed with null List<ServiceFfName>");
        Assert.assertTrue(PolicyWriterUtil.writeRemote(null, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed with empty List<ServiceFfName>");
        Assert.assertTrue(PolicyWriterUtil.writeRemote(Collections.emptyList(), NODE_ID, dataBroker));

        LOG.debug("scenario: fail with no writeOnlyTransaction");
        final List<ServiceFfName> remotes = Collections.singletonList(new ServiceFfNameBuilder().build());
        Assert.assertFalse(PolicyWriterUtil.writeRemote(remotes, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.writeRemote(remotes, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeRemote(remotes, NODE_ID, dataBroker));
    }

    @Test
    public void testWriteServicePaths() throws Exception {
        LOG.debug("scenario: succeed with empty List<remote>");
        Assert.assertTrue(PolicyWriterUtil.writeServicePaths(Collections.emptyList(), NODE_ID, dataBroker));

        LOG.debug("scenario: fail with no writeOnlyTransaction");
        final List<ServiceChain> serviceChains = Collections.singletonList(new ServiceChainBuilder()
                .setServicePath(Collections.singletonList(new ServicePathBuilder()
                        .setServicePathId(42L)
                        .build()))
                .build());
        Assert.assertFalse(PolicyWriterUtil.writeServicePaths(serviceChains, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.writeServicePaths(serviceChains, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeServicePaths(serviceChains, NODE_ID, dataBroker));
    }

    @Test
    public void testRemoveServicePaths() throws Exception {
        LOG.debug("scenario: succeed with service path present null");
        Assert.assertTrue(PolicyWriterUtil.removeServicePaths(null, NODE_ID, dataBroker));

        LOG.debug("scenario: succeed with no service path present");
        Assert.assertTrue(PolicyWriterUtil.removeServicePaths(Collections.emptyList(), NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, no writeOnlyTransaction");
        final List<ServiceChain> serviceChains = Collections.singletonList(new ServiceChainBuilder()
                .setServicePath(Collections.singletonList(new ServicePathBuilder()
                        .setServicePathId(42L)
                        .build()))
                .build());
        Assert.assertFalse(PolicyWriterUtil.removeServicePaths(serviceChains, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertTrue(PolicyWriterUtil.removeServicePaths(serviceChains, NODE_ID, dataBroker));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.removeServicePaths(serviceChains, NODE_ID, dataBroker));
    }
}