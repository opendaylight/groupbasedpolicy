/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TestUtils {

    // taken from org.opendaylight.groupbasedpolicy.renderer.faas.FaasPolicyManagerTest
    // by Khaldoon Al-zoubi; modified as necessary
    public static ResolvedPolicy newResolvedPolicy(TenantId tenantId, ContractId contractId, SubjectName subjectName,
            RuleName ruleName, EndpointGroupId consumerEpgId, EndpointGroupId providerEpgId, Classifier classifier) {
        ResolvedPolicyBuilder builder = new ResolvedPolicyBuilder();
        builder.setConsumerEpgId(consumerEpgId);
        builder.setConsumerTenantId(tenantId);
        builder.setProviderEpgId(providerEpgId);
        builder.setProviderTenantId(tenantId);
        List<PolicyRuleGroupWithEndpointConstraints> pRulesGrpsWEp = new ArrayList<>();
        PolicyRuleGroupWithEndpointConstraintsBuilder pRulesGrpWEp =
                new PolicyRuleGroupWithEndpointConstraintsBuilder();
        List<PolicyRuleGroup> pRulesGrps = new ArrayList<>();
        PolicyRuleGroupBuilder pRulesGrp = new PolicyRuleGroupBuilder();
        pRulesGrp.setContractId(contractId);
        pRulesGrp.setTenantId(tenantId);
        pRulesGrp.setSubjectName(subjectName);
        pRulesGrp.setResolvedRule(ImmutableList
            .of(new ResolvedRuleBuilder().setName(ruleName).setClassifier(ImmutableList.of(classifier)).build()));
        pRulesGrps.add(pRulesGrp.build());
        pRulesGrpWEp.setPolicyRuleGroup(pRulesGrps);
        pRulesGrpsWEp.add(pRulesGrpWEp.build());
        builder.setPolicyRuleGroupWithEndpointConstraints(pRulesGrpsWEp);
        return builder.build();
    }

    public static InstanceIdentifier<Classifier> getClassifierIid(
            Map<InstanceIdentifier<Classifier>, Classifier> resolvedClassifiers) {
        Map.Entry<InstanceIdentifier<Classifier>, Classifier> firstEntry =
                resolvedClassifiers.entrySet().iterator().next();
        return firstEntry.getKey();
    }
}
