/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;

public class MockPolicyManager extends PolicyManager {

    private static short offSet = 0;
    public MockPolicyManager(EndpointManager endpointManager) {
        super(null, null, endpointManager, null, offSet);
    }

}
