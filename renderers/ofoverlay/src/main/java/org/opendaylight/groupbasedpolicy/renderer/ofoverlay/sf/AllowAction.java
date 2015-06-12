/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;


/**
 * Allow action
 */
public class AllowAction extends Action {

    protected static final ActionDefinitionId ID = new ActionDefinitionId("f942e8fd-e957-42b7-bd18-f73d11266d17");
    /**
     * Access control - allow action-definition
     */
    public static final ActionDefinition DEFINITION = new ActionDefinitionBuilder().setId(ID)
        .setName(new ActionName("allow"))
        .setDescription(new Description("Allow the specified traffic to pass"))
        .build();

    // How allow is implemented in the PolicyEnforcer table
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action allow = nxOutputRegAction(NxmNxReg7.class);

    @Override
    public ActionDefinitionId getId() {
        return ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return DEFINITION;
    }

    @Override
    public List<ActionBuilder> updateAction(List<ActionBuilder> actions,
                                            Map<String, Object> params,
                                            Integer order,
                                            NetworkElements netElements,
                                            PolicyPair policyPair,
                                            FlowMap flowMap,
                                            OfContext ctx,
                                            Direction direction) {
        /*
         * Allow action doesn't use parameters
         * TODO: check to make sure ActionBuilder w/allow isn't already present
         */
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(allow)
          .setOrder(order);
        actions.add(ab);
        return actions;
    }

    @Override
    public boolean isValid(ActionInstance actionInstance) {
        return true;
    }

}
