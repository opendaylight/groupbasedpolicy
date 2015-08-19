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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public class SubjectFeatureInstancesValidatorTest {

    private SubjectFeatureInstancesValidator validator;

    @Before
    public void initialise() {
        validator = new SubjectFeatureInstancesValidator();
    }

    @Test
    public void getChildObjectsTest() {
        SubjectFeatureInstances objectToValidate = mock(SubjectFeatureInstances.class);
        ActionInstance actionInstance = mock(ActionInstance.class);
        when(objectToValidate.getActionInstance()).thenReturn(Arrays.asList(actionInstance));
        ClassifierInstance classifierInstance = mock(ClassifierInstance.class);
        when(objectToValidate.getClassifierInstance()).thenReturn(Arrays.asList(classifierInstance));

        List<ChildOf<SubjectFeatureInstances>> childObjects = validator.getChildObjects(objectToValidate);
        Assert.assertTrue(childObjects.contains(actionInstance));
        Assert.assertTrue(childObjects.contains(classifierInstance));
    }

    @Test
    public void getTypeTest() {
        Assert.assertEquals(SubjectFeatureInstances.class, validator.getType());
    }

}
