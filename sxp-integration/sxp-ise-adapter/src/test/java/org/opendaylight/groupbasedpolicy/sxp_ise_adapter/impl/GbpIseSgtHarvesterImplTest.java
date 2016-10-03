/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.RestClientFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.connection.config.HeaderBuilder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link GbpIseSgtHarvesterImpl}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RestClientFactory.class})
public class GbpIseSgtHarvesterImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(GbpIseSgtHarvesterImplTest.class);

    public static final TenantId TENANT_ID = new TenantId("unit-tenant-id-1");
    public static final Uri ISE_REST_URL = new Uri("https://example.org:9060");
    public final String iseReplyAllSgts;
    public final String iseReplySgtDetail;

    @Mock
    private SgtInfoProcessor processor;
    @Mock
    private Client client;
    @Mock
    private WebResource webResource;
    @Mock
    private WebResource.Builder builder;
    @Mock
    private ClientResponse response;

    private IseSourceConfig config;
    private IseContext iseContext;

    private GbpIseSgtHarvesterImpl harvester;

    public GbpIseSgtHarvesterImplTest() throws IOException {
        iseReplyAllSgts = IseResourceTestHelper.readLocalResource("./rawIse-allSgts1.xml");
        iseReplySgtDetail = IseResourceTestHelper.readLocalResource("./rawIse-sgtDetail.xml");
    }

    @Before
    public void setUp() throws Exception {
        config = new IseSourceConfigBuilder()
                .setTenant(TENANT_ID)
                .setConnectionConfig(new ConnectionConfigBuilder()
                        .setConnectionTimeout(10)
                        .setReadTimeout(20)
                        .setHeader(Collections.singletonList(new HeaderBuilder()
                                .setName("hName")
                                .setValue("hValue")
                                .build()))
                        .setIseRestUrl(ISE_REST_URL)
                        .build())
                .build();

        iseContext = new IseContext(config);
        harvester = new GbpIseSgtHarvesterImpl(processor);
    }

    @Test
    public void testHarvest() throws Exception {
        Mockito.when(response.getEntity(String.class)).thenReturn(iseReplyAllSgts, iseReplySgtDetail);
        Mockito.when(builder.get(Matchers.<Class<ClientResponse>>any())).thenReturn(response);
        Mockito.when(webResource.getRequestBuilder()).thenReturn(builder);
        Mockito.when(webResource.path(Matchers.anyString())).thenReturn(webResource);
        Mockito.when(client.resource(Matchers.<String>any())).thenReturn(webResource);
        stub(method(RestClientFactory.class, "createIseClient")).toReturn(client);

        Mockito.when(processor.processSgtInfo(Matchers.eq(TENANT_ID), Matchers.<List<SgtInfo>>any())).thenReturn(
                Futures.immediateCheckedFuture(null));

        final ListenableFuture<Collection<SgtInfo>> harvestResult = harvester.harvestAll(iseContext);
        final Collection<SgtInfo> addedSgts = harvestResult.get(2, TimeUnit.SECONDS);

        final InOrder inOrder = Mockito.inOrder(client, webResource, builder);
        inOrder.verify(client).resource(ISE_REST_URL.getValue());
        // all sgts
        inOrder.verify(webResource).path("/ers/config/sgt");
        inOrder.verify(webResource).getRequestBuilder();
        inOrder.verify(builder).header(Matchers.anyString(),Matchers.anyString());
        inOrder.verify(builder).get(ClientResponse.class);
        // sgt detail
        inOrder.verify(webResource).path("/ers/config/sgt/abc123");
        inOrder.verify(webResource).getRequestBuilder();
        inOrder.verify(builder).header(Matchers.anyString(),Matchers.anyString());
        inOrder.verify(builder).get(ClientResponse.class);
        inOrder.verifyNoMoreInteractions();

        Assert.assertEquals(1, addedSgts.size());
    }
}