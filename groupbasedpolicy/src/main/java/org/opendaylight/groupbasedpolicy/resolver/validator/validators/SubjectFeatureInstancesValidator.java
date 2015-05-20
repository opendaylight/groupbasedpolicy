package org.opendaylight.groupbasedpolicy.resolver.validator.validators;

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.groupbasedpolicy.resolver.validator.AbstractValidator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yangtools.yang.binding.ChildOf;

/**
 * Validator for {@link SubjectFeatureInstances}
 */
public class SubjectFeatureInstancesValidator extends AbstractValidator<SubjectFeatureInstances> {

    @Override
    protected List<ChildOf<SubjectFeatureInstances>> getChildObjects(SubjectFeatureInstances objectToValidate) {
        List<ChildOf<SubjectFeatureInstances>> childObjects = new ArrayList<>();

        childObjects.addAll(objectToValidate.getActionInstance());
        childObjects.addAll(objectToValidate.getClassifierInstance());

        return childObjects;
    }

    @Override
    public Class<SubjectFeatureInstances> getType() {
        return SubjectFeatureInstances.class;
    }

}
