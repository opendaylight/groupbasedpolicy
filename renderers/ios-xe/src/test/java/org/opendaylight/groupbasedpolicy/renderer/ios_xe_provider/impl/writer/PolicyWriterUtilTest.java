/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
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
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.ServiceChainingUtil;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.ClassNameType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
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
    public void testWriteClassMap() throws Exception {
        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        final ClassMap classMap = new ClassMapBuilder().setName("unit-classMapEntry-name").build();
        Assert.assertFalse(PolicyWriterUtil.writeClassMap(classMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.writeClassMap(classMap, getLocation()));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(
                        new ClassMapBuilder().setName("asd").build())));
        Assert.assertTrue(PolicyWriterUtil.writeClassMap(classMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->null");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(null);
        Assert.assertFalse(PolicyWriterUtil.writeClassMap(classMap, getLocation()));
    }

    @Test
    public void testRemoveClassMap() throws Exception {
        LOG.debug("scenario: pass through with null classMapEntries collection");
        Assert.assertTrue(PolicyWriterUtil.removeClassMap(null, getLocation()));

        LOG.debug("scenario: pass through with empty classMapEntries collection");
        final ClassMap classMap = new ClassMapBuilder().setName("unit-classMapEntry-name").build();
        Assert.assertTrue(PolicyWriterUtil.removeClassMap(classMap, getLocation()));

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(new ClassBuilder().build());
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction"))
                .toReturn(java.util.Optional.empty());
        Assert.assertFalse(PolicyWriterUtil.removeClassMap(classMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.removeClassMap(classMap, getLocation()));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(null);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
        Assert.assertTrue(PolicyWriterUtil.removeClassMap(classMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->false");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(new ClassBuilder().build());
        Assert.assertFalse(PolicyWriterUtil.removeClassMap(classMap, getLocation()));
    }

    @Test
    public void testWritePolicyMap() throws Exception {
        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        final PolicyMap policyMap = new PolicyMapBuilder().setName("unit-policyMap-name").build();
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(policyMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(policyMap, getLocation()));

        LOG.debug("scenario: fail with empty classEntries collection");
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(policyMap, getLocation()));

        LOG.debug("scenario: succeed with one entry, available writeOnlyTransaction, available readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfReadOnlyTransaction")).toReturn(rTxOptional);
        Mockito.when(rTx.read(Mockito.eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<ClassMap>>any()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(
                        new ClassMapBuilder().setName("asd").build())));
        Assert.assertTrue(PolicyWriterUtil.writePolicyMap(policyMap, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, available readOnlyTransaction, check->null");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(null);
        Assert.assertFalse(PolicyWriterUtil.writePolicyMap(policyMap, getLocation()));
    }

    @Test
    public void testRemovePolicyMapEntry() throws Exception {
        LOG.debug("scenario: pass through with empty classEntries collection");
        final Class entry = new ClassBuilder().setName(new ClassNameType("unit-class-name")).build();
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntry(entry, getLocation()));

        LOG.debug("scenario: fail with one entry, no writeOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(new ClassBuilder().build());
        Assert.assertFalse(PolicyWriterUtil.removePolicyMapEntry(entry, getLocation()));

        LOG.debug("scenario: fail with one entry, available writeOnlyTransaction, no readOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.removePolicyMapEntry(entry, getLocation()));
    }

    @Test
    public void testWriteInterface() throws Exception {
        LOG.debug("scenario: fail with no writeOnlyTransaction");
        Assert.assertFalse(PolicyWriterUtil.writeInterface(getLocation()));

        LOG.debug("scenario: fail - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertFalse(PolicyWriterUtil.writeInterface(getLocation()));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeInterface(getLocation()));
    }

    @Test
    public void testWriteRemote() throws Exception {
        LOG.debug("scenario: succeed with empty List<ServiceFfName>");
        final ServiceFfName forwarder = new ServiceFfNameBuilder().setName("unit-service-forwarder-name").build();
        Assert.assertFalse(PolicyWriterUtil.writeRemote(forwarder, getLocation()));

        LOG.debug("scenario: fail - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertFalse(PolicyWriterUtil.writeRemote(forwarder, getLocation()));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeRemote(forwarder, getLocation()));
    }

    @Test
    public void testWriteServicePath() throws Exception {
        LOG.debug("scenario: fail with no writeOnlyTransaction");
        final ServiceChain serviceChain = new ServiceChainBuilder()
                .setServicePath(Collections.singletonList(new ServicePathBuilder()
                        .setServicePathId(42L)
                        .build()))
                .build();
        Assert.assertFalse(PolicyWriterUtil.writeServicePath(serviceChain, getLocation()));

        LOG.debug("scenario: fail - available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertFalse(PolicyWriterUtil.writeServicePath(serviceChain, getLocation()));

        LOG.debug("scenario: succeed - available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.writeServicePath(serviceChain, getLocation()));
    }

    @Test
    public void testRemoveServicePath() throws Exception {
        LOG.debug("scenario: fail with service path present, no writeOnlyTransaction");
        PowerMockito.stub(PowerMockito.method(PolicyWriterUtil.class, "netconfRead")).toReturn(new ClassBuilder().build());
        final ServiceChain serviceChain = new ServiceChainBuilder()
                .setServicePath(Collections.singletonList(new ServicePathBuilder()
                        .setServicePathId(42L)
                        .build()))
                .build();
        Assert.assertFalse(PolicyWriterUtil.removeServicePath(serviceChain, getLocation()));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, no submit future");
        PowerMockito.stub(PowerMockito.method(NetconfTransactionCreator.class, "netconfWriteOnlyTransaction")).toReturn(wTxOptional);
        Assert.assertFalse(PolicyWriterUtil.removeServicePath(serviceChain, getLocation()));

        LOG.debug("scenario: fail with service path present, available writeOnlyTransaction, available future");
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Assert.assertTrue(PolicyWriterUtil.removeServicePath(serviceChain, getLocation()));
    }

    private PolicyManagerImpl.PolicyMapLocation getLocation() {
        final String POLICY_MAP = "policy-map";
        final String INTERFACE = "interface";
        final NodeId nodeId = new NodeId("node-id");
        final String IP_ADDRESS = "ip-address";
        return new PolicyManagerImpl.PolicyMapLocation(POLICY_MAP, INTERFACE, nodeId, IP_ADDRESS, dataBroker);
    }
}