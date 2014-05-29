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

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;

/**
 * The policy resolver is a utility for renderers to help in resolving
 * group-based policy into a form that is easier to apply to the actual network.
 * 
 * <p>For any pair of endpoint groups, there is a set of rules that could apply 
 * to the endpoints on that group based on the policy configuration.  The exact
 * list of rules that apply to a given pair of endpoints depends on the 
 * conditions that are active on the endpoints.
 * 
 * In a more formal sense: Let there be endpoint groups G_n, and for each G_n a 
 * set of conditions C_n that can apply to endpoints in G_n.  Further, let S be 
 * the set of lists of rules defined in the policy.  Our policy can be 
 * represented as a function F: (G_n, 2^C_n, G_m, 2^C_m) -> S, where 2^C_n 
 * represents the power set of C_n. In other words, we want to map all the 
 * possible tuples of pairs of endpoints along with their active conditions 
 * onto the right list of rules to apply.
 * 
 * <p>We need to be able to query against this policy model, enumerate the 
 * relevant classes of traffic and endpoints, and notify renderers when there
 * are changes to policy as it applies to active sets of endpoints and 
 * endpoint groups.
 * 
 * <p>The policy resolver will maintain the necessary state for all tenants
 * in its control domain, which is the set of tenants for which 
 * policy listeners have been registered.
 * 
 * @author readams
 */
public interface PolicyResolverService {

    /**
     * Get the policy that currently applies to a endpoints. 
     * with the specified groups and conditions.  The rules are normalized 
     * such that rules with a direction of "out" apply to traffic from the 
     * first endpoint to the second endpoint.
     * 
     * @param ep1Group the endpoint group for the first endpoint 
     * @param ep1Conds The conditions that apply to the first endpoint
     * @param ep2Group the endpoint group for the second endpoint 
     * @param ep2Conds The conditions that apply to the second endpoint.
     * @return a list of {@link Rule} that apply to the endpoints.
     */
    public List<Rule> getPolicy(EndpointGroupId ep1Group, 
                                Collection<Condition> ep1Conds,
                                EndpointGroupId ep2Group, 
                                Collection<Condition> ep2Conds);

    /**
     * Register a listener to receive update events.
     * @param listener the {@link PolicyListener} object to receive the update
     * events
     */
    public PolicyScope registerListener(PolicyListener listener);

    /**
     * Remove the listener registered for the given {@link PolicyScopeImpl}.
     * @param scope the scope to remove
     * @see PolicyResolver#registerListener(PolicyListener)
     */
    public void removeListener(PolicyScope scope);

}