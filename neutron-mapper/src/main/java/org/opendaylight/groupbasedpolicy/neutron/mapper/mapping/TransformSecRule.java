package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.IpProtoClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.L4Classifier;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef.ConnectionTracking;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstanceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class TransformSecRule {

    private static final Logger LOG = LoggerFactory.getLogger(TransformSecRule.class);
    private static final List<ActionRef> ACTION_REF_ALLOW = ImmutableList.of(new ActionRefBuilder().setName(
            MappingUtils.ACTION_ALLOW.getName())
        .setOrder(0)
        .build());
    private final NeutronSecurityRule secRule;
    private final TenantId tenantId;
    private final EndpointGroupId providerEpgId;
    private final EndpointGroupId consumerEpgId;
    private final SubjectName subjectName;
    private final ClauseName clauseName;
    private final IpPrefix ipPrefix;
    private final int subjectOrder;
    private final ClassifierName classifierName;
    private final RuleName ruleName;

    /**
     * If a {@link NeutronSecurityRule#getSecurityRuleGroupID()} is {@link MappingUtils#EPG_DHCP_ID}
     * or {@link MappingUtils#EPG_ROUTER_ID} then the neutron security rule can contain remote ip
     * prefix besides remote ip security group. I this case {@link #getConsumerEpgId()} returns remote security group id.
     *
     * @param secRule
     */
    public TransformSecRule(NeutronSecurityRule secRule) {
        this.secRule = checkNotNull(secRule);
        tenantId = new TenantId(Utils.normalizeUuid(secRule.getSecurityRuleTenantID()));
        providerEpgId = new EndpointGroupId(secRule.getSecurityRuleGroupID());
        if (!Strings.isNullOrEmpty(secRule.getSecurityRemoteGroupID())) {
            consumerEpgId = new EndpointGroupId(secRule.getSecurityRemoteGroupID());
            if (isEpgIdRouterOrDhcp(providerEpgId) && !Strings.isNullOrEmpty(secRule.getSecurityRuleRemoteIpPrefix())) {
                ipPrefix = Utils.createIpPrefix(secRule.getSecurityRuleRemoteIpPrefix());
            } else {
                ipPrefix = null;
            }
            subjectOrder = 0;
        } else if (!Strings.isNullOrEmpty(secRule.getSecurityRuleRemoteIpPrefix())) {
            consumerEpgId = MappingUtils.EPG_ANY_ID;
            ipPrefix = Utils.createIpPrefix(secRule.getSecurityRuleRemoteIpPrefix());
            subjectOrder = 0;
        } else {
            consumerEpgId = MappingUtils.EPG_ANY_ID;
            ipPrefix = null;
            subjectOrder = 1;
        }
        subjectName = createSubjectName();
        clauseName = new ClauseName(subjectName.getValue());
        classifierName = new ClassifierName(MappingUtils.NEUTRON_RULE__ + secRule.getSecurityRuleUUID());
        ruleName = new RuleName(MappingUtils.NEUTRON_RULE__ + "Allow--" + classifierName.getValue());
    }

    private SubjectName createSubjectName() {
        if (ipPrefix == null) {
            return new SubjectName(MappingUtils.NEUTRON_RULE__ + providerEpgId.getValue() + "__"
                    + consumerEpgId.getValue());
        }
        String prefix = Utils.getStringIpPrefix(ipPrefix).replace('/', '_');
        return new SubjectName(MappingUtils.NEUTRON_RULE__ + providerEpgId.getValue() + "__" + prefix + "__"
                + consumerEpgId.getValue());
    }

    public Clause createClause() {
        ClauseBuilder clauseBuilder = new ClauseBuilder().setName(clauseName).setSubjectRefs(
                ImmutableList.of(subjectName));
        if (ipPrefix != null) {
            clauseBuilder.setConsumerMatchers(new ConsumerMatchersBuilder().setEndpointIdentificationConstraints(
                    new EndpointIdentificationConstraintsBuilder().setL3EndpointIdentificationConstraints(
                            new L3EndpointIdentificationConstraintsBuilder().setPrefixConstraint(
                                    ImmutableList.<PrefixConstraint>of(new PrefixConstraintBuilder().setIpPrefix(
                                            ipPrefix).build())).build()).build()).build());
            if (isEpgIdRouterOrDhcp(providerEpgId)) {
                clauseBuilder.setProviderMatchers(new ProviderMatchersBuilder().setEndpointIdentificationConstraints(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.EndpointIdentificationConstraintsBuilder().setL3EndpointIdentificationConstraints(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder().setPrefixConstraint(
                                        ImmutableList.<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint>of(new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder().setIpPrefix(
                                                ipPrefix)
                                            .build()))
                                    .build())
                            .build())
                    .build());
            }
        }
        return clauseBuilder.build();
    }

    private static boolean isEpgIdRouterOrDhcp(EndpointGroupId epgId) {
        return (MappingUtils.EPG_ROUTER_ID.equals(epgId) || MappingUtils.EPG_DHCP_ID.equals(epgId));
    }

    public ClassifierInstance createClassifier() {
        ClassifierInstanceBuilder classifierBuilder = new ClassifierInstanceBuilder().setName(classifierName);
        List<ParameterValue> params = new ArrayList<>();
        Integer portMin = secRule.getSecurityRulePortMin();
        Integer portMax = secRule.getSecurityRulePortMax();
        if (portMin != null && portMax != null) {
            classifierBuilder.setClassifierDefinitionId(L4Classifier.DEFINITION.getId());
            if (portMin.equals(portMax)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(L4Classifier.DST_PORT_PARAM))
                    .setIntValue(portMin.longValue())
                    .build());
            } else {
                params.add(new ParameterValueBuilder().setName(new ParameterName(L4Classifier.DST_PORT_RANGE_PARAM))
                    .setRangeValue(
                            new RangeValueBuilder().setMin(portMin.longValue()).setMax(portMax.longValue()).build())
                    .build());
            }
        }
        String protocol = secRule.getSecurityRuleProtocol();
        if (!Strings.isNullOrEmpty(protocol)) {
            if (classifierBuilder.getClassifierDefinitionId() == null) {
                classifierBuilder.setClassifierDefinitionId(IpProtoClassifier.DEFINITION.getId());
            }
            if (NeutronUtils.TCP.equals(protocol)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(IpProtoClassifier.PROTO_PARAM))
                    .setIntValue(IpProtoClassifier.TCP_VALUE)
                    .build());
            } else if (NeutronUtils.UDP.equals(protocol)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(IpProtoClassifier.PROTO_PARAM))
                    .setIntValue(IpProtoClassifier.UDP_VALUE)
                    .build());
            } else if (NeutronUtils.ICMP.equals(protocol)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(IpProtoClassifier.PROTO_PARAM))
                    .setIntValue(1L)
                    .build());
            } else if (NeutronUtils.NULL.equals(protocol)) {
                LOG.debug("Protocol is not specified in security group rule {}", secRule.getSecurityRuleUUID());
            } else {
                throw new IllegalArgumentException("Protocol " + protocol + " is not supported.");
            }
        }
        String ethertype = secRule.getSecurityRuleEthertype();
        if (!Strings.isNullOrEmpty(ethertype)) {
            if (classifierBuilder.getClassifierDefinitionId() == null) {
                classifierBuilder.setClassifierDefinitionId(EtherTypeClassifier.DEFINITION.getId());
            }
            if (NeutronUtils.IPv4.equals(ethertype)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(EtherTypeClassifier.ETHERTYPE_PARAM))
                    .setIntValue(EtherTypeClassifier.IPv4_VALUE)
                    .build());
            } else if (NeutronUtils.IPv6.equals(ethertype)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(EtherTypeClassifier.ETHERTYPE_PARAM))
                    .setIntValue(EtherTypeClassifier.IPv6_VALUE)
                    .build());
            } else {
                throw new IllegalArgumentException("Ethertype " + ethertype + " is not supported.");
            }
        }
        return classifierBuilder.setParameterValue(params).build();
    }

    public Rule createRule(int order) {
        return new RuleBuilder().setName(ruleName)
            .setOrder(order)
            .setActionRef(ACTION_REF_ALLOW)
            .setClassifierRef(ImmutableList.of(createClassifierRef()))
            .build();
    }

    public Subject createSubject() {
        return new SubjectBuilder().setName(subjectName).setOrder(subjectOrder).build();
    }

    private ClassifierRef createClassifierRef() {
        ClassifierRefBuilder classifierRefBuilder = new ClassifierRefBuilder().setName(classifierName)
            .setConnectionTracking(ConnectionTracking.Reflexive)
            .setInstanceName(classifierName);
        String direction = secRule.getSecurityRuleDirection();
        if (NeutronUtils.INGRESS.equals(direction)) {
            classifierRefBuilder.setDirection(Direction.In);
        } else if (NeutronUtils.EGRESS.equals(direction)) {
            classifierRefBuilder.setDirection(Direction.Out);
        } else {
            throw new IllegalArgumentException("Direction " + direction + " from security group rule "
                    + secRule.getSecurityRuleUUID() + " is not supported. Direction can be only 'ingress' or 'egress'.");
        }
        return classifierRefBuilder.build();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public EndpointGroupId getProviderEpgId() {
        return providerEpgId;
    }

    public EndpointGroupId getConsumerEpgId() {
        return consumerEpgId;
    }

    public SubjectName getSubjectName() {
        return subjectName;
    }

    public ClauseName getClauseName() {
        return clauseName;
    }

    public IpPrefix getIpPrefix() {
        return ipPrefix;
    }

    public ClassifierName getClassifierName() {
        return classifierName;
    }

    public RuleName getRuleName() {
        return ruleName;
    }

}
