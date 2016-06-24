/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.UnconfiguredRendererEndpoint;

/**
 * Purpose: placeholder for
 * <ul>
 *     <li>{@link PolicyWriter}</li>
 *     <li>{@link Status} parts</li>
 * </ul>
 */
public class PolicyConfigurationContext {

    private final List<UnconfiguredRendererEndpoint> unconfiguredRendererEPBag;
    private PolicyWriter policyWriter;
    private RendererEndpoint currentRendererEP;

    public PolicyConfigurationContext() {
        unconfiguredRendererEPBag = new ArrayList<>();
    }

    /**
     * @return policyWriter for mountpoint currently being configured
     */
    public PolicyWriter getPolicyWriter() {
        return policyWriter;
    }

    /**
     * @param policyWriter for mountpoint currently being configured
     */
    public void setPolicyWriter(final PolicyWriter policyWriter) {
        this.policyWriter = policyWriter;
    }

    /**
     * @return list of not configurable policies
     */
    public List<UnconfiguredRendererEndpoint> getUnconfiguredRendererEPBag() {
        return unconfiguredRendererEPBag;
    }

    /**
     * append given endpoint to collection of not configurable policies
     * @param endpoint not configurable endpoint
     */
    public void appendUnconfiguredRendererEP(UnconfiguredRendererEndpoint endpoint) {
        unconfiguredRendererEPBag.add(endpoint);
    }

    /**
     * @param currentRendererEP endpoint currently being configured
     */
    public void setCurrentRendererEP(final RendererEndpoint currentRendererEP) {
        this.currentRendererEP = currentRendererEP;
    }

    /**
     * @return endpoint currently being configured
     */
    public RendererEndpoint getCurrentRendererEP() {
        return currentRendererEP;
    }
}
