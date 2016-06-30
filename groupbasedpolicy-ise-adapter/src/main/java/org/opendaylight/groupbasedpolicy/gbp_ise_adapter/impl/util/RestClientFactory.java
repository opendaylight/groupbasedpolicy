/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.gbp_ise_adapter.impl.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.util.TrustManagerUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.ise.adapter.model.rev160630.gbp.ise.adapter.ise.harvest.config.ConnectionConfig;

/**
 * Purpose: setup ise-ready jersey {@link Client}
 */
public class RestClientFactory {

    private RestClientFactory() {
        throw new IllegalAccessError("factory class - no instances supported");
    }

    /**
     * @param connectionConfig config provided
     * @return initiated jersey client - ready to talk to ise
     *
     * @throws GeneralSecurityException in case when insecure certificate hack fails
     */
    public static Client createIseClient(final ConnectionConfig connectionConfig) throws GeneralSecurityException {
        final DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties()
                .put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectionConfig.getConnectionTimeout());
        clientConfig.getProperties()
                .put(ClientConfig.PROPERTY_READ_TIMEOUT, connectionConfig.getReadTimeout());

        hackInsecureCertificate(clientConfig);
        return Client.create(clientConfig);
    }

    private static void hackInsecureCertificate(final ClientConfig clientConfigArg)
            throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts = new TrustManager[]{TrustManagerUtils.getAcceptAllTrustManager()};

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, null);

        clientConfigArg.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                (s, sslSession) -> true,
                sslContext
        ));
    }
}
