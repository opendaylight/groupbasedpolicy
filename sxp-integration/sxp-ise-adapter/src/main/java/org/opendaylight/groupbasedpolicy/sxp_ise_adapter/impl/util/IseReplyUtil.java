/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.StringReader;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;

/**
 * Purpose: provide common functionality upon ise reply message
 */
public class IseReplyUtil {

    public static final String EXPRESSION_SGT_ALL_LINK_HREFS = "/ns3:searchResult/ns3:resources/ns5:resource/link/@href";
    public static final String EXPRESSION_SGT_DETAIL = "./ns4:sgt";
    public static final String EXPRESSION_SGT_NAME_ATTR = "./@name";
    public static final String EXPRESSION_SGT_UUID_ATTR = "./@id";
    public static final String EXPRESSION_SGT_VALUE = "./value/text()";

    private IseReplyUtil() {
        throw new IllegalAccessError("util class - no instances supported");
    }


    public static String deliverResponse(final WebResource.Builder requestBuilder) {
        return requestBuilder.get(ClientResponse.class).getEntity(String.class);
    }

    /**
     * @return initiated xpath with ise namespace context injected
     */
    public static XPath setupXpath() {
        final NamespaceContext nsContext = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                final String outcome;
                if (prefix == null) {
                    throw new NullPointerException("Null prefix");
                }

                if ("ns5".equals(prefix)) {
                    outcome = "ers.ise.cisco.com";
                } else if ("ns3".equals(prefix)) {
                    outcome = "v2.ers.ise.cisco.com";
                } else if ("ns4".equals(prefix)) {
                    outcome = "trustsec.ers.ise.cisco.com";
                } else {
                    outcome = XMLConstants.NULL_NS_URI;
                }
                return outcome;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        };

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(nsContext);
        return xpath;
    }

    public static InputSource createInputSource(final String rawSgtDetail) {
        return new InputSource(new StringReader(rawSgtDetail));
    }
}
