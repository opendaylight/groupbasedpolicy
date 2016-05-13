package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;

public class SecRuleNameDecoderTest {

    private SecurityRuleBuilder secRule;

    @Before
    public void setUp() throws Exception {
        secRule = new SecurityRuleBuilder().setId(new Uuid("01234567-abcd-ef01-0123-0123456789ab"));
    }

    @Test
    public final void testGetClassifierRefName() {
        secRule.setDirection(DirectionIngress.class);
        secRule.setEthertype(EthertypeV4.class);
        ClassifierName clsfInstanceName = SecRuleNameDecoder.getClassifierInstanceName(secRule.build());
        String crName = new StringBuilder().append(Direction.In.name())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(clsfInstanceName.getValue())
            .toString();
        ClassifierName expectedClsfRefName = new ClassifierName(crName);
        assertEquals(expectedClsfRefName, SecRuleNameDecoder.getClassifierRefName(secRule.build()));
    }

    @Test
    public final void testGetClassifierInstanceName() {
        secRule.setDirection(DirectionIngress.class);
        secRule.setEthertype(EthertypeV4.class);
        secRule.setProtocol(ProtocolTcp.class);
        secRule.setPortRangeMin(8010);
        secRule.setPortRangeMax(8020);
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
        String frmtClsfName = String.format(frmtBuilder.toString(), 8010, 8020, secRule.getProtocol().getSimpleName(),
                secRule.getEthertype().getSimpleName());
        ClassifierName expectedClsfInstanceName = new ClassifierName(frmtClsfName);
        assertEquals(expectedClsfInstanceName, SecRuleNameDecoder.getClassifierInstanceName(secRule.build()));
    }

    @Test
    public final void testGetClauseName_noRemoteIpPrefix() {
        secRule.setDirection(DirectionIngress.class);
        secRule.setEthertype(EthertypeV4.class);
        ClauseName expectedClauseName = new ClauseName(SecRuleNameDecoder.getSubjectName(secRule.build()));
        assertEquals(expectedClauseName, SecRuleNameDecoder.getClauseName(secRule.build()));
    }

    @Test
    public final void testGetClauseName_remoteIpPrefix() {
        secRule.setDirection(DirectionIngress.class);
        secRule.setEthertype(EthertypeV4.class);
        secRule.setRemoteIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/8")));
        ClauseName expectedClauseName = new ClauseName(SecRuleNameDecoder.getSubjectName(secRule.build()).getValue()
                + MappingUtils.NAME_DOUBLE_DELIMETER + "10.0.0.0_8");
        assertEquals(expectedClauseName, SecRuleNameDecoder.getClauseName(secRule.build()));
    }
}
