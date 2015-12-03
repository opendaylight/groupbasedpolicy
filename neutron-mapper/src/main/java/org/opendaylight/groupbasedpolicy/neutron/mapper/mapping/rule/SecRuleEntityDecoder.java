/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.EtherTypeClassifier;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef.ConnectionTracking;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstanceBuilder;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class SecRuleEntityDecoder {

    private SecRuleEntityDecoder() {
        throw new UnsupportedOperationException("Cannot create an instace.");
    }

    public static TenantId getTenantId(NeutronSecurityRule secRule) {
        return new TenantId(Utils.normalizeUuid(secRule.getSecurityRuleTenantID()));
    }

    public static EndpointGroupId getProviderEpgId(NeutronSecurityRule secRule) {
        return new EndpointGroupId(Utils.normalizeUuid(secRule.getSecurityRuleGroupID()));
    }

    /**
     * @return {@code null} if {@link NeutronSecurityRule#getSecurityRemoteGroupID()} is null
     */
    public static @Nullable EndpointGroupId getConsumerEpgId(NeutronSecurityRule secRule) {
        if (Strings.isNullOrEmpty(secRule.getSecurityRemoteGroupID())) {
            return null;
        }
        return new EndpointGroupId(Utils.normalizeUuid(secRule.getSecurityRemoteGroupID()));
    }

    public static ContractId getContractId(NeutronSecurityRule secRule) {
        return new ContractId(Utils.normalizeUuid(secRule.getSecurityRuleUUID()));
    }

    public static ClassifierInstance getClassifierInstance(NeutronSecurityRule secRule) {
        ClassifierInstanceBuilder classifierBuilder = new ClassifierInstanceBuilder();
        List<ParameterValue> params = new ArrayList<>();
        Integer portMin = secRule.getSecurityRulePortMin();
        Integer portMax = secRule.getSecurityRulePortMax();
        if (portMin != null && portMax != null) {
            classifierBuilder.setClassifierDefinitionId(L4ClassifierDefinition.DEFINITION.getId());
            if (portMin.equals(portMax)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM))
                    .setIntValue(portMin.longValue())
                    .build());
            } else {
                params.add(new ParameterValueBuilder().setName(new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
                    .setRangeValue(
                            new RangeValueBuilder().setMin(portMin.longValue()).setMax(portMax.longValue()).build())
                    .build());
            }
        }
        Long protocol = getProtocol(secRule);
        if (protocol != null) {
            if (classifierBuilder.getClassifierDefinitionId() == null) {
                classifierBuilder.setClassifierDefinitionId(IpProtoClassifierDefinition.DEFINITION.getId());
            }
            params.add(new ParameterValueBuilder().setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
                .setIntValue(protocol)
                .build());
        }
        Long ethertype = getEtherType(secRule);
        if (ethertype != null) {
            if (classifierBuilder.getClassifierDefinitionId() == null) {
                classifierBuilder.setClassifierDefinitionId(EtherTypeClassifierDefinition.DEFINITION.getId());
            }
            params.add(new ParameterValueBuilder().setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
                .setIntValue(ethertype)
                .build());
        }
        ClassifierName classifierName = SecRuleNameDecoder.getClassifierInstanceName(secRule);
        return classifierBuilder.setParameterValue(params).setName(new ClassifierName(classifierName)).build();
    }

    public static ClassifierRef getClassifierRef(NeutronSecurityRule secRule) {
        checkNotNull(secRule);
        ClassifierName classifierInstanceName = SecRuleNameDecoder.getClassifierInstanceName(secRule);
        ClassifierRefBuilder classifierRefBuilder = new ClassifierRefBuilder()
            .setConnectionTracking(ConnectionTracking.Reflexive).setInstanceName(classifierInstanceName);
        Direction direction = getDirection(secRule);
        classifierRefBuilder.setDirection(direction);
        ClassifierName classifierRefName = SecRuleNameDecoder.getClassifierRefName(secRule);
        return classifierRefBuilder.setName(classifierRefName).build();
    }

    /**
     * @param secRule
     * @return direction resolved from {@link NeutronSecurityRule#getSecurityRuleDirection()}
     * @throws IllegalArgumentException if return value of
     *         {@link NeutronSecurityRule#getSecurityRuleDirection()} is other than "ingress" or
     *         "egress"
     */
    public static Direction getDirection(NeutronSecurityRule secRule) {
        String direction = secRule.getSecurityRuleDirection();
        if (NeutronUtils.INGRESS.equals(direction)) {
            return Direction.In;
        }
        if (NeutronUtils.EGRESS.equals(direction)) {
            return Direction.Out;
        }
        throw new IllegalArgumentException("Direction " + direction + " from security group rule "
                + secRule.getSecurityRuleUUID() + " is not supported. Direction can be only 'ingress' or 'egress'.");
    }

    /**
     * @param secRule {@link NeutronSecurityRule#getSecurityRuleRemoteIpPrefix()} is used for EIC
     *        and subject selection
     * @return clause with the subject and with a consumer matcher containing EIC
     */
    public static Clause getClause(NeutronSecurityRule secRule) {
        checkNotNull(secRule);
        SubjectName subjectName = SecRuleNameDecoder.getSubjectName(secRule);
        ClauseBuilder clauseBuilder =
                new ClauseBuilder().setSubjectRefs(ImmutableList.of(subjectName)).setName(SecRuleNameDecoder.getClauseName(secRule));
        String remoteIpPrefix = secRule.getSecurityRuleRemoteIpPrefix();
        if (!Strings.isNullOrEmpty(remoteIpPrefix)) {
            clauseBuilder.setConsumerMatchers(createConsumerMatchersWithEic(remoteIpPrefix));
        }
        return clauseBuilder.build();
    }

    private static ConsumerMatchers createConsumerMatchersWithEic(String remoteIpPrefix) {
        IpPrefix ipPrefix = Utils.createIpPrefix(remoteIpPrefix);
        PrefixConstraint consumerPrefixConstraint = new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build();
        EndpointIdentificationConstraints eic =
                new EndpointIdentificationConstraintsBuilder()
                    .setL3EndpointIdentificationConstraints(new L3EndpointIdentificationConstraintsBuilder()
                        .setPrefixConstraint(ImmutableList.<PrefixConstraint>of(consumerPrefixConstraint)).build())
                    .build();
        return new ConsumerMatchersBuilder().setEndpointIdentificationConstraints(eic).build();
    }

    public static boolean isEtherTypeOfOneWithinTwo(NeutronSecurityRule one, NeutronSecurityRule two) {
        Long oneEtherType = getEtherType(one);
        Long twoEtherType = getEtherType(two);
        return twoIsNullOrEqualsOne(oneEtherType, twoEtherType);
    }

    public static boolean isProtocolOfOneWithinTwo(NeutronSecurityRule one, NeutronSecurityRule two) {
        Long oneProtocol = getProtocol(one);
        Long twoProtocol = getProtocol(two);
        return twoIsNullOrEqualsOne(oneProtocol, twoProtocol);
    }

    private static <T> boolean twoIsNullOrEqualsOne(T one, T two) {
        if (two == null)
            return true;
        if (two.equals(one))
            return true;
        return false;
    }

    public static boolean isPortsOfOneWithinTwo(NeutronSecurityRule one, NeutronSecurityRule two) {
        Integer onePortMin = one.getSecurityRulePortMin();
        Integer onePortMax = one.getSecurityRulePortMax();
        Integer twoPortMin = two.getSecurityRulePortMin();
        Integer twoPortMax = two.getSecurityRulePortMax();
        if (twoPortMin == null && twoPortMax == null) {
            return true;
        }
        if ((onePortMin != null && twoPortMin != null && onePortMin >= twoPortMin)
                && (onePortMax != null && twoPortMax != null && onePortMax <= twoPortMax)) {
            return true;
        }
        return false;
    }

    /**
     * @param secRule
     * @return {@code null} if {@link NeutronSecurityRule#getSecurityRuleEthertype()} is null or
     *         empty; value of {@link EtherTypeClassifierDefinition#IPv4_VALUE} or
     *         {@link EtherTypeClassifierDefinition#IPv6_VALUE}
     * @throws IllegalArgumentException if return value of
     *         {@link NeutronSecurityRule#getSecurityRuleEthertype()} is not empty/null and is other
     *         than "IPv4" or "IPv6"
     */
    public static Long getEtherType(NeutronSecurityRule secRule) {
        String ethertype = secRule.getSecurityRuleEthertype();
        if (Strings.isNullOrEmpty(ethertype)) {
            return null;
        }
        if (NeutronUtils.IPv4.equals(ethertype)) {
            return EtherTypeClassifierDefinition.IPv4_VALUE;
        }
        if (NeutronUtils.IPv6.equals(ethertype)) {
            return EtherTypeClassifierDefinition.IPv6_VALUE;
        }
        throw new IllegalArgumentException("Ethertype " + ethertype + " is not supported.");
    }

    /**
     * @param secRule
     * @return {@code null} if {@link NeutronSecurityRule#getSecurityRuleProtocol()} is null or
     *         empty; Otherwise protocol number
     * @throws IllegalArgumentException if return value of
     *         {@link NeutronSecurityRule#getSecurityRuleProtocol()} is not empty/null and is other
     *         than "tcp", "udp", "icmp"
     */
    public static Long getProtocol(NeutronSecurityRule secRule) {
        String protocol = secRule.getSecurityRuleProtocol();
        if (Strings.isNullOrEmpty(protocol)) {
            return null;
        }
        if (NeutronUtils.TCP.equals(protocol)) {
            return IpProtoClassifierDefinition.TCP_VALUE;
        }
        if (NeutronUtils.UDP.equals(protocol)) {
            return IpProtoClassifierDefinition.UDP_VALUE;
        }
        if (NeutronUtils.ICMP.equals(protocol)) {
            return IpProtoClassifierDefinition.ICMP_VALUE;
        }
        throw new IllegalArgumentException("Protocol " + protocol + " is not supported.");
    }
}
