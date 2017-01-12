/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.Parent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.ParentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;

import com.google.common.collect.ImmutableList;

public class TestResources {

    private static final String EP1_IP = "10.0.0.10/32";
    private static final String EP2_EP = "10.0.0.20/32";
    private static final String EP1_MAC = "aa:bb:cc:dd:ee:ff";
    private static final String EP2_MAC = "ff:ee:dd:cc:bb:aa";
    private static final ContextId L2_FD_ID = new ContextId("l2FdId");
    private static final ContextId L2_BD_ID = new ContextId("l2BridgeDomainId");
    private static final ContextId L3_CTX_ID = new ContextId("l3CtxId");
    private static final SubjectName SUBJECT_NAME = new SubjectName("subject");
    private static final ContractId CONTRACT_ID = new ContractId("contract");
    private static final RuleName RULE_NAME_IN = new RuleName("rule_in");
    private static final RuleName RULE_NAME_OUT = new RuleName("rule_out");
    private static final ClassifierName CLASSIF_NAME = new ClassifierName("cl-name");

    private static final NetworkDomainId SUBNET_ID = new NetworkDomainId("subnet");
    private static final IpPrefix SUBNET_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.0/24"));
    private static final IpAddress VIRTUAL_ROUTER_IP = new IpAddress(new Ipv4Address("10.0.0.1"));

    protected static final  TenantId TENANT_ID = new TenantId("tenant1");

    public final AddressEndpointWithLocation l2AddrEp1 = l2AddressEndpointWithLocation(EP1_MAC, L2_BD_ID,
            EP1_IP, L3_CTX_ID);
    public final AddressEndpointWithLocation l3AddrEp1 = l3AddressEndpointWithLocation(EP1_MAC, L2_BD_ID,
            EP1_IP, L3_CTX_ID);
    public final AddressEndpointWithLocation l2AddrEp2 = l2AddressEndpointWithLocation(EP2_MAC, L2_BD_ID,
            EP2_EP, L3_CTX_ID);
    public final AddressEndpointWithLocation l3AddrEp2 = l3AddressEndpointWithLocation(EP2_MAC, L2_BD_ID,
            EP2_EP, L3_CTX_ID);

    PolicyContext createPolicyContext() {
        List<AddressEndpointWithLocation> addrEps = new ArrayList<>();
        addrEps.add(l2AddrEp1);
        addrEps.add(l3AddrEp1);
        addrEps.add(l2AddrEp2);
        addrEps.add(l3AddrEp2);
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.setEndpoints(new EndpointsBuilder().setAddressEndpointWithLocation(addrEps).build()).build();
        config.setRendererEndpoints(new RendererEndpointsBuilder().setRendererEndpoint(createRendEps()).build());
        config.setRuleGroups(createRuleGroups());
        config.setRendererForwarding(new RendererForwardingBuilder().setRendererForwardingByTenant(
                ImmutableList.<RendererForwardingByTenant>of(createForwarding())).build());
        return new PolicyContext(new RendererPolicyBuilder().setConfiguration(config.build()).build());
    }

    private List<RendererEndpoint> createRendEps() {
        List<RendererEndpoint> rEps = new ArrayList<>();
        PeerEndpointBuilder pEp1 = peerEndpoint(l2AddrEp1).setRuleGroupWithRendererEndpointParticipation(
                ImmutableList.<RuleGroupWithRendererEndpointParticipation>of(createRuleGroup(CONTRACT_ID, SUBJECT_NAME,
                        TENANT_ID, EndpointPolicyParticipation.PROVIDER)));
        PeerEndpointBuilder pEp2 = peerEndpoint(l2AddrEp2).setRuleGroupWithRendererEndpointParticipation(
                ImmutableList.<RuleGroupWithRendererEndpointParticipation>of(createRuleGroup(CONTRACT_ID, SUBJECT_NAME,
                        TENANT_ID, EndpointPolicyParticipation.CONSUMER)));
        RendererEndpointBuilder l2RendEp1 = rendererEndpoint(l2AddrEp1);
        l2RendEp1.setPeerEndpoint(ImmutableList.<PeerEndpoint>of(pEp2.build()));
        RendererEndpointBuilder l2RendEp2 = rendererEndpoint(l2AddrEp2);
        l2RendEp2.setPeerEndpoint(ImmutableList.<PeerEndpoint>of(pEp1.build()));
        rEps.add(l2RendEp1.build());
        rEps.add(l2RendEp2.build());
        return rEps;
    }

    private RuleGroupWithRendererEndpointParticipation createRuleGroup(ContractId ctrctId, SubjectName sn,
            TenantId tnntId, EndpointPolicyParticipation participation) {
        return new RuleGroupWithRendererEndpointParticipationBuilder().setRendererEndpointParticipation(participation)
            .setContractId(ctrctId)
            .setSubjectName(sn)
            .setTenantId(tnntId)
            .build();
    }

    public RuleGroups createRuleGroups() {
        ParameterValue param = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE)
            .setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .build();
        Classifier classif_in = new ClassifierBuilder().setClassifierDefinitionId(EtherTypeClassifierDefinition.ID)
            .setDirection(Direction.In)
            .setName(CLASSIF_NAME)
            .setParameterValue(ImmutableList.<ParameterValue>of(param))
            .build();
        Classifier classif_out = new ClassifierBuilder().setClassifierDefinitionId(EtherTypeClassifierDefinition.ID)
            .setDirection(Direction.Out)
            .setName(CLASSIF_NAME)
            .setParameterValue(ImmutableList.<ParameterValue>of(param))
            .build();
        Action action = new ActionBuilder().setActionDefinitionId(AllowActionDefinition.ID)
            .setName(new ActionName("Allow"))
            .build();
        ResolvedRule ruleIn = resolveRule(RULE_NAME_IN, ImmutableList.<Classifier>of(classif_in),
                ImmutableList.<Action>of(action), 0);
        ResolvedRule ruleOut = resolveRule(RULE_NAME_OUT, ImmutableList.<Classifier>of(classif_out),
                ImmutableList.<Action>of(action), 1);
        RuleGroup ruleGroup = new RuleGroupBuilder().setTenantId(TENANT_ID)
            .setOrder(0)
            .setSubjectName(SUBJECT_NAME)
            .setContractId(CONTRACT_ID)
            .setResolvedRule(ImmutableList.<ResolvedRule>of(ruleIn, ruleOut))
            .build();
        return new RuleGroupsBuilder().setRuleGroup(ImmutableList.<RuleGroup>of(ruleGroup)).build();
    }

    private ResolvedRule resolveRule(RuleName ruleName, List<Classifier> classifs, List<Action> actions, Integer order) {
        ResolvedRuleBuilder rrBuilder = new ResolvedRuleBuilder();
        rrBuilder.setClassifier(classifs);
        rrBuilder.setAction(actions);
        rrBuilder.setName(ruleName);
        rrBuilder.setOrder(order);
        return rrBuilder.build();
    }

    private RendererForwardingByTenant createForwarding() {
        RendererForwardingContext l2Fd = createRendererForwardingCtx(L2_FD_ID, new Name("l2fd"), L2FloodDomain.class).setParent(
                createParent(L2_BD_ID, L2BridgeDomain.class))
            .build();
        RendererForwardingContext l2Bd = createRendererForwardingCtx(L3_CTX_ID, new Name("l3ctx"), L3Context.class).setParent(
                createParent(L3_CTX_ID, L3Context.class))
            .build();
        RendererForwardingContext l3Ctx = createRendererForwardingCtx(L2_BD_ID, new Name("l2bd"), L2BridgeDomain.class).build();
        RendererNetworkDomain subnet = new RendererNetworkDomainBuilder().setNetworkDomainId(SUBNET_ID)
            .setName(new Name("subnet"))
            .setNetworkDomainType(Subnet.class)
            .setParent(createParent(L2_FD_ID, L2FloodDomain.class))
            .addAugmentation(
                    SubnetAugmentRenderer.class,
                    new SubnetAugmentRendererBuilder().setSubnet(
                            new SubnetBuilder().setIpPrefix(SUBNET_PREFIX).setVirtualRouterIp(VIRTUAL_ROUTER_IP).build())
                        .build())
            .build();
        return new RendererForwardingByTenantBuilder().setTenantId(TENANT_ID)
            .setRendererForwardingContext(ImmutableList.<RendererForwardingContext>of(l2Fd, l2Bd, l3Ctx))
            .setRendererNetworkDomain(ImmutableList.<RendererNetworkDomain>of(subnet))
            .build();
    }

    RendererForwardingContextBuilder createRendererForwardingCtx(ContextId id, Name name,
            Class<? extends ContextType> type) {
        return new RendererForwardingContextBuilder().setName(name).setContextId(id).setContextType(type);
    }

    Parent createParent(ContextId ctxId, Class<? extends ContextType> type) {
        return new ParentBuilder().setContextId(ctxId).setContextType(type).build();
    }

    RendererEndpointBuilder rendererEndpoint(AddressEndpointWithLocation addrEp) {
        return new RendererEndpointBuilder().setAddress(addrEp.getAddress())
            .setAddressType(addrEp.getAddressType())
            .setContextId(addrEp.getContextId())
            .setContextType(addrEp.getContextType());

    }

    PeerEndpointBuilder peerEndpoint(AddressEndpointWithLocation addrEp) {
        return new PeerEndpointBuilder().setAddress(addrEp.getAddress())
            .setAddressType(addrEp.getAddressType())
            .setContextId(addrEp.getContextId())
            .setContextType(addrEp.getContextType());
    }

    AddressEndpointWithLocation l3AddressEndpointWithLocation(String macAddress, ContextId macAddrContextId,
            String ipAddress, ContextId ipAddressContextId) {
        ChildEndpoint childEndpoint = new ChildEndpointBuilder().setAddress(macAddress)
            .setAddressType(MacAddressType.class)
            .setContextType(L2BridgeDomain.class)
            .setContextId(macAddrContextId)
            .build();
        return new AddressEndpointWithLocationBuilder().setAddress(ipAddress)
            .setAddressType(IpPrefixType.class)
            .setContextId(ipAddressContextId)
            .setContextType(L3Context.class)
            .setChildEndpoint(ImmutableList.<ChildEndpoint>of(childEndpoint))
            .build();
    }

    AddressEndpointWithLocation l2AddressEndpointWithLocation(String macAddress, ContextId macAddrContextId,
            String ipAddress, ContextId ipAddressContextId) {
        ParentEndpoint parentEndpoint = new ParentEndpointBuilder().setAddress(ipAddress)
            .setAddressType(IpPrefixType.class)
            .setContextType(L3Context.class)
            .setContextId(ipAddressContextId)
            .build();
        return new AddressEndpointWithLocationBuilder().setAddress(macAddress)
            .setAddressType(MacAddressType.class)
            .setContextId(macAddrContextId)
            .setContextType(L2BridgeDomain.class)
            .setParentEndpointChoice(
                    new ParentEndpointCaseBuilder().setParentEndpoint(ImmutableList.<ParentEndpoint>of(parentEndpoint))
                        .build())
            .build();
    }
}
