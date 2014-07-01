/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.io.Serializable;
import java.util.Comparator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Static methods for manipulating group-based policy tenants
 * @author readams
 */
public class TenantUtils {
    /**
     * A comparator that assigns the natural ordering for rules, null-aware
     * @author readams
     */
    public static class RuleComparator 
            implements Comparator<Rule>, Serializable {
        private static final long serialVersionUID = -994507116060864552L;

        @Override
        public int compare(Rule o1, Rule o2) {
            return ComparisonChain.start()
                .compare(o1.getOrder(), o2.getOrder(), 
                         Ordering.natural().nullsLast())
                .result();
        }
        
    }

    /**
     * An instance of RuleComparator
     */
    public static final RuleComparator RULE_COMPARATOR = new RuleComparator();

    /**
     * Generate an {@link InstanceIdentifier} for an {@link Tenant}
     * @param tenantKey a tenant key
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Tenant> tenantIid(TenantKey tenantKey) {
        return InstanceIdentifier.builder(Tenants.class)
                .child(Tenant.class, tenantKey)
                .build();
    }

    /**
     * Generate an {@link InstanceIdentifier} for an {@link Tenant}
     * @param tenantId a tenant id
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Tenant> tenantIid(TenantId tenantId) {
        return tenantIid(new TenantKey(tenantId));
    }
    
    /**
     * Find a contract with a specified ID within a tenant
     * @param tenant the {@link Tenant} to search
     * @param contractId the {@link ContractId} to search for
     * @return the {@link Contract} if it exists, null otherwise
     */
    public static Contract findContract(Tenant tenant, 
                                        ContractId contractId) {
        if (tenant.getContract() != null) {
            for (Contract c : tenant.getContract()) {
                if (contractId.equals(c.getId())) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Find a clause with a specified name within a contract
     * @param tenant the {@link Contract} to search
     * @param contractId the {@link ClauseName} to search for
     * @return the {@link Clause} if it exists, null otherwise
     */
    public static Clause findClause(Contract contract, 
                                    ClauseName clauseName) {
        if (contract.getClause() != null) {
            for (Clause c : contract.getClause()) {
                if (clauseName.equals(c.getName())) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Find a subject with a specified name within a contract
     * @param tenant the {@link Contract} to search
     * @param subjectName the {@link SubjectName} to search for
     * @return the {@link Subject} if it exists, null otherwise
     */
    public static Subject findSubject(Contract contract, 
                                      SubjectName subjectName) {
        if (contract.getSubject() != null) {
            for (Subject c : contract.getSubject()) {
                if (subjectName.equals(c.getName())) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Find a target with a specified name within a contract
     * @param contract the {@link Contract} to search
     * @param targetName the {@link TargetName} to search for
     * @return the {@link Target} if it exists, null otherwise
     */
    public static Target findTarget(Contract contract, 
                                    TargetName targetName) {
        if (contract.getTarget() != null) {
            for (Target t : contract.getTarget()) {
                if (targetName.equals(t.getName())) {
                    return t;
                }
            }
        }
        return null;
    }
    
    /**
     * Find an endpoint group with a specified ID within a tenant
     * @param tenant the {@link Tenant} to search
     * @param egId the {@link EndpointGroupId} to search for
     * @return the {@link EndpointGroup} if it exists, null otherwise
     */
    public static EndpointGroup findEndpointGroup(Tenant tenant, 
                                                  EndpointGroupId egId) {
        if (tenant.getEndpointGroup() != null) {
            for (EndpointGroup eg : tenant.getEndpointGroup()) {
                if (egId.equals(eg.getId())) {
                    return eg;
                }
            }
        }
        return null;
    }
    
    /**
     * Find a consumer named selector in an endpoint group
     * @param eg the {@link EndpointGroup} to search
     * @param name the {@link NamedSelectorName} to search for
     * @return the {@link ConsumerNamedSelector} if it exists, null otherwise
     */
    public static ConsumerNamedSelector findCns(EndpointGroup eg,
                                                SelectorName name) {
        if (eg.getConsumerNamedSelector() != null) {
            for (ConsumerNamedSelector s : eg.getConsumerNamedSelector()) {
                if (name.equals(s.getName()))
                    return s;
            }
        }
        return null;
    }
    
    /**
     * Find a consumer target selector in an endpoint group
     * @param eg the {@link EndpointGroup} to search
     * @param name the {@link SelectorName} to search for
     * @return the {@link ConsumerNamedSelector} if it exists, null otherwise
     */
    public static ConsumerTargetSelector findCts(EndpointGroup eg,
                                                 SelectorName name) {
        if (eg.getConsumerTargetSelector() != null) {
            for (ConsumerTargetSelector s : eg.getConsumerTargetSelector()) {
                if (name.equals(s.getName()))
                    return s;
            }
        }
        return null;
    }

    /**
     * Find a provider named selector in an endpoint group
     * @param eg the {@link EndpointGroup} to search
     * @param name the {@link NamedSelectorName} to search for
     * @return the {@link ProviderNamedSelector} if it exists, null otherwise
     */
    public static ProviderNamedSelector findPns(EndpointGroup eg,
                                                SelectorName name) {
        if (eg.getProviderNamedSelector() != null) {
            for (ProviderNamedSelector s : eg.getProviderNamedSelector()) {
                if (name.equals(s.getName()))
                    return s;
            }
        }
        return null;
    }
    
    /**
     * Find a provider target selector in an endpoint group
     * @param eg the {@link EndpointGroup} to search
     * @param name the {@link TargetSelectorName} to search for
     * @return the {@link ProviderNamedSelector} if it exists, null otherwise
     */
    public static ProviderTargetSelector findPts(EndpointGroup eg,
                                                 SelectorName name) {
        if (eg.getProviderTargetSelector() != null) {
            for (ProviderTargetSelector s : eg.getProviderTargetSelector()) {
                if (name.equals(s.getName()))
                    return s;
            }
        }
        return null;
    }    
}
