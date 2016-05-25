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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class L2L3DomainExtensionModule extends org.opendaylight.controller.config.yang.config.domain_extension.l2_l3.impl.AbstractL2L3DomainExtensionModule {

    private static final Logger LOG = LoggerFactory.getLogger(L2L3DomainExtensionModule.class);

    public L2L3DomainExtensionModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public L2L3DomainExtensionModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.domain_extension.l2_l3.impl.L2L3DomainExtensionModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DomainSpecificRegistry domainSpecificRegistry = getDomainSpecificRegistryDependency();
        Preconditions.checkNotNull(domainSpecificRegistry);
        L2L3NetworkDomainAugmentor l2l3NetworkDomainAugmentor =
                new L2L3NetworkDomainAugmentor(domainSpecificRegistry.getNetworkDomainAugmentorRegistry());
        LOG.info("{} successfully started.", L2L3DomainExtensionModule.class.getCanonicalName());
        return l2l3NetworkDomainAugmentor;
    }

}
