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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

public class IpProtoClassifierTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> supportedParameterValues = Classifier.IP_PROTO_CL.getSupportedParameterValues();

        Assert.assertEquals(1, supportedParameterValues.size());
        Assert.assertEquals(ClassifierTestUtils.SUPPORTED_PARAM_NAME_IP,
                supportedParameterValues.get(0).getParameterName().getValue());

        Assert.assertEquals(IpProtoClassifierDefinition.DEFINITION, Classifier.IP_PROTO_CL.getClassifierDefinition());
        Assert.assertEquals(IpProtoClassifierDefinition.ID, Classifier.IP_PROTO_CL.getId());
        Assert.assertEquals(Classifier.ETHER_TYPE_CL, Classifier.IP_PROTO_CL.getParent());
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoMissing() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM,
                EtherTypeClassifierDefinition.IPv4_VALUE));

        Assert.assertNull(IpProtoClassifier.getIpProtoValue(params));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_NOT_SPECIFIED);
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoNull() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM, null));

        Assert.assertNull(IpProtoClassifier.getIpProtoValue(params));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_PARAMETER_IS_NOT_PRESENT);
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                IpProtoClassifierDefinition.TCP_VALUE));

        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);

        Assert.assertEquals(ClassifierTestUtils.TCP, IpProtoClassifier.getIpProtoValue(params));
    }
}
