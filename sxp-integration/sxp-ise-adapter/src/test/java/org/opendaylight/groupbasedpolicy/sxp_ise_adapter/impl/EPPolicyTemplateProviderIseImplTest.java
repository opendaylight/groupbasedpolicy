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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.IseReplyUtil;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.RestClientFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.connection.config.HeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Test for {@link EPPolicyTemplateProviderIseImpl}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IseReplyUtil.class, RestClientFactory.class})
public class EPPolicyTemplateProviderIseImplTest {

    private static final Sgt SGT = new Sgt(42);
    private static final Sgt SGT_LOW = new Sgt(1);
    private static final Sgt SGT_HI = new Sgt(100);
    private static final TenantId TENANT_ID = new TenantId("tenant-01");

    @Mock
    private XPath xpathWalker;
    @Mock
    private Client iseClient;
    @Mock
    private WebResource.Builder wrBuilder;

    private EPPolicyTemplateProviderIseImpl templateProvider;

    @Before
    public void setUp() throws Exception {
        templateProvider = new EPPolicyTemplateProviderIseImpl();
    }

    @Test
    public void testProvideTemplate_noConfig() throws Exception {
        final Optional<EndpointPolicyTemplateBySgt> endpointPolicyTemplateBySgt = templateProvider.provideTemplate(SGT);
        Assert.assertFalse(endpointPolicyTemplateBySgt.isPresent());
    }

    @Test
    public void testProvideTemplate_config() throws Exception {
        final ConnectionConfig connectionConfig = new ConnectionConfigBuilder()
                .setIseRestUrl(new Uri("http://example.org"))
                .setConnectionTimeout(10)
                .setReadTimeout(10)
                .setHeader(Collections.singletonList(new HeaderBuilder()
                        .setName("headerName")
                        .setValue("headerValue")
                        .build()))
                .build();
        final IseSourceConfig sourceConfig = new IseSourceConfigBuilder()
                .setTenant(TENANT_ID)
                .setConnectionConfig(connectionConfig)
                .setSgtRangeMin(SGT_LOW)
                .setSgtRangeMax(SGT_HI)
                .build();

        final String rawResponse = "";
        stub(method(IseReplyUtil.class, "deliverResponse")).toReturn(rawResponse);
        stub(method(IseReplyUtil.class, "setupXpath")).toReturn(xpathWalker);
        stub(method(RestClientFactory.class, "createIseClient", ConnectionConfig.class)).toReturn(iseClient);
        stub(method(RestClientFactory.class, "createRequestBuilder", WebResource.class, List.class, String.class)).toReturn(wrBuilder);


        final String epgName = "name-for-sgt42";
        final Node sgtNameNode = Mockito.mock(Node.class);
        Mockito.when(sgtNameNode.getNodeValue()).thenReturn(epgName);
        Mockito.when(xpathWalker.evaluate(Matchers.same(IseReplyUtil.EXPRESSION_SGT_NAME_ATTR),
                Matchers.<InputSource>any(), Matchers.same(XPathConstants.NODE)))
                .thenReturn(sgtNameNode);

        templateProvider.assignIseSourceConfig(sourceConfig);
        final Optional<EndpointPolicyTemplateBySgt> templateWrap = templateProvider.provideTemplate(SGT);

        Assert.assertTrue(templateWrap.isPresent());
        final EndpointPolicyTemplateBySgt template = templateWrap.get();
        Assert.assertEquals(SGT, template.getSgt());
        Assert.assertNull(template.getConditions());
        Assert.assertEquals(TENANT_ID, template.getTenant());
        Assert.assertEquals(1, template.getEndpointGroups().size());
        Assert.assertEquals(epgName, template.getEndpointGroups().get(0).getValue());
        Assert.assertEquals(TemplateGenerated.class, template.getOrigin());
    }
}