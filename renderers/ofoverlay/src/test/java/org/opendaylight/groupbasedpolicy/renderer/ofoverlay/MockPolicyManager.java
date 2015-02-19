/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;

public class MockPolicyManager extends PolicyManager {

    public MockPolicyManager(PolicyResolver policyResolver,
                             EndpointManager endpointManager) {
        super(null, policyResolver, null, endpointManager, null, null);
    }

}
