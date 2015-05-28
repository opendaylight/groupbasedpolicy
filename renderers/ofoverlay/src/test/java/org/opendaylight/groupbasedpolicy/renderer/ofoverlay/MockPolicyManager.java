/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

public class MockPolicyManager extends PolicyManager {

    private static short offSet = 0;
    public MockPolicyManager(PolicyResolver policyResolver,
                             EndpointManager endpointManager) {
        super(null, policyResolver, null, endpointManager, null, null, offSet, new MacAddress("01:23:45:67:89:00"));
    }

}
