/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

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
    private final Map<String, NetworkDomain> networkDomains =
            new HashMap<>();
    private final Map<ClassifierName, ClassifierInstance> classifiers =
            new HashMap<>();
    private final Map<ActionName, ActionInstance> actions =
            new HashMap<>();
    private final Map<String, Set<SubnetId>> subnetMap = new HashMap<>();
    
    public IndexedTenant(Tenant tenant) {
        super();
        this.tenant = tenant;
        this.hashCode = tenant.hashCode();
        
        if (tenant.getEndpointGroup() != null) {
            for (EndpointGroup eg : tenant.getEndpointGroup()) {
                endpointGroups.put(eg.getId(), eg);
            }
        }
        if (tenant.getContract() != null) {
            for (Contract c : tenant.getContract()) {
                contracts.put(c.getId(), c);
            }
        }
        if (tenant.getL3Context() != null) {
            for (L3Context c : tenant.getL3Context()) {
                networkDomains.put(c.getId().getValue(), c);
            }
        }
        if (tenant.getL2BridgeDomain() != null) {
            for (L2BridgeDomain c : tenant.getL2BridgeDomain()) {
                networkDomains.put(c.getId().getValue(), c);
            }
        }
        if (tenant.getL2FloodDomain() != null) {
            for (L2FloodDomain c : tenant.getL2FloodDomain()) {
                networkDomains.put(c.getId().getValue(), c);
            }
        }
        if (tenant.getSubnet() != null) {
            for (Subnet s : tenant.getSubnet()) {
                networkDomains.put(s.getId().getValue(), s);
                Set<SubnetId> sset = subnetMap.get(s.getParent().getValue());
                if (sset == null) {
                    subnetMap.put(s.getParent().getValue(), 
                                  sset = new HashSet<SubnetId>());
                }
                sset.add(s.getId());
            }
        }
        if (tenant.getSubjectFeatureInstances() != null) {
            SubjectFeatureInstances sfi = tenant.getSubjectFeatureInstances();
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

    /**
     * Get the underlying tenant object
     * @return the {@link Tenant}
     */
    public Tenant getTenant() {
        return tenant;
    }
    
    /**
     * Look up the network domain specified
     * @param id the {@link NetworkDomainId}
     * @return the {@link NetworkDomain} if it exists, or <code>null</code> 
     * otherwise
     */
    public NetworkDomain getNetworkDomain(NetworkDomainId id) {
        return networkDomains.get(id.getValue());
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
     * Get the layer 3 context for the specified network domain by walking
     * up the hierarchy
     * @param id the {@link NetworkDomainId} for the network domain
     * @return the {@link L3Context} or <code>null</code> if it does not exist
     */
    public L3Context resolveL3Context(NetworkDomainId id) {
        return resolveDomain(L3Context.class, id);
    }

    /**
     * Get the layer 2 bridge domain for the specified network domain by walking
     * up the hierarchy
     * @param id the {@link NetworkDomainId} for the network domain
     * @return the {@link L2BridgeDomain} or <code>null</code> if it does
     * not exist
     */
    public L2BridgeDomain resolveL2BridgeDomain(NetworkDomainId id) {
        return resolveDomain(L2BridgeDomain.class, id);
    }

    /**
     * Get the layer 2 flood domain for the specified network domain by walking
     * up the hierarchy
     * @param id the {@link NetworkDomainId} for the network domain
     * @return the {@link L2FloodDomain} or <code>null</code> if it does
     * not exist
     */
    public L2FloodDomain resolveL2FloodDomain(NetworkDomainId id) {
        return resolveDomain(L2FloodDomain.class, id);
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
            if (visited.contains(id)) return null;
            visited.add(id);
            Set<SubnetId> cursset = subnetMap.get(id.getValue());
            if (cursset != null)
                sset.addAll(cursset);
            NetworkDomain d = networkDomains.get(id.getValue());
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
                return (Subnet)networkDomains.get(input.getValue());
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

    private <C extends NetworkDomain> C resolveDomain(Class<C> domainClass,
                                                      NetworkDomainId id) {
        HashSet<NetworkDomainId> visited = new HashSet<>();        
        while (id != null) {
            if (visited.contains(id)) return null;
            visited.add(id);
            NetworkDomain d = networkDomains.get(id.getValue());
            if (d == null) return null;
            if (domainClass.isInstance(d)) return domainClass.cast(d);
            if (d instanceof Subnet)
                id = ((Subnet)d).getParent();
            else if (d instanceof L2BridgeDomain)
                id = ((L2BridgeDomain)d).getParent();
            else if (d instanceof L2FloodDomain)
                id = ((L2FloodDomain)d).getParent();
            else
                id = null;
        }
        return null;
    }
}
