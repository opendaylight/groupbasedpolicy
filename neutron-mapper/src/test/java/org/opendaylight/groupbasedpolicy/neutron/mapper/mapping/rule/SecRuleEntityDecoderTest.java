package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolUdp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.SecurityRuleAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;

public class SecRuleEntityDecoderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SecurityRuleBuilder secRuleBuilder;

    @Before
    public void setUp() throws Exception {
        secRuleBuilder = new SecurityRuleBuilder().setUuid(new Uuid("01234567-abcd-ef01-0123-0123456789ab"));
    }

    @Test
    public final void testGetContractId() {
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab",
                SecRuleEntityDecoder.getContractId(secRuleBuilder.build()).getValue());
    }

    @Test
    public final void testGetClassifierInstance_onlyEthertype() {
        secRuleBuilder.setEthertype(EthertypeV4.class);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRuleBuilder.build());
        Assert.assertEquals(EtherTypeClassifierDefinition.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is ether_type_IPv4
        String expectedName = new StringBuilder().append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(EthertypeV4.class.getSimpleName())
            .toString();
        Assert.assertEquals(expectedName, ci.getName().getValue());
        Assert.assertEquals(expectedName, ci.getName().getValue());
        List<ParameterValue> parameterValues = ci.getParameterValue();
        Assert.assertNotNull(parameterValues);
        Assert.assertEquals(1, parameterValues.size());
        ParameterValue parameter = parameterValues.get(0);
        Assert.assertEquals(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, parameter.getName().getValue());
        assertClassifierParameterValue(parameter, EtherTypeClassifierDefinition.IPv4_VALUE, null, null);
    }

    @Test
    public final void testGetClassifierInstance_EthertypeAndProtocol() {
        secRuleBuilder.setEthertype(EthertypeV4.class);
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        secRuleBuilder.setProtocol(protocolTcp);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRuleBuilder.build());
        Assert.assertEquals(IpProtoClassifierDefinition.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is ip_proto_tcp__ether_type_IPv4
        String expectedName = new StringBuilder().append(IpProtoClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(ProtocolTcp.class.getSimpleName())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(EthertypeV4.class.getSimpleName())
            .toString();
        Assert.assertEquals(expectedName, ci.getName().getValue());
        List<ParameterValue> parameterValues = ci.getParameterValue();
        Assert.assertNotNull(parameterValues);
        Assert.assertEquals(2, parameterValues.size());
        boolean containsEthertypeParam = false;
        boolean containsProtoParam = false;
        for (ParameterValue parameter : parameterValues) {
            ParameterName parameterName = parameter.getName();
            Assert.assertNotNull(parameterName);
            if (EtherTypeClassifierDefinition.ETHERTYPE_PARAM.equals(parameterName.getValue())) {
                containsEthertypeParam = true;
                assertClassifierParameterValue(parameter, EtherTypeClassifierDefinition.IPv4_VALUE, null, null);
            } else if (IpProtoClassifierDefinition.PROTO_PARAM.equals(parameterName.getValue())) {
                containsProtoParam = true;
                assertClassifierParameterValue(parameter, IpProtoClassifierDefinition.TCP_VALUE, null, null);
            } else {
                fail("This parameter is not expected: " + parameter);
            }
        }
        Assert.assertTrue("Classifier-instance does not contain ethertype parameter", containsEthertypeParam);
        Assert.assertTrue("Classifier-instance does not contain protocol parameter", containsProtoParam);
    }

    @Test
    public final void testGetClassifierInstance_EthertypeAndProtocolAndPort() {
        secRuleBuilder.setEthertype(EthertypeV4.class);
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        secRuleBuilder.setProtocol(protocolTcp);
        secRuleBuilder.setPortRangeMin(5);
        secRuleBuilder.setPortRangeMax(5);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRuleBuilder.build());
        Assert.assertEquals(L4ClassifierDefinition.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is l4_destport-5__ip_proto-tcp__ether_type-IPv4
        String expectedName = new StringBuilder().append(L4ClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_DELIMETER)
            .append(L4ClassifierDefinition.DST_PORT_PARAM)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRuleBuilder.getPortRangeMin())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(IpProtoClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(ProtocolTcp.class.getSimpleName())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(EthertypeV4.class.getSimpleName())
            .toString();
        System.out.println(expectedName);
        System.out.println(ci.getName().getValue());
        Assert.assertEquals(expectedName, ci.getName().getValue());
        List<ParameterValue> parameterValues = ci.getParameterValue();
        Assert.assertNotNull(parameterValues);
        Assert.assertEquals(3, parameterValues.size());
        boolean containsEthertypeParam = false;
        boolean containsProtoParam = false;
        boolean containsDstPortParam = false;
        for (ParameterValue parameter : parameterValues) {
            ParameterName parameterName = parameter.getName();
            Assert.assertNotNull(parameterName);
            if (EtherTypeClassifierDefinition.ETHERTYPE_PARAM.equals(parameterName.getValue())) {
                containsEthertypeParam = true;
                assertClassifierParameterValue(parameter, EtherTypeClassifierDefinition.IPv4_VALUE, null, null);
            } else if (IpProtoClassifierDefinition.PROTO_PARAM.equals(parameterName.getValue())) {
                containsProtoParam = true;
                assertClassifierParameterValue(parameter, IpProtoClassifierDefinition.TCP_VALUE, null, null);
            } else if (L4ClassifierDefinition.DST_PORT_PARAM.equals(parameterName.getValue())) {
                containsDstPortParam = true;
                assertClassifierParameterValue(parameter, 5L, null, null);
            } else {
                fail("This parameter is not expected: " + parameter);
            }
        }
        Assert.assertTrue("Classifier-instance does not contain ethertype parameter", containsEthertypeParam);
        Assert.assertTrue("Classifier-instance does not contain protocol parameter", containsProtoParam);
        Assert.assertTrue("Classifier-instance does not contain destination port parameter", containsDstPortParam);
    }

    private final void assertClassifierParameterValue(ParameterValue parameter, Long expectedIntValue,
            String expectedStringValue, RangeValue expectedRangeValue) {
        Assert.assertEquals(expectedIntValue, parameter.getIntValue());
        Assert.assertEquals(expectedStringValue, parameter.getStringValue());
        Assert.assertEquals(expectedRangeValue, parameter.getRangeValue());
    }

    @Test
    public final void testGetClassifierInstance_EthertypeAndProtocolAndPorts() {
        secRuleBuilder.setEthertype(EthertypeV4.class);
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        secRuleBuilder.setProtocol(protocolTcp);
        secRuleBuilder.setPortRangeMin(5);
        secRuleBuilder.setPortRangeMax(10);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRuleBuilder.build());
        Assert.assertEquals(L4ClassifierDefinition.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is l4_destport_range_min-5_max-10__ip_proto-tcp__ether_type-IPv4
        String expectedName = new StringBuilder().append(L4ClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_DELIMETER)
            .append(L4ClassifierDefinition.DST_PORT_RANGE_PARAM)
            .append(SecRuleNameDecoder.MIN_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRuleBuilder.getPortRangeMin())
            .append(SecRuleNameDecoder.MAX_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRuleBuilder.getPortRangeMax())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(IpProtoClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(ProtocolTcp.class.getSimpleName())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifierDefinition.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(EthertypeV4.class.getSimpleName())
            .toString();
        Assert.assertEquals(expectedName, ci.getName().getValue());
    }

    @Test
    public final void testGetClassifierRef() {
        secRuleBuilder.setDirection(DirectionIngress.class);
        secRuleBuilder.setEthertype(EthertypeV4.class);
        ClassifierName expectedName = SecRuleNameDecoder.getClassifierRefName(secRuleBuilder.build());
        ClassifierRef cr = SecRuleEntityDecoder.getClassifierRef(secRuleBuilder.build());
        Assert.assertEquals(expectedName, cr.getName());
    }

    @Test
    public final void testGetDirection_directionIngress() {
        secRuleBuilder.setDirection(DirectionIngress.class);
        Assert.assertEquals(Direction.In, SecRuleEntityDecoder.getDirection(secRuleBuilder.build()));
    }

    @Test
    public final void testGetDirection_directionEgress() {
        secRuleBuilder.setDirection(DirectionEgress.class);
        Assert.assertEquals(Direction.Out, SecRuleEntityDecoder.getDirection(secRuleBuilder.build()));
    }

    @Test
    public final void testGetDirection_directionNull() {
        secRuleBuilder.setDirection(null);
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getDirection(secRuleBuilder.build());
    }

    @Test
    public final void testGetDirection_directionUnknown() {
        secRuleBuilder.setDirection(UnknownDirection.class);
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getDirection(secRuleBuilder.build());
    }

    private static class UnknownDirection extends DirectionBase {
    }

    @Test
    public final void testGetClause_noRemoteIpPrefix() {
        secRuleBuilder.setDirection(DirectionIngress.class);
        secRuleBuilder.setEthertype(EthertypeV4.class);
        ClauseName expectedClauseName = SecRuleNameDecoder.getClauseName(secRuleBuilder.build());
        Clause clause = SecRuleEntityDecoder.getClause(secRuleBuilder.build());
        Assert.assertEquals(expectedClauseName, clause.getName());
        List<SubjectName> subjectRefs = clause.getSubjectRefs();
        Assert.assertNotNull(subjectRefs);
        Assert.assertEquals(1, subjectRefs.size());
        SubjectName subjectNameFromClause = subjectRefs.get(0);
        SubjectName expectedSubjectName = SecRuleNameDecoder.getSubjectName(secRuleBuilder.build());
        Assert.assertEquals(expectedSubjectName, subjectNameFromClause);
        Assert.assertNull(clause.getConsumerMatchers());
        Assert.assertNull(clause.getProviderMatchers());
    }

    @Test
    public final void testGetClause_remoteIpPrefix() {
        secRuleBuilder.setDirection(DirectionIngress.class);
        secRuleBuilder.setEthertype(EthertypeV4.class);
        secRuleBuilder.setRemoteIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/8")));
        Clause clause = SecRuleEntityDecoder.getClause(secRuleBuilder.build());
        ClauseName expectedClauseName = SecRuleNameDecoder.getClauseName(secRuleBuilder.build());
        Assert.assertEquals(expectedClauseName, clause.getName());
        List<SubjectName> subjectRefs = clause.getSubjectRefs();
        Assert.assertNotNull(subjectRefs);
        Assert.assertEquals(1, subjectRefs.size());
        SubjectName subjectNameFromClause = subjectRefs.get(0);
        SubjectName expectedSubjectName = SecRuleNameDecoder.getSubjectName(secRuleBuilder.build());
        Assert.assertEquals(expectedSubjectName, subjectNameFromClause);
        Assert.assertNull(clause.getProviderMatchers());
        ConsumerMatchers consumerMatchers = clause.getConsumerMatchers();
        Assert.assertNotNull(consumerMatchers);
        Assert.assertNull(consumerMatchers.getConditionMatcher());
        Assert.assertNull(consumerMatchers.getGroupIdentificationConstraints());
        EndpointIdentificationConstraints endpointIdentificationConstraints =
                consumerMatchers.getEndpointIdentificationConstraints();
        Assert.assertNotNull(endpointIdentificationConstraints);
        L3EndpointIdentificationConstraints l3EndpointIdentificationConstraints =
                endpointIdentificationConstraints.getL3EndpointIdentificationConstraints();
        Assert.assertNotNull(l3EndpointIdentificationConstraints);
        List<PrefixConstraint> prefixConstraints = l3EndpointIdentificationConstraints.getPrefixConstraint();
        Assert.assertNotNull(prefixConstraints);
        Assert.assertEquals(1, prefixConstraints.size());
        PrefixConstraint prefixConstraint = prefixConstraints.get(0);
        Assert.assertEquals(new Ipv4Prefix("10.0.0.0/8"), prefixConstraint.getIpPrefix().getIpv4Prefix());
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeIPv4() {
        SecurityRule one = secRuleBuilder.setEthertype(EthertypeV4.class).build();
        SecurityRule two = secRuleBuilder.setEthertype(EthertypeV4.class).build();
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeNull() {
        SecurityRule one = secRuleBuilder.setEthertype(EthertypeV4.class).build();
        SecurityRule two = secRuleBuilder.setEthertype(null).build();
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeNullTwoEthTypeNull() {
        SecurityRule one = secRuleBuilder.setEthertype(null).build();
        SecurityRule two = secRuleBuilder.setEthertype(null).build();
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeIPv6() {
        SecurityRule one = secRuleBuilder.setEthertype(EthertypeV4.class).build();
        SecurityRule two = secRuleBuilder.setEthertype(EthertypeV6.class).build();
        assertFalse(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeNullTwoEthTypeIPv4() {
        SecurityRule one = secRuleBuilder.setEthertype(null).build();
        SecurityRule two = secRuleBuilder.setEthertype(EthertypeV4.class).build();
        assertFalse(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolTcp() {
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule one = secRuleBuilder.setProtocol(protocolTcp).build();
        SecurityRule two = secRuleBuilder.setProtocol(protocolTcp).build();
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolNull() {
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule one = secRuleBuilder.setProtocol(protocolTcp).build();
        SecurityRule two = secRuleBuilder.setProtocol(null).build();
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolNullTwoProtocolNull() {
        SecurityRule one = secRuleBuilder.setProtocol(null).build();
        SecurityRule two = secRuleBuilder.setProtocol(null).build();
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolUdp() {
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule one = secRuleBuilder.setProtocol(protocolTcp).build();
        SecurityRuleAttributes.Protocol protocolUdp = new SecurityRuleAttributes.Protocol(ProtocolUdp.class);
        SecurityRule two = secRuleBuilder.setProtocol(protocolUdp).build();
        assertFalse(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolNullTwoProtocolTcp() {
        SecurityRule one = secRuleBuilder.setProtocol(null).build();
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule two = secRuleBuilder.setProtocol(protocolTcp).build();
        assertFalse(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinGtTwoPortMinOnePortMaxLtTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(6, 9);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinEqTwoPortMinOnePortMaxEqTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(5, 10);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinTwoPortMinNullOnePortMaxTwoPortMaxNull() {
        SecurityRule one = createSecRuleWithMinMaxPort(4, 9);
        SecurityRule two = createSecRuleWithMinMaxPort(null, null);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinNullTwoPortMinNullOnePortMaxNullTwoPortMaxNull() {
        SecurityRule one = createSecRuleWithMinMaxPort(null, null);
        SecurityRule two = createSecRuleWithMinMaxPort(null, null);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinNullTwoPortMinOnePortMaxNullTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(null, null);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinLtTwoPortMinOnePortMaxLtTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(4, 9);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinGtTwoPortMinOnePortMaxGtTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(6, 11);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinLtTwoPortMinOnePortMaxGtTwoPortMax() {
        SecurityRule one = createSecRuleWithMinMaxPort(4, 11);
        SecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    private SecurityRule createSecRuleWithMinMaxPort(Integer portMin, Integer portMax) {
        return secRuleBuilder.setPortRangeMin(portMin).setPortRangeMax(portMax).build();
    }

    @Test
    public final void testGetEtherType_ethertypeIPv4() {
        secRuleBuilder.setEthertype(EthertypeV4.class);
        Assert.assertEquals(EtherTypeClassifierDefinition.IPv4_VALUE, SecRuleEntityDecoder.getEtherType(secRuleBuilder.build()));
    }

    @Test
    public final void testGetEtherType_ethertypeIPv6() {
        secRuleBuilder.setEthertype(EthertypeV6.class);
        Assert.assertEquals(EtherTypeClassifierDefinition.IPv6_VALUE, SecRuleEntityDecoder.getEtherType(secRuleBuilder.build()));
    }

    @Test
    public final void testGetEtherType_ethertypeNull() {
        secRuleBuilder.setEthertype(null);
        Assert.assertNull(SecRuleEntityDecoder.getEtherType(secRuleBuilder.build()));
    }

    @Test
    public final void testGetEtherType_ethertypeUnknown() {
        secRuleBuilder.setEthertype(UnknownEthertype.class);
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getEtherType(secRuleBuilder.build());
    }

    private static class UnknownEthertype extends EthertypeBase {
    }

    @Test
    public final void testGetProtocol_protoTcp() {
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        secRuleBuilder.setProtocol(protocolTcp);
        Assert.assertEquals(IpProtoClassifierDefinition.TCP_VALUE, SecRuleEntityDecoder.getProtocol(secRuleBuilder.build()));
    }

    @Test
    public final void testGetProtocol_protoUdp() {
        SecurityRuleAttributes.Protocol protocolUdp = new SecurityRuleAttributes.Protocol(ProtocolUdp.class);
        secRuleBuilder.setProtocol(protocolUdp);
        Assert.assertEquals(IpProtoClassifierDefinition.UDP_VALUE, SecRuleEntityDecoder.getProtocol(secRuleBuilder.build()));
    }

    @Test
    public final void testGetProtocol_protoIcmp() {
        SecurityRuleAttributes.Protocol protocolIcmp = new SecurityRuleAttributes.Protocol(ProtocolIcmp.class);
        secRuleBuilder.setProtocol(protocolIcmp);
        Assert.assertEquals(IpProtoClassifierDefinition.ICMP_VALUE, SecRuleEntityDecoder.getProtocol(secRuleBuilder.build()));
    }

    @Test
    public final void testGetProtocol_protoNull() {
        secRuleBuilder.setProtocol(null);
        Assert.assertNull(SecRuleEntityDecoder.getProtocol(secRuleBuilder.build()));
    }

    @Test
    public final void testGetProtocol_protoUnknown() {
        SecurityRuleAttributes.Protocol protocolUnknown = new SecurityRuleAttributes.Protocol(UnknownProtocol.class);
        secRuleBuilder.setProtocol(protocolUnknown);
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getProtocol(secRuleBuilder.build());
    }

    private static class UnknownProtocol extends ProtocolBase {
    }
}
