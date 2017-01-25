/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;

import com.google.common.base.Preconditions;

@Immutable
public class RendererResolvedPolicy implements Comparable<RendererResolvedPolicy> {

    private final EndpointPolicyParticipation rendererEndpointParticipation;
    private final ResolvedRuleGroup ruleGroup;

    public RendererResolvedPolicy(@Nonnull EndpointPolicyParticipation rendererEndpointParticipation,
            @Nonnull ResolvedRuleGroup ruleGroupInfo) {
        this.rendererEndpointParticipation = Preconditions.checkNotNull(rendererEndpointParticipation);
        this.ruleGroup = Preconditions.checkNotNull(ruleGroupInfo);
    }

    public EndpointPolicyParticipation getRendererEndpointParticipation() {
        return rendererEndpointParticipation;
    }

    public ResolvedRuleGroup getRuleGroup() {
        return ruleGroup;
    }

    @Override
    public int compareTo(RendererResolvedPolicy resolvedPolicy) {
        int comp = ruleGroup.compareTo(resolvedPolicy.getRuleGroup());
        if (comp == 0 && (rendererEndpointParticipation.getIntValue() != resolvedPolicy
            .getRendererEndpointParticipation().getIntValue())) {
            return 1;
        }
        return comp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((rendererEndpointParticipation == null) ? 0 : rendererEndpointParticipation.hashCode());
        result = prime * result + ((ruleGroup == null) ? 0 : ruleGroup.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RendererResolvedPolicy other = (RendererResolvedPolicy) obj;
        if (rendererEndpointParticipation != other.rendererEndpointParticipation)
            return false;
        if (ruleGroup == null) {
            if (other.ruleGroup != null)
                return false;
        } else if (!ruleGroup.equals(other.ruleGroup))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RendererResolvedPolicy [rendererEndpointParticipation=" + rendererEndpointParticipation + ", ruleGroup="
                + ruleGroup + "]";
    }

}
