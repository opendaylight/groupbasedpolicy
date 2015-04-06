/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

public abstract class ParamDerivator {

    /**
     * It is possible to derive missing parameters.
     * <p>
     * Examle:
     * <p>
     * Ether-type parameter is missing. Derivation policy might instruct: ipv4 and ipv6 flows need
     * to be created if ether-type parameter is not present. In this case two derivations of
     * {@code params} should be returned. One with ipv4 ether-type, the other with ipv6.
     *
     * @param params parameters inserted by user
     * @return list of {@code params} updated with derived values
     */
    public abstract List<Map<String, ParameterValue>> deriveParameter(Map<String, ParameterValue> params);

    public static final ParamDerivator ETHER_TYPE_DERIVATOR = new ParamDerivator() {

        @Override
        public List<Map<String, ParameterValue>> deriveParameter(Map<String, ParameterValue> params) {

            if (!params.containsKey(EtherTypeClassifier.ETHERTYPE_PARAM)) {
                Map<String, ParameterValue> ipv4Params = new HashMap<>(params);
                Map<String, ParameterValue> ipv6Params = new HashMap<>(params);
                ipv4Params.put(EtherTypeClassifier.ETHERTYPE_PARAM, new ParameterValueBuilder().setIntValue(FlowUtils.IPv4)
                    .build());
                ipv6Params.put(EtherTypeClassifier.ETHERTYPE_PARAM, new ParameterValueBuilder().setIntValue(FlowUtils.IPv6)
                    .build());
                List<Map<String, ParameterValue>> derivedParams = new ArrayList<>();
                derivedParams.add(ipv4Params);
                derivedParams.add(ipv6Params);
                return derivedParams;
            }
            return Collections.singletonList(params);
        }
    };
}
