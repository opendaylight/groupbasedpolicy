/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.TemplateProviderDistributionTarget;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Test for {@link EPPolicyTemplateProviderRegistryImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPPolicyTemplateProviderRegistryImplTest {

    @Mock
    private EPPolicyTemplateProvider templateProvider;
    @Mock
    private TemplateProviderDistributionTarget<EPPolicyTemplateProvider> target1;
    @Mock
    private TemplateProviderDistributionTarget<EPPolicyTemplateProvider> target2;
    @Mock
    private TemplateProviderDistributionTarget<EPPolicyTemplateProvider> target3;

    private EPPolicyTemplateProviderRegistryImpl registry;

    @Before
    public void setUp() throws Exception {
        registry = new EPPolicyTemplateProviderRegistryImpl();
    }

    @Test
    public void testRegisterTemplateProvider() throws Exception {
        registry.addDistributionTarget(target1);
        final ObjectRegistration<EPPolicyTemplateProvider> registration = registry.registerTemplateProvider(templateProvider);
        Mockito.verify(target1).setTemplateProvider(templateProvider);
        registry.addDistributionTarget(target2);
        Mockito.verify(target2).setTemplateProvider(templateProvider);

        registration.close();
        Mockito.verify(target1).setTemplateProvider(null);
        Mockito.verify(target2).setTemplateProvider(null);
        registry.addDistributionTarget(target3);

        Mockito.verifyNoMoreInteractions(target1, target2, target3);
    }

    @Test
    public void testAddDistributionTarget() throws Exception {
        registry.addDistributionTarget(target1);
        Mockito.verify(target1, Mockito.never()).setTemplateProvider(Matchers.<EPPolicyTemplateProvider>any());

        registry.registerTemplateProvider(templateProvider);
        Mockito.verify(target1).setTemplateProvider(templateProvider);

        registry.addDistributionTarget(target2);
        Mockito.verify(target2).setTemplateProvider(templateProvider);
    }

    @Test
    public void testClose() throws Exception {
        registry.addDistributionTarget(target1);
        final ObjectRegistration<EPPolicyTemplateProvider> registration = registry.registerTemplateProvider(templateProvider);
        registry.addDistributionTarget(target2);
        Mockito.verify(target1).setTemplateProvider(templateProvider);
        Mockito.verify(target2).setTemplateProvider(templateProvider);

        registry.close();
        Mockito.verify(target1).setTemplateProvider(null);
        Mockito.verify(target2).setTemplateProvider(null);

        Mockito.verifyNoMoreInteractions(target1, target2);
    }
}