package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.SecGroupDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.ConfigDataStoreReader;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;

import com.google.common.base.Optional;
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
        NeutronSecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp,
                null);
        NeutronSecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverIn80Tcp10_1_1_0 = NeutronEntityFactory.securityRuleWithEtherType(
                "9dbb533d-d9b2-4dc9-bae7-ee60c8df184d", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp,
                null);
        serverIn80Tcp10_1_1_0.setSecurityRuleProtocol(NeutronUtils.TCP);
        serverIn80Tcp10_1_1_0.setSecurityRulePortMin(80);
        serverIn80Tcp10_1_1_0.setSecurityRulePortMax(80);
        serverIn80Tcp10_1_1_0.setSecurityRuleRemoteIpPrefix("10.1.1.0/24");
        NeutronSecurityRule serverInIp20_1_1_0 = NeutronEntityFactory.securityRuleWithEtherType(
                "adf7e558-de47-4f9e-a9b8-96e19db5d1ac", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp,
                null);
        serverInIp20_1_1_0.setSecurityRuleRemoteIpPrefix("20.1.1.0/24");
        NeutronSecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80Tcp10_1_1_0, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIp20_1_1_0, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Tenant> potentialTenant = rwTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.tenantIid(tenantId))
            .get();
        assertTrue(potentialTenant.isPresent());
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContractWithEic(contract, serverIn80Tcp10_1_1_0);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContractWithEic(contract, serverInIp20_1_1_0);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(goldOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID()),
                        new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID()),
                        new ContractId(serverOutIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(epg, ImmutableSet.of(
                new ContractId(serverIn80Tcp10_1_1_0.getSecurityRuleUUID()),
                new ContractId(serverInIp20_1_1_0.getSecurityRuleUUID()),
                new ContractId(goldInIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddAndDeleteNeutronSecurityRule() throws Exception {
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);

        final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        final String secRuleId1 = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        final String secRuleId2 = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        NeutronSecurityRule secRule1 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId1, tenantId,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, secGroupId1, secGroupId2);
        NeutronSecurityRule secRule2 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId2, tenantId,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, secGroupId2, secGroupId1);

        ruleAware.created(secRule1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        Optional<Contract> contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        Optional<EndpointGroup> epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        ruleAware.created(secRule2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId2)));

        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId2);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId2);
        PolicyAssert.assertContract(contract.get(), secRule2);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        Optional<EndpointGroup> epGroup2 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertProviderNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId2)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId1)));

        ruleAware.deleted(secRule2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        PolicyAssert.assertContractNotExists(dataBroker, tenantId, secRuleId2);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        epGroup2 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        ruleAware.deleted(secRule1);

        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);

        PolicyAssert.assertClassifierInstanceNotExists(dataBroker, secRule1);
        // TODO: Uncomment this when the life cycle of the Allow ActionInstance will be clarified.
        // PolicyAssert.assertActionInstanceNotExists(dataBroker, tenantId,
        // MappingUtils.ACTION_ALLOW.getName());
    }

    @Test
    public final void testAddNeutronSecurityRule_rulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        NeutronSecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp,
                null);
        NeutronSecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp,
                null);
        NeutronSecurityRule serverInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "adf7e558-de47-4f9e-a9b8-96e19db5d1ac", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverInIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(goldOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverInIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(goldOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverInIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddNeutronSecurityRule_asymmetricRulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        NeutronSecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, goldSecGrp,
                null);
        NeutronSecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        NeutronSecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, serverSecGrp,
                null);
        NeutronSecurityRule serverIn80TcpIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "adf7e558-de47-4f9e-a9b8-96e19db5d1ac", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, serverSecGrp,
                null);
        serverIn80TcpIpv4.setSecurityRuleProtocol(NeutronUtils.TCP);
        serverIn80TcpIpv4.setSecurityRulePortMin(80);
        serverIn80TcpIpv4.setSecurityRulePortMax(80);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80TcpIpv4, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverIn80TcpIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(goldOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverOutIpv4.getSecurityRuleUUID()),
                        new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getSecurityRuleUUID()),
                        new ContractId(serverIn80TcpIpv4.getSecurityRuleUUID())));
    }

    @Test
    public final void testAddNeutronSecurityRule_defaultSecGrp() throws Exception {
        String tenant = "111aaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String defaultSecGrp = "111fffff-ffff-ffff-ffff-ffffffffffff";
        NeutronSecurityRule defaultInIpv4Default = NeutronEntityFactory.securityRuleWithEtherType(
                "111ccccc-111c-cccc-cccc-cccccccccccc", tenant, NeutronUtils.IPv4, NeutronUtils.INGRESS, defaultSecGrp,
                defaultSecGrp);
        NeutronSecurityRule defaultInIpv6Default = NeutronEntityFactory.securityRuleWithEtherType(
                "222ccccc-111c-cccc-cccc-cccccccccccc", tenant, NeutronUtils.IPv6, NeutronUtils.INGRESS, defaultSecGrp,
                defaultSecGrp);
        NeutronSecurityRule defaultOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "333ccccc-111c-cccc-cccc-cccccccccccc", tenant, NeutronUtils.IPv4, NeutronUtils.EGRESS, defaultSecGrp,
                null);
        NeutronSecurityRule defaultOutIpv6 = NeutronEntityFactory.securityRuleWithEtherType(
                "444ccccc-111c-cccc-cccc-cccccccccccc", tenant, NeutronUtils.IPv6, NeutronUtils.EGRESS, defaultSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        secGroupDao.addSecGroup(NeutronEntityFactory.securityGroup(defaultSecGrp, tenant));
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(defaultInIpv4Default, rwTx);
        ruleAware.addNeutronSecurityRule(defaultInIpv6Default, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv4, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv6, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv4Default.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultInIpv4Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv6Default.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultInIpv6Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv4.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv6.getSecurityRuleUUID()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultOutIpv6);
    }

    @Test
    public void testConstructor_invalidArgument() throws Exception {
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        assertExceptionInConstructor(null, secRuleDao, secGroupDao);
        assertExceptionInConstructor(dataBroker, null, secGroupDao);
        assertExceptionInConstructor(dataBroker, secRuleDao, null);
        assertExceptionInConstructor(null, null, null);
    }

    private void assertExceptionInConstructor(DataBroker dataBroker, SecRuleDao secRuleDao, SecGroupDao secGroupDao) {
        try {
            new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
            fail(NullPointerException.class.getName() + " expected");
        } catch (NullPointerException ex) {
            // do nothing
        }
    }
}
