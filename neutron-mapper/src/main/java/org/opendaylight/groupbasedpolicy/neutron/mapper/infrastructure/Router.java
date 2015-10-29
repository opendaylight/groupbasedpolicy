/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstanceBuilder;

import com.google.common.collect.ImmutableList;

public class Router {

    private static final ClassifierName IPV4_NAME = new ClassifierName("IPv4");
    private static final ClassifierName IPV6_NAME = new ClassifierName("IPv6");
    private static final SubjectName ROUTER_SUBJECT_NAME = new SubjectName("ALLOW_IPv4_IPv6");
    private static final Description ROUTER_CONTRACT_DESC =
            new Description("Allow IPv4 and IPv6 communication between router interfaces and endpoints.");
    /**
     * ID of {@link Contract}
     */
    public static final ContractId CONTRACT_ID = new ContractId("111bc60e-1110-11e5-885d-feff819cdc9f");
    /**
     * Contains rules with action {@link MappingUtils#ACTION_REF_ALLOW} matching IPv4 and IPv6
     * communication in both directions
     */
    public static final Contract CONTRACT;

    private static final Name ROUTER_EPG_NAME = new Name("ROUTER_PORTS");
    private static final Description ROUTER_EPG_DESC = new Description("Represents router's interfaces.");
    /**
     * ID of {@link #EPG}
     */
    public static final EndpointGroupId EPG_ID = new EndpointGroupId("1118172e-cd84-4933-a35f-749f9a651de9");
    /**
     * Router endpoint-group providing {@link #CONTRACT}
     */
    public static final EndpointGroup EPG;
    /**
     * {@link ConsumerNamedSelector} pointing to {@link #CONTRACT}
     */
    public static final ConsumerNamedSelector CONTRACT_CONSUMER_SELECTOR;

    static {
        CONTRACT = createContractRouter();
        CONTRACT_CONSUMER_SELECTOR = createConsumerSelector(CONTRACT);
        EPG = createRouterEpg();
    }

    private static EndpointGroup createRouterEpg() {
        ProviderNamedSelector routerProviderSelector = createProviderSelector(CONTRACT);
        return new EndpointGroupBuilder().setId(EPG_ID)
            .setName(ROUTER_EPG_NAME)
            .setProviderNamedSelector(ImmutableList.of(routerProviderSelector))
            .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
            .setDescription(ROUTER_EPG_DESC)
            .build();
    }

    private static ProviderNamedSelector createProviderSelector(Contract contract) {
        SelectorName selectorName = new SelectorName(contract.getSubject().get(0).getName().getValue());
        return new ProviderNamedSelectorBuilder().setName(selectorName)
            .setContract(ImmutableList.of(contract.getId()))
            .build();
    }

    private static ConsumerNamedSelector createConsumerSelector(Contract contract) {
        SelectorName selectorName = new SelectorName(contract.getSubject().get(0).getName().getValue());
        return new ConsumerNamedSelectorBuilder().setName(selectorName)
            .setContract(ImmutableList.of(contract.getId()))
            .build();
    }

    private static Contract createContractRouter() {
        Rule endpointRouterIpv4Rule = createRuleAllow(IPV4_NAME, Direction.In);
        Rule routerEndpointIpv4Rule = createRuleAllow(IPV4_NAME, Direction.Out);
        Rule endpointRouterIpv6Rule = createRuleAllow(IPV6_NAME, Direction.In);
        Rule routerEndpointIpv6Rule = createRuleAllow(IPV6_NAME, Direction.Out);
        Subject subject = new SubjectBuilder().setName(ROUTER_SUBJECT_NAME)
            .setOrder(0)
            .setRule(ImmutableList.of(endpointRouterIpv4Rule, routerEndpointIpv4Rule, endpointRouterIpv6Rule,
                    routerEndpointIpv6Rule))
            .build();
        return new ContractBuilder().setId(CONTRACT_ID)
            .setSubject(ImmutableList.of(subject))
            .setDescription(ROUTER_CONTRACT_DESC)
            .build();
    }

    private static Rule createRuleAllow(ClassifierName classifierName, Direction direction) {
        ClassifierName name =
                new ClassifierName(direction.name() + MappingUtils.NAME_DOUBLE_DELIMETER + classifierName.getValue());
        ClassifierRef classifierRef = new ClassifierRefBuilder().setName(name)
            .setInstanceName(classifierName)
            .setDirection(direction)
            .build();
        return new RuleBuilder().setName(new RuleName(name))
            .setActionRef(MappingUtils.ACTION_REF_ALLOW)
            .setClassifierRef(ImmutableList.of(classifierRef))
            .build();
    }

    /**
     * puts clause with {@link L3EndpointIdentificationConstraints} in {@link ConsumerMatchers}
     * and {@link ProviderMatchers}. This clause points to subject in {@link #CONTRACT}.
     *
     * @param tenantId location of {@link #CONTRACT}
     * @param ipPrefix used in {@link L3EndpointIdentificationConstraints}
     * @param wTx transaction where entities are written
     */
    public static void writeRouterClauseWithConsProvEic(TenantId tenantId, @Nullable IpPrefix ipPrefix,
            WriteTransaction wTx) {
        Clause clause = createClauseWithConsProvEic(ipPrefix, ROUTER_SUBJECT_NAME);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.clauseIid(tenantId, CONTRACT_ID, clause.getName()),
                clause, true);
    }

    private static Clause createClauseWithConsProvEic(@Nullable IpPrefix ipPrefix, SubjectName subjectName) {
        ConsumerMatchers consumerMatchers = null;
        ProviderMatchers providerMatchers = null;
        StringBuilder clauseName = new StringBuilder();
        clauseName.append(subjectName.getValue());
        if (ipPrefix != null) {
            clauseName.append(MappingUtils.NAME_DOUBLE_DELIMETER).append(Utils.getStringIpPrefix(ipPrefix));
            consumerMatchers =
                    new ConsumerMatchersBuilder()
                        .setEndpointIdentificationConstraints(new EndpointIdentificationConstraintsBuilder()
                            .setL3EndpointIdentificationConstraints(new L3EndpointIdentificationConstraintsBuilder()
                                .setPrefixConstraint(
                                        ImmutableList.of(new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build()))
                                .build())
                            .build())
                        .build();
            providerMatchers =
                    new ProviderMatchersBuilder()
                        .setEndpointIdentificationConstraints(new EndpointIdentificationConstraintsBuilder()
                            .setL3EndpointIdentificationConstraints(new L3EndpointIdentificationConstraintsBuilder()
                                .setPrefixConstraint(
                                        ImmutableList.of(new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build()))
                                .build())
                            .build())
                        .build();
        }
        return new ClauseBuilder().setName(new ClauseName(clauseName.toString()))
            .setSubjectRefs(ImmutableList.of(subjectName))
            .setConsumerMatchers(consumerMatchers)
            .setProviderMatchers(providerMatchers)
            .build();
    }

    /**
     * Puts router entities (classifier-instances, {@link #CONTRACT} and {@link #EPG}) to
     * {@link LogicalDatastoreType#CONFIGURATION}
     *
     * @param tenantId location of router entities
     * @param wTx transaction where router entities are written
     */
    public static void writeRouterEntitiesToTenant(TenantId tenantId, WriteTransaction wTx) {
        Set<ClassifierInstance> classifierInstances = getAllClassifierInstances();
        for (ClassifierInstance ci : classifierInstances) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierInstanceIid(tenantId, ci.getName()), ci,
                    true);
        }
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, CONTRACT_ID), CONTRACT, true);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, EPG_ID), EPG, true);
    }

    /**
     * @return All classifier-instances used in {@link CONTRACT}
     */
    public static Set<ClassifierInstance> getAllClassifierInstances() {
        HashSet<ClassifierInstance> cis = new HashSet<>();
        cis.add(createIpv4());
        cis.add(createIpv6());
        return cis;
    }

    private static ClassifierInstance createIpv4() {
        return new ClassifierInstanceBuilder().setName(IPV4_NAME)
            .setClassifierDefinitionId(EtherTypeClassifierDefinition.DEFINITION.getId())
            .setParameterValue(createParams(EtherTypeClassifierDefinition.IPv4_VALUE))
            .build();
    }

    private static ClassifierInstance createIpv6() {
        return new ClassifierInstanceBuilder().setName(IPV6_NAME)
            .setClassifierDefinitionId(EtherTypeClassifierDefinition.DEFINITION.getId())
            .setParameterValue(createParams(EtherTypeClassifierDefinition.IPv6_VALUE))
            .build();
    }

    private static List<ParameterValue> createParams(long etherType) {
        List<ParameterValue> params = new ArrayList<>();
        params.add(new ParameterValueBuilder().setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .setIntValue(etherType)
            .build());
        return params;
    }
}
