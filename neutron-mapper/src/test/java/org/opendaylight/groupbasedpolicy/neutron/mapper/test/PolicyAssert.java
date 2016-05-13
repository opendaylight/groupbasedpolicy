package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleEntityDecoder;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleNameDecoder;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class PolicyAssert {

    private PolicyAssert() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    // asserts for tenant

    public static void assertTenantExists(DataBroker dataBroker, String tenantId) throws Exception {
        Optional<Tenant> tenant = ConfigDataStoreReader.readTenant(dataBroker, tenantId);
        assertTrue(tenant.isPresent());
    }

    public static void assertTenantNotExists(DataBroker dataBroker, String tenantId) throws Exception {
        Optional<Tenant> tenant = ConfigDataStoreReader.readTenant(dataBroker, tenantId);
        assertFalse(tenant.isPresent());
    }

    // asserts for contract

    public static void assertContractExists(DataBroker dataBroker, String tenantId, String contractId) throws Exception {
        Optional<Contract> contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, contractId);
        assertTrue(contract.isPresent());
    }

    public static void assertContractNotExists(DataBroker dataBroker, String tenantId, String contractId)
            throws Exception {
        Optional<Contract> contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, contractId);
        assertFalse(contract.isPresent());
    }

    public static void assertContractCount(DataBroker dataBroker, String tenantId, int expectedCount) throws Exception {
        Optional<Tenant> tenant = ConfigDataStoreReader.readTenant(dataBroker, tenantId);
        assertTrue(tenant.isPresent());
        Policy policy = tenant.get().getPolicy();
        if (policy == null) {
            assertEquals(expectedCount, 0);
        } else {
            List<Contract> contracts = policy.getContract();
            if (contracts != null) {
                assertEquals(expectedCount, policy.getContract().size());
            } else {
                assertEquals(expectedCount, 0);
            }
        }
    }

    public static void assertContractWithEic(Contract contract, SecurityRule secRule) {
        assertEquals(new ContractId(secRule.getId().getValue()), contract.getId());
        assertNull(contract.getQuality());
        assertNull(contract.getTarget());
        assertOneClauseWithEicWithOneSubject(contract, secRule);
        PolicyAssert.assertOneSubjectWithOneRule(contract, secRule);
    }

    private static void assertOneClauseWithEicWithOneSubject(Contract contract, SecurityRule secRule) {
        Clause clause = assertOneItem(contract.getClause());
        assertNull(clause.getAnyMatchers());
        IpPrefix expectedIpPrefix = secRule.getRemoteIpPrefix();
        assertNotNull(clause.getConsumerMatchers());
        IpPrefix ipPrefix = clause.getConsumerMatchers()
            .getEndpointIdentificationConstraints()
            .getL3EndpointIdentificationConstraints()
            .getPrefixConstraint()
            .get(0)
            .getIpPrefix();
        assertEquals(expectedIpPrefix, ipPrefix);
        SubjectName subjectRef = assertOneItem(clause.getSubjectRefs());
        assertEquals(SecRuleNameDecoder.getSubjectName(secRule), subjectRef);
    }

    public static void assertContract(Contract contract, SecurityRule secRule) {
        assertEquals(new ContractId(secRule.getId().getValue()), contract.getId());
        assertNull(contract.getQuality());
        assertNull(contract.getTarget());
        assertOneClauseWithOneSubject(contract, secRule);
        assertOneSubjectWithOneRule(contract, secRule);
    }

    private static void assertOneClauseWithOneSubject(Contract contract, SecurityRule secRule) {
        Clause clause = assertOneItem(contract.getClause());
        assertClauseWithOneSubject(clause, secRule);
    }

    private static void assertOneSubjectWithOneRule(Contract contract, SecurityRule secRule) {
        Subject subject = assertOneItem(contract.getSubject());
        assertSubjectWithOneRule(subject, secRule);
    }

    public static void assertEndpointGroupExists(DataBroker dataBroker, String tenantId, String endpointGroupId)
            throws Exception {
        Optional<EndpointGroup> epg = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, endpointGroupId);
        assertTrue(epg.isPresent());
    }

    public static void assertEndpointGroupNotExists(DataBroker dataBroker, String tenantId, String endpointGroupId)
            throws Exception {
        Optional<EndpointGroup> epg = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, endpointGroupId);
        assertFalse(epg.isPresent());
    }

    public static void assertEndpointGroupCount(DataBroker dataBroker, String tenantId, int expectedCount)
            throws Exception {
        Optional<Tenant> tenant = ConfigDataStoreReader.readTenant(dataBroker, tenantId);
        assertTrue(tenant.isPresent());
        Policy policy = tenant.get().getPolicy();
        if (policy == null) {
            assertEquals(expectedCount, 0);
        } else {
            List<EndpointGroup> endpointGroups = policy.getEndpointGroup();
            if (endpointGroups != null) {
                assertEquals(expectedCount, endpointGroups.size());
            } else {
                assertEquals(expectedCount, 0);
            }
        }
    }

    public static void assertIntraGroupPolicy(DataBroker dataBroker, String tenantId, String endpointGroupId,
            IntraGroupPolicy intraGroupPolicy) throws Exception {
        Optional<EndpointGroup> epg = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, endpointGroupId);
        assertTrue(epg.isPresent());
        assertEquals(intraGroupPolicy, epg.get().getIntraGroupPolicy());
    }

    // asserts for endpoint group selectors

    public static void assertNoProviderNamedSelectors(DataBroker dataBroker, String tenantId, String secGroupId)
            throws Exception {
        Optional<EndpointGroup> epg = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId);
        assertTrue(epg.isPresent());
        List<ProviderNamedSelector> selectors = epg.get().getProviderNamedSelector();
        assertTrue(selectors == null || selectors.isEmpty());
    }

    public static void assertNoConsumerNamedSelectors(DataBroker dataBroker, String tenantId, String secGroupId)
            throws Exception {
        Optional<EndpointGroup> epg = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId);
        assertTrue(epg.isPresent());
        List<ConsumerNamedSelector> selectors = epg.get().getConsumerNamedSelector();
        assertTrue(selectors == null || selectors.isEmpty());
    }

    public static void assertProviderNamedSelectors(EndpointGroup epg, Set<ContractId> expectedContracts) {
        Preconditions.checkNotNull(expectedContracts);
        assertNotNull(epg.getProviderNamedSelector());
        int numberOfContracts = 0;
        for (ProviderNamedSelector pns : epg.getProviderNamedSelector()) {
            assertNotNull(pns.getContract());
            numberOfContracts += pns.getContract().size();
            for (ContractId contractId : pns.getContract()) {
                assertTrue(expectedContracts.contains(contractId));
            }
        }
        assertEquals(expectedContracts.size(), numberOfContracts);
    }

    public static void assertConsumerNamedSelectors(EndpointGroup epg, Set<ContractId> expectedContracts) {
        Preconditions.checkNotNull(expectedContracts);
        assertNotNull(epg.getConsumerNamedSelector());
        int numberOfContracts = 0;
        for (ConsumerNamedSelector cns : epg.getConsumerNamedSelector()) {
            assertNotNull(cns.getContract());
            numberOfContracts += cns.getContract().size();
            for (ContractId contractId : cns.getContract()) {
                assertTrue(expectedContracts.contains(contractId));
            }
        }
        assertEquals(expectedContracts.size(), numberOfContracts);
    }

    // asserts for classifier

    public static void assertClassifierInstanceExists(DataBroker dataBroker, SecurityRule secRule)
            throws Exception {
        ClassifierInstance clsfInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Optional<ClassifierInstance> readClsfInstance = ConfigDataStoreReader.readClassifierInstance(dataBroker,
                secRule.getTenantId().getValue(), clsfInstance.getName());
        assertTrue(readClsfInstance.isPresent());
    }

    public static void assertClassifierInstanceExists(DataBroker dataBroker, String tenantId, String classifierName)
            throws Exception {
        Optional<ClassifierInstance> classifierInstance = ConfigDataStoreReader.readClassifierInstance(dataBroker,
                tenantId, new ClassifierName(classifierName));
        assertTrue(classifierInstance.isPresent());
    }

    public static void assertClassifierInstanceNotExists(DataBroker dataBroker, SecurityRule secRule)
            throws Exception {
        ClassifierInstance clsfInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Optional<ClassifierInstance> readClsfInstance = ConfigDataStoreReader.readClassifierInstance(dataBroker,
                secRule.getTenantId().getValue(), clsfInstance.getName());
        assertFalse(readClsfInstance.isPresent());
    }

    // asserts for action

    public static void assertActionInstanceExists(DataBroker dataBroker, String tenantId, ActionName actionName)
            throws Exception {
        Optional<ActionInstance> actionInstance = ConfigDataStoreReader.readActionInstance(dataBroker, tenantId,
                actionName);
        assertTrue(actionInstance.isPresent());
    }

    public static void assertActionInstanceNotExists(DataBroker dataBroker, String tenantId, ActionName actionName)
            throws Exception {
        Optional<ActionInstance> actionInstance = ConfigDataStoreReader.readActionInstance(dataBroker, tenantId,
                actionName);
        assertFalse(actionInstance.isPresent());
    }

    // asserts for clause

    public static void assertClauseWithOneSubject(Clause clause, SecurityRule secRule) {
        assertNull(clause.getAnyMatchers());
        assertNull(clause.getConsumerMatchers());
        assertNull(clause.getProviderMatchers());
        SubjectName subjectRef = assertOneItem(clause.getSubjectRefs());
        assertEquals(SecRuleNameDecoder.getSubjectName(secRule), subjectRef);
    }

    public static void assertClauseExists(DataBroker dataBroker, String tenantId, String contractId, String clauseName) {
        Optional<Clause> clause = ConfigDataStoreReader.readClause(dataBroker, tenantId, contractId, clauseName);
        assertTrue(clause.isPresent());
    }

    // asserts for subject

    public static void assertSubjectWithOneRule(Subject subject, SecurityRule secRule) {
        assertEquals(SecRuleNameDecoder.getSubjectName(secRule), subject.getName());
        Rule rule = assertOneItem(subject.getRule());
        assertRule(rule, secRule);
    }

    // asserts for rule

    public static void assertRule(Rule rule, SecurityRule secRule) {
        assertEquals(SecRuleNameDecoder.getRuleName(secRule), rule.getName());
        ActionRef actionRef = assertOneItem(rule.getActionRef());
        assertEquals(MappingUtils.ACTION_ALLOW.getName(), actionRef.getName());
        ClassifierRef classifierRef = assertOneItem(rule.getClassifierRef());
        assertEquals(SecRuleNameDecoder.getClassifierRefName(secRule), classifierRef.getName());
        assertEquals(SecRuleNameDecoder.getClassifierInstanceName(secRule), classifierRef.getInstanceName());
        assertEquals(SecRuleEntityDecoder.getDirection(secRule), classifierRef.getDirection());
    }

    public static void assertRule(Rule rule, SecurityRule secRule, int order) {
        assertRule(rule, secRule);
        assertEquals(order, rule.getOrder().intValue());
    }

    private static <T> T assertOneItem(Collection<T> c) {
        assertNotNull(c);
        assertTrue(c.size() == 1);
        return c.iterator().next();
    }

    // asserts for selector

    public static void assertConsumerNamedSelectorExists(DataBroker dataBroker, String tenantId, String egId,
                                                         String selectorName) {
        Optional<ConsumerNamedSelector> potentialCns = ConfigDataStoreReader.readConsumerNamedSelector(dataBroker,
                tenantId, egId, selectorName);
        assertTrue(potentialCns.isPresent());
    }

}
