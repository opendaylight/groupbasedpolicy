/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
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
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SgtGeneratorImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
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
    private SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> delegateCachedDao;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private EPPolicyTemplateProvider provider;
    @Mock
    private SgtGeneratorImpl sgtGeneratorImpl;
    @Mock
    private WriteTransaction wTx;


    private EndpointPolicyTemplateBySgtBuilder templateBld;
    private EPPolicyTemplateDaoFacadeImpl facade;
    private EndpointPolicyTemplateBySgt first;

    @Before
    public void setUp() throws Exception {
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture((Void) null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);
        templateBld = new EndpointPolicyTemplateBySgtBuilder()
                .setTenant(TENANT_ID)
                .setSgt(SGT)
                .setEndpointGroups(Collections.singletonList(EPG_ID));

        facade = new EPPolicyTemplateDaoFacadeImpl(dataBroker, delegateDao, delegateCachedDao, sgtGeneratorImpl);
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
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(Futures.immediateFuture(java.util.Optional.of(template)));
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
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(Futures.immediateFuture(java.util.Optional.empty()));
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
        Mockito.when(provider.provideTemplate(SGT)).thenReturn(Futures.immediateFailedCheckedFuture(TX_EXCEPTION));
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

    @Test
    public void testReadBy() throws Exception {
        Mockito.when(sgtGeneratorImpl.generateNextSgt(delegateCachedDao))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(new Sgt(42)));

        final EpPolicyTemplateValueKey lookupKey = new EpPolicyTemplateValueKey(TENANT_ID,
                Collections.singletonList(EPG_ID), Collections.emptyList());

        final ListenableFuture<Collection<EndpointPolicyTemplateBySgt>> templateFail = facade.readBy(lookupKey);
        Assert.assertTrue(templateFail.isDone());
        Assert.assertTrue(templateFail.get().isEmpty());

        final ListenableFuture<Collection<EndpointPolicyTemplateBySgt>> templateSuccess = facade.readBy(lookupKey);
        Assert.assertTrue(templateSuccess.isDone());
        final Collection<EndpointPolicyTemplateBySgt> templateBag = templateSuccess.get();
        Assert.assertFalse(templateBag.isEmpty());
        first = Iterables.getFirst(templateBag, null);
        Assert.assertNotNull(templateBag);
        Assert.assertEquals(TemplateGenerated.class, first.getOrigin());
        Assert.assertEquals(Collections.singletonList(EPG_ID), Iterables.getFirst(templateBag, null).getEndpointGroups());
        Assert.assertTrue(Iterables.getFirst(templateBag, null).getConditions().isEmpty());
        Assert.assertEquals(42, Iterables.getFirst(templateBag, null).getSgt().getValue().intValue());
    }
}