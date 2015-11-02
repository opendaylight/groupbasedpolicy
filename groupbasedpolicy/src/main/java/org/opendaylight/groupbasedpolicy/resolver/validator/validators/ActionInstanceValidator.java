/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator.validators;

import org.opendaylight.groupbasedpolicy.resolver.validator.AbstractValidator;
import org.opendaylight.groupbasedpolicy.resolver.validator.SimpleResult;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;

/**
 * Validator for {@link ActionInstance}
 */
public class ActionInstanceValidator extends AbstractValidator<ActionInstance> {

    @Override
    protected SimpleResult validateSelf(ActionInstance objectToValidate) {
        org.opendaylight.groupbasedpolicy.resolver.ActionInstanceValidator action = getPolicyResolver().getActionInstanceValidator(objectToValidate.getActionDefinitionId());
        if (action == null) {
            return new SimpleResult(false, "Action not registered in PolicyResolver.");
        }

        boolean isValid = action.isValid(objectToValidate);

        return new SimpleResult(isValid, "Validation result of " + action.getClass().getName() + " class.");
    }

    @Override
    public Class<ActionInstance> getType() {
        return ActionInstance.class;
    }

}
