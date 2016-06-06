/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.ReadableAsyncByKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Test for {@link EPPolicyTemplateListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPPolicyTemplateListenerImplTest {

    private static final Sgt SGT_1 = new Sgt(1);
    private static final KeyedInstanceIdentifier<EndpointPolicyTemplateBySgt, EndpointPolicyTemplateBySgtKey> EP_PL_TEMPLATE_PATH =
            EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH
                    .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(SGT_1));
    private static final DataTreeIdentifier<EndpointPolicyTemplateBySgt> TEMPLATE_TREE_PATH =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, EP_PL_TEMPLATE_PATH);
    private static final IpPrefix IP_PREFIX_1 = new IpPrefix(new Ipv4Prefix("1.2.3.4/32"));
    private static final IpPrefix IP_PREFIX_2 = new IpPrefix(new Ipv4Prefix("1.2.3.5/32"));
    private final EndpointPolicyTemplateBySgt EP_PL_TEMPLATE_VALUE;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SxpMapperReactor sxpMapper;
    @Mock
    private SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> simpleCachedDao;
    @Mock
    private DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;
    @Mock
    private ReadableAsyncByKey<Sgt, MasterDatabaseBinding> masterDBDao;
    @Mock
    private ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    @Mock
    private DataTreeModification<EndpointPolicyTemplateBySgt> dataTreeModification;
    @Mock
    private DataObjectModification<EndpointPolicyTemplateBySgt> dataObjectModification;

    private EPPolicyTemplateListenerImpl listener;

    public EPPolicyTemplateListenerImplTest() {
        EP_PL_TEMPLATE_VALUE = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT_1)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(Matchers.<DataTreeIdentifier>any(),
                Matchers.<DataTreeChangeListener>any())).thenReturn(listenerRegistration);
        listener = new EPPolicyTemplateListenerImpl(dataBroker, sxpMapper, simpleCachedDao, masterDBDao, epForwardingTemplateDao);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        Mockito.when(dataTreeModification.getRootPath()).thenReturn(TEMPLATE_TREE_PATH);
        Mockito.when(dataObjectModification.getDataAfter()).thenReturn(EP_PL_TEMPLATE_VALUE);

        final MasterDatabaseBinding masterDBBinding1 = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(SGT_1)
                .setIpPrefix(IP_PREFIX_1)
                .build();
        final MasterDatabaseBinding masterDBBinding2 = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(SGT_1)
                .setIpPrefix(IP_PREFIX_2)
                .build();

        final EndpointForwardingTemplateBySubnet epForwardingTemplate1 = new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(IP_PREFIX_1)
                .build();
        final EndpointForwardingTemplateBySubnet epForwardingTemplate2 = new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(IP_PREFIX_2)
                .build();

        Mockito.when(masterDBDao.readBy(Matchers.<Sgt>any())).thenReturn(
                Futures.immediateFuture(Lists.newArrayList(masterDBBinding1, masterDBBinding2)));
        Mockito.when(epForwardingTemplateDao.read(Matchers.<IpPrefix>any())).thenReturn(
                Futures.immediateFuture(Optional.of(epForwardingTemplate1)),
                Futures.immediateFuture(Optional.of(epForwardingTemplate2)));
        Mockito.when(sxpMapper.processTemplatesAndSxpMasterDB(Matchers.<EndpointPolicyTemplateBySgt>any(),
                Matchers.<EndpointForwardingTemplateBySubnet>any(), Matchers.<MasterDatabaseBinding>any())).thenReturn(
                RpcResultBuilder.success((Void) null).buildFuture());

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));

        final InOrder inOrder = Mockito.inOrder(masterDBDao, simpleCachedDao, epForwardingTemplateDao, sxpMapper);
        inOrder.verify(simpleCachedDao).update(SGT_1, EP_PL_TEMPLATE_VALUE);
        inOrder.verify(masterDBDao).readBy(SGT_1);
        inOrder.verify(epForwardingTemplateDao).read(IP_PREFIX_1);
        inOrder.verify(epForwardingTemplateDao).read(IP_PREFIX_2);
        inOrder.verify(sxpMapper).processTemplatesAndSxpMasterDB(EP_PL_TEMPLATE_VALUE, epForwardingTemplate1, masterDBBinding1);
        inOrder.verify(sxpMapper).processTemplatesAndSxpMasterDB(EP_PL_TEMPLATE_VALUE, epForwardingTemplate2, masterDBBinding2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}