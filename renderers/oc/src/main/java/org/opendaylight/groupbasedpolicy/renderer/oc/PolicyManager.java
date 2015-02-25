/*
 * Copyright (c) 2015 Juniper Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.oc;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage policies
 */
public class PolicyManager implements PolicyListener, L2DomainListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(PolicyManager.class);

    private final PolicyResolver policyResolver;
    private final PolicyScope policyScope;
    private final ScheduledExecutorService executor;

    public PolicyManager(DataBroker dataBroker,
                         PolicyResolver policyResolver,
                         L2DomainManager l2domainManager,
                         RpcProviderRegistry rpcRegistry,
                         ScheduledExecutorService executor) {
        super();
        this.executor = executor;
        this.policyResolver = policyResolver;

       policyScope = policyResolver.registerListener(this);
       l2domainManager.registerListener((L2DomainListener) this);
        LOG.debug("Initialized OC policy manager");
    }

    /**
     * Set the learning mode to the specified value
     * @param learningMode the learning mode to set
     */
    public void setLearningMode(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.oc.rev140528.OcConfig.LearningMode learningMode) {
        // No-op for now
    }

    public void L2DomainUpdated(L2FloodDomainId id) {
		// TODO Auto-generated method stub
	}

	@Override
	public void policyUpdated(Set<EgKey> updatedGroups) {
		// TODO Auto-generated method stub
	}
}