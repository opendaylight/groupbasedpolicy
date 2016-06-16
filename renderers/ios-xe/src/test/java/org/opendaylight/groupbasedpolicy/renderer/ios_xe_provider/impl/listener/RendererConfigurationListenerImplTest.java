/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

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
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Test for {@link RendererConfigurationListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RendererConfigurationListenerImplTest {

    private static final RendererName RENDERER_NAME = new RendererName("renderer1");
    private final RendererPolicy policy1;
    private final RendererPolicy policy2;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private PolicyManagerImpl policyManager;
    @Mock
    private ListenerRegistration<RendererConfigurationListenerImpl> listenerRegistration;
    @Mock
    private DataTreeModification<RendererPolicy> dataTreeModification;
    @Mock
    private DataObjectModification<RendererPolicy> rootNode;

    private RendererConfigurationListenerImpl listener;

    public RendererConfigurationListenerImplTest() {
        policy1 = createRendererPolicy(0);
        policy2 = createRendererPolicy(1);
    }

    private RendererPolicy createRendererPolicy(final int order) {
        return new RendererPolicyBuilder()
                .setConfiguration(new ConfigurationBuilder().setRuleGroups(
                        new RuleGroupsBuilder()
                                .setRuleGroup(Collections.singletonList(new RuleGroupBuilder()
                                        .setOrder(order)
                                        .build()))
                                .build())
                        .build())
                .setVersion((long) order)
                .build();
    }

    private RendererPolicy createVersion(final int order) {
        return new RendererPolicyBuilder()
                .setVersion((long) order)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<RendererPolicy>>any(),
                Matchers.<RendererConfigurationListenerImpl>any()))
                .thenReturn(listenerRegistration);
        listener = new RendererConfigurationListenerImpl(dataBroker, RENDERER_NAME, policyManager);
        Mockito.verify(dataBroker).registerDataTreeChangeListener(Matchers.<DataTreeIdentifier<RendererPolicy>>any(),
                Matchers.<RendererConfigurationListenerImpl>any());
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(listenerRegistration, dataBroker, policyManager);
    }

    @Test
    public void testOnDataTreeChanged_add() throws Exception {
        Mockito.when(rootNode.getDataBefore()).thenReturn(null);
        Mockito.when(rootNode.getDataAfter()).thenReturn(policy1);
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(policyManager).syncPolicy(policy1.getConfiguration(), null, 0);
    }

    @Test
    public void testOnDataTreeChanged_update() throws Exception {
        Mockito.when(rootNode.getDataBefore()).thenReturn(policy1);
        Mockito.when(rootNode.getDataAfter()).thenReturn(policy2);
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(policyManager).syncPolicy(policy2.getConfiguration(), policy1.getConfiguration(), 1);
    }

    @Test
    public void testOnDataTreeChanged_remove() throws Exception {
        Mockito.when(rootNode.getDataBefore()).thenReturn(policy2);
        Mockito.when(rootNode.getDataAfter()).thenReturn(createVersion(1));
        Mockito.when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(policyManager).syncPolicy(null, policy2.getConfiguration(), 1);
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