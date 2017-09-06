/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;


abstract class AddressMapper {

    private static final Logger LOG = LoggerFactory.getLogger(AddressMapper.class);

    private AccessListUtil.ACE_DIRECTION direction;

    private static final PortNumber DHCP_67 = new PortNumber(67);
    private static final PortNumber DHCP_68 = new PortNumber(68);
    private static final PortNumber DHCPV6_547 = new PortNumber(547);
    private static final PortNumber DHCPV6_548 = new PortNumber(548);
    private static final ImmutableMap<PortNumber, PortNumber> dhcpSockets =
            ImmutableMap.of(DHCP_67, DHCP_68, DHCP_68, DHCP_67, DHCPV6_548, DHCPV6_547, DHCPV6_547, DHCPV6_548);

    AddressMapper(AccessListUtil.ACE_DIRECTION direction) {
        this.direction = direction;
    }

    abstract void updateRule(AddressEndpointWithLocation addrEp, GbpAceBuilder aclRuleBuilder);

    //TODO implement for peers with no location
    abstract List<GbpAceBuilder> updateExtRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            ContainmentEndpoint cEp);

    public boolean updateRules(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            AddressEndpointWithLocation peerEp) {
        filterRulesWithIrrelevantAddresses(rules, localEp, peerEp);
        for (GbpAceBuilder rule : rules) {
            LOG.info("Rule: {} Updating rule between {} and {}. Start.", rule, localEp.getAddress(),
                    peerEp.getAddress());
            boolean isDhcpRule = dhcpSockets.entrySet().stream().anyMatch(dhcpSocket -> {
                boolean srcMatch = isInRange(rule.getSourcePortRangeBuilder(), dhcpSocket.getKey());
                boolean dstMatch = isInRange(rule.getDestinationPortRangeBuilder(), dhcpSocket.getValue());
                return srcMatch && dstMatch;
            });
            if (isDhcpRule) {
                if (!inSameSubnet(localEp, peerEp)) {
                    // do not process rules for DHCPs of other networks
                    LOG.info("Rule: {} Not updating rules between {} and {}. Returning false.", rule,
                            localEp.getAddress(), peerEp.getAddress());
                    return false;
                }
                // do not update addresses for DHCP traffic
                LOG.info("Rule: {} Not updating rule between {} and {}. Continue with next.", rule,
                        localEp.getAddress(), peerEp.getAddress());
                continue;
            }
            if (this instanceof SourceMapper) {
                LOG.info("Rule: {} Updating rule between {} and {}. SourceMapper.", rule, localEp.getAddress(),
                        peerEp.getAddress());
                if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(direction)) {
                    updateRule(localEp, rule);
                    continue;
                }
                updateRule(peerEp, rule);
            } else if (this instanceof DestinationMapper) {
                LOG.info("Rule: {} Updating rule between {} and {}. SourceMapper.", rule, localEp.getAddress(),
                        peerEp.getAddress());
                if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(direction)) {
                    updateRule(peerEp, rule);
                    continue;
                }
                updateRule(localEp, rule);
            }
        }
        LOG.info("Updating rules between {} and {}. Done, returning true.",
                localEp.getAddress(), peerEp.getAddress());
        return true;
    }

    private void filterRulesWithIrrelevantAddresses(List<GbpAceBuilder> rules, AddressEndpointWithLocation localEp,
            AddressEndpointWithLocation peerEp) {
        try {
            if (!IpPrefixType.class.equals(localEp.getAddressType())
                    || !IpPrefixType.class.equals(peerEp.getAddressType())) {
                return;
            }
            boolean addressV4Check = AccessListUtil.isIpv4Address(localEp.getAddress())
                    && AccessListUtil.isIpv4Address(peerEp.getAddress());
            boolean addressV6Check = AccessListUtil.isIpv6Address(localEp.getAddress())
                    && AccessListUtil.isIpv6Address(peerEp.getAddress());
            Predicate<GbpAceBuilder> p;
            if (addressV4Check) {
                p = rule -> rule.getEtherType().get() instanceof AceIpv6;
            } else if (addressV6Check) {
                p = rule -> rule.getEtherType().get() instanceof AceIpv4;
            } else {
                return;
            }
            List<GbpAceBuilder> rulesToRemove = rules.stream()
                .filter(rule -> rule.getEtherType().isPresent())
                .filter(p)
                .collect(Collectors.toList());
            rulesToRemove.forEach(rule -> { LOG.info("Filtering rules by ethertype {}", rule); rules.remove(rule);});
        } catch (UnknownHostException e) {
            LOG.error("Failed to parse addresses {}", e);
        }
    }

    private static boolean inSameSubnet(AddressEndpointWithLocation localEp, AddressEndpointWithLocation peerEp) {
        List<Predicate<AddressEndpointWithLocation>> list = new ArrayList<>();
        list.add(x -> x != null);
        list.add(x -> x.getContextType().equals(L3Context.class));
        list.add(x -> x.getChildEndpoint() != null && !x.getChildEndpoint().isEmpty());
        list.add(x -> x.getAbsoluteLocation() != null);
        list.add(x -> x.getChildEndpoint().get(0).getContextType().equals(L2BridgeDomain.class));
        list.add(x -> x.getChildEndpoint().get(0).getAddressType().equals(MacAddressType.class));
        if (list.stream().filter(i -> !i.apply(peerEp)).findFirst().isPresent()) {
            return false;
        }
        return localEp.getChildEndpoint().get(0).getContextId().equals(peerEp.getChildEndpoint().get(0).getContextId());
    }

    private static boolean isInRange(SourcePortRangeBuilder spr, PortNumber portNumber) {
        return spr != null && isInRange(spr.getLowerPort(), spr.getUpperPort(), portNumber);
    }

    private static boolean isInRange(DestinationPortRangeBuilder dpr, PortNumber portNumber) {
        return dpr != null && isInRange(dpr.getLowerPort(),dpr.getUpperPort(), portNumber);
    }

    private static boolean isInRange(PortNumber lower, PortNumber upper, PortNumber ref) {
        if (lower != null && upper != null) {
            return (lower.getValue() <= ref.getValue()) && (ref.getValue() <= upper.getValue());
        }
        return (lower != null && lower.getValue().equals(ref.getValue()))
                || (upper != null && upper.getValue().equals(ref.getValue()));
    }
}
