package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;

public class RuleGroupTest {

    private static final int ORDER = 5;
    private static final int ORDER_LESSER = 3;
    private static final int ORDER_BIGGER = 8;
    private static final String SN_VALUE = "sn_value";
    private static final String SN_OTHER = "sn_other";
    private static final String SN_COMES_BEFORE = "sn_armadillo";
    private static final String SN_COMES_AFTER = "sn_zebra";
    private static final String RULE_NAME = "ruleName";
    private static final String RULE_OTHER = "ruleOther";
    private static final String TENANT_ID = "tenantId";
    private static final String CONTRACT_ID = "contractId";

    private List<Rule> rules;
    private Integer order;
    private Tenant contractTenant;
    private Contract contract;
    private SubjectName subjectName;

    private RuleGroup ruleGroup;

    @Before
    public void init() {
        Rule rule = new RuleBuilder().setName(new RuleName(RULE_NAME)).build();
        rules = Collections.singletonList(rule);
        order = ORDER;
        contractTenant = new TenantBuilder().setId(new TenantId(TENANT_ID)).build();
        contract = new ContractBuilder().setId(new ContractId(CONTRACT_ID)).build();
        subjectName = new SubjectName(SN_VALUE);

        ruleGroup = new RuleGroup(rules, order, contractTenant, contract, subjectName);
    }

    @Test
    public void testConstructor() {
        assertNotNull(ruleGroup);
        assertEquals(rules, ruleGroup.getRules());
        assertEquals(order, ruleGroup.getOrder());
        assertEquals(contractTenant, ruleGroup.getContractTenant());
        assertEquals(contract, ruleGroup.getRelatedContract());
        assertEquals(subjectName, ruleGroup.getRelatedSubject());
    }

    @Test
    public void testEquals() {
        assertTrue(ruleGroup.equals(ruleGroup));
        assertFalse(ruleGroup.equals(null));
        assertFalse(ruleGroup.equals(new Object()));

        RuleGroup other;
        Integer orderOther = 3;
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subjectName);
        assertFalse(ruleGroup.equals(other));

        Rule ruleOther = new RuleBuilder().setName(new RuleName(RULE_OTHER)).build();
        List<Rule> rulesOther = Collections.singletonList(ruleOther);
        other = new RuleGroup(rulesOther, order, contractTenant, contract, subjectName);
        assertFalse(ruleGroup.equals(other));

        SubjectName subjectNameOther = new SubjectName(SN_OTHER);
        other = new RuleGroup(rules, order, contractTenant, contract, subjectNameOther);
        assertFalse(ruleGroup.equals(other));

        other = new RuleGroup(rules, order, contractTenant, contract, this.subjectName);
        assertTrue(ruleGroup.equals(other));

        ruleGroup = new RuleGroup(rules, null, contractTenant, contract, this.subjectName);
        assertFalse(ruleGroup.equals(other));
        other = new RuleGroup(rules, null, contractTenant, contract, this.subjectName);
        assertTrue(ruleGroup.equals(other));

        other = new RuleGroup(rules, order, contractTenant, contract, this.subjectName);
        ruleGroup = new RuleGroup(rules, order, contractTenant, contract, null);
        assertFalse(ruleGroup.equals(other));
        other = new RuleGroup(rules, order, contractTenant, contract, null);
        assertTrue(ruleGroup.equals(other));
    }

    @Test
    public void testCompareTo() {
        RuleGroup other;
        other = new RuleGroup(rules, order, contractTenant, contract, subjectName);
        assertEquals(0, ruleGroup.compareTo(other));

        Integer orderOther;
        orderOther = ORDER_LESSER;
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subjectName);
        assertEquals(1, ruleGroup.compareTo(other));

        orderOther = ORDER_BIGGER;
        other = new RuleGroup(rules, orderOther, contractTenant, contract, subjectName);
        assertEquals(-1, ruleGroup.compareTo(other));

        SubjectName subjectNameComesBefore = new SubjectName(SN_COMES_BEFORE);
        SubjectName subjectNameComesLater = new SubjectName(SN_COMES_AFTER);

        other = new RuleGroup(rules, order, contractTenant, contract, subjectNameComesBefore);
        assertEquals(1, ruleGroup.compareTo(other));

        other = new RuleGroup(rules, order, contractTenant, contract, subjectNameComesLater);
        assertEquals(-1, ruleGroup.compareTo(other));
    }

    @Test
    public void testToString() {
        String string = ruleGroup.toString();
        assertNotNull(string);
        assertFalse(string.isEmpty());
        assertTrue(string.contains(rules.toString()));
        assertTrue(string.contains(order.toString()));
    }
}
