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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.unconfigured.rule.group.UnconfiguredResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.UnconfiguredRendererEndpoint;

/**
 * Purpose: placeholder for
 * <ul>
 * <li>{@link PolicyManagerImpl.PolicyMapLocation}</li>
 * <li>{@link Status} parts</li>
 * </ul>
 */
public class PolicyConfigurationContext {

    private final List<UnconfiguredRendererEndpoint> unconfiguredRendererEPBag;
    private final List<CheckedFuture<Boolean, TransactionCommitFailedException>> cumulativeResult;
    private PolicyManagerImpl.PolicyMapLocation policyMapLocation;
    private RendererEndpoint currentRendererEP;
    private UnconfiguredResolvedRule currentUnconfiguredRule;

    public PolicyConfigurationContext() {
        unconfiguredRendererEPBag = new ArrayList<>();
        cumulativeResult = new ArrayList<>();
    }

    /**
     * Set transaction result to result pool
     *
     * @param result current result
     */
    public void setFutureResult(final CheckedFuture<Boolean, TransactionCommitFailedException> result) {
        cumulativeResult.add(result);
    }

    /**
     * append given endpoint to collection of not configurable policies
     *
     * @param endpoint not configurable endpoint
     */
    public void appendUnconfiguredRendererEP(UnconfiguredRendererEndpoint endpoint) {
        unconfiguredRendererEPBag.add(endpoint);
    }

    /**
     * @return policy-map location
     */
    public PolicyManagerImpl.PolicyMapLocation getPolicyMapLocation() {
        return policyMapLocation;
    }

    /**
     * @param policyMapLocation for actual policy-map/interface location
     */
    public void setPolicyMapLocation(final PolicyManagerImpl.PolicyMapLocation policyMapLocation) {
        this.policyMapLocation = policyMapLocation;
    }

    /**
     * @return endpoint currently being configured
     */
    public RendererEndpoint getCurrentRendererEP() {
        return currentRendererEP;
    }

    /**
     * @param currentRendererEP endpoint currently being configured
     */
    public void setCurrentRendererEP(final RendererEndpoint currentRendererEP) {
        this.currentRendererEP = currentRendererEP;
    }

    /**
     * @return list of not configurable policies
     */
    List<UnconfiguredRendererEndpoint> getUnconfiguredRendererEPBag() {
        return unconfiguredRendererEPBag;
    }

    /**
     * @return all unconfigured rules
     */
    public UnconfiguredResolvedRule getCurrentUnconfiguredRule() {
        return currentUnconfiguredRule;
    }

    /**
     * Add unconfigured rule to list
     *
     * @param unconfiguredResolvedRule unconfigured rule
     */
    public void setCurrentUnconfiguredRule(final UnconfiguredResolvedRule unconfiguredResolvedRule) {
        this.currentUnconfiguredRule = unconfiguredResolvedRule;
    }

    /**
     * @return get all transaction results as a list
     */
    ListenableFuture<List<Boolean>> getCumulativeResult() {
        return Futures.allAsList(cumulativeResult);
    }
}
