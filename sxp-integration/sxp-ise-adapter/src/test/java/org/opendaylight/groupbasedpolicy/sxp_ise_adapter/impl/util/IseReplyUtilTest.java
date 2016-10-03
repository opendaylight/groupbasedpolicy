/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util;

import static org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.IseResourceTestHelper.readLocalResource;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Test for {@link IseReplyUtil}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IseReplyUtilTest {

    private final String iseReplyAllSgts;
    private XPath xpath;

    public IseReplyUtilTest() throws IOException {
        iseReplyAllSgts = readLocalResource("./rawIse-allSgts2.xml");
    }

    @Before
    public void setUp() throws Exception {
        xpath = IseReplyUtil.setupXpath();
    }

    @Test
    public void filterNewResourcesByID() throws Exception {
        final Map<String, Integer> uuidMap = new HashMap<>();
        uuidMap.put("abc123", 42);

        final InputSource inputSource = IseReplyUtil.createInputSource(iseReplyAllSgts);
        final NodeList allSgtResourceNodes = IseReplyUtil.findAllSgtResourceNodes(xpath, inputSource);

        final Collection<Node> filteredNodes = IseReplyUtil.filterNewResourcesByID(uuidMap, xpath, allSgtResourceNodes);

        Assert.assertEquals(1, filteredNodes.size());
        Assert.assertEquals("https://example.org:9060/ers/config/sgt/abc124", Iterables.getFirst(filteredNodes, null).getNodeValue());
    }
}