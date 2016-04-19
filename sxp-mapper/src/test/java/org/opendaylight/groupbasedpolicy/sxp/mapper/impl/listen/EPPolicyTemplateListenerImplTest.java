/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

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
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.ReadableByKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

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
    private final EndpointPolicyTemplateBySgt EP_PL_TEMPLATE_VALUE;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SxpMapperReactor sxpMapper;
    @Mock
    private SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> simpleCachedDao;
    @Mock
    private ReadableByKey<Sgt, MasterDatabaseBinding> masterDBDao;
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
        listener = new EPPolicyTemplateListenerImpl(dataBroker, sxpMapper, simpleCachedDao, masterDBDao);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        Mockito.when(dataTreeModification.getRootPath()).thenReturn(TEMPLATE_TREE_PATH);
        Mockito.when(dataObjectModification.getDataAfter()).thenReturn(EP_PL_TEMPLATE_VALUE);

        final MasterDatabaseBinding masterDBBinding = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(SGT_1)
                .build();
        Mockito.when(masterDBDao.readBy(Matchers.<Sgt>any())).thenReturn(
                Futures.immediateFuture(Lists.newArrayList(masterDBBinding)));

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));

        final InOrder inOrder = Mockito.inOrder(masterDBDao, simpleCachedDao, sxpMapper);
        inOrder.verify(simpleCachedDao).update(SGT_1, EP_PL_TEMPLATE_VALUE);
        inOrder.verify(masterDBDao).readBy(SGT_1);
        inOrder.verify(sxpMapper).processPolicyAndSxpMasterDB(EP_PL_TEMPLATE_VALUE, masterDBBinding);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}