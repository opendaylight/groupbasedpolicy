/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.domain_extension.l2_l3.impl;

import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.L2L3NetworkDomainAugmentor;

public class L2L3DomainExtensionInstance implements AutoCloseable {

    private L2L3NetworkDomainAugmentor l2l3NetworkDomainAugmentor;

    public L2L3DomainExtensionInstance (DomainSpecificRegistry domainSpecificRegistry) {
         l2l3NetworkDomainAugmentor = new L2L3NetworkDomainAugmentor(domainSpecificRegistry.getNetworkDomainAugmentorRegistry());
    }

    @Override
    public void close() throws Exception {
        l2l3NetworkDomainAugmentor.close();
    }
}
