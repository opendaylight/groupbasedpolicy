/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Test for {@link EPPolicyTemplateDaoFacadeImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPPolicyTemplateDaoFacadeImplTest {

    private static final Sgt SGT = new Sgt(42);
    private static final TenantId TENANT_ID = new TenantId("tenant-01");
    private static final EndpointGroupId EPG_ID = new EndpointGroupId("epg-01");
    private static final TransactionCommitFailedException TX_EXCEPTION = new TransactionCommitFailedException("unit-txSubmit-error");

    @Rule
    public ExpectedException thrownRule = ExpectedException.none();

    @Mock
    private EPPolicyTemplateDaoImpl delegateDao;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private EPPolicyTemplateProvider provider;
    @Mock
    private WriteTransaction wTx;


    private EndpointPolicyTemplateBySgtBuilder templateBld;
    private EPPolicyTemplateDaoFacadeImpl facade;

    @Before
    public void setUp() throws Exception {
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture((Void) null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);
        templateBld = new EndpointPolicyTemplateBySgtBuilder()
                .setTenant(TENANT_ID)
                .setSgt(SGT)
                .setEndpointGroups(Collections.singletonList(EPG_ID));

        facade = new EPPolicyTemplateDaoFacadeImpl(dataBroker, delegateDao);
    }

    @Test
    public void testRead_trivial() throws Exception {
        final EndpointPolicyTemplateBySgt template = templateBld.build();
        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.of(template)));

        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> actual = facade.read(SGT);
        Assert.assertTrue(actual.isDone());
        Assert.assertTrue(actual.get().isPresent());
        Assert.assertEquals(template, actual.get().get());
    }

    @Test
    public void testRead_success() throws Exception {
        final EndpointPolicyTemplateBySgt template = templateBld
                .setOrigin(TemplateGenerated.class)
                .build();

        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.absent()));
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(java.util.Optional.of(template));
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        facade.setTemplateProvider(provider);


        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> actual = facade.read(SGT);
        Assert.assertTrue(actual.isDone());
        Assert.assertTrue(actual.get().isPresent());
        Assert.assertEquals(template, actual.get().get());
    }

    @Test
    public void testRead_failNoProvider() throws Exception {
        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.absent()));

        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> actual = facade.read(SGT);
        Assert.assertTrue(actual.isDone());
        Assert.assertFalse(actual.get().isPresent());
    }

    @Test
    public void testRead_failProviderMiss() throws Exception {
        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.absent()));
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(java.util.Optional.empty());
        facade.setTemplateProvider(provider);


        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> actual = facade.read(SGT);
        Assert.assertTrue(actual.isDone());
        Assert.assertFalse(actual.get().isPresent());
    }

    @Test
    public void testRead_failProviderStoreError() throws Exception {
        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.absent()));
        final EndpointPolicyTemplateBySgt template = templateBld
                .setOrigin(TemplateGenerated.class)
                .build();

        Mockito.when(delegateDao.read(SGT)).thenReturn(Futures.immediateFuture(Optional.absent()));
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(java.util.Optional.of(template));
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(TX_EXCEPTION));
        facade.setTemplateProvider(provider);

        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> actual = facade.read(SGT);
        Assert.assertTrue(actual.isDone());

        // prepare exception rule
        thrownRule.expect(ExecutionException.class);
        thrownRule.expectCause(new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(final Object item) {
                return TX_EXCEPTION == item;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("TransactionCommitFailedException");
            }
        });
        actual.get();
    }
}