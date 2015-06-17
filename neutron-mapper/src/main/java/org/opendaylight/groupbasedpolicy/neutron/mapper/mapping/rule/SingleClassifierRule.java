package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;

import com.google.common.collect.ImmutableList;

@Immutable
public class SingleClassifierRule {

    private final ClassifierInstance classifierInstance;
    private final ClassifierRef classifierRef;
    private final Rule rule;

    public SingleClassifierRule(NeutronSecurityRule secRule, int ruleOrder) {
        classifierInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        classifierRef = SecRuleEntityDecoder.getClassifierRef(secRule);
        rule = createRule(ruleOrder, secRule);
    }

    private Rule createRule(int order, NeutronSecurityRule secRule) {
        return new RuleBuilder().setName(SecRuleNameDecoder.getRuleName(secRule))
            .setOrder(order)
            .setActionRef(MappingUtils.ACTION_REF_ALLOW)
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
