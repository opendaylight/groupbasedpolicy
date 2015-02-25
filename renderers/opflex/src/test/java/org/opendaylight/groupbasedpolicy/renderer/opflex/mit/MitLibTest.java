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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject.Property;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.EnumInfo.EnumInfoBuilder;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyClassInfo.PolicyClassInfoBuilder;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyId;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MitLibTest {

	private static final String TEST_CLASS_1_NAME = "ClassBar";
	private static final String TEST_CLASS_2_NAME = "ClassBee";
	private static final String TEST_CLASS_3_NAME = "ClassZoo";
	private static final String TEST_PROP_1_NAME = "PropBoo";
	private static final String TEST_PROP_2_NAME = "PropFoo";
	private static final String TEST_PROP_3_NAME = "PropGoo";
	private static final String TEST_PROP_4_NAME = "PropFew";
	private static final String TEST_PROP_5_NAME = "PropLou";
	private static final String TEST_PROP_6_NAME = "PropDue";
	private static final String TEST_PROP_7_NAME = "PropSue";
	private static final String TEST_ENUM_1_NAME = "EnumBlue";
	private static final String TEST_ENUM_VAL_1_NAME = "Hello";
	private static final String TEST_ENUM_VAL_2_NAME = "World";
	private static final String TEST_DATA_2_STRING = "FooToYou";
	private static final String TEST_DATA_4_STRING = "00:01:02:03:04:05";
	private static final String TEST_DATA_6_STRING = "2148040771";
	private static final String TEST_DATA_7_STRING = "200";

	private static final int TEST_PROP_1_ID = 101;
	private static final int TEST_PROP_2_ID = 102;
	private static final int TEST_PROP_3_ID = 103;
	private static final int TEST_PROP_4_ID = 104;
	private static final int TEST_PROP_5_ID = 105;
	private static final int TEST_PROP_6_ID = 106;
	private static final int TEST_PROP_7_ID = 106;
	private static final int TEST_CLASS_1_ID = 201;
	private static final int TEST_CLASS_2_ID = 202;
	private static final int TEST_CLASS_3_ID = 203;
	private static final int TEST_ENUM_VAL_1_VAL = 3;
	private static final int TEST_ENUM_VAL_2_VAL = 4;

    private ObjectMapper objectMapper;
	private JsonNodeFactory jnf;

	public static class TestMit implements OpflexMit {

		private Map<String,PolicyClassInfo> metaDataMap = null;
		private Map<Long, String> classIdMap = null;

		public TestMit() {
			EnumInfo ei;
			PolicyPropertyInfo ppi;
			PolicyClassInfo pci;
			EnumInfoBuilder eib;
			PolicyPropertyInfoBuilder ppib;
			PolicyClassInfoBuilder pcib;
			List<PolicyPropertyInfo> ppil;
			List<PolicyPropertyId> classKeys;

			Map<String,PolicyClassInfo> metaData = new HashMap<String, PolicyClassInfo>();
			Map<Long, String> classIds = new HashMap<Long, String>();

			/*
			 * Construct a test MIT tree
			 *
			 * TODO: test vectored values
			 */
			ppil = new ArrayList<PolicyPropertyInfo>();
	        classKeys = new ArrayList<PolicyPropertyId>();
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_1_ID)).
	             setPropName(TEST_PROP_1_NAME).
	             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
	             setClassId(TEST_CLASS_2_ID).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_2_ID)).
	             setPropName(TEST_PROP_2_NAME).
	             setType(PolicyPropertyInfo.PropertyType.STRING).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        classKeys.add(ppi.getPropId());
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_3_ID)).
	             setPropName(TEST_PROP_3_NAME).
	             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        pcib = new PolicyClassInfoBuilder();
	        pcib.setClassId(TEST_CLASS_1_ID).
	             setClassName(TEST_CLASS_1_NAME).
	             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
	             setProperty(ppil).
	             setKey(classKeys);
	        pci = pcib.build();
	        metaData.put(pci.getClassName(), pci);
	        classIds.put(new Long(pci.getClassId()), pci.getClassName());

	        ppil = new ArrayList<PolicyPropertyInfo>();
	        classKeys = new ArrayList<PolicyPropertyId>();
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_4_ID)).
	             setPropName(TEST_PROP_4_NAME).
	             setType(PolicyPropertyInfo.PropertyType.MAC).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        eib = new EnumInfoBuilder();
	        eib.setName(TEST_ENUM_1_NAME);
	        eib.setEnumValue(TEST_ENUM_VAL_1_NAME,new BigInteger(String.valueOf(TEST_ENUM_VAL_1_VAL)));
	        eib.setEnumValue(TEST_ENUM_VAL_2_NAME,new BigInteger(String.valueOf(TEST_ENUM_VAL_2_VAL)));
	        ei = eib.build();
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_5_ID)).
	             setPropName(TEST_PROP_5_NAME).
	             setType(PolicyPropertyInfo.PropertyType.ENUM8).
	             setEnumInfo(ei).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        pcib = new PolicyClassInfoBuilder();
	        pcib.setClassId(TEST_CLASS_2_ID).
	             setClassName(TEST_CLASS_2_NAME).
	             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
	             setProperty(ppil).
	             setKey(classKeys);
	        pci = pcib.build();
	        metaData.put(pci.getClassName(), pci);
	        classIds.put(new Long(pci.getClassId()), pci.getClassName());

	        ppil = new ArrayList<PolicyPropertyInfo>();
	        classKeys = new ArrayList<PolicyPropertyId>();
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_6_ID)).
	             setPropName(TEST_PROP_6_NAME).
	             setType(PolicyPropertyInfo.PropertyType.U64).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        ppib = new PolicyPropertyInfoBuilder();
	        ppib.setPropId(new PolicyPropertyId(TEST_PROP_7_ID)).
	             setPropName(TEST_PROP_7_NAME).
	             setType(PolicyPropertyInfo.PropertyType.S64).
	             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
	        ppi = ppib.build();
	        ppil.add(ppi);
	        pcib = new PolicyClassInfoBuilder();
	        pcib.setClassId(TEST_CLASS_3_ID).
	             setClassName(TEST_CLASS_3_NAME).
	             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
	             setProperty(ppil).
	             setKey(classKeys);
	        pci = pcib.build();
	        metaData.put(pci.getClassName(), pci);
	        classIds.put(new Long(pci.getClassId()), pci.getClassName());

	        metaDataMap = Collections.unmodifiableMap(metaData);
	        classIdMap = Collections.unmodifiableMap(classIds);

		}

		@Override
		public PolicyClassInfo getClass(String name) {
			return metaDataMap.get(name);

		}

		@Override
		public PolicyClassInfo getClass(Long classId) {
			String className = classIdMap.get(classId);
			if (className == null) return null;
			return metaDataMap.get(className);
		}

	}

	private TestMit testMit;
	private ManagedObject testMo;
	private MitLib lib;

    @Before
    public void setUp() throws Exception {
    	lib = new MitLib();
    	testMit  = new TestMit();
		 objectMapper = new ObjectMapper();
		 objectMapper.configure(
		            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		 jnf = objectMapper.getNodeFactory();

    }

    private ManagedObject constructClass1Mo() {
    	List<Property> propList = new ArrayList<Property>();
    	Property prop = null;
    	ManagedObject mo = new ManagedObject();
    	mo = new ManagedObject();
    	mo.setSubject(TEST_CLASS_1_NAME);

        JsonNode jn = jnf.textNode(TEST_DATA_2_STRING);
    	prop = new Property();
    	prop.setName(TEST_PROP_2_NAME);
        prop.setData(jn);
    	propList.add(prop);
    	prop = new Property();
    	prop.setName(TEST_PROP_3_NAME);
        ObjectNode on = jnf.objectNode();
        on.put(MitLib.REFERENCE_SUBJECT, TEST_CLASS_3_NAME);
        on.put(MitLib.REFERENCE_URI, "/" + TEST_CLASS_3_NAME);
        prop.setData(on);
    	propList.add(prop);
    	mo.setProperties(propList);
    	return mo;

    }

    private ManagedObject constructClass2Mo() {
    	List<Property> propList = new ArrayList<Property>();
    	Property prop = null;
    	ManagedObject mo = new ManagedObject();
    	mo = new ManagedObject();
    	mo.setSubject(TEST_CLASS_2_NAME);

    	prop = new Property();
    	prop.setName(TEST_PROP_4_NAME);
        JsonNode jn = jnf.textNode(TEST_DATA_4_STRING);
        prop.setData(jn);
    	propList.add(prop);
    	prop = new Property();
    	prop.setName(TEST_PROP_5_NAME);
        jn = jnf.textNode(TEST_ENUM_VAL_2_NAME);
        prop.setData(jn);
    	propList.add(prop);
    	mo.setProperties(propList);

    	return mo;
    }

    private ManagedObject constructClass3Mo() {
    	List<Property> propList = new ArrayList<Property>();
    	Property prop = null;
    	ManagedObject mo = new ManagedObject();
    	mo = new ManagedObject();

    	mo.setSubject(TEST_CLASS_3_NAME);

    	prop = new Property();
        prop.setName(TEST_PROP_6_NAME);
        JsonNode jn = jnf.numberNode(new BigInteger(TEST_DATA_6_STRING));
        prop.setData(jn);
    	propList.add(prop);
    	prop = new Property();
        jn = jnf.numberNode(new BigInteger(TEST_DATA_7_STRING));
        prop.setName(TEST_PROP_7_NAME);
        prop.setData(jn);
    	propList.add(prop);
    	mo.setProperties(propList);

    	return mo;
    }

    @Test
    public void testDeserializeMoProperties() throws Exception {
    	PolicyObjectInstance poi = null;

    	/*
    	 * Each MO is a class, with the properties of the class
    	 * matching the properties of the MO. Construct the MO,
    	 * run through the deserializer, and verify that the
    	 * resulting object instance is of the right class and
    	 * has all the properties with correct values
    	 */

    	// Test MO/Class 1
    	testMo = constructClass1Mo();

    	poi = lib.deserializeMoProperties(testMo, testMit);
    	assertTrue(poi.getClassId() == TEST_CLASS_1_ID);
    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_2_ID),
    			             PolicyPropertyInfo.PropertyType.STRING,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	assertTrue(poi.getString(new PolicyPropertyId(TEST_PROP_2_ID)).equals(TEST_DATA_2_STRING));

    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_3_ID),
    			             PolicyPropertyInfo.PropertyType.REFERENCE,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	PolicyObjectInstance.PolicyReference ref = poi.getReference(new PolicyPropertyId(TEST_PROP_3_ID));
    	assertTrue(ref.getClassId() == TEST_CLASS_3_ID);
    	assertTrue(ref.getUri().getValue().equals("/" + TEST_CLASS_3_NAME));

    	// Test MO/Class 2
    	testMo = constructClass2Mo();
    	poi = lib.deserializeMoProperties(testMo, testMit);

    	assertTrue(poi.getClassId() == TEST_CLASS_2_ID);
    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_4_ID),
    			             PolicyPropertyInfo.PropertyType.MAC,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	MacAddress mac = poi.getMacAddress(new PolicyPropertyId(TEST_PROP_4_ID));
    	assertTrue(mac.equals(new MacAddress(TEST_DATA_4_STRING)));

    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_5_ID),
    			             PolicyPropertyInfo.PropertyType.ENUM8,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	assertTrue(poi.getUint64(new PolicyPropertyId(TEST_PROP_5_ID)).intValue() == TEST_ENUM_VAL_2_VAL);

    	// Test MO/Class 3
    	testMo = constructClass3Mo();
    	poi = lib.deserializeMoProperties(testMo, testMit);

    	assertTrue(poi.getClassId() == TEST_CLASS_3_ID);
    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_6_ID),
    			             PolicyPropertyInfo.PropertyType.U64,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	assertTrue(poi.getUint64(new PolicyPropertyId(TEST_PROP_6_ID)).equals(new BigInteger(TEST_DATA_6_STRING)));
    	assertTrue(mac.equals(new MacAddress(TEST_DATA_4_STRING)));

    	assertTrue(poi.isSet(new PolicyPropertyId(TEST_PROP_7_ID),
    			             PolicyPropertyInfo.PropertyType.S64,
    			             PolicyPropertyInfo.PropertyCardinality.SCALAR));
    	assertTrue(poi.getInt64(new PolicyPropertyId(TEST_PROP_7_ID)) == new Long(TEST_DATA_7_STRING));


    }

    @Test
    public void testSerializeMoProperties() throws Exception {
    	PolicyClassInfo pci = null;
    	PolicyObjectInstance poi = null;
    	boolean prop2Found = false, prop3Found = false,
    			prop4Found = false, prop5Found = false,
    			prop6Found = false, prop7Found = false;

    	/*
    	 * Construct the PolicyObjectInfo object by
    	 * running it through the deserializer
    	 */

    	testMo = constructClass1Mo();
    	poi = lib.deserializeMoProperties(testMo,  testMit);
    	pci = testMit.getClass(TEST_CLASS_1_NAME);
    	lib.serializeMoProperties( pci, poi, testMo, testMit);
    	List<Property> props = testMo.getProperties();
    	assertTrue(props.size() == 2);
    	for (Property prop: props) {

    		if (prop.getName().equals(TEST_PROP_2_NAME)) {
                assertTrue(prop.getData().asText().equals(TEST_DATA_2_STRING));
    	    	prop2Found = true;
    		}

    		if (prop.getName().equals(TEST_PROP_3_NAME)) {
    	    	assertTrue(prop.getName().equals(TEST_PROP_3_NAME));
                assertTrue(prop.getData().has(MitLib.REFERENCE_SUBJECT));
                JsonNode jn = prop.getData().findValue(MitLib.REFERENCE_SUBJECT);
                assertTrue(jn.asText().equals(TEST_CLASS_3_NAME));
                assertTrue(prop.getData().has(MitLib.REFERENCE_URI));
                jn = prop.getData().findValue(MitLib.REFERENCE_URI);
                assertTrue(jn.asText().equals("/" + TEST_CLASS_3_NAME));
                prop3Found = true;
    		}
    	}
    	assertTrue(prop2Found);
    	assertTrue(prop3Found);

    	testMo = constructClass2Mo();
    	poi = lib.deserializeMoProperties(testMo,  testMit);
    	pci = testMit.getClass(TEST_CLASS_2_NAME);
    	lib.serializeMoProperties( pci, poi, testMo, testMit);
    	props = testMo.getProperties();
    	assertTrue(props.size() == 2);
    	for (Property prop: props) {

    		if (prop.getName().equals(TEST_PROP_4_NAME)) {
                assertTrue(prop.getData().asText().equals(TEST_DATA_4_STRING));
    	    	prop4Found = true;
    		}
    		if (prop.getName().equals(TEST_PROP_5_NAME)) {
                assertTrue(prop.getData().asText().equals(TEST_ENUM_VAL_2_NAME));
    			prop5Found = true;
    		}
    	}
    	assertTrue(prop4Found);
    	assertTrue(prop5Found);


    	testMo = constructClass3Mo();
    	poi = lib.deserializeMoProperties(testMo,  testMit);
    	pci = testMit.getClass(TEST_CLASS_3_NAME);
    	lib.serializeMoProperties( pci, poi, testMo, testMit);
    	props = testMo.getProperties();
    	assertTrue(props.size() == 2);
    	for (Property prop: props) {
    		if (prop.getName().equals(TEST_PROP_6_NAME)) {
                assertTrue(prop.getData().asText().equals(TEST_DATA_6_STRING));
    	    	prop6Found = true;
    		}
    		if (prop.getName().equals(TEST_PROP_7_NAME)) {
                assertTrue(prop.getData().asText().equals(TEST_DATA_7_STRING));
    			prop7Found = true;
    		}
    	}
    	assertTrue(prop6Found);
    	assertTrue(prop7Found);

    }

}
