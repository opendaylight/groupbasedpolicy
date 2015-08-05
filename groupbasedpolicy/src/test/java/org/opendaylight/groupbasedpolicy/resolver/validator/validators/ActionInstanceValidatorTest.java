/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *s
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator.validators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.validator.SimpleResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;

public class ActionInstanceValidatorTest {

    private ActionInstanceValidator validator;
    private PolicyResolver policyResolver;
    private org.opendaylight.groupbasedpolicy.resolver.ActionInstanceValidator action;

    @Before
    public void initialise() {
        validator = new ActionInstanceValidator();
        policyResolver = mock(PolicyResolver.class);
        action = mock(org.opendaylight.groupbasedpolicy.resolver.ActionInstanceValidator.class);
        when(policyResolver.getActionInstanceValidator(any(ActionDefinitionId.class))).thenReturn(action);
        validator.setPolicyResolver(policyResolver);
    }

    @Test
    public void validateSelfTestValidFalse() {
        ActionInstance objectToValidate = mock(ActionInstance.class);
        when(action.isValid(objectToValidate)).thenReturn(false);

        SimpleResult simpleResult = validator.validateSelf(objectToValidate);
        Assert.assertNotNull(simpleResult);
        Assert.assertTrue(simpleResult.isFailure());
        Assert.assertTrue(simpleResult.getDescription().contains("ActionInstance"));
    }

    @Test
    public void validateSelfTestValidTrue() {
        ActionInstance objectToValidate = mock(ActionInstance.class);
        when(action.isValid(objectToValidate)).thenReturn(true);

        SimpleResult simpleResult = validator.validateSelf(objectToValidate);
        Assert.assertNotNull(simpleResult);
        Assert.assertTrue(simpleResult.isSuccess());
        Assert.assertTrue(simpleResult.getDescription().contains("ActionInstance"));
    }

    @Test
    public void validateSelfTestValidActionNull() {
        ActionInstance objectToValidate = mock(ActionInstance.class);
        when(policyResolver.getActionInstanceValidator(any(ActionDefinitionId.class))).thenReturn(null);

        SimpleResult simpleResult = validator.validateSelf(objectToValidate);
        Assert.assertNotNull(simpleResult);
        Assert.assertTrue(simpleResult.isFailure());
    }

    @Test
    public void getTypeTest() {
        Assert.assertEquals(ActionInstance.class, validator.getType());
    }
}
