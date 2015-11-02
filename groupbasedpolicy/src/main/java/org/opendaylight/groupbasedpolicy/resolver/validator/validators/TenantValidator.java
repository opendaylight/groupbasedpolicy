/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator.validators;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.groupbasedpolicy.resolver.validator.AbstractValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.yang.binding.ChildOf;

/**
 * Validator for {@link Tenant}
 */
public class TenantValidator extends AbstractValidator<Tenant> {

    @Override
    protected List<ChildOf<Tenant>> getChildObjects(Tenant objectToValidate) {
        List<ChildOf<Tenant>> childObjects = new ArrayList<>();

        childObjects.add(objectToValidate.getSubjectFeatureInstances());

        return childObjects;
    }

    @Override
    public Class<Tenant> getType() {
        return Tenant.class;
    }

}
