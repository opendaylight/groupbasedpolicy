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
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleEntityDecoder;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleNameDecoder;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.IpProtoClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.L4Classifier;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;

public class SecRuleEntityDecoderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private NeutronSecurityRule secRule;

    @Before
    public void setUp() throws Exception {
        secRule = new NeutronSecurityRule();
    }

    @Test
    public final void testGetTenantId_lowercaseUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567-abcd-ef01-0123-0123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_uppercaseUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567-ABCD-EF01-0123-0123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_mixUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567-ABCD-ef01-0123-0123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_noSlashLowercaseUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567abcdef0101230123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_noSlashUppercaseUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567ABCDEF0101230123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_noSlashMixUuidTenantID() {
        secRule.setSecurityRuleTenantID("01234567ABCDef0101230123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getTenantId(secRule)
            .getValue());
    }

    @Test
    public final void testGetTenantId_emptyUuidTenantID() {
        secRule.setSecurityRuleTenantID("");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getTenantId(secRule);
    }

    @Test
    public final void testGetTenantId_badLengthUuidTenantID() {
        secRule.setSecurityRuleTenantID("abc");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getTenantId(secRule);
    }

    @Test
    public final void testGetTenantId_badContentUuidTenantID() {
        secRule.setSecurityRuleTenantID("xyz34567-abcd-ef01-0123-0123456789ab");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getTenantId(secRule);
    }

    @Test
    public final void testGetTenantId_nullUuidTenantID() {
        secRule.setSecurityRuleTenantID(null);
        thrown.expect(NullPointerException.class);
        SecRuleEntityDecoder.getTenantId(secRule);
    }

    @Test
    public final void testGetProviderEpgId_lowercaseUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567-abcd-ef01-0123-0123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_uppercaseUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567-ABCD-EF01-0123-0123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_mixUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567-ABCD-ef01-0123-0123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_noSlashLowercaseUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567abcdef0101230123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_noSlashUppercaseUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567ABCDEF0101230123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_noSlashMixUuidGroupID() {
        secRule.setSecurityRuleGroupID("01234567ABCDef0101230123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getProviderEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetProviderEpgId_emptyUuidGroupID() {
        secRule.setSecurityRuleGroupID("");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getProviderEpgId(secRule);
    }

    @Test
    public final void testGetProviderEpgId_badLengthUuidGroupID() {
        secRule.setSecurityRuleGroupID("abcdxy");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getProviderEpgId(secRule);
    }

    @Test
    public final void testGetProviderEpgId_badContentUuidGroupID() {
        secRule.setSecurityRuleGroupID("xyz34567-abcd-ef01-0123-0123456789ab");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getProviderEpgId(secRule);
    }

    @Test
    public final void testGetProviderEpgId_nullUuidGroupID() {
        secRule.setSecurityRuleGroupID(null);
        thrown.expect(NullPointerException.class);
        SecRuleEntityDecoder.getProviderEpgId(secRule);
    }

    @Test
    public final void testGetConsumerEpgId_lowercaseUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567-abcd-ef01-0123-0123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_uppercaseUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567-ABCD-EF01-0123-0123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_mixUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567-ABCD-ef01-0123-0123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_noSlashLowercaseUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567abcdef0101230123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_noSlashUppercaseUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567ABCDEF0101230123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_noSlashMixUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("01234567ABCDef0101230123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getConsumerEpgId(secRule)
            .getValue());
    }

    @Test
    public final void testGetConsumerEpgId_emptyUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("");
        Assert.assertSame(null, SecRuleEntityDecoder.getConsumerEpgId(secRule));
    }

    @Test
    public final void testGetConsumerEpgId_badLengthUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("abc");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getConsumerEpgId(secRule);
    }

    @Test
    public final void testGetConsumerEpgId_badContentUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID("xyz34567-abcd-ef01-0123-0123456789ab");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getConsumerEpgId(secRule);
    }

    @Test
    public final void testGetConsumerEpgId_nullUuidRemoteGroupID() {
        secRule.setSecurityRemoteGroupID(null);
        Assert.assertSame(null, SecRuleEntityDecoder.getConsumerEpgId(secRule));
    }

    @Test
    public final void testGetContractId_lowercaseUuidID() {
        secRule.setSecurityRuleUUID("01234567-abcd-ef01-0123-0123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_uppercaseUuidID() {
        secRule.setSecurityRuleUUID("01234567-ABCD-EF01-0123-0123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_mixUuidID() {
        secRule.setSecurityRuleUUID("01234567-ABCD-ef01-0123-0123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_noSlashLowercaseUuidID() {
        secRule.setSecurityRuleUUID("01234567abcdef0101230123456789ab");
        Assert.assertEquals("01234567-abcd-ef01-0123-0123456789ab", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_noSlashUppercaseUuidID() {
        secRule.setSecurityRuleUUID("01234567ABCDEF0101230123456789AB");
        Assert.assertEquals("01234567-ABCD-EF01-0123-0123456789AB", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_noSlashMixUuidID() {
        secRule.setSecurityRuleUUID("01234567ABCDef0101230123456789Ab");
        Assert.assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", SecRuleEntityDecoder.getContractId(secRule)
            .getValue());
    }

    @Test
    public final void testGetContractId_emptyUuidID() {
        secRule.setSecurityRuleUUID("");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getContractId(secRule);
    }

    @Test
    public final void testGetContractId_badLengthUuidID() {
        secRule.setSecurityRuleUUID("abcdxy");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getContractId(secRule);
    }

    @Test
    public final void testGetContractId_badContentUuidID() {
        secRule.setSecurityRuleUUID("xyz34567-abcd-ef01-0123-0123456789ab");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getContractId(secRule);
    }

    @Test
    public final void testGetContractId_nullUuidID() {
        secRule.setSecurityRuleUUID(null);
        thrown.expect(NullPointerException.class);
        SecRuleEntityDecoder.getContractId(secRule);
    }

    @Test
    public final void testGetClassifierInstance_onlyEthertype() {
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Assert.assertEquals(EtherTypeClassifier.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is ether_type_IPv4
        String expectedName = new StringBuilder().append(EtherTypeClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.IPv4)
            .toString();
        Assert.assertEquals(expectedName, ci.getName().getValue());
        Assert.assertEquals(expectedName, ci.getName().getValue());
        List<ParameterValue> parameterValues = ci.getParameterValue();
        Assert.assertNotNull(parameterValues);
        Assert.assertEquals(1, parameterValues.size());
        ParameterValue parameter = parameterValues.get(0);
        Assert.assertEquals(EtherTypeClassifier.ETHERTYPE_PARAM, parameter.getName().getValue());
        assertClassifierParameterValue(parameter, EtherTypeClassifier.IPv4_VALUE, null, null);
    }

    @Test
    public final void testGetClassifierInstance_EthertypeAndProtocol() {
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleProtocol(NeutronUtils.TCP);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Assert.assertEquals(IpProtoClassifier.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is ip_proto_tcp__ether_type_IPv4
        String expectedName = new StringBuilder().append(IpProtoClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.TCP)
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.IPv4)
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
            if (EtherTypeClassifier.ETHERTYPE_PARAM.equals(parameterName.getValue())) {
                containsEthertypeParam = true;
                assertClassifierParameterValue(parameter, EtherTypeClassifier.IPv4_VALUE, null, null);
            } else if (IpProtoClassifier.PROTO_PARAM.equals(parameterName.getValue())) {
                containsProtoParam = true;
                assertClassifierParameterValue(parameter, IpProtoClassifier.TCP_VALUE, null, null);
            } else {
                fail("This parameter is not expected: " + parameter);
            }
        }
        Assert.assertTrue("Classifier-instance does not contain ethertype parameter", containsEthertypeParam);
        Assert.assertTrue("Classifier-instance does not contain protocol parameter", containsProtoParam);
    }

    @Test
    public final void testGetClassifierInstance_EthertypeAndProtocolAndPort() {
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleProtocol(NeutronUtils.TCP);
        secRule.setSecurityRulePortMin(5);
        secRule.setSecurityRulePortMax(5);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Assert.assertEquals(L4Classifier.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is l4_destport-5__ip_proto-tcp__ether_type-IPv4
        String expectedName = new StringBuilder().append(L4Classifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_DELIMETER)
            .append(L4Classifier.DST_PORT_PARAM)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRule.getSecurityRulePortMin())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(IpProtoClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.TCP)
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.IPv4)
            .toString();
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
            if (EtherTypeClassifier.ETHERTYPE_PARAM.equals(parameterName.getValue())) {
                containsEthertypeParam = true;
                assertClassifierParameterValue(parameter, EtherTypeClassifier.IPv4_VALUE, null, null);
            } else if (IpProtoClassifier.PROTO_PARAM.equals(parameterName.getValue())) {
                containsProtoParam = true;
                assertClassifierParameterValue(parameter, IpProtoClassifier.TCP_VALUE, null, null);
            } else if (L4Classifier.DST_PORT_PARAM.equals(parameterName.getValue())) {
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
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleProtocol(NeutronUtils.TCP);
        secRule.setSecurityRulePortMin(5);
        secRule.setSecurityRulePortMax(10);
        ClassifierInstance ci = SecRuleEntityDecoder.getClassifierInstance(secRule);
        Assert.assertEquals(L4Classifier.DEFINITION.getId(), ci.getClassifierDefinitionId());
        // name is l4_destport_range_min-5_max-10__ip_proto-tcp__ether_type-IPv4
        String expectedName = new StringBuilder().append(L4Classifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_DELIMETER)
            .append(L4Classifier.DST_PORT_RANGE_PARAM)
            .append(SecRuleNameDecoder.MIN_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRule.getSecurityRulePortMin())
            .append(SecRuleNameDecoder.MAX_PORT)
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(secRule.getSecurityRulePortMax())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(IpProtoClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.TCP)
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(EtherTypeClassifier.DEFINITION.getName().getValue())
            .append(MappingUtils.NAME_VALUE_DELIMETER)
            .append(NeutronUtils.IPv4)
            .toString();
        Assert.assertEquals(expectedName, ci.getName().getValue());
    }

    @Test
    public final void testGetClassifierRef() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        ClassifierName expectedName = SecRuleNameDecoder.getClassifierRefName(secRule);
        ClassifierRef cr = SecRuleEntityDecoder.getClassifierRef(secRule);
        Assert.assertEquals(expectedName, cr.getName());
    }

    @Test
    public final void testGetDirection_directionIngress() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        Assert.assertEquals(Direction.In, SecRuleEntityDecoder.getDirection(secRule));
    }

    @Test
    public final void testGetDirection_directionEgress() {
        secRule.setSecurityRuleDirection(NeutronUtils.EGRESS);
        Assert.assertEquals(Direction.Out, SecRuleEntityDecoder.getDirection(secRule));
    }

    @Test
    public final void testGetDirection_directionNull() {
        secRule.setSecurityRuleDirection(null);
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getDirection(secRule);
    }

    @Test
    public final void testGetDirection_directionUnknown() {
        secRule.setSecurityRuleDirection("foo");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getDirection(secRule);
    }

    @Test
    public final void testGetClause_noRemoteIpPrefix() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        ClauseName expectedClauseName = SecRuleNameDecoder.getClauseName(secRule);
        Clause clause = SecRuleEntityDecoder.getClause(secRule);
        Assert.assertEquals(expectedClauseName, clause.getName());
        List<SubjectName> subjectRefs = clause.getSubjectRefs();
        Assert.assertNotNull(subjectRefs);
        Assert.assertEquals(1, subjectRefs.size());
        SubjectName subjectNameFromClause = subjectRefs.get(0);
        SubjectName expectedSubjectName = SecRuleNameDecoder.getSubjectName(secRule);
        Assert.assertEquals(expectedSubjectName, subjectNameFromClause);
        Assert.assertNull(clause.getConsumerMatchers());
        Assert.assertNull(clause.getProviderMatchers());
    }

    @Test
    public final void testGetClause_remoteIpPrefix() {
        secRule.setSecurityRuleDirection(NeutronUtils.INGRESS);
        secRule.setSecurityRuleEthertype(NeutronUtils.IPv4);
        secRule.setSecurityRuleRemoteIpPrefix("10.0.0.0/8");
        Clause clause = SecRuleEntityDecoder.getClause(secRule);
        ClauseName expectedClauseName = SecRuleNameDecoder.getClauseName(secRule);
        Assert.assertEquals(expectedClauseName, clause.getName());
        List<SubjectName> subjectRefs = clause.getSubjectRefs();
        Assert.assertNotNull(subjectRefs);
        Assert.assertEquals(1, subjectRefs.size());
        SubjectName subjectNameFromClause = subjectRefs.get(0);
        SubjectName expectedSubjectName = SecRuleNameDecoder.getSubjectName(secRule);
        Assert.assertEquals(expectedSubjectName, subjectNameFromClause);
        Assert.assertNull(clause.getProviderMatchers());
        ConsumerMatchers consumerMatchers = clause.getConsumerMatchers();
        Assert.assertNotNull(consumerMatchers);
        Assert.assertNull(consumerMatchers.getConditionMatcher());
        Assert.assertNull(consumerMatchers.getGroupIdentificationConstraints());
        EndpointIdentificationConstraints endpointIdentificationConstraints = consumerMatchers.getEndpointIdentificationConstraints();
        Assert.assertNotNull(endpointIdentificationConstraints);
        L3EndpointIdentificationConstraints l3EndpointIdentificationConstraints = endpointIdentificationConstraints.getL3EndpointIdentificationConstraints();
        Assert.assertNotNull(l3EndpointIdentificationConstraints);
        List<PrefixConstraint> prefixConstraints = l3EndpointIdentificationConstraints.getPrefixConstraint();
        Assert.assertNotNull(prefixConstraints);
        Assert.assertEquals(1, prefixConstraints.size());
        PrefixConstraint prefixConstraint = prefixConstraints.get(0);
        Assert.assertEquals(new Ipv4Prefix("10.0.0.0/8"), prefixConstraint.getIpPrefix().getIpv4Prefix());
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeIPv4() {
        NeutronSecurityRule one = createSecRuleWithEtherType(NeutronUtils.IPv4);
        NeutronSecurityRule two = createSecRuleWithEtherType(NeutronUtils.IPv4);
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeNull() {
        NeutronSecurityRule one = createSecRuleWithEtherType(NeutronUtils.IPv4);
        NeutronSecurityRule two = createSecRuleWithEtherType(null);
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeNullTwoEthTypeNull() {
        NeutronSecurityRule one = createSecRuleWithEtherType(null);
        NeutronSecurityRule two = createSecRuleWithEtherType(null);
        assertTrue(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeIPv4TwoEthTypeIPv6() {
        NeutronSecurityRule one = createSecRuleWithEtherType(NeutronUtils.IPv4);
        NeutronSecurityRule two = createSecRuleWithEtherType(NeutronUtils.IPv6);
        assertFalse(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsEtherTypeOfOneWithinTwo_oneEthTypeNullTwoEthTypeIPv4() {
        NeutronSecurityRule one = createSecRuleWithEtherType(null);
        NeutronSecurityRule two = createSecRuleWithEtherType(NeutronUtils.IPv4);
        assertFalse(SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two));
    }

    private final NeutronSecurityRule createSecRuleWithEtherType(String etherType) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRuleEthertype(etherType);
        return secRule;
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolTcp() {
        NeutronSecurityRule one = createSecRuleWithProtocol(NeutronUtils.TCP);
        NeutronSecurityRule two = createSecRuleWithProtocol(NeutronUtils.TCP);
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolNull() {
        NeutronSecurityRule one = createSecRuleWithProtocol(NeutronUtils.TCP);
        NeutronSecurityRule two = createSecRuleWithProtocol(null);
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolNullTwoProtocolNull() {
        NeutronSecurityRule one = createSecRuleWithProtocol(null);
        NeutronSecurityRule two = createSecRuleWithProtocol(null);
        assertTrue(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolTcpTwoProtocolUdp() {
        NeutronSecurityRule one = createSecRuleWithProtocol(NeutronUtils.TCP);
        NeutronSecurityRule two = createSecRuleWithProtocol(NeutronUtils.UDP);
        assertFalse(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    @Test
    public void testIsProtocolOfOneWithinTwo_oneProtocolNullTwoProtocolTcp() {
        NeutronSecurityRule one = createSecRuleWithProtocol(null);
        NeutronSecurityRule two = createSecRuleWithProtocol(NeutronUtils.TCP);
        assertFalse(SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two));
    }

    private NeutronSecurityRule createSecRuleWithProtocol(String protocol) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRuleProtocol(protocol);
        return secRule;
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinGtTwoPortMinOnePortMaxLtTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(6, 9);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinEqTwoPortMinOnePortMaxEqTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(5, 10);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinTwoPortMinNullOnePortMaxTwoPortMaxNull() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(4, 9);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(null, null);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinNullTwoPortMinNullOnePortMaxNullTwoPortMaxNull() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(null, null);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(null, null);
        assertTrue(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinNullTwoPortMinOnePortMaxNullTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(null, null);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinLtTwoPortMinOnePortMaxLtTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(4, 9);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinGtTwoPortMinOnePortMaxGtTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(6, 11);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    @Test
    public final void testIsPortsOfOneWithinTwo_onePortMinLtTwoPortMinOnePortMaxGtTwoPortMax() {
        NeutronSecurityRule one = createSecRuleWithMinMaxPort(4, 11);
        NeutronSecurityRule two = createSecRuleWithMinMaxPort(5, 10);
        assertFalse(SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two));
    }

    private NeutronSecurityRule createSecRuleWithMinMaxPort(Integer portMin, Integer portMax) {
        NeutronSecurityRule secRule = new NeutronSecurityRule();
        secRule.setSecurityRulePortMin(portMin);
        secRule.setSecurityRulePortMax(portMax);
        return secRule;
    }

    @Test
    public final void testGetEtherType_ethertypeIPv4() {
        secRule.setSecurityRuleEthertype("IPv4");
        Assert.assertEquals(EtherTypeClassifier.IPv4_VALUE, SecRuleEntityDecoder.getEtherType(secRule));
    }

    @Test
    public final void testGetEtherType_ethertypeIPv6() {
        secRule.setSecurityRuleEthertype("IPv6");
        Assert.assertEquals(EtherTypeClassifier.IPv6_VALUE, SecRuleEntityDecoder.getEtherType(secRule));
    }

    @Test
    public final void testGetEtherType_ethertypeNull() {
        secRule.setSecurityRuleEthertype(null);
        Assert.assertNull(SecRuleEntityDecoder.getEtherType(secRule));
    }

    @Test
    public final void testGetEtherType_ethertypeEmptyString() {
        secRule.setSecurityRuleEthertype("");
        Assert.assertNull(SecRuleEntityDecoder.getEtherType(secRule));
    }

    @Test
    public final void testGetEtherType_ethertypeUnknown() {
        secRule.setSecurityRuleEthertype("foo");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getEtherType(secRule);
    }

    @Test
    public final void testGetProtocol_protoTcp() {
        secRule.setSecurityRuleProtocol("tcp");
        Assert.assertEquals(IpProtoClassifier.TCP_VALUE, SecRuleEntityDecoder.getProtocol(secRule));
    }

    @Test
    public final void testGetProtocol_protoUdp() {
        secRule.setSecurityRuleProtocol("udp");
        Assert.assertEquals(IpProtoClassifier.UDP_VALUE, SecRuleEntityDecoder.getProtocol(secRule));
    }

    @Test
    public final void testGetProtocol_protoIcmp() {
        secRule.setSecurityRuleProtocol("icmp");
        Assert.assertEquals(IpProtoClassifier.ICMP_VALUE, SecRuleEntityDecoder.getProtocol(secRule));
    }

    @Test
    public final void testGetProtocol_protoNull() {
        secRule.setSecurityRuleProtocol(null);
        Assert.assertNull(SecRuleEntityDecoder.getProtocol(secRule));
    }

    @Test
    public final void testGetProtocol_protoEmptyString() {
        secRule.setSecurityRuleProtocol("");
        Assert.assertNull(SecRuleEntityDecoder.getProtocol(secRule));
    }

    @Test
    public final void testGetProtocol_protoUnknown() {
        secRule.setSecurityRuleProtocol("foo");
        thrown.expect(IllegalArgumentException.class);
        SecRuleEntityDecoder.getProtocol(secRule);
    }
}
