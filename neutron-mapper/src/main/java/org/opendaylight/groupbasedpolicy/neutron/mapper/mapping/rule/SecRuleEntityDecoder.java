/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.AllowActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolUdp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.SecurityRuleAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

public class SecRuleEntityDecoder {

    private SecRuleEntityDecoder() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static ContractId getContractId(SecurityRule secRule) {
        return new ContractId(secRule.getUuid().getValue());
    }

    public static ClassifierInstance getClassifierInstance(SecurityRule secRule) {
        ClassifierInstanceBuilder classifierBuilder = new ClassifierInstanceBuilder();
        List<ParameterValue> params = new ArrayList<>();
        Integer portMin = secRule.getPortRangeMin();
        Integer portMax = secRule.getPortRangeMax();
        if (portMin != null && portMax != null) {
            classifierBuilder.setClassifierDefinitionId(L4ClassifierDefinition.DEFINITION.getId());
            if (portMin.equals(portMax)) {
                params.add(new ParameterValueBuilder().setName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM))
                    .setIntValue(portMin.longValue())
                    .build());
            } else {
                params.add(new ParameterValueBuilder()
                    .setName(new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
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
            params.add(new ParameterValueBuilder()
                .setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
                .setIntValue(protocol)
                .build());
        }
        Long ethertype = getEtherType(secRule);
        if (ethertype != null) {
            if (classifierBuilder.getClassifierDefinitionId() == null) {
                classifierBuilder.setClassifierDefinitionId(EtherTypeClassifierDefinition.DEFINITION.getId());
            }
            params.add(new ParameterValueBuilder()
                .setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
                .setIntValue(ethertype)
                .build());
        }
        ClassifierName classifierName = SecRuleNameDecoder.getClassifierInstanceName(secRule);
        return classifierBuilder.setParameterValue(params).setName(new ClassifierName(classifierName)).build();
    }

    public static ActionRef createActionRefFromActionChoice(ActionChoice action) {
        if (action instanceof SfcActionCase) {
            return MappingUtils.createSfcActionRef(((SfcActionCase) action).getSfcChainName());
        } else if (action instanceof AllowActionCase) {
            return MappingUtils.ACTION_REF_ALLOW;
        }
        return null;
    }

    public static ClassifierRef getClassifierRef(SecurityRule secRule) {
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
     * Resolves Direction for provided secRule.
     *
     * @param secRule rule for which Direction is resolved.
     * @return direction resolved from {@link SecurityRule#getDirection()}
     * @throws IllegalArgumentException if return value of
     *         {@link SecurityRule#getDirection()} is other than {@link DirectionIngress} or
     *         {@link DirectionEgress}
     */
    public static Direction getDirection(SecurityRule secRule) {
        Class<? extends DirectionBase> direction = secRule.getDirection();
        if (direction == null) {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
        if (direction.isAssignableFrom(DirectionIngress.class)) {
            return Direction.In;
        }
        if (direction.isAssignableFrom(DirectionEgress.class)) {
            return Direction.Out;
        }
        throw new IllegalArgumentException("Direction " + direction + " from security group rule "
                + secRule + " is not supported. Direction can be only 'ingress' or 'egress'.");
    }

    /**
     * Resolves Clause for provided secRule.
     *
     * @param secRule {@link SecurityRule#getRemoteIpPrefix()} is used for EIC
     *        and subject selection
     * @return clause with the subject and with a consumer matcher containing EIC
     */
    public static Clause getClause(SecurityRule secRule) {
        checkNotNull(secRule);
        SubjectName subjectName = SecRuleNameDecoder.getSubjectName(secRule);
        ClauseBuilder clauseBuilder = new ClauseBuilder()
            .setSubjectRefs(ImmutableList.of(subjectName))
            .setName(SecRuleNameDecoder
                .getClauseName(secRule));
        IpPrefix remoteIpPrefix = secRule.getRemoteIpPrefix();
        if (remoteIpPrefix != null) {
            clauseBuilder.setConsumerMatchers(createConsumerMatchersWithEic(remoteIpPrefix));
        }
        return clauseBuilder.build();
    }

    private static ConsumerMatchers createConsumerMatchersWithEic(IpPrefix ipPrefix) {
        PrefixConstraint consumerPrefixConstraint = new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build();
        EndpointIdentificationConstraints eic =
                new EndpointIdentificationConstraintsBuilder()
                    .setL3EndpointIdentificationConstraints(new L3EndpointIdentificationConstraintsBuilder()
                        .setPrefixConstraint(ImmutableList.<PrefixConstraint>of(consumerPrefixConstraint)).build())
                    .build();
        return new ConsumerMatchersBuilder().setEndpointIdentificationConstraints(eic).build();
    }

    public static boolean isEtherTypeOfOneWithinTwo(SecurityRule one, SecurityRule two) {
        Long oneEtherType = getEtherType(one);
        Long twoEtherType = getEtherType(two);
        return twoIsNullOrEqualsOne(oneEtherType, twoEtherType);
    }

    public static boolean isProtocolOfOneWithinTwo(SecurityRule one, SecurityRule two) {
        Long oneProtocol = getProtocol(one);
        Long twoProtocol = getProtocol(two);
        return twoIsNullOrEqualsOne(oneProtocol, twoProtocol);
    }

    private static <T> boolean twoIsNullOrEqualsOne(T one, T two) {
        if (two == null) {
            return true;
        }
        if (two.equals(one)) {
            return true;
        }
        return false;
    }

    public static boolean isPortsOfOneWithinTwo(SecurityRule one, SecurityRule two) {
        Integer onePortMin = one.getPortRangeMin();
        Integer onePortMax = one.getPortRangeMax();
        Integer twoPortMin = two.getPortRangeMin();
        Integer twoPortMax = two.getPortRangeMax();
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
     * Resolves EtherType for provided secRule.
     *
     * @param secRule rule for which EtherType is resolved.
     * @return {@code null} if {@link SecurityRule#getEthertype()} is null; Otherwise ethertype
     *         number
     * @throws IllegalArgumentException if return value of
     *         {@link SecurityRule#getEthertype()} is other {@link EthertypeV4} or
     *         {@link EthertypeV6}
     */
    public static Long getEtherType(SecurityRule secRule) {
        Class<? extends EthertypeBase> ethertype = secRule.getEthertype();
        if (ethertype == null) {
            return null;
        }
        if (ethertype.isAssignableFrom(EthertypeV4.class)) {
            return EtherTypeClassifierDefinition.IPv4_VALUE;
        }
        if (ethertype.isAssignableFrom(EthertypeV6.class)) {
            return EtherTypeClassifierDefinition.IPv6_VALUE;
        }
        throw new IllegalArgumentException("Ethertype " + ethertype + " is not supported.");
    }

    /**
     * Resolves Protocol for provided secRule.
     *
     * @param secRule rule for which Protocol is resolved.
     * @return {@code null} if {@link SecurityRule#getProtocol()} is null; Otherwise protocol number
     * @throws IllegalArgumentException if return value of
     *         {@link SecurityRule#getProtocol()} is other than {@link ProtocolTcp},
     *         {@link ProtocolUdp}, {@link ProtocolIcmp}, {@link ProtocolIcmpV6}
     */
    public static Long getProtocol(SecurityRule secRule) {
        SecurityRuleAttributes.Protocol protocol = secRule.getProtocol();
        if (protocol == null) {
            return null;
        }
        if (protocol.getUint8() != null) {
            return protocol.getUint8().longValue();
        }
        if (protocol.getIdentityref() != null) {
            if (protocol.getIdentityref().equals(ProtocolTcp.class)) {
                return IpProtoClassifierDefinition.TCP_VALUE;
            }
            if (protocol.getIdentityref().equals(ProtocolUdp.class)) {
                return IpProtoClassifierDefinition.UDP_VALUE;
            }
            if (protocol.getIdentityref().equals(ProtocolIcmp.class)) {
                return IpProtoClassifierDefinition.ICMP_VALUE;
            }
            if (protocol.getIdentityref().equals(ProtocolIcmpV6.class)) {
                return IpProtoClassifierDefinition.ICMPv6_VALUE;
            }
        }
        throw new IllegalArgumentException("Neutron Security Rule Protocol value " + protocol + " is not supported.");
    }
}
