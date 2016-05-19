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
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.ParameterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.Int;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

public class IpProtoClassifierTest {

    @Test
    public void testGetId() {
        assertEquals(IpProtoClassifierDefinition.ID, Classifier.IP_PROTO_CL.getId());
    }

    @Test
    public void testGetClassifierDefinition() {
        assertEquals(IpProtoClassifierDefinition.DEFINITION, Classifier.IP_PROTO_CL.getClassifierDefinition());
    }

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> valuesList = Classifier.IP_PROTO_CL.getSupportedParameterValues();
        assertEquals(1, valuesList.size());

        SupportedParameterValues values = valuesList.get(0);
        assertNotNull(values);
        assertEquals(IpProtoClassifierDefinition.PROTO_PARAM, values.getParameterName().getValue());
        ParameterType pt = values.getParameterType();
        assertTrue(pt instanceof Int);
    }

    @Test
    public void testCheckPresenceOfRequiredParams() {
        Map<String, ParameterValue> params = prepareParams(IpProtoClassifierDefinition.UDP_VALUE);

        try {
            Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
        } catch (IllegalArgumentException e) {
            fail("Required parameter missing");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPresenceOfRequiredParams_noParam() {
        Map<String, ParameterValue> params = new HashMap<>();

        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPresenceOfRequiredParams_nullValue() {
        Map<String, ParameterValue> params = prepareParams_nullValue();

        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testGetIpProtoValue() {
        Long expected = IpProtoClassifierDefinition.UDP_VALUE;

        assertEquals(expected, IpProtoClassifier.getIpProtoValue(prepareParams(expected)));
    }

    @Test
    public void testGetIpProtoValue_badParams() {
        Map<String, ParameterValue> params = prepareParams_nullValue();

        assertNull(IpProtoClassifier.getIpProtoValue(params));
        assertNull(IpProtoClassifier.getIpProtoValue(new HashMap<String, ParameterValue>()));
        assertNull(IpProtoClassifier.getIpProtoValue(null));
    }

    @Test
    public void testGetParent() {
        assertEquals(Classifier.IP_PROTO_CL.getParent(), Classifier.ETHER_TYPE_CL);
    }

    private Map<String, ParameterValue> prepareParams(Long intValue) {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().setIntValue(intValue).build();
        params.put(IpProtoClassifierDefinition.PROTO_PARAM, pv);
        return params;
    }

    private Map<String, ParameterValue> prepareParams_nullValue() {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().build();
        params.put(IpProtoClassifierDefinition.PROTO_PARAM, pv);
        return params;
    }

}
