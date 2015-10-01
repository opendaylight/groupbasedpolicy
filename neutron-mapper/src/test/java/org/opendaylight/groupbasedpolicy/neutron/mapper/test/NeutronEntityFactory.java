package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;

public final class NeutronEntityFactory {

    private NeutronEntityFactory() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    public static NeutronSecurityGroup securityGroup(String id, String tenantId) {
        NeutronSecurityGroup secGrp = new NeutronSecurityGroup();
        secGrp.setSecurityGroupUUID(id);
        secGrp.setSecurityGroupTenantID(tenantId);
        secGrp.setSecurityRules(new ArrayList<NeutronSecurityRule>());
        return secGrp;
    }

    public static NeutronSecurityGroup securityGroupWithName(String id, String tenantId, String name) {
        NeutronSecurityGroup secGrp = new NeutronSecurityGroup();
        secGrp.setSecurityGroupUUID(id);
        secGrp.setSecurityGroupTenantID(tenantId);
        secGrp.setSecurityGroupName(name);
        return secGrp;
    }

    public static NeutronSecurityGroup securityGroup(String id, String tenantId, List<NeutronSecurityRule> secRules) {
        NeutronSecurityGroup secGrp = new NeutronSecurityGroup();
        secGrp.setSecurityGroupUUID(id);
        secGrp.setSecurityGroupTenantID(tenantId);
        secGrp.setSecurityRules(secRules);
        return secGrp;
    }

    public static NeutronSecurityRule securityRuleWithGroupIds(String id, String tenant, String ownerGroupId,
            String remoteGroupId) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRuleUUID(id);
        secRule.setSecurityRuleTenantID(tenant);
        secRule.setSecurityRuleGroupID(ownerGroupId);
        secRule.setSecurityRemoteGroupID(remoteGroupId);
        return secRule;
    }

    public static NeutronSecurityRule securityRuleWithoutGroupIds(String id, String tenant, String etherType,
            String direction, String protocol, int portMin, int portMax) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRuleUUID(id);
        secRule.setSecurityRuleTenantID(tenant);
        secRule.setSecurityRuleEthertype(etherType);
        secRule.setSecurityRuleDirection(direction);
        secRule.setSecurityRuleProtocol(protocol);
        secRule.setSecurityRulePortMin(portMin);
        secRule.setSecurityRulePortMax(portMax);
        return secRule;
    }

    public static NeutronSecurityRule securityRuleWithEtherType(String id, String tenant, String etherType,
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
}
