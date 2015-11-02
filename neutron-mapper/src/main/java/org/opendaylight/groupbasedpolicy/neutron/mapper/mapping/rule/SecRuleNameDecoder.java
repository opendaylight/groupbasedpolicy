/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.IpProtoClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.L4Classifier;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;

import com.google.common.base.Strings;

public class SecRuleNameDecoder {

    final static String MIN_PORT = "_min";
    final static String MAX_PORT = "_max";

    private SecRuleNameDecoder() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static SubjectName getSubjectName(NeutronSecurityRule secRule) {
        RuleName ruleName = SecRuleNameDecoder.getRuleName(secRule);
        return new SubjectName(ruleName);
    }

    public static RuleName getRuleName(NeutronSecurityRule secRule) {
        ClassifierName classifierRefName = SecRuleNameDecoder.getClassifierRefName(secRule);
        String ruleName = new StringBuilder(MappingUtils.ACTION_ALLOW.getName().getValue())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER).append(classifierRefName.getValue()).toString();
        return new RuleName(ruleName);
    }

    public static ClassifierName getClassifierRefName(NeutronSecurityRule secRule) {
        Direction direction = SecRuleEntityDecoder.getDirection(secRule);
        ClassifierName classifierInstanceName = getClassifierInstanceName(secRule);
        String crName = new StringBuilder().append(direction.name())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(classifierInstanceName.getValue())
            .toString();
        return new ClassifierName(crName);
    }

    public static ClassifierName getClassifierInstanceName(NeutronSecurityRule secRule) {
        StringBuilder keyBuilder = new StringBuilder();
        Integer portMin = secRule.getSecurityRulePortMin();
        Integer portMax = secRule.getSecurityRulePortMax();
        if (portMin != null && portMax != null) {
            keyBuilder.append(L4Classifier.DEFINITION.getName().getValue());
            if (portMin.equals(portMax)) {
                keyBuilder.append(MappingUtils.NAME_DELIMETER)
                    .append(L4Classifier.DST_PORT_PARAM)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMin.longValue());
            } else {
                keyBuilder.append(MappingUtils.NAME_DELIMETER)
                    .append(L4Classifier.DST_PORT_RANGE_PARAM)
                    .append(MIN_PORT)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMin.longValue())
                    .append(MAX_PORT)
                    .append(MappingUtils.NAME_VALUE_DELIMETER)
                    .append(portMax.longValue());
            }
        }
        String protocol = secRule.getSecurityRuleProtocol();
        if (!Strings.isNullOrEmpty(protocol)) {
            if (keyBuilder.length() > 0) {
                keyBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER);
            }
            keyBuilder.append(IpProtoClassifier.DEFINITION.getName().getValue())
                .append(MappingUtils.NAME_VALUE_DELIMETER)
                .append(protocol);
        }
        String ethertype = secRule.getSecurityRuleEthertype();
        if (!Strings.isNullOrEmpty(ethertype)) {
            if (keyBuilder.length() > 0) {
                keyBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER);
            }
            keyBuilder.append(EtherTypeClassifier.DEFINITION.getName().getValue())
                .append(MappingUtils.NAME_VALUE_DELIMETER)
                .append(ethertype);
        }
        return new ClassifierName(keyBuilder.toString());
    }

    public static ClauseName getClauseName(NeutronSecurityRule secRule) {
        String remoteIpPrefix = secRule.getSecurityRuleRemoteIpPrefix();
        SubjectName subjectName = getSubjectName(secRule);
        if (Strings.isNullOrEmpty(remoteIpPrefix)) {
            return new ClauseName(subjectName);
        }
        return new ClauseName(
                subjectName.getValue() + MappingUtils.NAME_DOUBLE_DELIMETER + remoteIpPrefix.replace('/', '_'));
    }

}
