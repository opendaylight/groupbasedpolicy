/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ProviderMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;

public class ServiceUtil {

    protected static Contract createContract(ContractId cid, List<Subject> subjects, Description description) {
        return new ContractBuilder().setId(cid).setSubject(subjects).setDescription(description).build();
    }

    protected static ClassifierInstance createClassifInstance(ClassifierName name, ClassifierDefinitionId id,
        List<ParameterValue> pv) {
        return new ClassifierInstanceBuilder().setName(name)
            .setClassifierDefinitionId(id)
            .setParameterValue(pv)
            .build();
    }

    protected static EndpointGroupBuilder createEpgBuilder(EndpointGroupId epgId, Name name, Description description) {
        return new EndpointGroupBuilder().setId(epgId)
            .setName(name)
            .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
            .setDescription(description);
    }

    protected static List<ParameterValue> createParams(long etherType, long proto, @Nullable Long srcPort,
        @Nullable Long dstPort) {
        List<ParameterValue> params = new ArrayList<>();
        if (srcPort != null) {
            params.add(new ParameterValueBuilder().setName(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM))
                .setIntValue(srcPort)
                .build());
        }
        if (dstPort != null) {
            params.add(new ParameterValueBuilder().setName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM))
                .setIntValue(dstPort)
                .build());
        }
        params.add(new ParameterValueBuilder().setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
            .setIntValue(proto)
            .build());
        params.add(new ParameterValueBuilder().setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .setIntValue(etherType)
            .build());
        return params;
    }

    protected static Clause createClauseWithConsProvEic(@Nullable IpPrefix ipPrefix, SubjectName subjectName) {
        ConsumerMatchers consumerMatchers = null;
        ProviderMatchers providerMatchers = null;
        StringBuilder clauseName = new StringBuilder();
        clauseName.append(subjectName.getValue());
        if (ipPrefix != null) {
            clauseName.append(MappingUtils.NAME_DOUBLE_DELIMETER).append(Utils.getStringIpPrefix(ipPrefix));
            consumerMatchers =
                new ConsumerMatchersBuilder().setEndpointIdentificationConstraints(
                    new EndpointIdentificationConstraintsBuilder().setL3EndpointIdentificationConstraints(
                        new L3EndpointIdentificationConstraintsBuilder().setPrefixConstraint(
                            ImmutableList.of(new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build())).build())
                        .build()).build();
            providerMatchers =
                new ProviderMatchersBuilder().setEndpointIdentificationConstraints(
                    new EndpointIdentificationConstraintsBuilder().setL3EndpointIdentificationConstraints(
                        new L3EndpointIdentificationConstraintsBuilder().setPrefixConstraint(
                            ImmutableList.of(new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build())).build())
                        .build()).build();
        }
        return new ClauseBuilder().setName(new ClauseName(clauseName.toString()))
            .setSubjectRefs(ImmutableList.of(subjectName))
            .setConsumerMatchers(consumerMatchers)
            .setProviderMatchers(providerMatchers)
            .build();
    }

    protected static Rule createRuleAllow(ClassifierName classifierName, Direction direction) {
        ClassifierName name =
            new ClassifierName(direction.name() + MappingUtils.NAME_DOUBLE_DELIMETER + classifierName.getValue());
        ClassifierRef classifierRef =
            new ClassifierRefBuilder().setName(name).setInstanceName(classifierName).setDirection(direction).build();
        return new RuleBuilder().setName(new RuleName(name))
            .setActionRef(ImmutableList.<ActionRef>of(MappingUtils.ACTION_REF_ALLOW))
            .setClassifierRef(ImmutableList.of(classifierRef))
            .build();
    }

    protected static ProviderNamedSelector createProviderSelector(Contract contract) {
        SelectorName selectorName = new SelectorName(contract.getSubject().get(0).getName().getValue());
        return new ProviderNamedSelectorBuilder().setName(selectorName)
            .setContract(ImmutableList.of(contract.getId()))
            .build();
    }

    protected static ConsumerNamedSelector createConsumerSelector(Contract contract) {
        SelectorName selectorName = new SelectorName(contract.getSubject().get(0).getName().getValue());
        return new ConsumerNamedSelectorBuilder().setName(selectorName)
            .setContract(ImmutableList.of(contract.getId()))
            .build();
    }
}
