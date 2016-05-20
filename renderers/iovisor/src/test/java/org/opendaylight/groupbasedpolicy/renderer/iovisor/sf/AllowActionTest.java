/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;

public class AllowActionTest {

    AllowAction action;

    @Before
    public void init() {
        action = new AllowAction();
    }

    @Test
    public void testGetId() {
        assertEquals(action.getId(), AllowActionDefinition.ID);
    }

    @Test
    public void testGetActionDef() {
        assertEquals(action.getActionDef(), AllowActionDefinition.DEFINITION);
    }

    @Test
    public void testGetSupportedParameterValues() {
        assertTrue(action.getSupportedParameterValues().isEmpty());
    }

    @Test
    public void testValidate() {
        ActionInstance actionInstance = new ActionInstanceBuilder().build();
        ValidationResult result = action.validate(actionInstance);
        assertTrue(result.isValid());
    }

}
