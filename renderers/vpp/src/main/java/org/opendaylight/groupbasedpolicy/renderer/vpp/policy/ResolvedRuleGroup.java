/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

/**
 * Represent a group of rules applied to a given pair of endpoints.
 * Includes references back to the normalized policy that resulted in the rule
 * group.
 */
@Immutable
public class ResolvedRuleGroup implements Comparable<ResolvedRuleGroup> {

    private static final ResolvedRuleComparator RULE_COMPARATOR = new ResolvedRuleComparator();
    private ImmutableSortedSet<ResolvedRule> rules;
    private final Integer order;
    private final TenantId contractTenantId;
    private final ContractId contractId;
    private final SubjectName relatedSubject;

    public static class ResolvedRuleComparator implements Comparator<ResolvedRule> {

        @Override
        public int compare(ResolvedRule o1, ResolvedRule o2) {
            return ComparisonChain.start()
                .compare(o1.getOrder(), o2.getOrder(), Ordering.natural().nullsLast())
                .compare(o1.getName().getValue(), o2.getName().getValue(), Ordering.natural().nullsLast())
                .result();
        }

    }

    public ResolvedRuleGroup(@Nonnull RuleGroup ruleGroup) {
        this.rules = ImmutableSortedSet.copyOf(RULE_COMPARATOR, ruleGroup.getResolvedRule());
        this.order = ruleGroup.getOrder();
        this.contractTenantId = ruleGroup.getTenantId();
        this.contractId = ruleGroup.getContractId();
        this.relatedSubject = ruleGroup.getSubjectName();
    }

    public ResolvedRuleGroup(List<ResolvedRule> rules, Integer order, TenantId contractTenantId, ContractId contractId,
            SubjectName subject) {
        this.rules = ImmutableSortedSet.copyOf(RULE_COMPARATOR, rules);
        this.order = order;
        this.contractTenantId = contractTenantId;
        this.contractId = contractId;
        this.relatedSubject = subject;
    }

    /**
     * @return sorted {@link Rule} list
     */
    public ImmutableSortedSet<ResolvedRule> getRules() {
        return rules;
    }

    public Integer getOrder() {
        return order;
    }

    public TenantId getContractTenantId() {
        return contractTenantId;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public SubjectName getRelatedSubject() {
        return relatedSubject;
    }

    public RuleGroupKey getRelatedRuleGroupKey() {
        return new RuleGroupKey(contractId, relatedSubject, contractTenantId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((order == null) ? 0 : order.hashCode());
        result = prime * result + ((rules == null) ? 0 : rules.hashCode());
        result = prime * result + ((relatedSubject == null) ? 0 : relatedSubject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResolvedRuleGroup other = (ResolvedRuleGroup) obj;
        if (order == null) {
            if (other.order != null)
                return false;
        } else if (!order.equals(other.order))
            return false;
        if (rules == null) {
            if (other.rules != null)
                return false;
        } else if (!rules.equals(other.rules))
            return false;
        if (relatedSubject == null) {
            if (other.relatedSubject != null)
                return false;
        } else if (!relatedSubject.equals(other.relatedSubject))
            return false;
        return true;
    }

    @Override
    public int compareTo(ResolvedRuleGroup o) {
        return ComparisonChain.start()
            .compare(order, o.order, Ordering.natural().nullsLast())
            .compare(relatedSubject.getValue(), o.relatedSubject.getValue(), Ordering.natural().nullsLast())
            .result();
    }

}
