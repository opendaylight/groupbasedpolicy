/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.net.UnknownHostException;
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DestinationMapper extends AddressMapper {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationMapper.class);

    DestinationMapper(ACE_DIRECTION direction) {
        super(direction);
    }

    @Override
    List<GbpAceBuilder> updateExtRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            ContainmentEndpoint cEp) {
        // TODO external networking.
        return rules;
    }

    @Override
    void updateRule(AddressEndpointWithLocation addrEp, GbpAceBuilder aclRuleBuilder) {
        String address;
        if (addrEp.getContextType().isAssignableFrom(L3Context.class)) {
            address = addrEp.getAddress();
        } else {
            List<ParentEndpoint> parentEndpoints = EndpointUtils.getParentEndpoints(addrEp.getParentEndpointChoice());
            if (parentEndpoints == null || parentEndpoints.isEmpty()
                    || !parentEndpoints.get(0).getContextType().isAssignableFrom(L3Context.class)) {
                LOG.warn("Cannot resolve IP address for endpoint {}", addrEp);
                return;
            }
            address = parentEndpoints.get(0).getAddress();
        }
        try {
            AccessListUtil.setDestinationL3Address(aclRuleBuilder, address);
        } catch (UnknownHostException e) {
            LOG.error("Failed to parse address {}. Cannot apply ACL entry {}. {}", address, aclRuleBuilder, e);
        }
    }
}
