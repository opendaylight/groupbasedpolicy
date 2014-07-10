/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * The policy scope object represents a scope for policy-related information.
 * A renderer that addresses a particular scope can express this as a 
 * {@link PolicyScope} with an associates {@link PolicyListener} that can
 * receive relevant updates.
 * @see PolicyResolver 
 * @author readams
 */
public class PolicyScope {

    /**
     * The parent policy resolver
     */
    private final PolicyResolver resolver;
    
    /**
     * The listener for this policy scope
     */
    private final PolicyListener listener;

    /**
     * The set of policy scope elements that we want to listen to.
     */
    private Set<EgKey> scopeElements;
    
    public PolicyScope(PolicyResolver resolver,
                       PolicyListener listener) {
        super();
        this.resolver = resolver;
        this.listener = listener;
        Map<EgKey,Boolean> smap = new ConcurrentHashMap<>();
        scopeElements = Collections.newSetFromMap(smap);
    }

    // ***********
    // PolicyScope
    // ***********

    /**
     * Add the endpoint group from the given tenant and endpoint group to the
     * scope of updates
     * @param tenant the tenant for the endpoint group
     * @param endpointGroup the endpoint group to add.  This is the consumer
     * of the contract
     */
    public void addToScope(TenantId tenant, EndpointGroupId endpointGroup) {
        synchronized (this) {
            scopeElements.add(new EgKey(tenant, endpointGroup));
            resolver.subscribeTenant(tenant);
        }
    }

    /**
     * Add all endpoint groups in the given tenant to the scope of updates
     * @param tenant the tenant to add.
     */
    public void addToScope(TenantId tenant) {
        addToScope(tenant, null);
    }

    /**
     * Remove an endpoint group from the given tenant and endpoint group from 
     * the scope of updates
     * @param tenant the tenant for the endpoint group
     * @param endpointGroup the endpoint group to remove.  This is the consumer
     * of the contract
     */
    public void removeFromScope(TenantId tenant, 
                                EndpointGroupId endpointGroup) {
        synchronized (this) {
            boolean canUnsubscribe = false;
            scopeElements.remove(new EgKey(tenant, endpointGroup));
            for (EgKey element : scopeElements) {
                if (element.getTenantId().equals(tenant)) {
                    canUnsubscribe = false;
                    break;
                }
            }
            if (canUnsubscribe) {
                resolver.unsubscribeTenant(tenant);
            }
        }
    }

    /**
     * Remove an endpoint group from the given tenant from 
     * the scope of updates
     * @param tenant the tenant for the endpoint group
     */
    public void removeFromScope(TenantId tenant) {
        removeFromScope(tenant, null);
    }

    /**
     * Check whether the policy scope applies to the given tenant and endpoint
     * group
     * @param tenant the tenant to look up
     * @param endpointGroup the endpoint group to look up.  May be null, 
     * in which case will only check if the policy scope applies to the entire
     * tenant
     * @return <code>true</code> if the policy scope applies to the given
     * tenant and endpoint group.
     */
    public boolean contains(TenantId tenant, EndpointGroupId endpointGroup) {
        EgKey pse = new EgKey(tenant, endpointGroup);
        if (scopeElements.contains(pse)) return true;
        pse = new EgKey(tenant, null);
        return scopeElements.contains(pse);
                
    }

    /**
     * Get the policy listener for this scope
     * @return the policy listener
     */
    public PolicyListener getListener() {
        return listener;
    }
}
