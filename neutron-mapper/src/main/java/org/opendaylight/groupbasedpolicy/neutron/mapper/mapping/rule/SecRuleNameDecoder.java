/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.SecurityRuleAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

public class SecRuleNameDecoder {

    final static String MIN_PORT = "_min";
    final static String MAX_PORT = "_max";

    private SecRuleNameDecoder() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static SubjectName getSubjectName(SecurityRule secRule) {
        RuleName ruleName = SecRuleNameDecoder.getRuleName(secRule);
        return new SubjectName(ruleName);
    }

    public static RuleName getRuleName(SecurityRule secRule) {
        ClassifierName classifierRefName = SecRuleNameDecoder.getClassifierRefName(secRule);
        String ruleName = new StringBuilder(MappingUtils.ACTION_ALLOW.getName().getValue())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER).append(classifierRefName.getValue()).toString();
        return new RuleName(ruleName);
    }

    public static ClassifierName getClassifierRefName(SecurityRule secRule) {
        Direction direction = SecRuleEntityDecoder.getDirection(secRule);
        ClassifierName classifierInstanceName = getClassifierInstanceName(secRule);
        String crName = new StringBuilder().append(direction.name())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(classifierInstanceName.getValue())
            .toString();
        return new ClassifierName(crName);
    }

    public static ClassifierName getClassifierInstanceName(SecurityRule secRule) {
        StringBuilder keyBuilder = new StringBuilder();
        Integer portMin = secRule.getPortRangeMin();
        Integer portMax = secRule.getPortRangeMax();
        if (portMin != null && portMax != null) {
            keyBuilder.append(L4ClassifierDefinition.DEFINITION.getName().getValue());
            if (portMin.equals(portMax)) {
                keyBuilder.append(MappingUtils.NAME_DELIMETER)
                    .append(L4ClassifierDefinition.DST_PORT_PARAM)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMin.longValue());
            } else {
                keyBuilder.append(MappingUtils.NAME_DELIMETER)
                    .append(L4ClassifierDefinition.DST_PORT_RANGE_PARAM)
                    .append(MIN_PORT)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMin.longValue())
                    .append(MAX_PORT)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMax.longValue());
            }
        }
        SecurityRuleAttributes.Protocol protocol = secRule.getProtocol();
        if (protocol != null) {
            if (keyBuilder.length() > 0) {
                keyBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER);
            }
            String protocolString = "";
            if (protocol.getUint8() != null) {
                protocolString = protocol.getUint8().toString();
            } else if (protocol.getIdentityref() != null) {
                protocolString = protocol.getIdentityref().getSimpleName();
            }
            keyBuilder.append(IpProtoClassifierDefinition.DEFINITION.getName().getValue())
                .append(MappingUtils.NAME_VALUE_DELIMETER)
                .append(protocolString);
        }
        Class<? extends EthertypeBase> ethertype = secRule.getEthertype();
        if (ethertype != null) {
            if (keyBuilder.length() > 0) {
                keyBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER);
            }
            keyBuilder.append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
                .append(MappingUtils.NAME_VALUE_DELIMETER)
                .append(ethertype.getSimpleName());
        }
        return new ClassifierName(keyBuilder.toString());
    }

    public static ClauseName getClauseName(SecurityRule secRule) {
        IpPrefix remoteIpPrefix = secRule.getRemoteIpPrefix();
        SubjectName subjectName = getSubjectName(secRule);
        if (remoteIpPrefix == null) {
            return new ClauseName(subjectName);
        }
        return new ClauseName(subjectName.getValue() + MappingUtils.NAME_DOUBLE_DELIMETER
                + Utils.getStringIpPrefix(remoteIpPrefix).replace('/', '_'));
    }

}
