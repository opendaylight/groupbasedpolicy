/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.sxp_ise_adapter;

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
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi.SxpEpProviderProvider;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.GbpIseAdapterProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for {@link SxpIseAdapterProviderInstance}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpIseAdapterProviderInstance.class})
public class SxpIseAdapterProviderInstanceTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private BindingAwareBroker bindingAwareBroker;
    @Mock
    private ClusterSingletonServiceProvider clusterSingletonService;
    @Mock
    private SxpEpProviderProvider sxpEpProvider;
    @Mock
    private GbpIseAdapterProvider gbpIseAdapterProvider;
    @Mock
    private ClusterSingletonServiceRegistration clusterSingletonRegistration;

    private SxpIseAdapterProviderInstance instance;

    @Before
    public void setUp() throws Exception {
        Mockito.when(clusterSingletonService.registerClusterSingletonService(Matchers.<ClusterSingletonService>any()))
                .thenReturn(clusterSingletonRegistration);
        whenNew(GbpIseAdapterProvider.class).withArguments(dataBroker, bindingAwareBroker, sxpEpProvider).thenReturn(gbpIseAdapterProvider);

        instance = new SxpIseAdapterProviderInstance(dataBroker, bindingAwareBroker, clusterSingletonService, sxpEpProvider);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(dataBroker, bindingAwareBroker, clusterSingletonService, sxpEpProvider,
                gbpIseAdapterProvider, clusterSingletonRegistration);
    }

    @Test
    public void testInitialize() throws Exception {
        instance.initialize();
        Mockito.verify(clusterSingletonService).registerClusterSingletonService(instance);
    }

    @Test
    public void testInstantiateServiceInstance() throws Exception {
        instance.instantiateServiceInstance();

        verifyNew(GbpIseAdapterProvider.class).withArguments(dataBroker, bindingAwareBroker, sxpEpProvider);
    }

    @Test
    public void testCloseServiceInstance() throws Exception {
        instance.instantiateServiceInstance();
        final ListenableFuture<Void> future = instance.closeServiceInstance();

        Mockito.verify(gbpIseAdapterProvider).close();
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
}