/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import com.google.common.collect.Lists;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.cache.DSTreeBasedCache;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache.EpPolicyTemplateCacheKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Test for {@link EpPolicyTemplateBySgtListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EpPolicyTemplateBySgtListenerImplTest {

    private static final Sgt SGT1 = new Sgt(1);
    private static final TenantId TENANT1 = new TenantId("tenant1");
    private static final TenantId TENANT2 = new TenantId("tenant2");
    @Mock
    private DataBroker dataBroker;
    @Mock
    private DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> cache;
    @Mock
    private ListenerRegistration<EpPolicyTemplateBySgtListenerImpl> listenerRegistration;
    @Mock
    private DataTreeModification<EndpointPolicyTemplateBySgt> dataTreeModification;
    @Mock
    private DataObjectModification<EndpointPolicyTemplateBySgt> rootNode;

    private final EndpointPolicyTemplateBySgt template1;
    private final EndpointPolicyTemplateBySgt template2;

    private EpPolicyTemplateBySgtListenerImpl listener;

    public EpPolicyTemplateBySgtListenerImplTest() {
        this.template1 = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT1)
                .setTenant(TENANT1)
                .setEndpointGroups(Lists.newArrayList(new EndpointGroupId("epg1"), new EndpointGroupId("epg2")))
                .setConditions(Lists.newArrayList(new ConditionName("condition1"), new ConditionName("condition2")))
                .build();

        this.template2 = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT1)
                .setTenant(TENANT2)
                .setEndpointGroups(Lists.newArrayList(new EndpointGroupId("epg3"), new EndpointGroupId("epg4")))
                .setConditions(Lists.newArrayList(new ConditionName("condition2"), new ConditionName("condition3")))
                .build();
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<EndpointPolicyTemplateBySgt>>any(),
                Matchers.<EpPolicyTemplateBySgtListenerImpl>any()))
                .thenReturn(listenerRegistration);
        listener = new EpPolicyTemplateBySgtListenerImpl(dataBroker, cache);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(cache);
    }

    @Test
    public void testOnDataTreeChanged_add() throws Exception {
        Mockito.when(rootNode.getDataAfter()).thenReturn(template1);
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(cache).add(template1);
    }

    @Test
    public void testOnDataTreeChanged_remove() throws Exception {
        Mockito.when(rootNode.getDataBefore()).thenReturn(template1);
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(cache).invalidate(template1);
    }

    @Test
    public void testOnDataTreeChanged_update() throws Exception {
        Mockito.when(rootNode.getDataBefore()).thenReturn(template1);
        Mockito.when(rootNode.getDataAfter()).thenReturn(template2);
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(cache).update(template1, template2);
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
        listener.close();
        Mockito.verify(listenerRegistration, Mockito.times(2)).close();
    }
}