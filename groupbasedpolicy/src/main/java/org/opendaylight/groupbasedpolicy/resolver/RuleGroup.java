/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Represent a group of rules that could apply to a given pair of endpoints.  
 * Includes references back to the normalized policy that resulted in the rule 
 * group.
 * @author readams
 */
@Immutable 
public class RuleGroup implements Comparable<RuleGroup> {
    final List<Rule> rules;
    final Integer order;
    final Tenant contractTenant;
    final Contract relatedContract;
    final SubjectName relatedSubject;

    public RuleGroup(List<Rule> rules, Integer order,
                     Tenant contractTenant, Contract contract,
                     SubjectName subject) {
        super();
        this.rules = rules;
        this.order = order;
        this.contractTenant = contractTenant;
        this.relatedContract = contract;
        this.relatedSubject = subject;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Integer getOrder() {
        return order;
    }

    public Tenant getContractTenant() {
        return contractTenant;
    }

    public Contract getRelatedContract() {
        return relatedContract;
    }

    public SubjectName getRelatedSubject() {
        return relatedSubject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((order == null) ? 0 : order.hashCode());
        result = prime * result + ((rules == null) ? 0 : rules.hashCode());
        result = prime * result +
                 ((relatedSubject == null) ? 0 : relatedSubject.hashCode());
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
        RuleGroup other = (RuleGroup) obj;
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
    public int compareTo(RuleGroup o) {
        return ComparisonChain.start()
            .compare(order, o.order, 
                     Ordering.natural().nullsLast())
            .compare(relatedSubject.getValue(), o.relatedSubject.getValue(),
                     Ordering.natural().nullsLast())
            .result();
    }

    @Override
    public String toString() {
        return "RuleGroup [rules=" + rules + ", order=" + order +
               ", contractTenant=" + contractTenant.getId() + 
               ", relatedContract=" + relatedContract.getId() + 
               ", relatedSubject=" + relatedSubject + "]";
    }

}