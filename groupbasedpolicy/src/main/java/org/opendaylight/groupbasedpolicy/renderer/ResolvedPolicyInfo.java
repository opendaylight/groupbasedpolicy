/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupKey;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;

public class ResolvedPolicyInfo {

    private final ImmutableTable<ConsEpgKey, ProvEpgKey, ResolvedPolicy> policyByEpgs;
    private final Map<PolicyRuleGroupKey, PolicyRuleGroup> policyRuleGrpByKey = new HashMap<>();

    public ResolvedPolicyInfo(ResolvedPolicies resolvedPolicies) {
        if (resolvedPolicies.getResolvedPolicy() == null) {
            policyByEpgs = ImmutableTable.of();
        } else {
            Builder<ConsEpgKey, ProvEpgKey, ResolvedPolicy> policyByEpgsBuilder = new Builder<>();
            for (ResolvedPolicy resolvedPolicy : resolvedPolicies.getResolvedPolicy()) {
                policyByEpgsBuilder.put(
                        new EpgKeyDto(resolvedPolicy.getConsumerEpgId(), resolvedPolicy.getConsumerTenantId()),
                        new EpgKeyDto(resolvedPolicy.getProviderEpgId(), resolvedPolicy.getProviderTenantId()),
                        resolvedPolicy);
                for (PolicyRuleGroupWithEndpointConstraints ruleGrpWithEpConstraints : resolvedPolicy
                    .getPolicyRuleGroupWithEndpointConstraints()) {
                    for (PolicyRuleGroup ruleGrp : ruleGrpWithEpConstraints.getPolicyRuleGroup()) {
                        policyRuleGrpByKey.put(ruleGrp.getKey(), ruleGrp);
                    }
                }
            }
            policyByEpgs = policyByEpgsBuilder.build();
        }
    }

    public Optional<PolicyRuleGroup> getPolicyRuleGroup(PolicyRuleGroupKey policyRuleGrpKey) {
        return Optional.fromNullable(policyRuleGrpByKey.get(policyRuleGrpKey));
    }

    public ImmutableSet<ProvEpgKey> findProviderPeers(ConsEpgKey consKey) {
        return policyByEpgs.row(consKey).keySet();
    }

    public ImmutableSet<ConsEpgKey> findConsumerPeers(ProvEpgKey provKey) {
        return policyByEpgs.column(provKey).keySet();
    }

    public Optional<ResolvedPolicy> findPolicy(ConsEpgKey consKey, ProvEpgKey provKey) {
        return Optional.fromNullable(policyByEpgs.get(consKey, provKey));
    }

}
