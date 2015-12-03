/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef.ConnectionTracking;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.EndpointConditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.EndpointConditionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.AllCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.AllConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.AnyCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.AnyConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.NoneCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.endpoint.conditions.endpoint.conditions.NoneConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.ConsumerEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.ProviderEndpointConstraintsBuilder;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class PolicyInfoUtils {

    private PolicyInfoUtils() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    public static List<ResolvedPolicy> buildResolvedPolicy(Table<EgKey, EgKey, Policy> policyMap) {
        List<ResolvedPolicy> resolvedPolicies = new ArrayList<>();
        for (Cell<EgKey, EgKey, Policy> policyCell : policyMap.cellSet()) {
            ResolvedPolicyBuilder resolvedPolicyBuilder = new ResolvedPolicyBuilder();
            resolvedPolicyBuilder.setConsumerEpgId(policyCell.getRowKey().getEgId());
            resolvedPolicyBuilder.setConsumerTenantId(policyCell.getRowKey().getTenantId());
            resolvedPolicyBuilder.setProviderEpgId(policyCell.getColumnKey().getEgId());
            resolvedPolicyBuilder.setProviderTenantId(policyCell.getColumnKey().getTenantId());
            resolvedPolicyBuilder.setPolicyRuleGroupWithEndpointConstraints(
                    buildPolicyRuleGroupWithEndpointConstraints(policyCell.getValue()));
            resolvedPolicies.add(resolvedPolicyBuilder.build());
        }
        return resolvedPolicies;
    }

    private static List<PolicyRuleGroupWithEndpointConstraints> buildPolicyRuleGroupWithEndpointConstraints(
            Policy policy) {
        List<PolicyRuleGroupWithEndpointConstraints> policyRuleGroupWithEndpointConstraintsList = new ArrayList<>();
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> ruleGrpsWECCell : policy.getRuleMap()
            .cellSet()) {
            PolicyRuleGroupWithEndpointConstraintsBuilder policyRuleGroupWithEndpointConstraintsBuilder =
                    new PolicyRuleGroupWithEndpointConstraintsBuilder();

            // consumer
            EndpointConditions consConditionSet = buildConditionSet(ruleGrpsWECCell.getRowKey().getConditionSet());
            EndpointIdentificationConstraints consEndpointIdentificationConstraints =
                    buildEndpointIdentificationConstraints(ruleGrpsWECCell.getRowKey());
            ConsumerEndpointConstraintsBuilder consumerEndpointConstraintsBuilder =
                    new ConsumerEndpointConstraintsBuilder();

            if (consConditionSet != null) {
                consumerEndpointConstraintsBuilder.setEndpointConditions(consConditionSet);
            }
            if (consEndpointIdentificationConstraints != null) {
                consumerEndpointConstraintsBuilder
                    .setEndpointIdentificationConstraints(consEndpointIdentificationConstraints);
            }
            if (consConditionSet != null || consEndpointIdentificationConstraints != null) {
                policyRuleGroupWithEndpointConstraintsBuilder
                    .setConsumerEndpointConstraints(consumerEndpointConstraintsBuilder.build());
            }

            // provider
            EndpointConditions provConditionSet = buildConditionSet(ruleGrpsWECCell.getColumnKey().getConditionSet());
            EndpointIdentificationConstraints provEndpointIdentificationConstraints =
                    buildEndpointIdentificationConstraints(ruleGrpsWECCell.getColumnKey());
            ProviderEndpointConstraintsBuilder providerEndpointConstraintsBuilder =
                    new ProviderEndpointConstraintsBuilder();

            if (provConditionSet != null) {
                providerEndpointConstraintsBuilder.setEndpointConditions(provConditionSet);
            }
            if (provEndpointIdentificationConstraints != null) {
                providerEndpointConstraintsBuilder
                    .setEndpointIdentificationConstraints(provEndpointIdentificationConstraints);
            }
            if (provConditionSet != null || provEndpointIdentificationConstraints != null) {
                policyRuleGroupWithEndpointConstraintsBuilder
                    .setProviderEndpointConstraints(providerEndpointConstraintsBuilder.build());
            }

            // Policy groups
            List<PolicyRuleGroup> policyRuleGroups = new ArrayList<>();
            for (RuleGroup ruleGrp : ruleGrpsWECCell.getValue()) {
                PolicyRuleGroupBuilder policyRuleGroupBuilder = new PolicyRuleGroupBuilder();
                policyRuleGroupBuilder.setContractId(ruleGrp.getRelatedContract().getId());
                policyRuleGroupBuilder.setSubjectName(ruleGrp.getRelatedSubject());
                policyRuleGroupBuilder.setTenantId(ruleGrp.getContractTenant().getId());
                policyRuleGroupBuilder
                    .setResolvedRule(buildResolvedRules(ruleGrp.getRules(), ruleGrp.getContractTenant()));
                policyRuleGroups.add(policyRuleGroupBuilder.build());
            }
            policyRuleGroupWithEndpointConstraintsBuilder.setPolicyRuleGroup(policyRuleGroups);

            // add policy group with EP constraints
            policyRuleGroupWithEndpointConstraintsList.add(policyRuleGroupWithEndpointConstraintsBuilder.build());
        }
        return policyRuleGroupWithEndpointConstraintsList;
    }

    private static List<ResolvedRule> buildResolvedRules(List<Rule> rules, Tenant tenant) {
        if (rules == null) {
            return null;
        }
        List<ResolvedRule> resolvedRules = new ArrayList<>();
        for (Rule rule : rules) {
            ResolvedRuleBuilder resolvedRuleBuilder = new ResolvedRuleBuilder();
            resolvedRuleBuilder.setOrder(rule.getOrder());
            resolvedRuleBuilder.setName(rule.getName());
            resolvedRuleBuilder.setKey(new ResolvedRuleKey(rule.getName()));

            List<Classifier> classifiers = new ArrayList<>();
            for (ClassifierRef cls : rule.getClassifierRef()) {
                ClassifierBuilder classifierBuilder = new ClassifierBuilder();
                classifierBuilder.setConnectionTracking(convertConnectionTracking(cls.getConnectionTracking()));
                classifierBuilder.setDirection(cls.getDirection());
                classifierBuilder.setKey(new ClassifierKey(cls.getName()));
                classifierBuilder.setName(cls.getName());
                ClassifierInstance classifierInstance = readClassifierInstance(tenant, cls.getInstanceName());
                if (classifierInstance != null) {
                    classifierBuilder.setClassifierDefinitionId(classifierInstance.getClassifierDefinitionId());
                    classifierBuilder.setParameterValue(classifierInstance.getParameterValue());
                }
                classifiers.add(classifierBuilder.build());
            }
            resolvedRuleBuilder.setClassifier(classifiers);

            List<Action> actions = new ArrayList<>();
            for (ActionRef act : rule.getActionRef()) {
                ActionBuilder actionBuilder = new ActionBuilder();
                actionBuilder.setName(act.getName());
                actionBuilder.setKey(new ActionKey(act.getName()));
                actionBuilder.setOrder(act.getOrder());
                ActionInstance actionInstance = readActionInstance(tenant, act.getName());
                if (actionInstance != null) {
                    actionBuilder.setActionDefinitionId(actionInstance.getActionDefinitionId());
                    actionBuilder.setParameterValue(actionInstance.getParameterValue());
                }
                actions.add(actionBuilder.build());
            }
            resolvedRuleBuilder.setAction(actions);

            // add rule
            resolvedRules.add(resolvedRuleBuilder.build());
        }
        return resolvedRules;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier.ConnectionTracking convertConnectionTracking(
            ConnectionTracking connectionTracking) {
        if (connectionTracking == null) {
            return null;
        }
        return org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier.ConnectionTracking
            .forValue(connectionTracking.getIntValue());
    }

    private static ClassifierInstance readClassifierInstance(Tenant tenant, ClassifierName instanceName) {
        for (ClassifierInstance instance : tenant.getSubjectFeatureInstances().getClassifierInstance()) {
            if (instance.getName().equals(instanceName)) {
                return instance;
            }
        }
        return null;
    }

    private static ActionInstance readActionInstance(Tenant tenant, ActionName instanceName) {
        for (ActionInstance instance : tenant.getSubjectFeatureInstances().getActionInstance()) {
            if (instance.getName().equals(instanceName)) {
                return instance;
            }
        }
        return null;
    }

    private static EndpointIdentificationConstraints buildEndpointIdentificationConstraints(
            EndpointConstraint endpointConstraint) {
        if (isNullOrEmpty(endpointConstraint.getL3EpPrefixes())) {
            return null;
        }
        List<PrefixConstraint> prefixConstraints = new ArrayList<>();
        prefixConstraints.addAll(endpointConstraint.getL3EpPrefixes());
        L3EndpointIdentificationConstraintsBuilder l3EndpointIdentificationConstraintsBuilder =
                new L3EndpointIdentificationConstraintsBuilder();
        l3EndpointIdentificationConstraintsBuilder.setPrefixConstraint(prefixConstraints);
        EndpointIdentificationConstraintsBuilder endpointIdentificationConstraintsBuilder =
                new EndpointIdentificationConstraintsBuilder();
        endpointIdentificationConstraintsBuilder
            .setL3EndpointIdentificationConstraints(l3EndpointIdentificationConstraintsBuilder.build());
        return endpointIdentificationConstraintsBuilder.build();
    }

    private static EndpointConditions buildConditionSet(ConditionSet conditionSet) {
        EndpointConditionsBuilder endpointConditionsBuilder = new EndpointConditionsBuilder();

        if (isNullOrEmpty(conditionSet.getTypeAll()) && isNullOrEmpty(conditionSet.getTypeAny())
                && isNullOrEmpty(conditionSet.getTypeNone())) {
            return null;
        }

        if (!isNullOrEmpty(conditionSet.getTypeAll())) {
            List<AllCondition> allConditions = new ArrayList<>();
            for (ConditionName c : conditionSet.getTypeAll()) {
                AllConditionBuilder allConditionBuilder = new AllConditionBuilder();
                allConditionBuilder.setName(c);
                allConditions.add(allConditionBuilder.build());
            }
            endpointConditionsBuilder.setAllCondition(allConditions);
        }
        if (!isNullOrEmpty(conditionSet.getTypeNone())) {
            List<NoneCondition> noneConditions = new ArrayList<>();
            for (ConditionName c : conditionSet.getTypeNone()) {
                NoneConditionBuilder noneConditionBuilder = new NoneConditionBuilder();
                noneConditionBuilder.setName(c);
                noneConditions.add(noneConditionBuilder.build());
            }
            endpointConditionsBuilder.setNoneCondition(noneConditions);
        }
        if (!isNullOrEmpty(conditionSet.getTypeAny())) {
            Set<ConditionName> typeAnyMergedSet = new HashSet<>();
            for (Set<ConditionName> cSet : conditionSet.getTypeAny()) {
                typeAnyMergedSet.addAll(cSet);
            }
            List<AnyCondition> anyConditions = new ArrayList<>();
            for (ConditionName c : typeAnyMergedSet) {
                AnyConditionBuilder anyConditionBuilder = new AnyConditionBuilder();
                anyConditionBuilder.setName(c);
                anyConditions.add(anyConditionBuilder.build());
            }
            endpointConditionsBuilder.setAnyCondition(anyConditions);
        }

        return endpointConditionsBuilder.build();
    }

    private static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return (collection == null || collection.isEmpty());
    }
}
