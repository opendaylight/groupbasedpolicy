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



import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyClassInfo.PolicyClassInfoBuilder;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyId;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyInfoBuilder;

public class PolicyClassInfoTest {

	List<PolicyPropertyId> testKeyList;
	List<PolicyPropertyInfo> testPropertyInfoList;
	PolicyPropertyInfo testPpi;

	private static final int TEST_PROP_ID = 200;
	private static final int TEST_CLASS_ID = 100;
	private static final String TEST_CLASS_NAME = "foobar";
	private static final PolicyClassInfo.PolicyClassType TEST_CLASS_TYPE =
			PolicyClassInfo.PolicyClassType.POLICY;

    @Before
    public void setUp() throws Exception {
        PolicyPropertyInfoBuilder ppib = new PolicyPropertyInfoBuilder();
        testPpi = ppib.build();
        testKeyList = new ArrayList<PolicyPropertyId>();
        testKeyList.add(new PolicyPropertyId(TEST_PROP_ID));
        testPropertyInfoList = new ArrayList<PolicyPropertyInfo>();
        testPropertyInfoList.add(testPpi);
    }

    @Test
    public void testBuilder() throws Exception {
    	PolicyClassInfoBuilder pcib = new PolicyClassInfoBuilder();
    	PolicyClassInfo pci = null;
    	pcib.setClassId(TEST_CLASS_ID);
    	pcib.setClassName(TEST_CLASS_NAME);
    	pcib.setKey(testKeyList);
    	pcib.setPolicyType(TEST_CLASS_TYPE);
    	pcib.setProperty(testPropertyInfoList);
    	pci = pcib.build();

    	assertTrue(pci.getClassId() == TEST_CLASS_ID);
    	assertTrue(pci.getClassName().equals(TEST_CLASS_NAME));
    	assertTrue(pci.getKeys().get(0) == testKeyList.get(0));
    	assertTrue(pci.getPolicyType() == TEST_CLASS_TYPE);
    	assertTrue(pci.getProperties().get(0) == testPropertyInfoList.get(0));

    }

}
