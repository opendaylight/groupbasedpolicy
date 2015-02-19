/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : tbachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PolicyUriTest {

	private static final String TEST_STRING_1 = "foo";
	private static final String TEST_STRING_2 = "bar";
	private static final String TEST_STRING_3 = "boo";
	private static final String TEST_TOKEN_1 = "PolicyUniverse";
	private static final String TEST_TOKEN_2 = "PolicySpace";
	private static final String TEST_TOKEN_3 = "7a44000e-056d-4566-bbb0-32b973f90369";
	private static final String FULL_URI = PolicyUri.POLICY_URI_SEP + TEST_TOKEN_1 +
			                               PolicyUri.POLICY_URI_SEP + TEST_TOKEN_2 +
			                               PolicyUri.POLICY_URI_SEP + TEST_TOKEN_3;


    @Test
    public void testConstructors() throws Exception {
    	PolicyUri uri1 = new PolicyUri(FULL_URI);
    	PolicyUri uri2 = new PolicyUri(uri1);
    	assertTrue(uri1.equals(uri2));
    	List<String> tokens = new ArrayList<String>();
    	tokens.add(TEST_TOKEN_1);
    	tokens.add(TEST_TOKEN_2);
    	tokens.add(TEST_TOKEN_3);
    	PolicyUri uri3 = new PolicyUri(tokens);
    	assertTrue(uri3.equals(uri1));
    	PolicyUri uri4 = new PolicyUri();
    	uri4.push(TEST_TOKEN_1);
    	uri4.push(TEST_TOKEN_2);
    	uri4.push(TEST_TOKEN_3);
    	assertTrue(uri4.equals(uri1));
    }

    @Test
    public void testPushPop() throws Exception {
    	PolicyUri uri = new PolicyUri();

    	uri.push(TEST_STRING_1);
    	uri.push(TEST_STRING_2);
    	uri.push(TEST_STRING_3);

    	assertTrue(uri.pop().equals(TEST_STRING_3));
    	assertTrue(uri.pop().equals(TEST_STRING_2));

    	uri.push(TEST_STRING_3);
    	uri.push(TEST_STRING_1);

    	assertTrue(uri.pop().equals(TEST_STRING_1));
    	assertTrue(uri.pop().equals(TEST_STRING_3));
    	assertTrue(uri.pop().equals(TEST_STRING_1));
    	assertTrue(uri.pop() == null);

    }

    @Test
    public void testFullUri1() throws Exception {
    	List<String> TEST_TOKENS =
    			Arrays.asList(TEST_TOKEN_1,
    					      TEST_TOKEN_2,
    					      TEST_TOKEN_3);
    	PolicyUri uri = new PolicyUri(TEST_TOKENS);
    	assertTrue(uri.pop().equals(TEST_TOKEN_3));
    	assertTrue(uri.pop().equals(TEST_TOKEN_2));
    	assertTrue(uri.pop().equals(TEST_TOKEN_1));
    }

    @Test
    public void testFullUri2() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);
    	assertTrue(uri.pop().equals(TEST_TOKEN_3));
    	assertTrue(uri.pop().equals(TEST_TOKEN_2));
    	assertTrue(uri.pop().equals(TEST_TOKEN_1));

    }

    @Test
    public void testGetParent() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);
    	String parent = uri.getParent();
    	uri.pop();
    	assertTrue(parent.equals(uri.originalPath()));
    }

    @Test
    public void testValid() throws Exception {
    	PolicyUri uri = new PolicyUri();

    	assertFalse(uri.valid());
    	uri.push(TEST_TOKEN_1);
    	assertTrue(uri.valid());
    }

    @Test
    public void testGetElement() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);

    	String element = uri.getElement(0);
    	assertTrue(element.equals(TEST_TOKEN_1));
    	element = uri.getElement(1);
    	assertTrue(element.equals(TEST_TOKEN_2));
    	element = uri.getElement(2);
    	assertTrue(element.equals(TEST_TOKEN_3));
    }

    @Test
    public void testTotalElements() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);
    	assertTrue(uri.totalElements() == 3);
    }

    @Test
    public void testContains() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);
    	assertFalse(uri.contains(TEST_STRING_1));
    	assertTrue(uri.contains(TEST_TOKEN_3));
    }

    @Test
    public void testStrings() throws Exception {
    	PolicyUri uri = new PolicyUri(FULL_URI);
    	assertTrue(uri.toString().equals(FULL_URI));
    	assertTrue(uri.originalPath().equals(FULL_URI));
    }

}
