/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;

public class MockFaasEndpointManagerListener extends FaasEndpointManagerListener {

    private Endpoint expectedEndpoint;

    public MockFaasEndpointManagerListener(FaasPolicyManager policyManager, DataBroker dataProvider,
            Executor executor) {
        super(policyManager, dataProvider, executor);
    }

    // *******************************************************
    // Test Stubs
    // *******************************************************
    @Override
    protected void processEndpoint(Endpoint endpoint) {
        if (expectedEndpoint != null) {
            assertTrue("FaasEndpointManagerListener.processEndpoint", expectedEndpoint.equals(endpoint));
        }
    }

    // *******************************************************
    // The following Methods are to input test data
    // *******************************************************
    public void setExpectedEndpoint(Endpoint testEndpoint) {
        this.expectedEndpoint = testEndpoint;
    }
}
