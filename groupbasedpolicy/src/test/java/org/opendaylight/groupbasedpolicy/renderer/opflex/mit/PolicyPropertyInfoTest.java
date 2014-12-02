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

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyInfoBuilder;

public class PolicyPropertyInfoTest {


	private static int TEST_PROP_NUM = 200;
	private static int TEST_CLASS_ID = 100;
	private static String TEST_PROP_NAME = "foobar";
	private static PolicyPropertyInfo.PolicyPropertyId propId;
	private static PolicyPropertyInfo.PropertyCardinality TEST_PROP_CARDINALITY =
			PolicyPropertyInfo.PropertyCardinality.SCALAR;
	private static PolicyPropertyInfo.PropertyType TEST_PROP_TYPE =
			PolicyPropertyInfo.PropertyType.COMPOSITE;
	@Mock
	EnumInfo mockEnumInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    	propId = new PolicyPropertyInfo.PolicyPropertyId(TEST_PROP_NUM);
    }

    @Test
    public void builderTest() throws Exception {
    	PolicyPropertyInfoBuilder ppib = new PolicyPropertyInfoBuilder();
    	PolicyPropertyInfo ppi;
    	ppib.setClassId(TEST_CLASS_ID);
    	ppib.setEnumInfo(mockEnumInfo);
    	ppib.setPropCardinality(TEST_PROP_CARDINALITY);
    	ppib.setPropId(propId);
    	ppib.setPropName(TEST_PROP_NAME);
    	ppib.setType(TEST_PROP_TYPE);
    	ppi = ppib.build();
    	assertTrue(ppi.getClassId() == TEST_CLASS_ID);
    	assertTrue(ppi.getEnumInfo() == mockEnumInfo);
    	assertTrue(ppi.getPropCardinality() == TEST_PROP_CARDINALITY);
    	assertTrue(ppi.getPropId() == propId);
    	assertTrue(ppi.getPropName().equals(TEST_PROP_NAME));
    	assertTrue(ppi.getType() == TEST_PROP_TYPE);

    }

}
