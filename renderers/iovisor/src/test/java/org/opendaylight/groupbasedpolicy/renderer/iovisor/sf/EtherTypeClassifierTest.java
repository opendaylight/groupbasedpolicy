/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.ParameterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.Int;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

public class EtherTypeClassifierTest {

    @Test
    public void testGetId() {
        assertEquals(EtherTypeClassifierDefinition.ID, Classifier.ETHER_TYPE_CL.getId());
    }

    @Test
    public void testGetClassifierDefinition() {
        assertEquals(EtherTypeClassifierDefinition.DEFINITION, Classifier.ETHER_TYPE_CL.getClassifierDefinition());
    }

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> valuesList = Classifier.ETHER_TYPE_CL.getSupportedParameterValues();
        assertEquals(1, valuesList.size());

        SupportedParameterValues values = valuesList.get(0);
        assertNotNull(values);
        assertEquals(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, values.getParameterName().getValue());
        ParameterType pt = values.getParameterType();
        assertTrue(pt instanceof Int);
    }

    @Test
    public void testCheckPresenceOfRequiredParams() {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.ARP_VALUE).build();
        params.put(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, pv);

        try {
            Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
        } catch (IllegalArgumentException e) {
            fail("Required parameter missing");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPresenceOfRequiredParams_noParam() {
        Map<String, ParameterValue> params = new HashMap<>();

        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPresenceOfRequiredParams_nullValue() {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().build();
        params.put(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, pv);

        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testGetParent() {
        assertNull(Classifier.ETHER_TYPE_CL.getParent());
    }

}
