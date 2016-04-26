package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.ConfigDataStoreReader;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class NeutronSecurityGroupAwareDataStoreTest extends GbpDataBrokerTest {

    @Test
    public void testAddAndDeleteNeutronSecurityGroup_noSecurityRules() throws Exception {
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        NeutronSecurityGroupAware groupAware = new NeutronSecurityGroupAware(dataBroker, ruleAware, secGroupDao);

        final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";

        NeutronSecurityGroup secGroup1 = NeutronEntityFactory.securityGroup(secGroupId1, tenantId);
        NeutronSecurityGroup secGroup2 = NeutronEntityFactory.securityGroup(secGroupId2, tenantId);

        groupAware.created(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);

        groupAware.deleted(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);

        groupAware.created(secGroup1);
        groupAware.created(secGroup2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId2, IntraGroupPolicy.RequireContract);

        groupAware.deleted(secGroup2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        groupAware.deleted(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);
    }

    @Test
    public void testAddAndDeleteNeutronSecurityGroup_withSecurityRules() throws Exception {
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        NeutronSecurityGroupAware groupAware = new NeutronSecurityGroupAware(dataBroker, ruleAware, secGroupDao);

        final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        final String secRuleId1 = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        final String secRuleId2 = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        NeutronSecurityRule secRule1 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId1, tenantId,
                NeutronUtils.IPv4, NeutronUtils.EGRESS, secGroupId1, null);

        NeutronSecurityRule secRule2 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId2, tenantId,
                NeutronUtils.IPv4, NeutronUtils.INGRESS, secGroupId2, secGroupId1);

        NeutronSecurityGroup secGroup1 = NeutronEntityFactory.securityGroup(secGroupId1, tenantId,
                ImmutableList.of(secRule1));

        NeutronSecurityGroup secGroup2 = NeutronEntityFactory.securityGroup(secGroupId2, tenantId,
                ImmutableList.of(secRule2));

        groupAware.created(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        Optional<Contract> contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        Optional<EndpointGroup> epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        groupAware.deleted(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);

        PolicyAssert.assertClassifierInstanceNotExists(dataBroker, secRule1);
        // TODO: Uncomment this when the life cycle of the Allow ActionInstance will be clarified.
        // PolicyAssert.assertActionInstanceNotExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        groupAware.created(secGroup1);
        groupAware.created(secGroup2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId2);
        PolicyAssert.assertContract(contract.get(), secRule2);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId2)));
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        Optional<EndpointGroup> epGroup2 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertProviderNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId2)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId2, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 2);
        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        groupAware.deleted(secGroup2);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        groupAware.deleted(secGroup1);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);

        PolicyAssert.assertClassifierInstanceNotExists(dataBroker, secRule1);
        // TODO: Uncomment this when the life cycle of the Allow ActionInstance will be clarified.
        // PolicyAssert.assertActionInstanceNotExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());
    }

    @Test
    public void testConstructor_invalidArgument() throws Exception {
        DataBroker dataBroker = getDataBroker();
        SecRuleDao secRuleDao = new SecRuleDao();
        SecGroupDao secGroupDao = new SecGroupDao();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker, secRuleDao, secGroupDao);
        assertExceptionInConstructor(null, ruleAware, secGroupDao);
        assertExceptionInConstructor(dataBroker, null, secGroupDao);
        assertExceptionInConstructor(dataBroker, ruleAware, null);
        assertExceptionInConstructor(null, null, null);
    }

    private void assertExceptionInConstructor(DataBroker dataBroker, NeutronSecurityRuleAware secRuleAware,
            SecGroupDao secGroupDao) {
        try {
            new NeutronSecurityGroupAware(dataBroker, secRuleAware, secGroupDao);
            fail(NullPointerException.class.getName() + " expected");
        } catch (NullPointerException ex) {
            // do nothing
        }
    }
}
