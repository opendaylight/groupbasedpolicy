/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.RendererResolvedPolicy;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

 /*
  * Transforms Renderer policy into access-list configuration for Honeycomb.
  *
  * */

public class AccessListUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AccessListUtil.class);
    static final String UNDERSCORE = "_";
    private static final String PERMIT_EXTERNAL_INGRESS = "permit_external_ingress";
    private static final String PERMIT_EXTERNAL_EGRESS = "permit_external_egress";
    private static final String DENY_INGRESS_IPV4 = "deny_ingress_ipv4";
    private static final String DENY_INGRESS_IPV6 = "deny_ingress_ipv6";
    private static final String DENY_EGRESS_IPV4 = "deny_egress_ipv4";
    private static final String DENY_EGRESS_IPV6 = "deny_egress_ipv6";

    public enum ACE_DIRECTION {
        INGRESS, EGRESS
    }

    // hiding default public constructor
    private AccessListUtil() {}

    static void configureLocalRules(PolicyContext ctx, RendererEndpointKey rEpKey, ACE_DIRECTION policyDirection,
            AccessListWrapper aclWrapper) {
        ctx.getPolicyTable()
            .row(rEpKey)
            .keySet()
            .stream()
            .filter(peerEpKey -> peerHasLocation(ctx, peerEpKey))
            .forEach(peerEpKey -> {
                ctx.getPolicyTable().get(rEpKey, peerEpKey).forEach(resolvedRules -> {
                    List<GbpAceBuilder> rules = new ArrayList<>();
                    Direction classifDir = calculateClassifDirection(resolvedRules.getRendererEndpointParticipation(),
                            policyDirection);
                    rules.addAll(resolveAclRulesFromPolicy(resolvedRules, classifDir, rEpKey, peerEpKey));
                    updateAddressesInRules(rules, rEpKey, peerEpKey, ctx, policyDirection, true);
                    aclWrapper.writeRules(rules);

                });
            });
    }

    /**
     * Resolves direction for classifiers that will be applied to INBOUND or OUTBOUND direction.
     * </p>
     * Rule is applied in INGRESS direction when participation is PROVIDER and classifier direction is OUT
     * </p>
     * Rule is applied in INGRESS direction when participation is CONSUMER and classifier direction is IN
     * </p>
     * INBOUND direction is applied otherwise.
     * </p>
     * Based on this
     * </p>
     * OUT classifier direction is resolved for INGRESS traffic when participation is PROVIDER
     * </p>
     * OUT classifier direction is resolved for EGRESS traffic when participation is CONSUMER
     * </p>
     * IN is resolved otherwise.
     * </p>
     * @param participation provider or consumer
     * @param direction EGRESS or INGRESS
     * @return Direction that classifiers should match for given policy direction.
     */
     static Direction calculateClassifDirection(EndpointPolicyParticipation participation, ACE_DIRECTION direction) {
        if (EndpointPolicyParticipation.PROVIDER.equals(participation) && ACE_DIRECTION.INGRESS.equals(direction)) {
            return Direction.Out;
        }
        if (EndpointPolicyParticipation.CONSUMER.equals(participation) && ACE_DIRECTION.EGRESS.equals(direction)) {
            return Direction.Out;
        }
        return Direction.In;
    }

    static void updateAddressesInRules(List<GbpAceBuilder> rules, RendererEndpointKey rEpKey,
            PeerEndpointKey peerEpKey, PolicyContext ctx, ACE_DIRECTION policyDirection,
            boolean resolveForLocationPeers) {
        for (AddressMapper addrMapper : Arrays.asList(new SourceMapper(policyDirection),
                new DestinationMapper(policyDirection))) {
            if (peerHasLocation(ctx, peerEpKey) && resolveForLocationPeers) {
                addrMapper.updateRules(rules, findAddrEp(ctx, rEpKey), findAddrEp(ctx, peerEpKey));
            } else if (!peerHasLocation(ctx, peerEpKey) && !resolveForLocationPeers) {
                addrMapper.updateExtRules(rules, findAddrEp(ctx, rEpKey), null);
            }
        }
    }

    private static boolean peerHasLocation(PolicyContext ctx, PeerEndpointKey peerEpKey) {
        return ctx.getAddrEpByKey().get(
                AddressEndpointUtils.fromPeerEpKey(peerEpKey)) != null;
    }

    static AddressEndpointWithLocation findAddrEp(PolicyContext ctx, RendererEndpointKey rEpKey) {
        return ctx.getAddrEpByKey().get(
                AddressEndpointUtils.fromRendererEpKey(rEpKey));
    }

    private static AddressEndpointWithLocation findAddrEp(PolicyContext ctx, PeerEndpointKey rEpKey) {
        return ctx.getAddrEpByKey().get(
                AddressEndpointUtils.fromPeerEpKey(rEpKey));
    }

    /** Transform a resolved rule to ACE with corresponding classification and action fields
     *
     * @param resolvedPolicy resolved rules, with the same participation - provider or consumer
     * @param direction rules matching corresponding direction will be collected
     * @return resolved ACE entries
     */

     static @Nonnull String resolveAceName(@Nonnull RuleName ruleName, @Nonnull RendererEndpointKey key,
            @Nonnull PeerEndpointKey peer) {
        return ruleName.getValue() + "_" + key.getAddress() + "_" + peer.getAddress();
    }

    private static List<GbpAceBuilder> resolveAclRulesFromPolicy(RendererResolvedPolicy resolvedPolicy,
            Direction direction, RendererEndpointKey r, PeerEndpointKey p) {
        List<GbpAceBuilder> aclRules = new ArrayList<>();
        for (ResolvedRule resolvedRule : resolvedPolicy.getRuleGroup().getRules()) {
            Optional<GbpAceBuilder> resolveAce = resolveAceClassifersAndAction(resolvedRule, direction, resolveAceName(resolvedRule.getName(), r, p));
            if(resolveAce.isPresent()) {
                aclRules.add(resolveAce.get());
            }
        }
        return aclRules;
    }

    public static Optional<GbpAceBuilder> resolveAceClassifersAndAction(ResolvedRule resolvedRule, Direction direction, String ruleName) {
        Map<String, ParameterValue> params = resolveClassifParamsForDir(direction, resolvedRule.getClassifier());
        if (params.isEmpty()) {
            return Optional.absent();
        }
        org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier classif =
                resolveImplementedClassifForDir(direction, resolvedRule.getClassifier());
        GbpAceBuilder aclRuleBuilder = new GbpAceBuilder(ruleName);
                //new GbpAceBuilder(resolvedRule.getName().getValue() + UNDERSCORE + namePasphrase);
        boolean updated = classif != null && classif.updateMatch(aclRuleBuilder, params);
        Optional<Actions> optAction = resolveActions(resolvedRule.getAction());
        if (!optAction.isPresent() || !updated) {
            LOG.error("Failed to process rule {}. Resolved parameters {}, resolved classifier. Actions resolved: {}"
                    + "{}.", resolvedRule.getName().getValue(), params, classif, optAction.isPresent());
            return Optional.absent();
        }
        aclRuleBuilder.setAction(optAction.get());
        return Optional.of(aclRuleBuilder);
    }

    private static org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier resolveImplementedClassifForDir(
            @Nonnull Direction direction, @Nonnull List<Classifier> classifiers) {
        org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier feasibleClassifier = null;
        for (Classifier cl : classifiers) {
            if (direction.equals(cl.getDirection()) || direction.equals(Direction.Bidirectional)) {
                org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier classif =
                        SubjectFeatures.getClassifier(cl.getClassifierDefinitionId());
                if (feasibleClassifier == null) {
                    feasibleClassifier = classif;
                }
                if (classif.getParent() != null && classif.getParent().equals(feasibleClassifier)) {
                    feasibleClassifier = classif;
                }
            }
        }
        return feasibleClassifier;
    }

    private static Map<String, ParameterValue> resolveClassifParamsForDir(Direction direction,
            List<Classifier> classifier) {
        Map<String, ParameterValue> params = new HashMap<>();
        classifier.stream()
            .filter(classif -> direction.equals(classif.getDirection()) || direction.equals(Direction.Bidirectional))
            .forEach(classif -> {
                classif.getParameterValue()
                    .stream()
                    .filter(v -> params.get(v.getName().getValue()) == null) // not unique
                    .filter(v -> v.getIntValue() != null || v.getStringValue() != null || v.getRangeValue() != null)
                    .forEach(v -> params.put(v.getName().getValue(), v));
            });
        return params;
    }

    private static Optional<Actions> resolveActions(List<Action> actions) {
        for (Action action : actions) {
            if (AllowActionDefinition.ID
                .equals(action.getActionDefinitionId())) {
                return Optional
                    .of(new ActionsBuilder().setPacketHandling(new PermitBuilder().setPermit(true).build()).build());
            }
        }
        return Optional.absent();
    }

    /*
     * so far any traffic heading to/from outside of managed domain is permitted for demonstration
     * purposes
     * TODO initial workaround for external networking
     */
   static Optional<GbpAceBuilder> allowExternalNetworksForEp(@Nonnull AddressEndpointWithLocation addrEp,
            AccessListUtil.ACE_DIRECTION dir) {
        List<ParentEndpoint> parentEndpoints = EndpointUtils.getParentEndpoints(addrEp.getParentEndpointChoice());
        if (parentEndpoints.isEmpty()) {
            return Optional.absent();
        }
        for (ParentEndpoint parentEp : parentEndpoints) {
            InetAddress byName;
            try {
                byName = InetAddress.getByName(substringBeforeSlash(parentEp.getAddress()));
            } catch (UnknownHostException e) {
                LOG.error("Failed to parse IP address {}", e);
                return Optional.absent();
            }
            if (byName instanceof Inet4Address) {
                if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(dir)) {
                    return Optional.of(new GbpAceBuilder(PERMIT_EXTERNAL_INGRESS).setIpAddresses(
                            new Ipv4Prefix(parentEp.getAddress()), null).setPermit());
                } else {
                    return Optional.of(new GbpAceBuilder(PERMIT_EXTERNAL_EGRESS).setIpAddresses(null,
                            new Ipv4Prefix(parentEp.getAddress())).setPermit());
                }
            } else if (byName instanceof Inet6Address) {
                if (AccessListUtil.ACE_DIRECTION.INGRESS.equals(dir)) {
                    return Optional.of(new GbpAceBuilder(PERMIT_EXTERNAL_INGRESS).setIpAddresses(
                            new Ipv6Prefix(parentEp.getAddress()), null).setPermit());
                } else {
                    return Optional.of(new GbpAceBuilder(PERMIT_EXTERNAL_EGRESS).setIpAddresses(null,
                            new Ipv6Prefix(parentEp.getAddress())).setPermit());
                }
            }
        }
        return Optional.absent();
    }

    /**
     * Helps stripping address part of a CIDR
     */
    private static String substringBeforeSlash(String address) {
        return (address.contains("/") && address.split("/").length > 0) ? address.split("/")[0] : address;
    }

    static List<GbpAceBuilder> denyDomainSubnets(@Nonnull PolicyContext ctx, @Nonnull ACE_DIRECTION policyDirection) {
        List<GbpAceBuilder> aclRuleBuilders = new ArrayList<>();
        for (RendererForwardingByTenant rf : ctx.getPolicy()
            .getConfiguration()
            .getRendererForwarding()
            .getRendererForwardingByTenant()) {
            rf.getRendererNetworkDomain()
                .stream()
                .filter(rnd -> org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.Subnet.class
                    .equals(rnd.getNetworkDomainType()))
                .forEach(rnd -> {
                    SubnetAugmentRenderer subnetAug = rnd.getAugmentation(SubnetAugmentRenderer.class);
                    // subnetAug should not be null
                    subnetAug.getSubnet();
                    if (policyDirection.equals(ACE_DIRECTION.INGRESS) && subnetAug.getSubnet().isIsTenant()) {
                        aclRuleBuilders.add(denyIngressTrafficForPrefix(subnetAug.getSubnet()));
                    }
                    else if (subnetAug.getSubnet().isIsTenant()) {
                        aclRuleBuilders.add(denyEgressTrafficForPrefix(subnetAug.getSubnet()));
                    }
                });
        }
        return aclRuleBuilders;
    }

    private static GbpAceBuilder denyEgressTrafficForPrefix(Subnet subnet) {
        IpPrefix ipPrefix = subnet.getIpPrefix();
        if (ipPrefix.getIpv4Prefix() != null) {
            return new GbpAceBuilder(DENY_EGRESS_IPV4 + UNDERSCORE + String.valueOf(ipPrefix.getValue()))
                .setIpAddresses(ipPrefix.getIpv4Prefix(), null).setDeny();
        } else if (ipPrefix.getIpv6Prefix() != null) {
            return new GbpAceBuilder(DENY_EGRESS_IPV6 + UNDERSCORE + String.valueOf(ipPrefix.getValue()))
                .setIpAddresses(ipPrefix.getIpv6Prefix(), null).setDeny();
        }
        throw new IllegalStateException("Unknown prefix type " + subnet.getIpPrefix());
    }

    static void setSourceL3Address(GbpAceBuilder rule, String address) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(substringBeforeSlash(address));
        if (addr instanceof Inet6Address) {
            rule.setIpAddresses(new Ipv6Prefix(address), null);
        } else {
            rule.setIpAddresses(new Ipv4Prefix(address), null);
        }
    }

    static void setDestinationL3Address(GbpAceBuilder rule, String address) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(substringBeforeSlash(address));
        if (addr instanceof Inet6Address) {
            rule.setIpAddresses(null, new Ipv6Prefix(address));
        } else {
            rule.setIpAddresses(null, new Ipv4Prefix(address));
        }
    }

    static GbpAceBuilder denyIngressTrafficForPrefix(Subnet subnet) {
        IpPrefix ipPrefix = subnet.getIpPrefix();
        if (ipPrefix.getIpv4Prefix() != null) {
            return new GbpAceBuilder(DENY_INGRESS_IPV4 + UNDERSCORE + String.valueOf(ipPrefix.getValue()))
                .setIpAddresses(null, ipPrefix.getIpv4Prefix()).setDeny();
        } else if (ipPrefix.getIpv6Prefix() != null) {
            return new GbpAceBuilder(DENY_INGRESS_IPV6 + UNDERSCORE + String.valueOf(ipPrefix.getValue()))
                .setIpAddresses(null, ipPrefix.getIpv6Prefix()).setDeny();
        }
        throw new IllegalStateException("Unknown prefix type " + subnet.getIpPrefix());
    }
}
