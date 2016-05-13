/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;

/**
 * Wrap some convenient indexes around a {@link Tenant} object
 * @author readams
 */
@Immutable
public class IndexedTenant {
    private final Tenant tenant;
    private final int hashCode;

    private final Map<EndpointGroupId, EndpointGroup> endpointGroups =
            new HashMap<>();
    private final Map<ContractId, Contract> contracts =
            new HashMap<>();
    private final Table<String, Class<? extends NetworkDomainId>, NetworkDomain> networkDomains = HashBasedTable.<String, Class<? extends NetworkDomainId>, NetworkDomain>create();
    private final Map<ClassifierName, ClassifierInstance> classifiers =
            new HashMap<>();
    private final Map<ActionName, ActionInstance> actions =
            new HashMap<>();
    private final Map<String, Set<SubnetId>> subnetMap = new HashMap<>();
    private Set<ExternalImplicitGroup> externalImplicitGroups = Collections.emptySet();

    public IndexedTenant(Tenant tenant) {
        this.tenant = tenant;
        this.hashCode = tenant.hashCode();
        if (tenant.getPolicy() != null) {
            processPolicy(tenant.getPolicy());
        }
        if (tenant.getForwardingContext() != null) {
            processForwardingContext(tenant.getForwardingContext());
        }
    }

    private void processPolicy(Policy policy) {
        if (policy.getEndpointGroup() != null) {
            for (EndpointGroup eg : policy.getEndpointGroup()) {
                endpointGroups.put(eg.getId(), eg);
            }
        }
        if (policy.getExternalImplicitGroup() != null) {
            externalImplicitGroups = ImmutableSet.copyOf(policy.getExternalImplicitGroup());
        }
        if (policy.getContract() != null) {
            for (Contract c : policy.getContract()) {
                contracts.put(c.getId(), c);
            }
        }
        if (policy.getSubjectFeatureInstances() != null) {
            SubjectFeatureInstances sfi = policy.getSubjectFeatureInstances();
            if (sfi.getClassifierInstance() != null) {
                for (ClassifierInstance ci : sfi.getClassifierInstance()) {
                    classifiers.put(ci.getName(), ci);
                }
            }
            if (sfi.getActionInstance() != null) {
                for (ActionInstance action : sfi.getActionInstance()) {
                    actions.put(action.getName(), action);
                }
            }
        }
    }

    private void processForwardingContext(ForwardingContext fwCtx) {
        if (fwCtx.getL3Context() != null) {
            for (L3Context c : fwCtx.getL3Context()) {
                networkDomains.put(c.getId().getValue(), L3ContextId.class, c);
            }
        }
        if (fwCtx.getL2BridgeDomain() != null) {
            for (L2BridgeDomain c : fwCtx.getL2BridgeDomain()) {
                networkDomains.put(c.getId().getValue(), L2BridgeDomainId.class, c);
            }
        }
        if (fwCtx.getL2FloodDomain() != null) {
            for (L2FloodDomain c : fwCtx.getL2FloodDomain()) {
                networkDomains.put(c.getId().getValue(), L2FloodDomainId.class, c);
            }
        }
        if (fwCtx.getSubnet() != null) {
            for (Subnet s : fwCtx.getSubnet()) {
                networkDomains.put(s.getId().getValue(), SubnetId.class, s);
                Set<SubnetId> sset = subnetMap.get(s.getParent().getValue());
                if (sset == null) {
                    subnetMap.put(s.getParent().getValue(), sset = new HashSet<SubnetId>());
                }
                sset.add(s.getId());
            }
        }
    }

    /**
     * Get the underlying tenant object
     * @return the {@link Tenant}
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Gets all external implicit groups in the tenant
     * @return immutable set of EIGs
     */
    public Set<ExternalImplicitGroup> getExternalImplicitGroups() {
        return externalImplicitGroups;
    }

    /**
     * Look up the endpoint group specified
     * @param id the {@link EndpointGroupId}
     * @return the {@link EndpointGroup} if it exists, or <code>null</code>
     * otherwise
     */
    public EndpointGroup getEndpointGroup(EndpointGroupId id) {
        return endpointGroups.get(id);
    }

    /**
     * Look up the contract specified
     * @param id the {@link ContractId}
     * @return the {@link Contract} if it exists, or <code>null</code>
     * otherwise
     */
    public Contract getContract(ContractId id) {
        return contracts.get(id);
    }

    /**
     * Look up the classifier instance specified
     * @param name the {@link ClassifierName}
     * @return the {@link ClassifierInstance} if it exists, or <code>null</code>
     * otherwise
     */
    public ClassifierInstance getClassifier(ClassifierName name) {
        return classifiers.get(name);
    }

    /**
     * Look up the classifier instance specified
     * @param name the {@link ActionName}
     * @return the {@link ActionInstance} if it exists, or <code>null</code>
     * otherwise
     */
    public ActionInstance getAction(ActionName name) {
        return actions.get(name);
    }

    /**
     * Get the layer 3 context
     * @param id the {@link L3ContextId}
     * @return the {@link L3Context} or <code>null</code> if it does not exist
     */
    public L3Context resolveL3Context(L3ContextId id) {
        return resolveDomain(L3Context.class, id);
    }

    /**
     * Get the layer 3 context for the specified l2 bridge domain by walking
     * up the hierarchy
     * @param id the {@link L2BridgeDomainId}
     * @return the {@link L3Context} or <code>null</code> if it does not exist
     */
    public L3Context resolveL3Context(L2BridgeDomainId id) {
        return resolveDomain(L3Context.class, id);
    }

    /**
     * Get the layer 3 context for the specified l2 flood domain by walking
     * up the hierarchy
     * @param id the {@link L2FloodDomainId}
     * @return the {@link L3Context} or <code>null</code> if it does not exist
     */
    public L3Context resolveL3Context(L2FloodDomainId id) {
        return resolveDomain(L3Context.class, id);
    }

    /**
     * Get the layer 2 bridge domain
     * @param id the {@link L2BridgeDomainId}
     * @return the {@link L2BridgeDomain} or <code>null</code> if it does not exist
     */
    public L2BridgeDomain resolveL2BridgeDomain(L2BridgeDomainId id) {
        return resolveDomain(L2BridgeDomain.class, id);
    }

    /**
     * Get the layer 2 bridge domain for the specified l2 flood domain by walking
     * up the hierarchy
     * @param id the {@link L2FloodDomainId}
     * @return the {@link L2BridgeDomain} or <code>null</code> if it does not exist
     */
    public L2BridgeDomain resolveL2BridgeDomain(L2FloodDomainId id) {
        return resolveDomain(L2BridgeDomain.class, id);
    }

    /**
     * Get the layer 2 flood domain
     * @param id the {@link L2FloodDomainId}
     * @return the {@link L2FloodDomain} or <code>null</code> if it does not exist
     */
    public L2FloodDomain resolveL2FloodDomain(L2FloodDomainId id) {
        return resolveDomain(L2FloodDomain.class, id);
    }

    /**
     * Get the subnet based on it's ID. Since subnet is on the bottom
     * of the forwarding hierarchy, there is no other upstream domain
     * to resolved.
     *
     * @param id of the {@link SubnetId}
     * @return the {@link Subnet} or <code>null</code> if it does
     * not exist
     */
    public Subnet resolveSubnet(SubnetId id) {
        return resolveDomain(Subnet.class, id);
    }

    /**
     * Resolve all subnets applicable to the given network domain ID
     * @param id the {@link NetworkDomainId}
     * @return the set of subnets.  Cannot be null, but could be empty.
     */
    public Collection<Subnet> resolveSubnets(NetworkDomainId id) {
        Set<SubnetId> sset = new HashSet<>();
        HashSet<NetworkDomainId> visited = new HashSet<>();
        while (id != null) {
            if (visited.contains(id)) break;
            visited.add(id);
            Set<SubnetId> cursset = subnetMap.get(id.getValue());
            if (cursset != null)
                sset.addAll(cursset);
            NetworkDomain d = networkDomains.get(id.getValue(), id.getClass());
            if (d == null) break;
            if (d instanceof Subnet) {
                id = ((Subnet)d).getParent();
                sset.add(((Subnet) d).getId());
            }
            else if (d instanceof L2BridgeDomain)
                id = ((L2BridgeDomain)d).getParent();
            else if (d instanceof L2FloodDomain)
                id = ((L2FloodDomain)d).getParent();
            else
                id = null;
        }
        return Collections2.transform(sset, new Function<SubnetId, Subnet>() {
            @Override
            public Subnet apply(SubnetId input) {
                return (Subnet)networkDomains.get(input.getValue(), SubnetId.class);
            }
        });
    }

    // ******
    // Object
    // ******

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndexedTenant other = (IndexedTenant) obj;
        if (tenant == null) {
            if (other.tenant != null)
                return false;
        } else if (!tenant.equals(other.tenant))
            return false;
        return true;
    }

    // **************
    // Implementation
    // **************

    @SuppressWarnings("unchecked")
    private <C extends NetworkDomain, I extends NetworkDomainId> C resolveDomain(Class<C> domainClass, I id) {
        SetMultimap<I, Class<? extends NetworkDomainId>> visited = HashMultimap.create();
        while (id != null) {
            // TODO condition
            if (visited.get(id) != null && visited.containsEntry(id, id.getClass())) {
                return null;
            }
            visited.put(id, id.getClass());
            NetworkDomain d = networkDomains.get(id.getValue(), id.getClass());
            if (d == null)
                return null;
            if (domainClass.isInstance(d))
                return domainClass.cast(d);
            if (d instanceof L2BridgeDomain) {
                id = (I) ((L2BridgeDomain) d).getParent();
            } else if (d instanceof L2FloodDomain) {
                id = (I) ((L2FloodDomain) d).getParent();
            } else {
                id = null;
            }
        }
        return null;
    }
}
