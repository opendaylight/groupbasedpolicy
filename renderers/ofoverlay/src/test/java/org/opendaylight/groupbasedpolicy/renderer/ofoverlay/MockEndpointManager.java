/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;

/**
 * Class for mocking up endpoints for unit tests
 * @author readams
 */
public class MockEndpointManager extends EndpointManager {

    public MockEndpointManager() {
        super(null, null, null, null);
    }

    public void addEndpoint(Endpoint ep) {
        updateEndpoint(null, ep);
    }
}
