/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.listen;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.MasterDatabaseBindingListener;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SxpMapperReactor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointForwardingTemplateBySubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Test for {@link MasterDatabaseBindingListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MasterDatabaseBindingListenerImplTest {

    private static final Sgt SGT_1 = new Sgt(1);
    private static final IpPrefix IP_PREFIX = new IpPrefix(new Ipv4Prefix("1.2.3.4/32"));
    private static final KeyedInstanceIdentifier<MasterDatabaseBinding, MasterDatabaseBindingKey> MASTER_DB_PATH =
            MasterDatabaseBindingListener.SXP_TOPOLOGY_PATH
                    .child(Node.class, new NodeKey(new NodeId("utNodeId")))
                    .augmentation(SxpNodeIdentity.class)
                    .child(SxpDomains.class)
                    .child(SxpDomain.class, new SxpDomainKey("global"))
                    .child(MasterDatabase.class)
                    .child(MasterDatabaseBinding.class, new MasterDatabaseBindingKey(IP_PREFIX));
    private static final DataTreeIdentifier<MasterDatabaseBinding> MASTER_DB_BINDING_TREE_PATH =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, MASTER_DB_PATH);
    private final MasterDatabaseBinding MASTER_DB_BINDING_VALUE;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SxpMapperReactor sxpMapper;
    @Mock
    private DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao;
    @Mock
    private DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;
    @Mock
    private SimpleCachedDao<IpPrefix, MasterDatabaseBinding> cachedDao;
    @Mock
    private ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    @Mock
    private DataTreeModification<MasterDatabaseBinding> dataTreeModification;
    @Mock
    private DataObjectModification<MasterDatabaseBinding> dataObjectModification;

    private MasterDatabaseBindingListenerImpl listener;

    public MasterDatabaseBindingListenerImplTest() {
        MASTER_DB_BINDING_VALUE = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(SGT_1)
                .setIpPrefix(IP_PREFIX)
                .build();
    }


    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(Matchers.<DataTreeIdentifier>any(),
                Matchers.<ClusteredDataTreeChangeListener>any())).thenReturn(listenerRegistration);
        listener = new MasterDatabaseBindingListenerImpl(dataBroker, sxpMapper, cachedDao, epPolicyTemplateDao,
                epForwardingTemplateDao);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        Mockito.when(dataTreeModification.getRootPath()).thenReturn(MASTER_DB_BINDING_TREE_PATH);
        Mockito.when(dataObjectModification.getDataAfter()).thenReturn(MASTER_DB_BINDING_VALUE);

        // prepare epPolicy template
        final EndpointPolicyTemplateBySgt epPolicyTemplate = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT_1)
                .build();
        Mockito.when(epPolicyTemplateDao.read(Matchers.<Sgt>any())).thenReturn(
                Futures.immediateFuture(Optional.of(epPolicyTemplate)));

        // prepare epForwarding template
        final IpPrefix ipPrefixSubnet = new IpPrefix(new Ipv4Prefix("1.2.3.0/24"));
        final EndpointForwardingTemplateBySubnet epForwardingTemplate = new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(ipPrefixSubnet)
                .build();
        Mockito.when(epForwardingTemplateDao.read(Matchers.<IpPrefix>any())).thenReturn(
                Futures.immediateFuture(Optional.of(epForwardingTemplate)));

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));

        final InOrder inOrder = Mockito.inOrder(cachedDao, epPolicyTemplateDao, epForwardingTemplateDao, sxpMapper);
        inOrder.verify(cachedDao).update(IP_PREFIX, MASTER_DB_BINDING_VALUE);
        inOrder.verify(epPolicyTemplateDao).read(SGT_1);
        inOrder.verify(epForwardingTemplateDao).read(IP_PREFIX);
        inOrder.verify(sxpMapper).processTemplatesAndSxpMasterDB(epPolicyTemplate, epForwardingTemplate, MASTER_DB_BINDING_VALUE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}