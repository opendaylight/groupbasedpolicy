package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;

public class RuleGroupTest {

    private Rule rule;
    private List<Rule> rules;
    private Integer order;
    private Tenant contractTenant;
    private Contract contract;
    private SubjectName subject;
    private String subjectValue;

    private RuleGroup ruleGroup;

    @Before
    public void initialisation() {
        rule = mock(Rule.class);
        rules = Arrays.asList(rule);
        order = Integer.valueOf(5);
        contractTenant = mock(Tenant.class);
        contract = mock(Contract.class);
        subject = mock(SubjectName.class);

        subjectValue = "value";
        when(subject.getValue()).thenReturn(subjectValue);

        ruleGroup = new RuleGroup(rules, order, contractTenant, contract, subject);
    }

    @Test
    public void constructorTest() {
        Assert.assertNotNull(ruleGroup);
        Assert.assertEquals(rules, ruleGroup.getRules());
        Assert.assertEquals(order, ruleGroup.getOrder());
        Assert.assertEquals(contractTenant, ruleGroup.getContractTenant());
        Assert.assertEquals(contract, ruleGroup.getRelatedContract());
        Assert.assertEquals(subject, ruleGroup.getRelatedSubject());
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(ruleGroup.equals(ruleGroup));
        Assert.assertFalse(ruleGroup.equals(null));
        Assert.assertFalse(ruleGroup.equals(new Object()));

        RuleGroup other;
        Integer orderOther = Integer.valueOf(3);
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subject);
        Assert.assertFalse(ruleGroup.equals(other));

        Rule ruleOther = mock(Rule.class);
        List<Rule> rulesOther = Arrays.asList(ruleOther);
        other = new RuleGroup(rulesOther, order, contractTenant, contract, subject);
        Assert.assertFalse(ruleGroup.equals(other));

        SubjectName subjectOther = mock(SubjectName.class);
        other = new RuleGroup(rules, order, contractTenant, contract, subjectOther);
        Assert.assertFalse(ruleGroup.equals(other));

        other = new RuleGroup(rules, order, contractTenant, contract, subject);
        Assert.assertTrue(ruleGroup.equals(other));

        ruleGroup = new RuleGroup(rules, null, contractTenant, contract, subject);
        Assert.assertFalse(ruleGroup.equals(other));
        other = new RuleGroup(rules, null, contractTenant, contract, subject);
        Assert.assertTrue(ruleGroup.equals(other));

        other = new RuleGroup(rules, order, contractTenant, contract, subject);
        ruleGroup = new RuleGroup(rules, order, contractTenant, contract, null);
        Assert.assertFalse(ruleGroup.equals(other));
        other = new RuleGroup(rules, order, contractTenant, contract, null);
        Assert.assertTrue(ruleGroup.equals(other));
    }

    @Test
    public void compareToTest() {
        RuleGroup other;
        other = new RuleGroup(rules, order, contractTenant, contract, subject);
        Assert.assertEquals(0, ruleGroup.compareTo(other));

        Integer orderOther;
        orderOther = Integer.valueOf(3);
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subject);
        Assert.assertEquals(1, ruleGroup.compareTo(other));

        orderOther = Integer.valueOf(8);
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subject);
        Assert.assertEquals(-1, ruleGroup.compareTo(other));

        SubjectName subjectOther = mock(SubjectName.class);

        when(subjectOther.getValue()).thenReturn("valu");
        other = new RuleGroup(rules, order, contractTenant, contract, subjectOther);
        Assert.assertEquals(1, ruleGroup.compareTo(other));

        when(subjectOther.getValue()).thenReturn("valuee");
        other = new RuleGroup(rules, order, contractTenant, contract, subjectOther);
        Assert.assertEquals(-1, ruleGroup.compareTo(other));
    }

    @Test
    public void toStringTest() {
        String string = ruleGroup.toString();
        Assert.assertNotNull(string);
        Assert.assertFalse(string.isEmpty());
        Assert.assertTrue(string.contains(rules.toString()));
        Assert.assertTrue(string.contains(order.toString()));
    }
}
