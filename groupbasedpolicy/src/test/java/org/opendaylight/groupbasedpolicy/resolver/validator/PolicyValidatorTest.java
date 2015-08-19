/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class PolicyValidatorTest {

    DataContainer dataContainer;
    PolicyResolver policyResolver;

    @Before
    public void initialise() {
        dataContainer = mock(DataContainer.class);
        policyResolver = mock(PolicyResolver.class);

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void createValidatorTest() {
        Validator validator;
        validator = PolicyValidator.createValidator(dataContainer, policyResolver);
        Assert.assertNull(validator);

        when(dataContainer.getImplementedInterface()).thenReturn((Class) Tenant.class);
        validator = PolicyValidator.createValidator(dataContainer, policyResolver);
        Assert.assertNotNull(validator);
        Assert.assertEquals(policyResolver, ((AbstractValidator) validator).getPolicyResolver());

        when(dataContainer.getImplementedInterface()).thenReturn((Class) SubjectFeatureInstances.class);
        validator = PolicyValidator.createValidator(dataContainer, policyResolver);
        Assert.assertNotNull(validator);
        Assert.assertEquals(policyResolver, ((AbstractValidator) validator).getPolicyResolver());

        when(dataContainer.getImplementedInterface()).thenReturn((Class) ActionInstance.class);
        validator = PolicyValidator.createValidator(dataContainer, policyResolver);
        Assert.assertNotNull(validator);
        Assert.assertEquals(policyResolver, ((AbstractValidator) validator).getPolicyResolver());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void validateTest() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getImplementedInterface()).thenReturn((Class) Tenant.class);
        SubjectFeatureInstances subjectFeatureInstances = mock(SubjectFeatureInstances.class);
        when(tenant.getSubjectFeatureInstances()).thenReturn(subjectFeatureInstances);
        PolicyValidator.validate(tenant, policyResolver);

        when(subjectFeatureInstances.getImplementedInterface()).thenReturn((Class) SubjectFeatureInstances.class);
        PolicyValidator.validate(subjectFeatureInstances, policyResolver);

        ActionInstance actionInstance = mock(ActionInstance.class);
        when(actionInstance.getImplementedInterface()).thenReturn((Class) ActionInstance.class);
        PolicyValidator.validate(actionInstance, policyResolver);
    }

}
