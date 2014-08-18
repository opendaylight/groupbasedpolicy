/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opendaylight.groupbasedpolicy.resolver.MatcherUtils.GetLabelName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.LabelName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelectorBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Utilities useful for resolving the inheritance rules for the various objects
 * in the system
 * @author readams
 *
 */
public class InheritanceUtils {
    /**
     * Fully resolve the specified {@link Tenant}, returning a tenant with all 
     * items fully normalized.  This means that no items will have parent/child 
     * relationships and can be interpreted simply without regard to inheritance
     * rules 
     * @param unresolvedTenant the {@link Tenant} unresolved tenant to resolve
     * @return the fully-resolved {@link Tenant}
     */
    public static Tenant resolveTenant(Tenant unresolvedTenant) {
        HashMap<EndpointGroupId, EndpointGroup> resolvedEgs = new HashMap<>();
        HashMap<ContractId, Contract> resolvedContracts = new HashMap<>();
        
        if (unresolvedTenant.getEndpointGroup() != null) {
            for (EndpointGroup eg : unresolvedTenant.getEndpointGroup()) {
                resolveEndpointGroup(unresolvedTenant, eg, resolvedEgs);
            }
        }
        if (unresolvedTenant.getContract() != null) {
            for (Contract c : unresolvedTenant.getContract()) {
                resolveContract(unresolvedTenant, c, resolvedContracts);
            }
        }

        // XXX TODO - inherit from common tenant
        
        return new TenantBuilder()
            .setId(unresolvedTenant.getId())
            .setName(unresolvedTenant.getName())
            .setDescription(unresolvedTenant.getDescription())
            .setEndpointGroup(ImmutableList.copyOf(resolvedEgs.values()))
            .setContract(ImmutableList.copyOf(resolvedContracts.values()))
            .setContractRef(unresolvedTenant.getContractRef())
            .setSubjectFeatureInstances(unresolvedTenant.getSubjectFeatureInstances())
            .setL3Context(unresolvedTenant.getL3Context())
            .setL2BridgeDomain(unresolvedTenant.getL2BridgeDomain())
            .setL2FloodDomain(unresolvedTenant.getL2FloodDomain())
            .setSubnet(unresolvedTenant.getSubnet())
            .build();
    }

    // ****************
    // Helper functions
    // ****************
    
    private static void resolveEndpointGroup(Tenant unresolvedTenant,
                                             EndpointGroup unresolvedEg,
                                             HashMap<EndpointGroupId, 
                                                     EndpointGroup> resolvedEgs) {
        // put the unresolved object into the data structure to avoid loops
        resolvedEgs.put(unresolvedEg.getId(), unresolvedEg);
        
        // resolve parent if it hasn't been resolved already
        EndpointGroup parent = null;
        if (unresolvedEg.getParent() != null) {
            if (!resolvedEgs.containsKey(unresolvedEg.getParent())) {
                parent = TenantUtils.findEndpointGroup(unresolvedTenant, 
                                                       unresolvedEg.getParent());
                if (parent != null)
                    resolveEndpointGroup(unresolvedTenant, parent, resolvedEgs);
            }
            parent = resolvedEgs.get(unresolvedEg.getParent());
        }

        HashMap<SelectorName, ConsumerTargetSelector> resolvedCts = 
                new HashMap<>();
        HashMap<SelectorName, ConsumerNamedSelector> resolvedCns = 
                new HashMap<>();
        HashMap<SelectorName, ProviderTargetSelector> resolvedPts = 
                new HashMap<>();
        HashMap<SelectorName, ProviderNamedSelector> resolvedPns = 
                new HashMap<>();
        NetworkDomainId domain = unresolvedEg.getNetworkDomain();

        if (unresolvedEg.getConsumerTargetSelector() != null) {
            for (ConsumerTargetSelector s : unresolvedEg.getConsumerTargetSelector()) {
                resolveCts(unresolvedTenant, unresolvedEg, s, resolvedCts);
            }
        }
        if (unresolvedEg.getConsumerNamedSelector() != null) {
            for (ConsumerNamedSelector s : unresolvedEg.getConsumerNamedSelector()) {
                resolveCns(unresolvedTenant, unresolvedEg, s, resolvedCns);
            }
        }
        if (unresolvedEg.getProviderTargetSelector() != null) {
            for (ProviderTargetSelector s : unresolvedEg.getProviderTargetSelector()) {
                resolvePts(unresolvedTenant, unresolvedEg, s, resolvedPts);
            }
        }
        if (unresolvedEg.getProviderNamedSelector() != null) {
            for (ProviderNamedSelector s : unresolvedEg.getProviderNamedSelector()) {
                resolvePns(unresolvedTenant, unresolvedEg, s, resolvedPns);
            }
        }
        

        if (parent != null) {
            if (parent.getConsumerTargetSelector() != null) {
                for (ConsumerTargetSelector cts : parent.getConsumerTargetSelector()) {
                    if (!resolvedCts.containsKey(cts.getName()))
                        resolvedCts.put(cts.getName(), cts);
                }
            }
            if (parent.getConsumerNamedSelector() != null) {
                for (ConsumerNamedSelector cns : parent.getConsumerNamedSelector()) {
                    if (!resolvedCns.containsKey(cns.getName()))
                        resolvedCns.put(cns.getName(), cns);
                }
            }
            if (parent.getProviderTargetSelector() != null) {
                for (ProviderTargetSelector pts : parent.getProviderTargetSelector()) {
                    if (!resolvedPts.containsKey(pts.getName()))
                        resolvedPts.put(pts.getName(), pts);
                }
            }
            if (parent.getProviderNamedSelector() != null) {
                for (ProviderNamedSelector pns : parent.getProviderNamedSelector()) {
                    if (!resolvedPns.containsKey(pns.getName()))
                        resolvedPns.put(pns.getName(), pns);
                }
            }
            if (domain == null) {
                domain = parent.getNetworkDomain();
            }
        }

        // Note: do not set parent, or any of the values that only exist
        // for inheritance
        EndpointGroup resolvedEg = new EndpointGroupBuilder()
            .setId(unresolvedEg.getId())
            .setDescription(unresolvedEg.getDescription())
            .setConsumerTargetSelector(ImmutableList.copyOf(resolvedCts.values()))
            .setConsumerNamedSelector(ImmutableList.copyOf(resolvedCns.values()))
            .setProviderTargetSelector(ImmutableList.copyOf(resolvedPts.values()))
            .setProviderNamedSelector(ImmutableList.copyOf(resolvedPns.values()))
            .setNetworkDomain(domain)
            .build();
        resolvedEgs.put(resolvedEg.getId(), resolvedEg);
    }
    
    private static void resolveCts(Tenant unresolvedTenant,
                                   EndpointGroup unresolvedEg,
                                   ConsumerTargetSelector unresolvedTs,
                                   HashMap<SelectorName, 
                                           ConsumerTargetSelector> resolvedCts) {
        HashMap<QualityMatcherName, QualityMatcher> matchers = new HashMap<>();
        HashMap<RequirementName, Requirement> requirements = new HashMap<>();
        HashSet<EndpointGroupId> visited = new HashSet<>();

        resolveCtsAttr(unresolvedTenant, unresolvedEg, unresolvedTs.getName(), 
                       matchers, requirements, visited);
        
        ConsumerTargetSelector resolved = new ConsumerTargetSelectorBuilder()
            .setName(unresolvedTs.getName())
            .setQualityMatcher(ImmutableList.copyOf(matchers.values()))
            .setRequirement(ImmutableList.copyOf(requirements.values()))
            .build();
        resolvedCts.put(resolved.getName(), resolved);
    }
    
    private static void resolveCtsAttr(Tenant unresolvedTenant,
                                       EndpointGroup unresolvedEg,
                                       SelectorName name,
                                       HashMap<QualityMatcherName, 
                                               QualityMatcher> matchers,
                                       HashMap<RequirementName, 
                                               Requirement> requirements,
                                       HashSet<EndpointGroupId> visited) {
        if (unresolvedEg == null) return;
        if (visited.contains(unresolvedEg.getId())) return;
        visited.add(unresolvedEg.getId());
        if (unresolvedEg.getParent() != null) {
            resolveCtsAttr(unresolvedTenant, 
                           TenantUtils.findEndpointGroup(unresolvedTenant, 
                                                         unresolvedEg.getParent()),
                           name, 
                           matchers,
                           requirements,
                           visited);
        }
        resolveLabels(unresolvedEg.getRequirement(), requirements, 
                      MatcherUtils.getRequirementName);
        ConsumerTargetSelector unresolvedSelector = 
                TenantUtils.findCts(unresolvedEg, name);
        if (unresolvedSelector == null) return;
        resolveLabels(unresolvedSelector.getRequirement(), requirements, 
                      MatcherUtils.getRequirementName);
        resolveQualityMatcher(unresolvedSelector.getQualityMatcher(), matchers);
    }
    
    private static void resolveCns(Tenant unresolvedTenant,
                                   EndpointGroup unresolvedEg,
                                   ConsumerNamedSelector unresolvedTs,
                                   HashMap<SelectorName, 
                                           ConsumerNamedSelector> resolvedCns) {
        HashMap<RequirementName, Requirement> requirements = new HashMap<>();
        HashSet<ContractId> contracts = new HashSet<>();
        HashSet<EndpointGroupId> visited = new HashSet<>();

        resolveCnsAttr(unresolvedTenant, unresolvedEg, unresolvedTs.getName(), 
                       requirements, contracts, visited);
        
        ConsumerNamedSelector resolved = new ConsumerNamedSelectorBuilder()
            .setName(unresolvedTs.getName())
            .setRequirement(ImmutableList.copyOf(requirements.values()))
            .setContract(ImmutableList.copyOf(contracts))
            .build();
        resolvedCns.put(resolved.getName(), resolved);
    }
    
    private static void resolveCnsAttr(Tenant unresolvedTenant,
                                       EndpointGroup unresolvedEg,
                                       SelectorName name,
                                       HashMap<RequirementName, 
                                               Requirement> requirements,
                                       HashSet<ContractId> contracts,
                                       HashSet<EndpointGroupId> visited) {
        if (unresolvedEg == null) return;
        if (visited.contains(unresolvedEg.getId())) return;
        visited.add(unresolvedEg.getId());
        if (unresolvedEg.getParent() != null) {
            resolveCnsAttr(unresolvedTenant, 
                           TenantUtils.findEndpointGroup(unresolvedTenant, 
                                                         unresolvedEg.getParent()),
                           name, requirements, contracts, visited);
        }
        resolveLabels(unresolvedEg.getRequirement(), requirements, 
                      MatcherUtils.getRequirementName);
        ConsumerNamedSelector unresolvedSelector =
                TenantUtils.findCns(unresolvedEg, name);
        if (unresolvedSelector == null) return;
        resolveLabels(unresolvedSelector.getRequirement(), requirements,
                      MatcherUtils.getRequirementName);
        if (unresolvedSelector.getContract() != null) {
            contracts.addAll(unresolvedSelector.getContract());
        }
    }

    private static void resolvePts(Tenant unresolvedTenant,
                                   EndpointGroup unresolvedEg,
                                   ProviderTargetSelector unresolvedTs,
                                   HashMap<SelectorName, 
                                           ProviderTargetSelector> resolvedCts) {
        HashMap<QualityMatcherName, QualityMatcher> matchers = new HashMap<>();
        HashMap<CapabilityName, Capability> capabilities = new HashMap<>();
        HashSet<EndpointGroupId> visited = new HashSet<>();

        resolvePtsAttr(unresolvedTenant, unresolvedEg, unresolvedTs.getName(), 
                       matchers, capabilities, visited);
        
        ProviderTargetSelector resolved = new ProviderTargetSelectorBuilder()
            .setName(unresolvedTs.getName())
            .setQualityMatcher(ImmutableList.copyOf(matchers.values()))
            .setCapability(ImmutableList.copyOf(capabilities.values()))
            .build();
        resolvedCts.put(resolved.getName(), resolved);
    }
    
    private static void resolvePtsAttr(Tenant unresolvedTenant,
                                       EndpointGroup unresolvedEg,
                                       SelectorName name,
                                       HashMap<QualityMatcherName, 
                                               QualityMatcher> matchers,
                                       HashMap<CapabilityName, 
                                               Capability> capabilities,
                                       HashSet<EndpointGroupId> visited) {
        if (unresolvedEg == null) return;
        if (visited.contains(unresolvedEg.getId())) return;
        visited.add(unresolvedEg.getId());
        if (unresolvedEg.getParent() != null) {
           resolvePtsAttr(unresolvedTenant, 
                           TenantUtils.findEndpointGroup(unresolvedTenant, 
                                                         unresolvedEg.getParent()),
                           name, 
                           matchers,
                           capabilities, visited);
        }
        resolveLabels(unresolvedEg.getCapability(), capabilities, 
                      MatcherUtils.getCapabilityName);
        ProviderTargetSelector unresolvedSelector = 
                TenantUtils.findPts(unresolvedEg, name);
        if (unresolvedSelector == null) return;
        resolveLabels(unresolvedSelector.getCapability(), capabilities, 
                      MatcherUtils.getCapabilityName);
        resolveQualityMatcher(unresolvedSelector.getQualityMatcher(), matchers);
    }
    
    private static void resolvePns(Tenant unresolvedTenant,
                                   EndpointGroup unresolvedEg,
                                   ProviderNamedSelector unresolvedTs,
                                   HashMap<SelectorName, 
                                           ProviderNamedSelector> resolvedCns) {
        HashMap<CapabilityName, Capability> capabilities = new HashMap<>();
        HashSet<ContractId> contracts = new HashSet<>();
        HashSet<EndpointGroupId> visited = new HashSet<>();
        
        resolvePnsAttr(unresolvedTenant, unresolvedEg, unresolvedTs.getName(), 
                       capabilities, contracts, visited);
        
        ProviderNamedSelector resolved = new ProviderNamedSelectorBuilder()
            .setName(unresolvedTs.getName())
            .setCapability(ImmutableList.copyOf(capabilities.values()))
            .setContract(ImmutableList.copyOf(contracts))
            .build();
        resolvedCns.put(resolved.getName(), resolved);
    }
    
    private static void resolvePnsAttr(Tenant unresolvedTenant,
                                       EndpointGroup unresolvedEg,
                                       SelectorName name,
                                       HashMap<CapabilityName, 
                                               Capability> capabilities,
                                       HashSet<ContractId> contracts,
                                       HashSet<EndpointGroupId> visited) {
        if (unresolvedEg == null) return;
        if (visited.contains(unresolvedEg.getId())) return;
        visited.add(unresolvedEg.getId());
        if (unresolvedEg.getParent() != null) {
            resolvePnsAttr(unresolvedTenant, 
                           TenantUtils.findEndpointGroup(unresolvedTenant, 
                                                         unresolvedEg.getParent()),
                           name, capabilities, contracts, visited);
        }
        resolveLabels(unresolvedEg.getCapability(), capabilities, 
                      MatcherUtils.getCapabilityName);
        ProviderNamedSelector unresolvedSelector =
                TenantUtils.findPns(unresolvedEg, name);
        if (unresolvedSelector == null) return;
        resolveLabels(unresolvedSelector.getCapability(), capabilities,
                      MatcherUtils.getCapabilityName);
        if (unresolvedSelector.getContract() != null) {
            contracts.addAll(unresolvedSelector.getContract());
        }
    }
   
    private static void resolveContract(Tenant unresolvedTenant,
                                        Contract unresolvedContract,
                                        HashMap<ContractId, 
                                                Contract> resolvedContracts) {
        // put the unresolved object into the data structure to avoid loops
        resolvedContracts.put(unresolvedContract.getId(), unresolvedContract);

        // resolve parent if it hasn't been resolved already
        Contract parent = null;
        if (unresolvedContract.getParent() != null) {
            if (!resolvedContracts.containsKey(unresolvedContract.getParent())) {
                parent = TenantUtils.findContract(unresolvedTenant,
                                                  unresolvedContract.getParent());
                if (parent != null)
                    resolveContract(unresolvedTenant, 
                                    parent, 
                                    resolvedContracts);
            }
            parent = resolvedContracts.get(unresolvedContract.getParent());
        }
        
        HashMap<TargetName, Target> resolvedTargets = new HashMap<>();
        HashMap<ClauseName, Clause> resolvedClauses = new HashMap<>();
        HashMap<SubjectName, Subject> resolvedSubjects = new HashMap<>();

        if (unresolvedContract.getTarget() != null) {
            for (Target t : unresolvedContract.getTarget()) {
                resolveTarget(unresolvedTenant, unresolvedContract, 
                              t, resolvedTargets);
            }
        }
        if (unresolvedContract.getClause() != null) {
            for (Clause c : unresolvedContract.getClause()) {
                resolveClause(unresolvedTenant, unresolvedContract, 
                              c, resolvedClauses);
            }
        }
        if (unresolvedContract.getSubject() != null ) {
            for (Subject s : unresolvedContract.getSubject()) {
                resolveSubject(unresolvedTenant, unresolvedContract,
                               s, resolvedSubjects);
            }
        }

        if (parent != null) {
            if (parent.getTarget() != null) {
                for (Target t : parent.getTarget()) {
                    if (!resolvedTargets.containsKey(t.getName()))
                        resolvedTargets.put(t.getName(), t);
                }
            }
            if (parent.getClause() != null) {
                for (Clause c : parent.getClause()) {
                    if (!resolvedClauses.containsKey(c.getName()))
                        resolvedClauses.put(c.getName(), c);
                }
            }
            if (parent.getSubject() != null) {
                for (Subject s : parent.getSubject()) {
                    if (!resolvedSubjects.containsKey(s.getName()))
                        resolvedSubjects.put(s.getName(), s);
                }
            }
        }

        Contract resolvedContract = new ContractBuilder()
            .setId(unresolvedContract.getId())
            .setDescription(unresolvedContract.getDescription())
            .setTarget(ImmutableList.copyOf(resolvedTargets.values()))
            .setClause(ImmutableList.copyOf(resolvedClauses.values()))
            .setSubject(ImmutableList.copyOf(resolvedSubjects.values()))
            .build();
        resolvedContracts.put(resolvedContract.getId(), resolvedContract);
    }
    
    private static void resolveTarget(Tenant unresolvedTenant,
                                      Contract unresolvedContract,
                                      Target unresolvedTarget,
                                      HashMap<TargetName, Target> resolvedTargets) {
        HashMap<QualityName, Quality> qualities = new HashMap<>();
        HashSet<ContractId> visited = new HashSet<>();

        resolveTargetAttrs(unresolvedTenant, 
                           unresolvedContract, 
                           unresolvedTarget.getName(), 
                           qualities, visited);

        Target resolved = new TargetBuilder()
            .setName(unresolvedTarget.getName())
            .setQuality(ImmutableList.copyOf(qualities.values()))
            .build();
        resolvedTargets.put(resolved.getName(), resolved);
    }
    
    private static void resolveTargetAttrs(Tenant unresolvedTenant,
                                           Contract unresolvedContract,
                                           TargetName targetName,
                                           HashMap<QualityName, Quality> qualities,
                                           HashSet<ContractId> visited) {
        if (unresolvedContract == null) return;
        if (visited.contains(unresolvedContract.getId())) return;
        visited.add(unresolvedContract.getId());
        if (unresolvedContract.getParent() != null) {
            resolveTargetAttrs(unresolvedTenant, 
                               TenantUtils.findContract(unresolvedTenant, 
                                                        unresolvedContract.getParent()),
                               targetName, 
                               qualities, visited);
        }
        resolveLabels(unresolvedContract.getQuality(), qualities, 
                      MatcherUtils.getQualityName);
        Target unresolvedTarget = 
                TenantUtils.findTarget(unresolvedContract, targetName);
        resolveLabels(unresolvedTarget.getQuality(), qualities, 
                      MatcherUtils.getQualityName);        
    }

    private static void 
        resolveQualityMatcher(Collection<QualityMatcher> toResolve,
                              HashMap<QualityMatcherName,
                                      QualityMatcher> matchers) {
        if (toResolve == null) return;
        for (QualityMatcher qm : toResolve) {
            if (matchers.containsKey(qm.getName())) {
                QualityMatcher oqm = matchers.get(qm.getName());
                QualityMatcherBuilder qmb = new QualityMatcherBuilder();
                qmb.setName(qm.getName());
                qmb.setMatchType(oqm.getMatchType());
                if (qm.getMatchType() != null)
                    qmb.setMatchType(qm.getMatchType());

                HashMap<QualityName, MatcherQuality> qualities = 
                        new HashMap<>();
                resolveLabels(oqm.getMatcherQuality(), qualities, 
                              MatcherUtils.getMatcherQualityName);
                resolveLabels(qm.getMatcherQuality(), qualities, 
                              MatcherUtils.getMatcherQualityName);
                
                qmb.setMatcherQuality(ImmutableList.copyOf(qualities.values()));
                matchers.put(qm.getName(), qmb.build());
            } else {
                matchers.put(qm.getName(), qm);
            }
        }
    }

    private static void 
        resolveCapabilityMatcher(Collection<CapabilityMatcher> toResolve,
                                 HashMap<CapabilityMatcherName,
                                         CapabilityMatcher> matchers) {
        if (toResolve == null) return;
        for (CapabilityMatcher m : toResolve) {
            if (matchers.containsKey(m.getName())) {
                CapabilityMatcher om = matchers.get(m.getName());
                CapabilityMatcherBuilder mb = new CapabilityMatcherBuilder();
                mb.setName(m.getName());
                mb.setMatchType(om.getMatchType());
                if (m.getMatchType() != null)
                    mb.setMatchType(m.getMatchType());

                HashMap<CapabilityName, MatcherCapability> labels = 
                        new HashMap<>();
                resolveLabels(om.getMatcherCapability(), labels, 
                              MatcherUtils.getMatcherCapabilityName);
                resolveLabels(m.getMatcherCapability(), labels, 
                              MatcherUtils.getMatcherCapabilityName);
                
                mb.setMatcherCapability(ImmutableList.copyOf(labels.values()));
                matchers.put(m.getName(), mb.build());
            } else {
                matchers.put(m.getName(), m);
            }
        }
    }

    private static void 
        resolveRequirementMatcher(Collection<RequirementMatcher> toResolve,
                                  HashMap<RequirementMatcherName,
                                          RequirementMatcher> matchers) {
        if (toResolve == null) return;
        for (RequirementMatcher m : toResolve) {
            if (matchers.containsKey(m.getName())) {
                RequirementMatcher om = matchers.get(m.getName());
                RequirementMatcherBuilder mb = new RequirementMatcherBuilder();
                mb.setName(m.getName());
                mb.setMatchType(om.getMatchType());
                if (m.getMatchType() != null)
                    mb.setMatchType(m.getMatchType());

                HashMap<RequirementName, MatcherRequirement> labels = 
                        new HashMap<>();
                resolveLabels(om.getMatcherRequirement(), labels, 
                              MatcherUtils.getMatcherRequirementName);
                resolveLabels(m.getMatcherRequirement(), labels, 
                              MatcherUtils.getMatcherRequirementName);
                
                mb.setMatcherRequirement(ImmutableList.copyOf(labels.values()));
                matchers.put(m.getName(), mb.build());
            } else {
                matchers.put(m.getName(), m);
            }
        }
    }

    private static void 
        resolveConditionMatcher(Collection<ConditionMatcher> toResolve,
                                 HashMap<ConditionMatcherName,
                                         ConditionMatcher> matchers) {
        if (toResolve == null) return;
        for (ConditionMatcher m : toResolve) {
            if (matchers.containsKey(m.getName())) {
                ConditionMatcher om = matchers.get(m.getName());
                ConditionMatcherBuilder mb = new ConditionMatcherBuilder();
                mb.setName(m.getName());
                mb.setMatchType(om.getMatchType());
                if (m.getMatchType() != null)
                    mb.setMatchType(m.getMatchType());

                HashMap<ConditionName, Condition> labels = 
                        new HashMap<>();
                resolveLabels(om.getCondition(), labels, 
                              MatcherUtils.getConditionName);
                resolveLabels(m.getCondition(), labels, 
                              MatcherUtils.getConditionName);
                
                mb.setCondition(ImmutableList.copyOf(labels.values()));
                matchers.put(m.getName(), mb.build());
            } else {
                matchers.put(m.getName(), m);
            }
        }
    }

    private static void resolveClause(Tenant unresolvedTenant,
                                      Contract unresolvedContract,
                                      Clause unresolvedClause,
                                      HashMap<ClauseName, Clause> resolvedClauses) {
        HashMap<CapabilityMatcherName, CapabilityMatcher> capMatchers = new HashMap<>();
        HashMap<ConditionMatcherName, ConditionMatcher> provCondMatchers = new HashMap<>();
        HashMap<RequirementMatcherName, RequirementMatcher> reqMatchers = new HashMap<>();
        HashMap<ConditionMatcherName, ConditionMatcher> consCondMatchers = new HashMap<>();
        HashSet<SubjectName> subjectRefs = new HashSet<>();
        HashSet<ContractId> visited = new HashSet<>();

        resolveClauseAttr(unresolvedTenant, unresolvedContract, 
                          unresolvedClause.getName(), subjectRefs, 
                          capMatchers, provCondMatchers,
                          reqMatchers, consCondMatchers, visited);
        
        Clause resolved = new ClauseBuilder()
            .setName(unresolvedClause.getName())
            .setSubjectRefs(ImmutableList.copyOf(subjectRefs))
            .setProviderMatchers(new ProviderMatchersBuilder()
                .setCapabilityMatcher(ImmutableList.copyOf(capMatchers.values()))
                .setConditionMatcher(ImmutableList.copyOf(provCondMatchers.values()))
                .build())
            .setConsumerMatchers(new ConsumerMatchersBuilder()
                .setRequirementMatcher(ImmutableList.copyOf(reqMatchers.values()))
                .setConditionMatcher(ImmutableList.copyOf(consCondMatchers.values()))
                .build())
            .build();
        resolvedClauses.put(resolved.getName(), resolved);
    }
    
    private static void resolveClauseAttr(Tenant unresolvedTenant,
                                          Contract unresolvedContract,
                                          ClauseName clauseName,
                                          HashSet<SubjectName> subjectRefs,
                                          HashMap<CapabilityMatcherName, 
                                                  CapabilityMatcher> capMatchers,
                                          HashMap<ConditionMatcherName, 
                                                  ConditionMatcher> provCondMatchers,
                                          HashMap<RequirementMatcherName, 
                                                  RequirementMatcher> reqMatchers,
                                          HashMap<ConditionMatcherName, 
                                                  ConditionMatcher> consCondMatchers,
                                          HashSet<ContractId> visited) {
        if (unresolvedContract == null) return;
        if (visited.contains(unresolvedContract.getId())) return;
        visited.add(unresolvedContract.getId());
        if (unresolvedContract.getParent() != null) {
            resolveClauseAttr(unresolvedTenant, 
                              TenantUtils.findContract(unresolvedTenant, 
                                                       unresolvedContract.getParent()), 
                              clauseName, 
                              subjectRefs, 
                              capMatchers, 
                              provCondMatchers,
                              reqMatchers, 
                              consCondMatchers, visited);
        }
        
        Clause unresolvedClause =
                TenantUtils.findClause(unresolvedContract, clauseName);
        if (unresolvedClause == null) return;
        
        if (unresolvedClause.getProviderMatchers() != null) {
            ProviderMatchers pms = unresolvedClause.getProviderMatchers();
            resolveCapabilityMatcher(pms.getCapabilityMatcher(), capMatchers);
            resolveConditionMatcher(pms.getConditionMatcher(), provCondMatchers);
        }
        if (unresolvedClause.getConsumerMatchers() != null) {
            ConsumerMatchers cms = unresolvedClause.getConsumerMatchers();
            resolveRequirementMatcher(cms.getRequirementMatcher(), reqMatchers);
            resolveConditionMatcher(cms.getConditionMatcher(), consCondMatchers);
        }
        if (unresolvedClause.getSubjectRefs() != null)
            subjectRefs.addAll(unresolvedClause.getSubjectRefs());
    }

    private static class Mutable<O> {
        O value;
    }
    
    private static void resolveSubject(Tenant unresolvedTenant,
                                       Contract unresolvedContract,
                                       Subject unresolvedSubject,
                                       HashMap<SubjectName, Subject> resolvedSubjects) {
        Mutable<Integer> order = new Mutable<>();
        Mutable<List<Rule>> rules = new Mutable<>();
        rules.value = Collections.emptyList();
        HashSet<ContractId> visited = new HashSet<>();
        
        resolveSubjectAttr(unresolvedTenant, unresolvedContract, 
                           unresolvedSubject.getName(), order, rules, visited);
        
        Subject resolved = new SubjectBuilder()
            .setName(unresolvedSubject.getName())
            .setOrder(order.value)
            .setRule(rules.value)
            .build();
        resolvedSubjects.put(resolved.getName(), resolved);
    }
    
    private static Rule makeRule(Rule r, int order) {
        return new RuleBuilder()
            .setName(r.getName())
            .setActionRef(r.getActionRef())
            .setClassifierRef(r.getClassifierRef())
            .setOrder(order)
            .build();
    }
    
    private static void resolveSubjectAttr(Tenant unresolvedTenant,
                                           Contract unresolvedContract,
                                           SubjectName subjectName,
                                           Mutable<Integer> order,
                                           Mutable<List<Rule>> rules,
                                           HashSet<ContractId> visited) {
        if (unresolvedContract == null) return;
        if (visited.contains(unresolvedContract.getId())) return;
        visited.add(unresolvedContract.getId());
        if (unresolvedContract.getParent() != null) {
            resolveSubjectAttr(unresolvedTenant, 
                              TenantUtils.findContract(unresolvedTenant, 
                                                       unresolvedContract.getParent()), 
                              subjectName,
                              order,
                              rules, visited);
        }
        
        Subject unresolvedSubject = 
                TenantUtils.findSubject(unresolvedContract, subjectName);
        if (unresolvedSubject == null) return;
        if (unresolvedSubject.getOrder() != null)
            order.value = unresolvedSubject.getOrder();
        if (unresolvedSubject.getRule() != null) {
            ImmutableList.Builder<Rule> rbuilder = 
                    new ImmutableList.Builder<Rule>();
            ArrayList<Rule> nrules = 
                    new ArrayList<>(unresolvedSubject.getRule());
            Collections.sort(nrules, TenantUtils.RULE_COMPARATOR);
            int index = 0;
            for (Rule r : nrules) {
                rbuilder.add(makeRule(r, index++));
            }
            for (Rule r : rules.value) {
                rbuilder.add(makeRule(r, index++));
            }
            rules.value = rbuilder.build();
        }
    }
    
    /**
     * Given a partially-resolved set of labels, add the next item in the
     * inheritance ordering to the set of resolved labels.
     * @param toResolve the new set to add
     * @param labels the partially-resolved set
     * @param getName a function object to get the appropriate typed label name
     */
    private static <L extends Label, LN extends LabelName> void 
        resolveLabels(Collection<L> toResolve, HashMap<LN, L> labels, 
                      GetLabelName<L, LN> getName) {
        if (toResolve == null) return;
        for (L l : toResolve) {
            if (l.getInclusionRule() != null) {
                switch (l.getInclusionRule()) {
                case Include:
                    // override
                    labels.put(getName.getName(l), l);
                    break;
                case Exclude:
                    // remove
                    labels.remove(getName.getName(l));
                    break;
                }
            } else {
                // default to Include
                labels.put(getName.getName(l), l);
            }
        }
    }
}
