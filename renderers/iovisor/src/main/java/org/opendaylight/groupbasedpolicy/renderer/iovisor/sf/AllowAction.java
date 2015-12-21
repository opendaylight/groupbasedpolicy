/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import java.util.Collections;
import java.util.List;

import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.dto.ValidationResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.action.definition.SupportedParameterValues;

/**
 * Allow action
 */
public class AllowAction extends Action {

    @Override
    public ActionDefinitionId getId() {
        return AllowActionDefinition.ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return AllowActionDefinition.DEFINITION;
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
