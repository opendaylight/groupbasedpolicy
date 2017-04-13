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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DestinationMapper extends AddressMapper {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationMapper.class);
    private static final String METADATA_IP_PREFIX = "169.254.169.254/32";
    private static final String IN__METADATA = "In__METADATA";

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
        if (!EndpointUtils.getParentEndpoints(addrEp.getParentEndpointChoice()).isEmpty()) {
            // TODO more parents, when supported
            ParentEndpoint parentEp = EndpointUtils.getParentEndpoints(addrEp.getParentEndpointChoice()).get(0);
            if (parentEp != null && parentEp.getContextType().isAssignableFrom(L3Context.class)) {
                // TODO this is a fix for metadata agent in DHCP namespace, when we will fully support multiple IPs
                // per interface we shall rework this
                if (aclRuleBuilder.getName().contains(IN__METADATA)) {
                    LOG.trace("Setting dst IP address {} in rule {}", METADATA_IP_PREFIX, aclRuleBuilder);
                    try {
                        AccessListUtil.setDestinationL3Address(aclRuleBuilder, METADATA_IP_PREFIX);
                    } catch (UnknownHostException e) {
                        LOG.error("Failed to parse address {}. Cannot apply ACL entry {}. {}", METADATA_IP_PREFIX,
                            aclRuleBuilder, e);
                    }
                } else {
                    LOG.trace("Setting dst IP address {} in rule {}", parentEp.getAddress(), aclRuleBuilder);
                    try {
                        AccessListUtil.setDestinationL3Address(aclRuleBuilder, parentEp.getAddress());
                    } catch (UnknownHostException e) {
                        LOG.error("Failed to parse address {}. Cannot apply ACL entry {}. {}", parentEp.getAddress(),
                            aclRuleBuilder, e);
                    }
                }

            }
        }
    }
}
