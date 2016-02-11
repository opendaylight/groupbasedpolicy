package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

public class SingleRuleContractTest {

    @Test
    public void testConstructorAndGetters() {
        NeutronSecurityRule secRule = NeutronEntityFactory.securityRuleWithoutGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", NeutronUtils.IPv4,
                NeutronUtils.INGRESS, NeutronUtils.TCP, 8010, 8020);
        int subjectAndRuleOrder = 1;
        SingleRuleContract singleRuleContract = new SingleRuleContract(secRule, subjectAndRuleOrder, new Description(
                "contractDescription"), MappingUtils.ACTION_REF_ALLOW);

        SingleClassifierRule singleClsfRule = singleRuleContract.getSingleClassifierRule();
        assertNotNull(singleClsfRule);

        ClassifierInstance clsfInstance = singleClsfRule.getClassifierInstance();
        assertNotNull(clsfInstance);
        assertEquals(clsfInstance, SecRuleEntityDecoder.getClassifierInstance(secRule));

        ClassifierRef clsfRef = singleClsfRule.getClassifierRef();
        assertNotNull(clsfRef);
        assertEquals(clsfRef, SecRuleEntityDecoder.getClassifierRef(secRule));

        Rule rule = singleClsfRule.getRule();
        assertNotNull(rule);
        PolicyAssert.assertRule(rule, secRule, subjectAndRuleOrder);

        assertEquals(rule, singleRuleContract.getRule());

        Subject subject = singleRuleContract.getSubject();
        assertNotNull(subject);
        PolicyAssert.assertSubjectWithOneRule(subject, secRule);

        Clause clause = singleRuleContract.getClause();
        assertNotNull(clause);
        PolicyAssert.assertClauseWithOneSubject(clause, secRule);

        Contract contract = singleRuleContract.getContract();
        assertNotNull(contract);
        PolicyAssert.assertContract(contract, secRule);
    }
}
