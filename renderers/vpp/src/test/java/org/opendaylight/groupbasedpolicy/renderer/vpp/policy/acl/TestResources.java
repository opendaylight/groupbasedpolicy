/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.L4Classifier;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.SubnetBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import com.google.common.collect.ImmutableList;

public class TestResources extends CustomDataBrokerTest {

    protected enum SECURITY_GROUP {
        CLIENT, SERVER;
    }

    private static final String EP1_IP = "10.0.0.10/32";
    private static final String EP2_EP = "10.0.0.20/32";
    private static final String EP3_IP = "10.0.0.30/32";
    private static final String EP1_MAC = "aa:bb:cc:dd:ee:ff";
    private static final String EP2_MAC = "ff:ee:dd:cc:bb:aa";
    private static final String EP3_MAC = "11:11:22:22:33:33";
    protected static final NodeId NODE1 = new NodeId("node1");
    protected static final String NODE1_CONNECTOR_1 =
            "/ietf-interfaces:interfaces/ietf-interfaces:interface[ietf-interfaces:name='nodeConnector1']";
    protected static final String NODE1_CONNECTOR_2 =
            "/ietf-interfaces:interfaces/ietf-interfaces:interface[ietf-interfaces:name='nodeConnector2']";
    protected static final String NODE1_CONNECTOR_3 =
            "/ietf-interfaces:interfaces/ietf-interfaces:interface[ietf-interfaces:name='nodeConnector3']";
    private static final ContextId L2_FD_ID = new ContextId("l2FdId");
    protected static final ContextId L2_BD_ID = new ContextId("l2BridgeDomainId");
    protected static final ContextId L3_CTX_ID = new ContextId("l3CtxId");
    protected static final SubjectName SUBJECT_NAME = new SubjectName("subject");
    protected static final SubjectName SUBJECT_NAME2 = new SubjectName("subject2");
    protected static final ContractId CONTRACT_ID = new ContractId("contract");

    private static final NetworkDomainId SUBNET_ID = new NetworkDomainId("subnet");
    private static final IpPrefix SUBNET_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.0/24"));
    private static final IpAddress VIRTUAL_ROUTER_IP = new IpAddress(new Ipv4Address("10.0.0.1"));

    protected static final TenantId TENANT_ID = new TenantId("tenant1");

    public final AddressEndpointWithLocation l2AddrEp1 =
            l2AddressEndpointWithLocation(EP1_MAC, L2_BD_ID, EP1_IP, L3_CTX_ID);
    public final AddressEndpointWithLocation l3AddrEp1 = appendLocationToEndpoint(
            l3AddressEndpointWithLocation(EP1_MAC, L2_BD_ID, EP1_IP, L3_CTX_ID), NODE1, NODE1_CONNECTOR_1);
    public final AddressEndpointWithLocation l2AddrEp2 =
            l2AddressEndpointWithLocation(EP2_MAC, L2_BD_ID, EP2_EP, L3_CTX_ID);
    public final AddressEndpointWithLocation l3AddrEp2 = appendLocationToEndpoint(
            l3AddressEndpointWithLocation(EP2_MAC, L2_BD_ID, EP2_EP, L3_CTX_ID), NODE1, NODE1_CONNECTOR_2);

    public final AddressEndpointWithLocation l2AddrEp3 =
            l2AddressEndpointWithLocation(EP3_MAC, L2_BD_ID, EP3_IP, L3_CTX_ID);
    public final AddressEndpointWithLocation l3AddrEp3 = appendLocationToEndpoint(
            l3AddressEndpointWithLocation(EP3_MAC, L2_BD_ID, EP3_IP, L3_CTX_ID), NODE1, NODE1_CONNECTOR_3);

    protected AddressEndpointWithLocation appendLocationToEndpoint(AddressEndpointWithLocationBuilder builder,
            NodeId nodeId, String nodeConnector) {
        ExternalLocationCase extLocation = new ExternalLocationCaseBuilder().setExternalNodeConnector(nodeConnector)
            .setExternalNodeMountPoint(VppIidFactory.getNetconfNodeIid(nodeId))
            .build();
        return builder.setAbsoluteLocation(new AbsoluteLocationBuilder().setLocationType(extLocation).build()).build();
    }

    protected AddressEndpointWithLocation l2AddressEndpointWithLocation(String macAddress, ContextId macAddrContextId,
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
            .setParentEndpointChoice(new ParentEndpointCaseBuilder()
                .setParentEndpoint(ImmutableList.<ParentEndpoint>of(parentEndpoint)).build())
            .build();
    }

    protected AddressEndpointWithLocationBuilder l3AddressEndpointWithLocation(String macAddress,
            ContextId macAddrContextId, String ipAddress, ContextId ipAddressContextId) {
        ChildEndpoint childEndpoint = new ChildEndpointBuilder().setAddress(macAddress)
            .setAddressType(MacAddressType.class)
            .setContextType(L2BridgeDomain.class)
            .setContextId(macAddrContextId)
            .build();
        return new AddressEndpointWithLocationBuilder().setAddress(ipAddress)
            .setAddressType(IpPrefixType.class)
            .setContextId(ipAddressContextId)
            .setContextType(L3Context.class)
            .setChildEndpoint(ImmutableList.<ChildEndpoint>of(childEndpoint));
    }

    PolicyContext createPolicyContext(List<AddressEndpointWithLocation> addrEps,
            List<RendererEndpoint> rendererEndpoints, RuleGroups ruleGroups, RendererForwardingByTenant fwdng) {
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.setEndpoints(new EndpointsBuilder().setAddressEndpointWithLocation(addrEps).build()).build();
        config.setRendererEndpoints(new RendererEndpointsBuilder().setRendererEndpoint(rendererEndpoints).build());
        config.setRuleGroups(ruleGroups);
        config.setRendererForwarding(new RendererForwardingBuilder()
            .setRendererForwardingByTenant(ImmutableList.<RendererForwardingByTenant>of(fwdng)).build());
        return new PolicyContext(new RendererPolicyBuilder().setConfiguration(config.build()).build());
    }

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(AccessLists.class, VppAce.class);
    }

    public List<AddressEndpointWithLocation> createAddressEndpoints() {
        List<AddressEndpointWithLocation> addrEps = new ArrayList<>();
        addrEps.add(l2AddrEp1);
        addrEps.add(l3AddrEp1);
        addrEps.add(l2AddrEp2);
        addrEps.add(l3AddrEp2);
        addrEps.add(l2AddrEp3);
        addrEps.add(l3AddrEp3);
        return addrEps;
    }

    public List<RendererEndpoint> createRendEps() {
        List<RendererEndpoint> rEps = new ArrayList<>();
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp1.getKey()), SECURITY_GROUP.SERVER,
                peerEndpointKey(l3AddrEp2.getKey()), peerEndpointKey(l3AddrEp3.getKey())));
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp2.getKey()), SECURITY_GROUP.CLIENT,
                peerEndpointKey(l3AddrEp1.getKey())));
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp3.getKey()), SECURITY_GROUP.CLIENT,
                peerEndpointKey(l3AddrEp1.getKey())));
        return rEps;
    }

    protected RendererEndpointKey rendererEndpointKey(AddressEndpointWithLocationKey addrEp) {
        return new RendererEndpointKey(addrEp.getAddress(), addrEp.getAddressType(), addrEp.getContextId(),
                addrEp.getContextType());
    }

    protected PeerEndpointKey peerEndpointKey(AddressEndpointWithLocationKey addrEp) {
        return new PeerEndpointKey(addrEp.getAddress(), addrEp.getAddressType(), addrEp.getContextId(),
                addrEp.getContextType());
    }

    public RendererEndpoint createRendEndpoint(RendererEndpointKey key, SECURITY_GROUP sg,
            PeerEndpointKey... peerKeys) {
        EndpointPolicyParticipation participation;
        if (sg.equals(SECURITY_GROUP.SERVER)) {
            participation = EndpointPolicyParticipation.PROVIDER;
        } else if (sg.equals(SECURITY_GROUP.CLIENT)) {
            participation = EndpointPolicyParticipation.CONSUMER;
        } else {
            throw new IllegalStateException("Specify proper endpoint group participation");
        }
        List<PeerEndpoint> peers = new ArrayList<>();
        for (PeerEndpointKey peer : peerKeys) {
            PeerEndpoint pEp1 = new PeerEndpointBuilder().setKey(peer)
                .setRuleGroupWithRendererEndpointParticipation(
                        ImmutableList.<RuleGroupWithRendererEndpointParticipation>of(
                                createRuleGroup(CONTRACT_ID, SUBJECT_NAME, TENANT_ID, participation),
                                createRuleGroup(CONTRACT_ID, SUBJECT_NAME2, TENANT_ID, participation)))
                .build();
            peers.add(pEp1);
        }
        return new RendererEndpointBuilder().setKey(key).setPeerEndpoint(peers).build();
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
        ParameterValue ipv4Param = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE)
            .setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .build();
        ParameterValue tcpParam = new ParameterValueBuilder().setIntValue(IpProtoClassifierDefinition.TCP_VALUE)
            .setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
            .build();
        ParameterValue udpParam = new ParameterValueBuilder().setIntValue(IpProtoClassifierDefinition.UDP_VALUE)
            .setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
            .build();
        RuleGroup rg1 = createSimpleRuleGroups(ImmutableList.of(ipv4Param, tcpParam), IpProtoClassifierDefinition.ID,
                SUBJECT_NAME);
        RuleGroup rg2 = createSimpleRuleGroups(ImmutableList.of(ipv4Param, udpParam), IpProtoClassifierDefinition.ID,
                SUBJECT_NAME2);
        return new RuleGroupsBuilder().setRuleGroup(ImmutableList.<RuleGroup>of(rg1, rg2)).build();
    }

    public RuleGroups createRuleGroups_DhcpTest() {
        ParameterValue ipv4Param = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE)
            .setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .build();
        ParameterValue tcpParam = new ParameterValueBuilder().setIntValue(IpProtoClassifierDefinition.TCP_VALUE)
            .setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
            .build();
        ParameterValue udpParam = new ParameterValueBuilder().setIntValue(IpProtoClassifierDefinition.UDP_VALUE)
            .setName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM))
            .build();
        RuleGroup rg1 = createSimpleRuleGroups(ImmutableList.of(ipv4Param, tcpParam), IpProtoClassifierDefinition.ID,
                SUBJECT_NAME);
        RuleGroup rg2 = createSimpleRuleGroups(ImmutableList.of(ipv4Param, udpParam), IpProtoClassifierDefinition.ID,
                SUBJECT_NAME2);
        return new RuleGroupsBuilder().setRuleGroup(ImmutableList.<RuleGroup>of(rg1, rg2)).build();
    }

    private RuleGroup createSimpleRuleGroups(List<ParameterValue> pv, ClassifierDefinitionId id,
            SubjectName subjectName) {
        final ClassifierName classifierName = new ClassifierName("cl-name");
        Classifier classifIn = new ClassifierBuilder().setClassifierDefinitionId(IpProtoClassifierDefinition.ID)
            .setDirection(Direction.In)
            .setName(classifierName)
            .setParameterValue(pv)
            .build();
        Classifier classifOut = new ClassifierBuilder().setClassifierDefinitionId(IpProtoClassifierDefinition.ID)
            .setDirection(Direction.Out)
            .setName(classifierName)
            .setParameterValue(pv)
            .build();
        Action action = new ActionBuilder().setActionDefinitionId(AllowActionDefinition.ID)
            .setName(new ActionName("Allow"))
            .build();
        final RuleName ruleNameIn = new RuleName("rule_in" + subjectName.getValue());
        final RuleName ruleNameOut = new RuleName("rule_out" + subjectName.getValue());
        ResolvedRule ruleIn =
                resolveRule(ruleNameIn, ImmutableList.<Classifier>of(classifIn), ImmutableList.<Action>of(action), 0);
        ResolvedRule ruleOut =
                resolveRule(ruleNameOut, ImmutableList.<Classifier>of(classifOut), ImmutableList.<Action>of(action), 1);
        return new RuleGroupBuilder().setOrder(0)
            .setTenantId(TENANT_ID)
            .setSubjectName(subjectName)
            .setContractId(CONTRACT_ID)
            .setResolvedRule(ImmutableList.<ResolvedRule>of(ruleIn, ruleOut))
            .build();
    }

    private ResolvedRule resolveRule(RuleName ruleName, List<Classifier> classifs, List<Action> actions,
            Integer order) {
        ResolvedRuleBuilder rrBuilder = new ResolvedRuleBuilder();
        rrBuilder.setClassifier(classifs);
        rrBuilder.setAction(actions);
        rrBuilder.setName(ruleName);
        rrBuilder.setOrder(order);
        return rrBuilder.build();
    }

    protected RendererForwardingByTenant createForwarding() {
        RendererForwardingContext l2Fd = createRendererForwardingCtx(L2_FD_ID, new Name("l2fd"), L2FloodDomain.class)
            .setParent(createParent(L2_BD_ID, L2BridgeDomain.class)).build();
        RendererForwardingContext l2Bd = createRendererForwardingCtx(L3_CTX_ID, new Name("l3ctx"), L3Context.class)
            .setParent(createParent(L3_CTX_ID, L3Context.class)).build();
        RendererForwardingContext l3Ctx =
                createRendererForwardingCtx(L2_BD_ID, new Name("l2bd"), L2BridgeDomain.class).build();
        RendererNetworkDomain subnet =
                new RendererNetworkDomainBuilder().setNetworkDomainId(SUBNET_ID)
                    .setName(new Name("subnet"))
                    .setNetworkDomainType(Subnet.class)
                    .setParent(createParent(L2_FD_ID, L2FloodDomain.class))
                    .addAugmentation(SubnetAugmentRenderer.class,
                            new SubnetAugmentRendererBuilder().setSubnet(new SubnetBuilder().setIsTenant(true)
                                .setIpPrefix(SUBNET_PREFIX)
                                .setVirtualRouterIp(VIRTUAL_ROUTER_IP)
                                .build()).build())
                    .build();
        return new RendererForwardingByTenantBuilder().setTenantId(TENANT_ID)
            .setRendererForwardingContext(ImmutableList.<RendererForwardingContext>of(l2Fd, l2Bd, l3Ctx))
            .setRendererNetworkDomain(ImmutableList.<RendererNetworkDomain>of(subnet))
            .build();
    }

    private RendererForwardingContextBuilder createRendererForwardingCtx(ContextId id, Name name,
            Class<? extends ContextType> type) {
        return new RendererForwardingContextBuilder().setName(name).setContextId(id).setContextType(type);
    }

    private Parent createParent(ContextId ctxId, Class<? extends ContextType> type) {
        return new ParentBuilder().setContextId(ctxId).setContextType(type).build();
    }
}
