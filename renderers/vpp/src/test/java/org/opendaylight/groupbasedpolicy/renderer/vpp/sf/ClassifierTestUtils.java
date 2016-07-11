/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

import com.google.common.collect.ImmutableMap;

public class ClassifierTestUtils {

    public static final String MSG_NOT_SPECIFIED = "not specified";
    public static final String MSG_PARAMETER_IS_NOT_PRESENT = "parameter is not present";
    public static final String SUPPORTED_PARAM_NAME_ETH = "ethertype";
    public static final String SUPPORTED_PARAM_NAME_IP = "proto";

    static final Long TCP = 6L;

    static Map<String, ParameterValue> createIntValueParam(String paramName, Long value) {
        if (value == null)
            return ImmutableMap.of(paramName, new ParameterValueBuilder().build());
        else
            return ImmutableMap.of(paramName, new ParameterValueBuilder().setIntValue(value).build());
    }
}
