/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabaseBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link MasterDatabaseBindingDaoImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MasterDatabaseBindingDaoImplTest {

    private static final Sgt KEY_1 = new Sgt(1);
    private static final IpPrefix IP_PREFIX = new IpPrefix(new Ipv4Prefix("1.2.3.4/32"));
    private final Topology TOPOLOGY_VALUE;
    private final MasterDatabaseBinding MASTER_DB_BINDING_VALUE;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SimpleCachedDao<IpPrefix, MasterDatabaseBinding> cachedDao;
    @Mock
    private ReadOnlyTransaction rTx;

    private MasterDatabaseBindingDaoImpl dao;

    public MasterDatabaseBindingDaoImplTest() {
        MASTER_DB_BINDING_VALUE = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(KEY_1)
                .setIpPrefix(IP_PREFIX)
                .build();

        TOPOLOGY_VALUE = new TopologyBuilder()
                .setTopologyId(new TopologyId(Configuration.TOPOLOGY_NAME))
                .setNode(Lists.newArrayList(new NodeBuilder()
                        .setNodeId(new NodeId("utNodeId"))
                        .addAugmentation(SxpNodeIdentity.class, new SxpNodeIdentityBuilder()
                                .setSxpDomains(new SxpDomainsBuilder()
                                        .setSxpDomain(Collections.singletonList(new SxpDomainBuilder()
                                                .setDomainName("global")
                                                .setMasterDatabase(new MasterDatabaseBuilder()
                                                        .setMasterDatabaseBinding(Lists.newArrayList(MASTER_DB_BINDING_VALUE))
                                                        .build())
                                                .build()))
                                        .build())
                                .build())
                        .build()))
                .build();
    }


    @Before
    public void setUp() throws Exception {
        dao = new MasterDatabaseBindingDaoImpl(dataBroker, cachedDao);
    }

    @Test
    public void testRead_absent() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(Optional.<MasterDatabaseBinding>absent());
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<Topology>>any())).thenReturn(
                Futures.<Optional<Topology>, ReadFailedException>immediateCheckedFuture(
                        Optional.<Topology>absent()));


        final ListenableFuture<Optional<MasterDatabaseBinding>> read = dao.read(IP_PREFIX);
        Assert.assertTrue(read.isDone());
        Assert.assertFalse(read.get().isPresent());
    }

    @Test
    public void testRead_presentCached() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(Optional.of(MASTER_DB_BINDING_VALUE));

        final ListenableFuture<Optional<MasterDatabaseBinding>> read = dao.read(IP_PREFIX);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getSecurityGroupTag());
    }

    @Test
    public void testRead_presentDS() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(
                Optional.<MasterDatabaseBinding>absent(),
                Optional.of(MASTER_DB_BINDING_VALUE));
        Mockito.when(cachedDao.isEmpty()).thenReturn(true);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<Topology>>any())).thenReturn(
                Futures.<Optional<Topology>, ReadFailedException>immediateCheckedFuture(
                        Optional.of(TOPOLOGY_VALUE)));

        final ListenableFuture<Optional<MasterDatabaseBinding>> read = dao.read(IP_PREFIX);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getSecurityGroupTag());

        final InOrder inOrder = Mockito.inOrder(cachedDao);
        inOrder.verify(cachedDao).invalidateCache();
        inOrder.verify(cachedDao).update(IP_PREFIX, MASTER_DB_BINDING_VALUE);
        inOrder.verify(cachedDao).find(IP_PREFIX);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testReadBy() throws Exception {
        Mockito.when(cachedDao.isEmpty()).thenReturn(false);
        Mockito.when(cachedDao.values()).thenReturn(Collections.singleton(MASTER_DB_BINDING_VALUE));

        final ListenableFuture<Collection<MasterDatabaseBinding>> readByFt = dao.readBy(KEY_1);
        Assert.assertTrue(readByFt.isDone());
        Assert.assertEquals(1, readByFt.get().size());
    }
}