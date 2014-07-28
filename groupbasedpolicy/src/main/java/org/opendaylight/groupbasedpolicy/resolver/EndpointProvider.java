/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;

/**
 * When the policy changes, recompute the set of active condition groups
 * based on the endpoints in a particular endpoint group
 * @author readams
 */
public interface EndpointProvider {
    /**
     * Get a collection of endpoints in a particular endpoint group
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Collection<Endpoint> getEndpointsForGroup(EgKey eg);

    /**
     * Get the effective list of conditions that apply to a particular 
     * endpoint.  This could include additional conditions over the condition
     * labels directly represented in the endpoint object
     * @param endpoint the {@link Endpoint} to resolve
     * @return the list of {@link ConditionName}
     */
    public List<ConditionName> getCondsForEndpoint(Endpoint endpoint);
}
