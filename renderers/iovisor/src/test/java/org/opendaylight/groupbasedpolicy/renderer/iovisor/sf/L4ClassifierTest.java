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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.ParameterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.Int;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

public class L4ClassifierTest {

    private ParameterValue pvSrcPort80;
    private ParameterValue pvDstPort80;
    private ParameterValue pvDstPort_null;
    private ParameterValue pvSrcRange81_82;
    private ParameterValue pvDstRange81_82;
    private ParameterValue pvDstRange82_81;
    private ParameterValue pvDstRange_null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() {
        pvSrcPort80 = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM)).setIntValue(80L).build();
        pvDstPort80 = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).setIntValue(80L).build();
        pvDstPort_null = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).build();
        pvSrcRange81_82 = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM))
                .setRangeValue(new RangeValueBuilder().setMin(81L).setMax(82L).build())
                .build();
        pvDstRange81_82 = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
                .setRangeValue(new RangeValueBuilder().setMin(81L).setMax(82L).build())
                .build();
        pvDstRange82_81 = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
                .setRangeValue(new RangeValueBuilder().setMin(82L).setMax(81L).build())
                .build();
        pvDstRange_null = new ParameterValueBuilder().setName(
                new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
                //.setRangeValue(new RangeValueBuilder().setMin(82L).setMax(81L).build())
                .build();
    }

    @Test
    public void testGetId() {
        assertEquals(L4ClassifierDefinition.ID, Classifier.L4_CL.getId());
    }

    @Test
    public void testGetClassifierDefinition() {
        assertEquals(L4ClassifierDefinition.DEFINITION, Classifier.L4_CL.getClassifierDefinition());
    }

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> valuesList = Classifier.L4_CL.getSupportedParameterValues();
        assertEquals(4, valuesList.size());

        SupportedParameterValues values = valuesList.get(0);
        assertNotNull(values);
        assertEquals(L4ClassifierDefinition.SRC_PORT_PARAM, values.getParameterName().getValue());
        ParameterType pt = values.getParameterType();
        assertTrue(pt instanceof Int);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_Empty() throws Exception {
        // TODO check: sending empty map is ok?
        Classifier.L4_CL.checkPresenceOfRequiredParams(new HashMap<String, ParameterValue>());
    }

    @Test
    public void testCheckPresenceOfRequiredParams_SinglePorts() throws Exception {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_PARAM, pvSrcPort80);
        params.put(L4ClassifierDefinition.DST_PORT_PARAM, pvDstPort80);

        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_PortRanges() throws Exception {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM, pvSrcRange81_82);
        params.put(L4ClassifierDefinition.DST_PORT_RANGE_PARAM, pvDstRange81_82);

        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_DstPortNull() throws IllegalArgumentException {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_PARAM, pvSrcPort80);
        params.put(L4ClassifierDefinition.DST_PORT_PARAM, pvDstPort_null);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(L4Classifier.EXC_MSG_PARAM_VALUE_NOT_SPECIFIED);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_DstRangeNull() throws IllegalArgumentException {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_PARAM, pvSrcPort80);
        params.put(L4ClassifierDefinition.DST_PORT_RANGE_PARAM, pvDstRange_null);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(L4Classifier.EXC_MSG_PARAM_VALUE_NOT_SPECIFIED);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_ParamConflict() throws IllegalArgumentException {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_PARAM, pvSrcPort80);
        params.put(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM, pvSrcRange81_82);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(L4Classifier.EXC_MSG_MUT_EXCLUSIVE_PARAMS);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_RangeInverted() throws IllegalArgumentException {
        Map<String, ParameterValue> params = new HashMap<>();
        params.put(L4ClassifierDefinition.SRC_PORT_PARAM, pvSrcPort80);
        params.put(L4ClassifierDefinition.DST_PORT_RANGE_PARAM, pvDstRange82_81);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(L4Classifier.EXC_MSG_RANGE_VALUE_MISMATCH);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_emptyParams() {

        Classifier.L4_CL.checkPresenceOfRequiredParams(new HashMap<String, ParameterValue>());
    }

    @Test
    public void testGetParent() {
        assertEquals(Classifier.L4_CL.getParent(), Classifier.IP_PROTO_CL);
    }

}
