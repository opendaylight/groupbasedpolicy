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

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.EnumInfo.EnumInfoBuilder;

public class EnumInfoTest {

	private static final String TEST_NAME_1 = "Foo";
	private static final String TEST_NAME_2 = "Boo";
	private static final String TEST_NAME_3 = "Zoo";
	private static final String TEST_VALUE_1_STRING = "100";
	private static final String TEST_VALUE_2_STRING = "101";
	private static final String TEST_VALUE_3_STRING = "102";



    @Test
    public void testBuilder() throws Exception {
    	EnumInfoBuilder eib = new EnumInfoBuilder();

    	BigInteger bi1 = new BigInteger(TEST_VALUE_1_STRING);
    	BigInteger bi2 = new BigInteger(TEST_VALUE_2_STRING);
    	BigInteger bi3 = new BigInteger(TEST_VALUE_3_STRING);

    	eib.setEnumValue(TEST_NAME_1, bi1);
    	eib.setEnumValue(TEST_NAME_2, bi2);
    	eib.setEnumValue(TEST_NAME_3, bi3);

    	EnumInfo ei = eib.build();
    	assertTrue(ei.getEnumValue(TEST_NAME_1).equals(bi1));
    	assertTrue(ei.getEnumValue(bi1).equals(TEST_NAME_1));
    	assertTrue(ei.getEnumValue(TEST_NAME_2).equals(bi2));
    	assertTrue(ei.getEnumValue(bi2).equals(TEST_NAME_2));
    	assertTrue(ei.getEnumValue(TEST_NAME_3).equals(bi3));
    	assertTrue(ei.getEnumValue(bi3).equals(TEST_NAME_3));
    }

}
