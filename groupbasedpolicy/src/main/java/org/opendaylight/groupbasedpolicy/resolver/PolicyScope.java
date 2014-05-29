/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * The policy scope object represents a scope for policy-related information.
 * A renderer that addresses a particular scope can express this as a 
 * {@link PolicyScopeImpl} with an associates {@link PolicyListener} that can
 * receive relevant updates.
 * @see PolicyResolver 
 * @author readams
 */
public interface PolicyScope {

    /**
     * Add the endpoint group from the given tenant to the scope of updates
     * @param tenant the tenant for the endpoint group
     * @param endpointGroup the endpoint group to add
     */
    public void addToScope(TenantId tenant,
                           EndpointGroupId endpointGroup);

    /**
     * Add all endpoint groups in the given tenant to the scope of updates
     * @param tenant the tenant to add.
     */
    public void addToScope(TenantId tenant);

}