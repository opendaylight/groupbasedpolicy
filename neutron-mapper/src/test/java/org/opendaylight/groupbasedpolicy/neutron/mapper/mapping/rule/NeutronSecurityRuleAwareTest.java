package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.StatusCode;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.SecGroupDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.ConfigDataStoreReader;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;

public class NeutronSecurityRuleAwareTest extends GbpDataBrokerTest {

    private static final String RULE_ID = "00000000-0000-0000-0000-000000000001";
    private static final String RULE_TENANT_ID = "00000000-0000-0000-0000-000000000002";
    private static final String RULE_GROUP_ID = "00000000-0000-0000-0000-000000000003";

    @Test
    public final void testIsDirectionOpposite_InIn() {
        assertFalse(NeutronSecurityRuleAware.isDirectionOpposite(Direction.In, Direction.In));
    }

    @Test
    public final void testIsDirectionOpposite_OutOut() {
        assertFalse(NeutronSecurityRuleAware.isDirectionOpposite(Direction.Out, Direction.Out));
    }

    @Test
    public final void testIsDirectionOpposite_InOut() {
        assertTrue(NeutronSecurityRuleAware.isDirectionOpposite(Direction.In, Direction.Out));
    }

    @Test
    public final void testIsDirectionOpposite_OutIn() {
        assertTrue(NeutronSecurityRuleAware.isDirectionOpposite(Direction.Out, Direction.In));
    }

    @Test
    public void testNeutronSecurityRuleCreatedAndDeleted() throws Exception {
        DataBroker dataProvider = getDataBroker();
        SecGroupDao secGroupDao = new SecGroupDao();
        SecRuleDao secRuleDao = new SecRuleDao();
        NeutronSecurityRuleAware neutronSecurityRuleAware =
                new NeutronSecurityRuleAware(dataProvider, secRuleDao, secGroupDao);

        //create security rule and put to DS
        NeutronSecurityRule neutronRule = buildNeutronSecurityRule();
        assertEquals(neutronSecurityRuleAware.canCreateNeutronSecurityRule(neutronRule),
                StatusCode.OK);
        neutronSecurityRuleAware.neutronSecurityRuleCreated(neutronRule);

        //read security rule
        PolicyAssert.assertContractExists(dataProvider, RULE_TENANT_ID, RULE_ID);

        //compare
        Contract readContract = ConfigDataStoreReader.readContract(dataProvider, RULE_TENANT_ID, RULE_ID).get();
        assertNotNull(readContract);
        assertEquals(readContract.getId().getValue(), RULE_ID);

        assertEquals(neutronSecurityRuleAware.canUpdateNeutronSecurityRule(neutronRule, neutronRule),
                StatusCode.BAD_REQUEST);

        //delete rule
        assertEquals(neutronSecurityRuleAware.canDeleteNeutronSecurityRule(neutronRule),
                StatusCode.OK);
        neutronSecurityRuleAware.neutronSecurityRuleDeleted(neutronRule);
        PolicyAssert.assertContractNotExists(dataProvider, RULE_TENANT_ID, RULE_ID);
    }

    //create neutron security rule
    private NeutronSecurityRule buildNeutronSecurityRule() {
        NeutronSecurityRule neutronSecurityRule = new NeutronSecurityRule();
        neutronSecurityRule.setSecurityRuleUUID(RULE_ID);
        neutronSecurityRule.setSecurityRuleTenantID(RULE_TENANT_ID);
        neutronSecurityRule.setSecurityRuleGroupID(RULE_GROUP_ID);
        neutronSecurityRule.setSecurityRuleRemoteIpPrefix("192.0.0.1/24");
        neutronSecurityRule.setSecurityRulePortMin(1000);
        neutronSecurityRule.setSecurityRulePortMax(5000);
        neutronSecurityRule.setSecurityRuleProtocol("tcp");
        neutronSecurityRule.setSecurityRuleEthertype("IPv4");
        neutronSecurityRule.setSecurityRuleDirection("ingress");

        return neutronSecurityRule;
    }

}
