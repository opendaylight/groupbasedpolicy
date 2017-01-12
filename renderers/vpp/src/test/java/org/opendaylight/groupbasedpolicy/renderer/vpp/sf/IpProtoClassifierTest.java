/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

public class IpProtoClassifierTest {

    private Classifier ipProtoCl;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() {
         ipProtoCl = SubjectFeatures.getClassifier(IpProtoClassifierDefinition.ID);
    }

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> supportedParameterValues =
                ipProtoCl.getSupportedParameterValues();

        Assert.assertEquals(1, supportedParameterValues.size());
        Assert.assertEquals(ClassifierTestUtils.SUPPORTED_PARAM_NAME_IP,
                supportedParameterValues.get(0).getParameterName().getValue());

        Assert.assertEquals(IpProtoClassifierDefinition.DEFINITION, ipProtoCl.getClassifierDefinition());
        Assert.assertEquals(IpProtoClassifierDefinition.ID, ipProtoCl.getId());
        Assert.assertEquals(SubjectFeatures.getClassifier(EtherTypeClassifierDefinition.ID), ipProtoCl.getParent());
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoMissing() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM,
                EtherTypeClassifierDefinition.IPv4_VALUE));

        Assert.assertNull(IpProtoClassifier.getIpProtoValue(params));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_NOT_SPECIFIED);
        ipProtoCl.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoNull() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM, null));

        Assert.assertNull(IpProtoClassifier.getIpProtoValue(params));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_PARAMETER_IS_NOT_PRESENT);
        ipProtoCl.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                IpProtoClassifierDefinition.TCP_VALUE));

        ipProtoCl.checkPresenceOfRequiredParams(params);

        Assert.assertEquals(ClassifierTestUtils.TCP, IpProtoClassifier.getIpProtoValue(params));
    }
}
