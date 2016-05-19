/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

public class ParamDerivatorTest {

    private ParamDerivator derivator = ParamDerivator.ETHER_TYPE_DERIVATOR;

    @Test
    public void testDeriveParameter_noDerivation() {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE).build();
        params.put(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, pv);

        List<Map<String, ParameterValue>> result = derivator.deriveParameter(params);

        assertEquals(1, result.size());
        assertEquals(params, result.get(0));
    }

    @Test
    public void testDeriveParameter_withDerivation() {
        Map<String, ParameterValue> params = new HashMap<>();
        ParameterValue pv = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE).build();
        params.put("dummy key", pv);

        List<Map<String, ParameterValue>> derivedParams = derivator.deriveParameter(params);

        assertEquals(2, derivedParams.size());

        Map<String, ParameterValue> ipv4Params = derivedParams.get(0);
        Map<String, ParameterValue> ipv6Params = derivedParams.get(1);

        assertTrue(ipv4Params.containsKey(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));
        assertTrue(ipv6Params.containsKey(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));
    }

}
