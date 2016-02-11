/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;


import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

import com.google.common.collect.ImmutableList;

@Immutable
public class SingleClassifierRule {

    private final ClassifierInstance classifierInstance;
    private final ClassifierRef classifierRef;
    private final Rule rule;

    public SingleClassifierRule(NeutronSecurityRule secRule, int ruleOrder, List<ActionRef> actions) {
        classifierInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        classifierRef = SecRuleEntityDecoder.getClassifierRef(secRule);
        rule = createRule(ruleOrder, secRule, actions);
    }

    private Rule createRule(int order, NeutronSecurityRule secRule, List<ActionRef> actions) {
        return new RuleBuilder().setName(SecRuleNameDecoder.getRuleName(secRule))
            .setOrder(order)
            .setActionRef(actions)
            .setClassifierRef(ImmutableList.of(classifierRef))
            .build();
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

}
