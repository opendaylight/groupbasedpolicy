/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.util.Map;
import javax.net.ssl.SSLSession;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfigBuilder;

/**
 * Purpose: cover {@link RestClientFactory}
 */
@RunWith(MockitoJUnitRunner.class)
public class RestClientFactoryTest {

    @Mock
    private SSLSession sslSession;

    @Test
    public void testCreateIseClient() throws Exception {
        ConnectionConfig connectionConfig = new ConnectionConfigBuilder()
                .setConnectionTimeout(1)
                .setReadTimeout(2)
                .build();

        final Client iseClient = RestClientFactory.createIseClient(connectionConfig);
        final Map<String, Object> properties = iseClient.getProperties();
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals(1, properties.get("com.sun.jersey.client.property.connectTimeout"));
        Assert.assertEquals(2, properties.get("com.sun.jersey.client.property.readTimeout"));

        Assert.assertTrue(properties.get(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES) instanceof HTTPSProperties);
        final HTTPSProperties httpsProperties = (HTTPSProperties) properties.get(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES);
        Assert.assertTrue(httpsProperties.getHostnameVerifier().verify("xxx", sslSession));
    }
}
