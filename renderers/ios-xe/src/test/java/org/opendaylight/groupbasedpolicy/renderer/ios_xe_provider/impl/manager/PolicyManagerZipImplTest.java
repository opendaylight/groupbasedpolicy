/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link PolicyManagerZipImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyManagerZipImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerZipImplTest.class);

    @Mock
    private PolicyManager delegate;
    @Mock
    private Configuration configBefore;
    @Mock
    private Configuration configAfter;

    private PolicyManagerZipImpl policyManager;

    @Before
    public void setUp() throws Exception {
        policyManager = new PolicyManagerZipImpl(delegate);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testSyncPolicy_add() throws Exception {
        policyManager.syncPolicy(null, configAfter);
        policyManager.close();
        Mockito.verify(delegate).syncPolicy(null, configAfter);
    }

    @Test
    public void testSyncPolicy_update() throws Exception {
        policyManager.syncPolicy(configBefore, configAfter);
        policyManager.close();
        Mockito.verify(delegate).syncPolicy(configBefore, configAfter);
    }

    @Test
    public void testSyncPolicy_remove() throws Exception {
        policyManager.syncPolicy(configBefore, null);
        policyManager.close();
        Mockito.verify(delegate).syncPolicy(configBefore, null);
    }

    @Test
    public void testSyncPolicy_compress() throws Exception {
        // SCENARIO:
        // - send fist config to manager
        // - wait till the first one enters the delegate and then send the rest of 3 configs
        // - make the 1. one stuck in delegate till the rest of 3 configs make it to manager's structure (force them to zip)
        // - unblock fist one (let the zipped form of last 3 configs to proceed)

        final Configuration configAfter2 = new ConfigurationBuilder().build();
        final CountDownLatch latchForFirst = new CountDownLatch(1);
        final CountDownLatch latchForOthers = new CountDownLatch(1);

        Mockito.when(delegate.syncPolicy(Matchers.<Configuration>any(), Matchers.<Configuration>any()))
                .thenAnswer(new Answer<ListenableFuture<Boolean>>() {
                    @Override
                    public ListenableFuture<Boolean> answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        LOG.info("unlocking next pile of configs in order to get them zipped");
                        latchForOthers.countDown();
                        latchForFirst.await(1, TimeUnit.SECONDS);
                        LOG.info("FINALLY UNLOCKING DELEGATE1");
                        return Futures.immediateFuture(true);
                    }
                })
                .thenReturn(Futures.immediateFuture(true));

        final List<ListenableFuture<Boolean>> allResults = new ArrayList<>();
        allResults.add(policyManager.syncPolicy(null, configBefore));

        latchForOthers.await(1, TimeUnit.SECONDS);
        allResults.add(policyManager.syncPolicy(configBefore, configAfter));
        allResults.add(policyManager.syncPolicy(configAfter, null));
        allResults.add(policyManager.syncPolicy(null, configAfter2));
        latchForFirst.countDown();

        Futures.allAsList(allResults).get(1, TimeUnit.SECONDS);
        LOG.info("all configs finished");
        policyManager.close();
        final InOrder inOrder = Mockito.inOrder(delegate);
        inOrder.verify(delegate).syncPolicy(null, configBefore);
        inOrder.verify(delegate).syncPolicy(configBefore, configAfter2);
    }
}