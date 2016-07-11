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

public class EtherTypeClassifierTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetSupportedParameterValues() {
        List<SupportedParameterValues> supportedParameterValues =
                Classifier.ETHER_TYPE_CL.getSupportedParameterValues();

        Assert.assertEquals(1, supportedParameterValues.size());
        Assert.assertEquals(ClassifierTestUtils.SUPPORTED_PARAM_NAME_ETH,
                supportedParameterValues.get(0).getParameterName().getValue());

        Assert.assertEquals(EtherTypeClassifierDefinition.DEFINITION,
                Classifier.ETHER_TYPE_CL.getClassifierDefinition());
        Assert.assertEquals(EtherTypeClassifierDefinition.ID, Classifier.ETHER_TYPE_CL.getId());
        Assert.assertNull(Classifier.ETHER_TYPE_CL.getParent());
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_EtherTypeMissing() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_NOT_SPECIFIED);
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_EtherTypeNull() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, null));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ClassifierTestUtils.MSG_PARAMETER_IS_NOT_PRESENT);
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }
}
