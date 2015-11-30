/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.sf.actions.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.groupbasedpolicy.resolver.validator.ValidationResult;
import org.opendaylight.groupbasedpolicy.resolver.validator.ValidationResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.action.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;

/**
 * Allow action
 */
public class AllowAction extends Action {

    // How allow is implemented in the PolicyEnforcer table
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action allow = nxOutputRegAction(NxmNxReg7.class);

    @Override
    public ActionDefinitionId getId() {
        return AllowActionDefinition.ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return AllowActionDefinition.DEFINITION;
    }

    @Override
    public List<ActionBuilder> updateAction(List<ActionBuilder> actions,
                                            Map<String, Object> params,
                                            Integer order,
                                            NetworkElements netElements,
                                            PolicyPair policyPair,
                                            OfWriter ofWriter,
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
    public ValidationResult validate(ActionInstance actionInstance) {
        return new ValidationResultBuilder().success().build();
    }

    @Override
    public List<SupportedParameterValues> getSupportedParameterValues() {
        // allow action definition has no parameter
        return Collections.emptyList();
    }
}
