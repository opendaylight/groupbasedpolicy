/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;


abstract class AddressMapper {

    private AccessListUtil.ACE_DIRECTION direction;

    private static final PortNumber DHCP_67 = new PortNumber(67);
    private static final PortNumber DHCP_68 = new PortNumber(68);
    private static final PortNumber DHCPV6_547 = new PortNumber(547);
    private static final PortNumber DHCPV6_548 = new PortNumber(548);

    AddressMapper(AccessListUtil.ACE_DIRECTION direction) {
        this.direction = direction;
    }

    abstract void updateRule(AddressEndpointWithLocation addrEp, GbpAceBuilder aclRuleBuilder);

    //TODO implement for peers with no location
    abstract List<GbpAceBuilder> updateExtRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            ContainmentEndpoint cEp);

    public List<GbpAceBuilder> updateRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            AddressEndpointWithLocation peerEp) {
        if (this instanceof SourceMapper) {
            if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(direction)) {
                return updateRules(rules, localEp);
            }
            return updateRules(rules, peerEp);
        }
        if (this instanceof DestinationMapper) {
            if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(direction)) {
                return updateRules(rules, peerEp);
            }
            return updateRules(rules, localEp);
        }
        return rules;
    }

    private List<GbpAceBuilder> updateRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation addrEp) {
        for (GbpAceBuilder rule : rules) {
            if (isInRange(rule.getSourcePortRangeBuilder(), DHCP_67)
                    && isInRange(rule.getDestinationPortRangeBuilder(), DHCP_68)) {
                continue;
            }
            if (isInRange(rule.getSourcePortRangeBuilder(), DHCP_68)
                    && isInRange(rule.getDestinationPortRangeBuilder(), DHCP_67)) {
                continue;
            }
            if (isInRange(rule.getSourcePortRangeBuilder(), DHCPV6_547)
                    && isInRange(rule.getDestinationPortRangeBuilder(), DHCPV6_548)) {
                continue;
            }
            if (isInRange(rule.getSourcePortRangeBuilder(), DHCPV6_548)
                    && isInRange(rule.getDestinationPortRangeBuilder(), DHCPV6_547)) {
                continue;
            }
            updateRule(addrEp, rule);
        }
        return rules;
    }

    private boolean isInRange(SourcePortRangeBuilder spr, PortNumber portNumber) {
        return spr != null && isInRange(spr.getLowerPort(), spr.getUpperPort(), portNumber);
    }

    private boolean isInRange(DestinationPortRangeBuilder dpr, PortNumber portNumber) {
        return dpr != null && isInRange(dpr.getLowerPort(),dpr.getUpperPort(), portNumber);
    }

    private boolean isInRange(PortNumber lower, PortNumber upper, PortNumber ref) {
        if (lower != null && upper != null) {
            return (lower.getValue() <= ref.getValue()) && (ref.getValue() <= upper.getValue());
        }
        return (lower != null && lower.getValue().equals(ref.getValue()))
                || (upper != null && upper.getValue().equals(ref.getValue()));
    }
}
