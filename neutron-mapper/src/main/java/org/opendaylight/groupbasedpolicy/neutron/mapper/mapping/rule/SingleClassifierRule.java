/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

@Immutable
public class SingleClassifierRule {

    private final ClassifierInstance classifierInstance;
    private final ClassifierRef classifierRef;
    private final Rule rule;

    public SingleClassifierRule(SecurityRule secRule, int ruleBaseOrder, ActionChoice action) {
        classifierInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        classifierRef = SecRuleEntityDecoder.getClassifierRef(secRule);
        rule = createRule(
                updateOrderBasedOn(classifierInstance, action, ruleBaseOrder),
                secRule,
                action);
    }

    public ClassifierInstance getClassifierInstance() {
        return classifierInstance;
    }

    public ClassifierRef getClassifierRef() {
        return classifierRef;
    }

    public Rule getRule() {
        return rule;
    }

    private Rule createRule(int order, SecurityRule secRule, ActionChoice action) {
        return new RuleBuilder().setName(SecRuleNameDecoder.getRuleName(secRule))
            .setOrder(order)
            .setActionRef(ImmutableList.of(SecRuleEntityDecoder.createActionRefFromActionChoice(action)))
            .setClassifierRef(ImmutableList.<ClassifierRef>of(classifierRef))
            .build();
    }

    /**
     * Increases initial order of potential {@link Rule} containing single
     * {@link ClassifierInstance} based on parameter values of the instance.
     * <br>
     * The following section describes how L4 port specification is prioritized
     * from most specific to least specific:<br>
     * 1) src port AND dst port<br>
     * 2) (src port range AND dst port) OR (src port AND dst port range)<br>
     * 3) src port range AND dst port range<br>
     * 4) src port OR dst port<br>
     * 5) src port range OR dst port range<br>
     *
     * @param ci instance object of {@link ClassifierInstance}
     * @param baseOrder initial order value calculated for the {@link Rule}
     * @return value of incremented order based on parameter values.
     *
     * @see ParameterValue
     */
    @VisibleForTesting
    static int updateOrderBasedOn(ClassifierInstance ci, ActionChoice action, Integer baseOrder) {
        int delta = 0;
        if (ci.getClassifierDefinitionId().equals(EtherTypeClassifierDefinition.ID)) {
            delta = 350;
        } else if (ci.getClassifierDefinitionId().equals(IpProtoClassifierDefinition.ID)) {
            delta = 300;
        } else if (ci.getClassifierDefinitionId().equals(L4ClassifierDefinition.ID)) {
            delta = 200;
            List<ParameterValue> parameterValue = ci.getParameterValue();
            for (ParameterValue pv : parameterValue) {
                // SRC/DST_PORT_PARAM is considered to be more
                // specific than SRC/DST_PORT_RANGE_PARAM.
                if (isSrcOrDstPortParam(pv)) {
                    delta -= 30;
                } else if (isRangePortParam(pv)) {
                    delta -= pv.getRangeValue().getMin().equals(pv.getRangeValue().getMax()) ? 30 : 20;
                }
            }
        }
        if (action instanceof SfcActionCase) {
            delta -= 5;
        }
        // more specific CIs should have lower order calculated.
        return baseOrder + delta;
    }

    private static boolean isRangePortParam(ParameterValue pv) {
        String pvName = pv.getName().getValue();
        return pv.getRangeValue() != null && (pvName.equals(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM)
            || pvName.equals(L4ClassifierDefinition.DST_PORT_RANGE_PARAM));
    }

    private static boolean isSrcOrDstPortParam(ParameterValue pv) {
        String pvName = pv.getName().getValue();
        return pv.getIntValue() != null && (pvName.equals(L4ClassifierDefinition.SRC_PORT_PARAM)
            || pvName.equals(L4ClassifierDefinition.DST_PORT_PARAM));
    }

}
