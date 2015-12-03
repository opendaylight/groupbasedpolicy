package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;

public class SecRuleNameDecoderTest {

    private NeutronSecurityRule secRule;

    @Before
    public void setUp() throws Exception {
        secRule = new NeutronSecurityRule();
    }

    @Test
    public final void testGetClassifierRefName() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        ClassifierName clsfInstanceName = SecRuleNameDecoder.getClassifierInstanceName(secRule);
        String crName = new StringBuilder().append(Direction.In.name())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(clsfInstanceName.getValue())
            .toString();
        ClassifierName expectedClsfRefName = new ClassifierName(crName);
        assertEquals(expectedClsfRefName, SecRuleNameDecoder.getClassifierRefName(secRule));
    }

    @Test
    public final void testGetClassifierInstanceName() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleProtocol(NeutronUtils.TCP);
        secRule.setSecurityRulePortMin(8010);
        secRule.setSecurityRulePortMax(8020);
        StringBuilder frmtBuilder = new StringBuilder();
        frmtBuilder.append(L4ClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_DELIMETER)
            .append(L4ClassifierDefinition.DST_PORT_RANGE_PARAM)
            .append(SecRuleNameDecoder.MIN_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append("%d")
            .append(SecRuleNameDecoder.MAX_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append("%d")
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(IpProtoClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append("%s")
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append("%s");
        String frmtClsfName = String.format(frmtBuilder.toString(), 8010, 8020, secRule.getSecurityRuleProtocol(),
                secRule.getSecurityRuleEthertype());
        ClassifierName expectedClsfInstanceName = new ClassifierName(frmtClsfName);
        assertEquals(expectedClsfInstanceName, SecRuleNameDecoder.getClassifierInstanceName(secRule));
    }

    @Test
    public final void testGetClauseName_noRemoteIpPrefix() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        ClauseName expectedClauseName = new ClauseName(SecRuleNameDecoder.getSubjectName(secRule));
        assertEquals(expectedClauseName, SecRuleNameDecoder.getClauseName(secRule));
    }

    @Test
    public final void testGetClauseName_remoteIpPrefix() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleRemoteIpPrefix("10.0.0.0/8");
        ClauseName expectedClauseName = new ClauseName(SecRuleNameDecoder.getSubjectName(secRule).getValue()
                + MappingUtils.NAME_DOUBLE_DELIMETER + "10.0.0.0_8");
        assertEquals(expectedClauseName, SecRuleNameDecoder.getClauseName(secRule));
    }
}
