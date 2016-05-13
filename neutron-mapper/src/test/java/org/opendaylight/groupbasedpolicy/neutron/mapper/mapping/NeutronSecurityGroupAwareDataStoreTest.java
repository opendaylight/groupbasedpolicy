package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;

public class NeutronSecurityGroupAwareDataStoreTest extends GbpDataBrokerTest {

    @Test
    public void testAddAndDeleteNeutronSecurityGroup_noSecurityRules() throws Exception {
        DataBroker dataBroker = getDataBroker();
        NeutronSecurityGroupAware groupAware = new NeutronSecurityGroupAware(dataBroker);

        final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";

        SecurityGroup secGroup1 = NeutronEntityFactory.securityGroup(secGroupId1, tenantId);
        SecurityGroup secGroup2 = NeutronEntityFactory.securityGroup(secGroupId2, tenantId);

        groupAware.onCreated(secGroup1, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);

        groupAware.onDeleted(secGroup1, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);

        groupAware.onCreated(secGroup1, null);
        groupAware.onCreated(secGroup2, null);

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

        groupAware.onDeleted(secGroup2, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        groupAware.onDeleted(secGroup1, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);
    }

    @Test
    public void testConstructor_invalidArgument() throws Exception {
        try {
            new NeutronSecurityGroupAware(null);
            fail(NullPointerException.class.getName() + " expected");
        } catch (NullPointerException e) {
            // do nothing
        }
    }

}
