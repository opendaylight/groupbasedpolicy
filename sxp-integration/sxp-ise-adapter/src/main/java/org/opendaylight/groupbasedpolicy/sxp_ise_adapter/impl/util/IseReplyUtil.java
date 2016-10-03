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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Purpose: provide common functionality upon ise reply message
 */
public class IseReplyUtil {

    private static final String EXPRESSION_SGT_ALL_RESOURCES = "/ns3:searchResult/ns3:resources/ns5:resource";
    private static final String EXPRESSION_SGT_DETAIL_LINK = "./link/@href";
    private static final String EXPRESSION_SGT_DETAIL = "./ns4:sgt";
    private static final String EXPRESSION_SGT_NAME_ATTR = "./@name";
    private static final String EXPRESSION_SGT_UUID_ATTR = "./@id";
    private static final String EXPRESSION_SGT_VALUE = "./value/text()";

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

    /**
     * @param uuidToSgtMap map of existing sgts (by uuid)
     * @param xpath        xpath instance
     * @param sgtResources input node list
     * @return new/unknown sgts to explore
     * @throws XPathExpressionException in case xpath processing fails
     */
    public static Collection<Node> filterNewResourcesByID(final Map<String, Integer> uuidToSgtMap, final XPath xpath,
                                                          final NodeList sgtResources)
            throws XPathExpressionException {
        final Collection<Node> nodesToExplore = new ArrayList<>();
        for (int i = 0; i < sgtResources.getLength(); i++) {
            final String uuid = ((Node) xpath.evaluate(EXPRESSION_SGT_UUID_ATTR, sgtResources.item(i), XPathConstants.NODE)).getNodeValue();
            if (!uuidToSgtMap.containsKey(uuid)) {
                nodesToExplore.add(
                        (Node) xpath.evaluate(EXPRESSION_SGT_DETAIL_LINK, sgtResources.item(i), XPathConstants.NODE)
                );
            }
        }
        return nodesToExplore;
    }

    public static NodeList findAllSgtResourceNodes(final XPath xpath, final InputSource inputSource) throws XPathExpressionException {
        return (NodeList) xpath.evaluate(EXPRESSION_SGT_ALL_RESOURCES, inputSource,
                XPathConstants.NODESET);
    }

    public static Node gainSgtValue(final XPath xpath, final Node sgtNode) throws XPathExpressionException {
        return (Node) xpath.evaluate(EXPRESSION_SGT_VALUE, sgtNode, XPathConstants.NODE);
    }

    public static Node gainSgtUuid(final XPath xpath, final Node sgtNode) throws XPathExpressionException {
        return (Node) xpath.evaluate(EXPRESSION_SGT_UUID_ATTR, sgtNode, XPathConstants.NODE);
    }

    public static Node gainSgtName(final XPath xpath, final Node sgtNode) throws XPathExpressionException {
        return (Node) xpath.evaluate(EXPRESSION_SGT_NAME_ATTR, sgtNode, XPathConstants.NODE);
    }

    public static Node findSgtDetailNode(final XPath xpath, final String rawSgtDetail) throws XPathExpressionException {
        return (Node) xpath.evaluate(EXPRESSION_SGT_DETAIL, createInputSource(rawSgtDetail),
                XPathConstants.NODE);
    }
}
