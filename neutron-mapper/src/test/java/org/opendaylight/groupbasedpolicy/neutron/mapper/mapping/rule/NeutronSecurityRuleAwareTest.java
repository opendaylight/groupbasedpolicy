package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.ConfigDataStoreReader;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.NeutronBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;

public class NeutronSecurityRuleAwareTest extends NeutronMapperDataBrokerTest {

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
        NeutronSecurityRuleAware neutronSecurityRuleAware = new NeutronSecurityRuleAware(dataProvider);

        //create security rule and put to DS
        SecurityRule neutronRule = buildNeutronSecurityRule();
        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(neutronRule.getSecurityGroupId().getValue(),
                neutronRule.getTenantId().getValue()));
        Neutron neutron = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build()).build();
        neutronSecurityRuleAware.onCreated(neutronRule, neutron);

        //read security rule
        PolicyAssert.assertContractExists(dataProvider, RULE_TENANT_ID, RULE_ID);

        //compare
        Contract readContract = ConfigDataStoreReader.readContract(dataProvider, RULE_TENANT_ID, RULE_ID).get();
        assertNotNull(readContract);
        assertEquals(readContract.getId().getValue(), RULE_ID);

        //delete rule
        neutronSecurityRuleAware.onDeleted(neutronRule, neutron, null);
        PolicyAssert.assertContractNotExists(dataProvider, RULE_TENANT_ID, RULE_ID);
    }

    // create neutron security rule
    private SecurityRule buildNeutronSecurityRule() {
        return new SecurityRuleBuilder().setId(new Uuid(RULE_ID))
            .setTenantId(new Uuid(RULE_TENANT_ID))
            .setSecurityGroupId(new Uuid(RULE_GROUP_ID))
            .setRemoteIpPrefix(new IpPrefix(new Ipv4Prefix("192.0.0.1/24")))
            .setPortRangeMin(1000)
            .setPortRangeMax(5000)
            .setProtocol(ProtocolTcp.class)
            .setEthertype(EthertypeV4.class)
            .setDirection(DirectionIngress.class)
            .build();
    }

}
