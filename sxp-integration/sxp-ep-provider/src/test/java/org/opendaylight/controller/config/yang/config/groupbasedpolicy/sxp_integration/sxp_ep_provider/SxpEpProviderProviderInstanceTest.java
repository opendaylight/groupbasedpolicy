/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.sxp_ep_provider;

import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPToSgtMapper;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SxpEpProviderProviderImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi.SxpEpProviderProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.rev160722.SgtGeneratorConfig;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for {@link SxpEpProviderProviderInstance}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpEpProviderProviderInstance.class})
public class SxpEpProviderProviderInstanceTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private BindingAwareBroker bindingAwareBroker;
    @Mock
    private ClusterSingletonServiceProvider clusterSingletonService;
    @Mock
    private BaseEndpointService endpointService;
    @Mock
    private DomainSpecificRegistry domainSpecificRegistry;
    @Mock
    private SgtGeneratorConfig sgtGeneratorConfig;
    @Mock
    private SxpEpProviderProvider sxpEpProvider;
    @Mock
    private ClusterSingletonServiceRegistration clusterSingletonRegistration;
    @Mock
    private SxpEpProviderProviderImpl sxpEpProviderProvider;
    @Mock
    private EPToSgtMapper epToSgtMapper;
    @Mock
    private EPPolicyTemplateProviderRegistry epPolicyTemplateProviderRegistry;

    private SxpEpProviderProviderInstance instance;

    @Before
    public void setUp() throws Exception {
        Mockito.when(clusterSingletonService.registerClusterSingletonService(Matchers.<ClusterSingletonService>any()))
                .thenReturn(clusterSingletonRegistration);
        Mockito.when(sxpEpProviderProvider.getEPToSgtMapper()).thenReturn(epToSgtMapper);
        Mockito.when(sxpEpProviderProvider.getEPPolicyTemplateProviderRegistry()).thenReturn(epPolicyTemplateProviderRegistry);

        whenNew(SxpEpProviderProviderImpl.class)
                .withArguments(dataBroker, endpointService, domainSpecificRegistry, sgtGeneratorConfig)
                .thenReturn(sxpEpProviderProvider);

        instance = new SxpEpProviderProviderInstance(dataBroker, endpointService, domainSpecificRegistry,
                clusterSingletonService, sgtGeneratorConfig);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(dataBroker, bindingAwareBroker, clusterSingletonService, sxpEpProvider,
                clusterSingletonRegistration, sgtGeneratorConfig, endpointService, domainSpecificRegistry);
    }

    @Test
    public void testInitialize() throws Exception {
        instance.initialize();
        Mockito.verify(clusterSingletonService).registerClusterSingletonService(instance);
    }

    @Test
    public void testInstantiateServiceInstance() throws Exception {
        instance.instantiateServiceInstance();

        verifyNew(SxpEpProviderProviderImpl.class).withArguments(dataBroker, endpointService, domainSpecificRegistry,
                sgtGeneratorConfig);
    }

    @Test
    public void testCloseServiceInstance() throws Exception {
        instance.instantiateServiceInstance();
        final ListenableFuture<Void> future = instance.closeServiceInstance();

        Mockito.verify(sxpEpProviderProvider).close();
        Assert.assertTrue(future.isDone());
        Assert.assertNull(future.get());
    }

    @Test
    public void testCloseServiceInstance_null() throws Exception {
        final ListenableFuture<Void> future = instance.closeServiceInstance();

        Assert.assertTrue(future.isDone());
        Assert.assertNull(future.get());
    }

    @Test
    public void testClose() throws Exception {
        instance.initialize();
        Mockito.verify(clusterSingletonService).registerClusterSingletonService(instance);

        instance.close();
        Mockito.verify(clusterSingletonRegistration).close();
    }

    @Test
    public void testClose_null() throws Exception {
        instance.close();
    }

    @Test
    public void testGetIdentifier() throws Exception {
        final ServiceGroupIdentifier identifier = instance.getIdentifier();
        Assert.assertEquals("gbp-service-group-identifier", identifier.getValue());
    }

    @Test
    public void testGetEPToSgtMapper() throws Exception {
        instance.instantiateServiceInstance();
        Assert.assertEquals(epToSgtMapper, instance.getEPToSgtMapper());
    }

    @Test
    public void testGetEPPolicyTemplateProviderRegistry() throws Exception {
        instance.instantiateServiceInstance();
        Assert.assertEquals(epPolicyTemplateProviderRegistry, instance.getEPPolicyTemplateProviderRegistry());
    }
}