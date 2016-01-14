/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;

/**
 * Class for mocking up endpoints for unit tests
 * @author readams
 */
public class MockEndpointManager extends EndpointManager {

    private Map<EndpointL3Key, EndpointL3> endpointsL3 = new HashMap<>();

    public MockEndpointManager() {
        super(null, null, null, null, null);
    }

    public void addEndpoint(Endpoint ep) {
        processEndpoint(null, ep);
    }

    public void addL3Endpoint(EndpointL3 l3Ep) {
        endpointsL3.put(l3Ep.getKey(), l3Ep);
    }

    @Override
    protected Collection<EndpointL3> getL3Endpoints() {
        return endpointsL3.values();
    }
}
