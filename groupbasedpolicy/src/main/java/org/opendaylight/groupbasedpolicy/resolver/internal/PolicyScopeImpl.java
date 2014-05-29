/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolverService;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * Implementation for {@link PolicyScope}
 * @author readams
 */
public class PolicyScopeImpl implements PolicyScope {
    /**
     * The parent resolver for this scope.
     */
    private final PolicyResolverService resolver;
    
    /**
     * The listener for this policy scope
     */
    private final PolicyListener listener;

    /**
     * The set of policy scope elements that we want to listen to.
     */
    private Set<PolicyScopeElement> scopeElements;
    
    public PolicyScopeImpl(PolicyResolverService resolver, 
                           PolicyListener listener) {
        super();
        this.resolver = resolver;
        this.listener = listener;
        Map<PolicyScopeElement,Boolean> smap = new ConcurrentHashMap<>();
        scopeElements = Collections.newSetFromMap(smap);
    }

    // ***********
    // PolicyScope
    // ***********

    @Override
    public void addToScope(TenantId tenant, EndpointGroupId endpointGroup) {
        scopeElements.add(new PolicyScopeElement(tenant, endpointGroup));
    }

    @Override
    public void addToScope(TenantId tenant) {
        scopeElements.add(new PolicyScopeElement(tenant, null));        
    }
    
    // ***************
    // PolicyScopeImpl
    // ***************

    protected Set<PolicyScopeElement> getScopeElements() {
        return scopeElements;
    }
    
    protected static class PolicyScopeElement {
        private final TenantId tenant;
        private final EndpointGroupId endpointGroup;
        public PolicyScopeElement(TenantId tenant, 
                                  EndpointGroupId endpointGroup) {
            super();
            this.tenant = tenant;
            this.endpointGroup = endpointGroup;
        }
        public TenantId getTenant() {
            return tenant;
        }
        public EndpointGroupId getEndpointGroup() {
            return endpointGroup;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result +
                     ((endpointGroup == null) ? 0 : endpointGroup.hashCode());
            result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
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
            PolicyScopeElement other = (PolicyScopeElement) obj;
            if (endpointGroup == null) {
                if (other.endpointGroup != null)
                    return false;
            } else if (!endpointGroup.equals(other.endpointGroup))
                return false;
            if (tenant == null) {
                if (other.tenant != null)
                    return false;
            } else if (!tenant.equals(other.tenant))
                return false;
            return true;
        }
    }

}
