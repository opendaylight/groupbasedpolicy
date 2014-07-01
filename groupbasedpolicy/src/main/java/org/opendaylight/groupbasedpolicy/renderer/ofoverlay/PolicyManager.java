/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Set;

import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.EncapsulationFormat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage policies on switches by subscribing to updates from the 
 * policy resolver and information about endpoints from the endpoint 
 * registry
 * @author readams
 */
public class PolicyManager implements SwitchListener, PolicyListener {
    private static final Logger LOG = 
            LoggerFactory.getLogger(PolicyManager.class);

    private final PolicyResolver policyResolver;
    private final SwitchManager switchManager;
    private final EndpointManager endpointManager;
    private final SalFlowService flowService;
    
    private final PolicyScope policyScope;
    
    public PolicyManager(PolicyResolver policyResolver,
                         SwitchManager switchManager,
                         EndpointManager endpointManager, 
                         SalFlowService flowService) {
        super();
        this.policyResolver = policyResolver;
        this.switchManager = switchManager;
        this.endpointManager = endpointManager;
        this.flowService = flowService;
        
        policyScope = policyResolver.registerListener(this);
        
        switchManager.registerListener(this);
        LOG.debug("Initialized OFOverlay policy manager");
    }

    // **************
    // SwitchListener
    // **************

    @Override
    public void switchReady(NodeId sw) {

    }

    @Override
    public void switchRemoved(NodeId sw) {
        // TODO Auto-generated method stub
    }

    // **************
    // PolicyListener
    // **************
    
    @Override
    public void policyUpdated(Set<EgKey> updatedConsumers) {
        // TODO Auto-generated method stub
    }

    // *************
    // PolicyManager
    // *************
    
    /**
     * Set the encapsulation format the specified value
     * @param format The new format
     */
    public void setEncapsulationFormat(EncapsulationFormat format) {
        // No-op for now
    }
    
    // **************
    // Implementation
    // **************

}
