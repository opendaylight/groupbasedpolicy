package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;

public class SecRuleDaoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SecRuleDao secRuleDao;

    @Before
    public void setUp() throws Exception {
        secRuleDao = new SecRuleDao();
    }

    @Test
    public void testAddAndRemoveSecRule() {
        final String xSecGrpId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        final String ySecGrpId = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        final String nullSecGrpId = null;

        EndpointGroupId xEpGrpId = new EndpointGroupId(xSecGrpId);
        EndpointGroupId yEpGrpId = new EndpointGroupId(ySecGrpId);
        EndpointGroupId nullEpGrpId = null;

        NeutronSecurityRule secRule1 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", xSecGrpId, nullSecGrpId);

        NeutronSecurityRule secRule2 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", xSecGrpId, xSecGrpId);

        NeutronSecurityRule secRule3 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", xSecGrpId, ySecGrpId);

        NeutronSecurityRule secRule4 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", ySecGrpId, nullSecGrpId);

        NeutronSecurityRule secRule5 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", ySecGrpId, ySecGrpId);

        NeutronSecurityRule secRule6 = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", ySecGrpId, xSecGrpId);

        secRuleDao.addSecRule(secRule1);
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertAllOwnerSecGrps(xEpGrpId);

        secRuleDao.addSecRule(secRule2);
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1, secRule2);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId, secRule2);
        assertAllOwnerSecGrps(xEpGrpId);

        secRuleDao.addSecRule(secRule3);
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1, secRule2, secRule3);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId, secRule2);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, yEpGrpId, secRule3);
        assertAllOwnerSecGrps(xEpGrpId);

        secRuleDao.addSecRule(secRule4);
        assertSecRulesByOwnerSecGrpId(yEpGrpId, secRule4);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId, secRule4);
        assertAllOwnerSecGrps(xEpGrpId, yEpGrpId);

        secRuleDao.addSecRule(secRule5);
        assertSecRulesByOwnerSecGrpId(yEpGrpId, secRule4, secRule5);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, yEpGrpId, secRule5);
        assertAllOwnerSecGrps(xEpGrpId, yEpGrpId);

        secRuleDao.addSecRule(secRule6);
        assertSecRulesByOwnerSecGrpId(yEpGrpId, secRule4, secRule5, secRule6);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, yEpGrpId, secRule5);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, xEpGrpId, secRule6);
        assertAllOwnerSecGrps(xEpGrpId, yEpGrpId);

        // check once more security rules with owner group id = xSecGrpId
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1, secRule2, secRule3);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId, secRule2);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, yEpGrpId, secRule3);

        secRuleDao.removeSecRule(secRule6);
        assertSecRulesByOwnerSecGrpId(yEpGrpId, secRule4, secRule5);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, yEpGrpId, secRule5);
        assertAllOwnerSecGrps(xEpGrpId, yEpGrpId);

        secRuleDao.removeSecRule(secRule5);
        assertSecRulesByOwnerSecGrpId(yEpGrpId, secRule4);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId, secRule4);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId, secRule4);
        assertAllOwnerSecGrps(xEpGrpId, yEpGrpId);

        secRuleDao.removeSecRule(secRule4);

        // check once more security rules with owner group id = xSecGrpId
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1, secRule2, secRule3);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId, secRule2);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, yEpGrpId, secRule3);

        secRuleDao.removeSecRule(secRule3);
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1, secRule2);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId, secRule2);
        assertAllOwnerSecGrps(xEpGrpId);

        secRuleDao.removeSecRule(secRule2);
        assertSecRulesByOwnerSecGrpId(xEpGrpId, secRule1);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId, secRule1);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId, secRule1);
        assertAllOwnerSecGrps(xEpGrpId);

        secRuleDao.removeSecRule(secRule1);
        assertSecRulesByOwnerSecGrpId(xEpGrpId);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(xEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, nullEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, xEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(xEpGrpId, yEpGrpId);
        assertSecRulesByOwnerSecGrpId(yEpGrpId);
        assertSecRulesWithoutRemoteSecGrpBySecGrpId(yEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, nullEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, yEpGrpId);
        assertSecRulesBySecGrpIdAndRemoteSecGrpId(yEpGrpId, xEpGrpId);
        assertAllOwnerSecGrps();
    }

    @Test
    public void testAddSecGroup_nullArgument() {
        thrown.expect(NullPointerException.class);
        secRuleDao.addSecRule(null);
    }

    @Test
    public void testRemoveNonExistingSecRule() {
        NeutronSecurityRule secRule = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "cccccccc-cccc-cccc-cccc-cccccccccccc", "dddddddd-dddd-dddd-dddd-dddddddddddd");
        secRuleDao.removeSecRule(secRule);
    }

    @Test
    public void testRemoveNonExistingSecRule_remoteSecGroupIdIsNull() {
        NeutronSecurityRule secRule = NeutronEntityFactory.securityRuleWithGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "cccccccc-cccc-cccc-cccc-cccccccccccc", null);
        secRuleDao.removeSecRule(secRule);
    }

    private static void assertSecRules(Set<NeutronSecurityRule> secRules, NeutronSecurityRule... expectedSecRules) {
        assertNotNull(secRules);
        assertEquals(expectedSecRules.length, secRules.size());
        for (int i = 0; i < expectedSecRules.length; ++i) {
            assertTrue(secRules.contains(expectedSecRules[i]));
        }
    }

    private void assertSecRulesByOwnerSecGrpId(EndpointGroupId ownerEpGrpId, NeutronSecurityRule... expectedSecRules) {
        Set<NeutronSecurityRule> secRules = secRuleDao.getSecRulesByOwnerSecGrpId(ownerEpGrpId);
        assertSecRules(secRules, expectedSecRules);
    }

    private void assertSecRulesBySecGrpIdAndRemoteSecGrpId(EndpointGroupId ownerEpGrpId, EndpointGroupId remoteEpGrpId,
            NeutronSecurityRule... expectedSecRules) {
        Set<NeutronSecurityRule> secRules = secRuleDao.getSecRulesBySecGrpIdAndRemoteSecGrpId(ownerEpGrpId,
                remoteEpGrpId);
        assertSecRules(secRules, expectedSecRules);
    }

    private void assertSecRulesWithoutRemoteSecGrpBySecGrpId(EndpointGroupId ownerEpGrpId,
            NeutronSecurityRule... expectedSecRules) {
        Set<NeutronSecurityRule> secRules = secRuleDao.getSecRulesWithoutRemoteSecGrpBySecGrpId(ownerEpGrpId);
        assertSecRules(secRules, expectedSecRules);
    }

    private void assertAllOwnerSecGrps(EndpointGroupId... expectedEndpointGropuIds) {
        Set<EndpointGroupId> allOwnerSecGrps = secRuleDao.getAllOwnerSecGrps();
        assertNotNull(allOwnerSecGrps);
        assertEquals(expectedEndpointGropuIds.length, allOwnerSecGrps.size());
        for (int i = 0; i < expectedEndpointGropuIds.length; ++i) {
            assertTrue(allOwnerSecGrps.contains(expectedEndpointGropuIds[i]));
        }
    }
}
