/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator.validators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public class TenantValidatorTest {

    private TenantValidator validator;

    @Before
    public void initialise() {
        validator = new TenantValidator();
    }

    @Test
    public void getChildObjectsTest() {
        Tenant objectToValidate = mock(Tenant.class);
        SubjectFeatureInstances subjectFeatureInstances = mock(SubjectFeatureInstances.class);
        when(objectToValidate.getSubjectFeatureInstances()).thenReturn(subjectFeatureInstances);
        List<ChildOf<Tenant>> childObjects = validator.getChildObjects(objectToValidate);
        Assert.assertTrue(childObjects.contains(subjectFeatureInstances));
    }

    @Test
    public void getTypeTest() {
        Assert.assertEquals(Tenant.class, validator.getType());
    }
}
