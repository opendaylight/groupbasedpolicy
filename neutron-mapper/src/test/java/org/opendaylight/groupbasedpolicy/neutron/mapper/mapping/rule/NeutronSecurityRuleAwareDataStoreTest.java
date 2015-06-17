package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.SecGroupDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * END 2 END TESTING - inputs are Neutron entities and expected outputs are GBP entities in
 * datastore
 */
public class NeutronSecurityRuleAwareDataStoreTest extends GbpDataBrokerTest {

    @Test
    public final void testAddNeutronSecurityRule_rulesWithRemoteIpPrefix() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        NeutronSecurityRule goldInIpv4 = createSecRuleWithEtherType("166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp, null);
        NeutronSecurityRule goldOutIpv4 = createSecRuleWithEtherType("dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp, null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverIn80Tcp10_1_1_0 = createSecRuleWithEtherType("9dbb533d-d9b2-4dc9-bae7-ee60c8df184d",
                tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp, null);
        serverIn80Tcp10_1_1_0.setSecurityRuleProtocol(NeutronUtils.TCP);
        serverIn80Tcp10_1_1_0.setSecurityRulePortMin(80);
        serverIn80Tcp10_1_1_0.setSecurityRulePortMax(80);
        serverIn80Tcp10_1_1_0.setSecurityRuleRemoteIpPrefix("10.1.1.0/24");
        NeutronSecurityRule serverInIp20_1_1_0 = createSecRuleWithEtherType("adf7e558-de47-4f9e-a9b8-96e19db5d1ac",
                tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp, null);
        serverInIp20_1_1_0.setSecurityRuleRemoteIpPrefix("20.1.1.0/24");
        NeutronSecurityRule serverOutIpv4 = createSecRuleWithEtherType("8b9c48d3-44a8-46be-be35-6f3237d98071", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp, null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(createSecGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(createSecGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80Tcp10_1_1_0, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIp20_1_1_0, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Tenant> potentialTenant =
                rwTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.tenantIid(tenantId)).get();
        assertTrue(potentialTenant.isPresent());
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, serverOutIpv4);
        potentialContract = rwTx
            .read(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.contractIid(tenantId, new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContractWithEic(contract, serverIn80Tcp10_1_1_0);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContractWithEic(contract, serverInIp20_1_1_0);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        assertConsumerNamedSelectors(epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(goldOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID()),
                        new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID()),
                        new ContractId(serverOutIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        assertConsumerNamedSelectors(epg,
                ImmutableSet.of(new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID()),
                        new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID()),
                        new ContractId(goldInIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddNeutronSecurityRule_rulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        NeutronSecurityRule goldInIpv4 = createSecRuleWithEtherType("166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp, null);
        NeutronSecurityRule goldOutIpv4 = createSecRuleWithEtherType("dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp, null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverOutIpv4 = createSecRuleWithEtherType("8b9c48d3-44a8-46be-be35-6f3237d98071", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp, null);
        NeutronSecurityRule serverInIpv4 = createSecRuleWithEtherType("adf7e558-de47-4f9e-a9b8-96e19db5d1ac", tenant,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp, null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(createSecGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(createSecGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, serverInIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        assertConsumerNamedSelectors(epg, ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                new ContractId(goldOutIpv4.getSecurityRuleUUID()), new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                new ContractId(serverInIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        assertConsumerNamedSelectors(epg, ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                new ContractId(goldOutIpv4.getSecurityRuleUUID()), new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                new ContractId(serverInIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddNeutronSecurityRule_asymmetricRulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        NeutronSecurityRule goldInIpv4 = createSecRuleWithEtherType("166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp, null);
        NeutronSecurityRule goldOutIpv4 = createSecRuleWithEtherType("dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp, null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverOutIpv4 = createSecRuleWithEtherType("8b9c48d3-44a8-46be-be35-6f3237d98071", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp, null);
        NeutronSecurityRule serverIn80TcpIpv4 = createSecRuleWithEtherType("adf7e558-de47-4f9e-a9b8-96e19db5d1ac",
                tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp, null);
        serverIn80TcpIpv4.setSecurityRuleProtocol(NeutronUtils.TCP);
        serverIn80TcpIpv4.setSecurityRulePortMin(80);
        serverIn80TcpIpv4.setSecurityRulePortMax(80);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(createSecGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(createSecGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80TcpIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, serverIn80TcpIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        assertConsumerNamedSelectors(epg, ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                new ContractId(goldOutIpv4.getSecurityRuleUUID()), new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp)))
            .get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        assertConsumerNamedSelectors(epg, ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddNeutronSecurityRule_defaultSecGrp() throws Exception {
        String tenant = "111aaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String defaultSecGrp = "111fffff-ffff-ffff-ffff-ffffffffffff";
        NeutronSecurityRule defaultInIpv4Default = createSecRuleWithEtherType("111ccccc-111c-cccc-cccc-cccccccccccc",
                tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, defaultSecGrp, defaultSecGrp);
        NeutronSecurityRule defaultInIpv6Default = createSecRuleWithEtherType("222ccccc-111c-cccc-cccc-cccccccccccc",
                tenant, NeutronUtils.IPv6, NeutronUtils.INGRESS, defaultSecGrp, defaultSecGrp);
        NeutronSecurityRule defaultOutIpv4 = createSecRuleWithEtherType("333ccccc-111c-cccc-cccc-cccccccccccc", tenant,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, defaultSecGrp, null);
        NeutronSecurityRule defaultOutIpv6 = createSecRuleWithEtherType("444ccccc-111c-cccc-cccc-cccccccccccc", tenant,
                NeutronUtils.IPv6, NeutronUtils.EGRESS, defaultSecGrp, null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(createSecGroup(defaultSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(defaultInIpv4Default, rwTx);
        ruleAware.addNeutronSecurityRule(defaultInIpv6Default, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv6, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv4Default.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        assertContract(contract, defaultInIpv4Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv6Default.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, defaultInIpv6Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv4.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, defaultOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv6.getSecurityRuleUUID())))
            .get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        assertContract(contract, defaultOutIpv6);
    }

    private final void assertContractWithEic(Contract contract, NeutronSecurityRule secRule) {
        assertEquals(new ContractId(secRule.getSecurityRuleUUID()), contract.getId());
        assertNull(contract.getQuality());
        assertNull(contract.getTarget());
        assertOneClauseWithEicWithOneSubject(contract, secRule);
        assertOneSubjectWithOneRule(contract, secRule);
    }

    private final void assertOneClauseWithEicWithOneSubject(Contract contract, NeutronSecurityRule secRule) {
        Clause clause = assertOneItem(contract.getClause());
        assertNull(clause.getAnyMatchers());
        Preconditions.checkArgument(!Strings.isNullOrEmpty(secRule.getSecurityRuleRemoteIpPrefix()));
        IpPrefix expectedIpPrefix = Utils.createIpPrefix(secRule.getSecurityRuleRemoteIpPrefix());
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

    private final void assertContract(Contract contract, NeutronSecurityRule secRule) {
        assertEquals(new ContractId(secRule.getSecurityRuleUUID()), contract.getId());
        assertNull(contract.getQuality());
        assertNull(contract.getTarget());
        assertOneClauseWithOneSubject(contract, secRule);
        assertOneSubjectWithOneRule(contract, secRule);
    }

    private final void assertOneClauseWithOneSubject(Contract contract, NeutronSecurityRule secRule) {
        Clause clause = assertOneItem(contract.getClause());
        assertNull(clause.getAnyMatchers());
        assertNull(clause.getConsumerMatchers());
        assertNull(clause.getProviderMatchers());
        SubjectName subjectRef = assertOneItem(clause.getSubjectRefs());
        assertEquals(SecRuleNameDecoder.getSubjectName(secRule), subjectRef);
    }

    private final void assertOneSubjectWithOneRule(Contract contract, NeutronSecurityRule secRule) {
        Subject subject = assertOneItem(contract.getSubject());
        assertEquals(SecRuleNameDecoder.getSubjectName(secRule), subject.getName());
        Rule rule = assertOneItem(subject.getRule());
        assertEquals(SecRuleNameDecoder.getRuleName(secRule), rule.getName());
        ActionRef actionRef = assertOneItem(rule.getActionRef());
        assertEquals(MappingUtils.ACTION_ALLOW.getName(), actionRef.getName());
        ClassifierRef classifierRef = assertOneItem(rule.getClassifierRef());
        assertEquals(SecRuleNameDecoder.getClassifierRefName(secRule), classifierRef.getName());
        assertEquals(SecRuleNameDecoder.getClassifierInstanceName(secRule), classifierRef.getInstanceName());
        assertEquals(SecRuleEntityDecoder.getDirection(secRule), classifierRef.getDirection());
    }

    private final <T> T assertOneItem(Collection<T> c) {
        assertNotNull(c);
        assertTrue(c.size() == 1);
        return c.iterator().next();
    }

    private final void assertConsumerNamedSelectors(EndpointGroup epg, Set<ContractId> expectedContracts) {
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

    private final NeutronSecurityRule createSecRuleWithEtherType(String id, String tenant, String etherType,
            String direction, String ownerGroupId, String remoteGroupId) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRuleUUID(id);
        secRule.setSecurityRuleTenantID(tenant);
        secRule.setSecurityRuleEthertype(etherType);
        secRule.setSecurityRuleDirection(direction);
        secRule.setSecurityRuleGroupID(ownerGroupId);
        secRule.setSecurityRemoteGroupID(remoteGroupId);
        return secRule;
    }

    private final NeutronSecurityGroup createSecGroup(String id, String tenant) {
        NeutronSecurityGroup secGrp = new NeutronSecurityGroup();
        secGrp.setSecurityGroupUUID(id);
        secGrp.setSecurityGroupTenantID(tenant);
        return secGrp;
    }
}
