/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi.SxpEpProviderProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Purpose: cover {@link GbpIseAdapterProvider}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GbpIseAdapterProvider.class})
public class GbpIseAdapterProviderTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private BindingAwareBroker broker;
    @Mock
    private BindingAwareBroker.ProviderContext providerContext;
    @Mock
    private ListenerRegistration<GbpIseConfigListener> registration;
    @Mock
    private SgtToEpgGeneratorImpl generator1;
    @Mock
    private SgtToEPTemplateGeneratorImpl generator2;
    @Mock
    private GbpIseSgtHarvesterImpl harvester;
    @Mock
    private GbpIseConfigListenerImpl listener;
    @Mock
    private SxpEpProviderProvider sxpEpProvider;
    @Mock
    private EPPolicyTemplateProviderRegistry templateProviderRegistry;
    @Mock
    private EPPolicyTemplateProviderIseImpl templateProvider;

    private GbpIseAdapterProvider provider;

    @Before
    public void setUp() throws Exception {
        Mockito.when(sxpEpProvider.getEPPolicyTemplateProviderRegistry())
                .thenReturn(templateProviderRegistry);
        provider = new GbpIseAdapterProvider(dataBroker, broker, sxpEpProvider);
        Mockito.verify(broker).registerProvider(provider);
    }

    @Test
    public void testOnSessionInitiated() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<IseSourceConfig>>any(),
                Matchers.<GbpIseConfigListener>any())).thenReturn(registration);

        // prepare expectation for objects in onSessionInitiated (powerMock requirement for later checking)
        whenNew(SgtToEpgGeneratorImpl.class).withAnyArguments().thenReturn(generator1);
        whenNew(SgtToEPTemplateGeneratorImpl.class).withAnyArguments().thenReturn(generator2);
        whenNew(GbpIseSgtHarvesterImpl.class).withArguments(generator1, generator2).thenReturn(harvester);
        whenNew(GbpIseConfigListenerImpl.class).withAnyArguments().thenReturn(listener);
        whenNew(EPPolicyTemplateProviderIseImpl.class).withNoArguments().thenReturn(templateProvider);
        provider.onSessionInitiated(providerContext);

        Mockito.verify(templateProviderRegistry).registerTemplateProvider(templateProvider);
        // check if all expected object got constructed and wired
        verifyNew(SgtToEpgGeneratorImpl.class).withArguments(dataBroker);
        verifyNew(SgtToEPTemplateGeneratorImpl.class).withArguments(dataBroker);
        verifyNew(GbpIseSgtHarvesterImpl.class).withArguments(generator1, generator2);
        verifyNew(GbpIseConfigListenerImpl.class).withArguments(dataBroker, harvester, templateProvider);

        // close provider check
        provider.close();
        Mockito.verify(registration).close();
    }
}
