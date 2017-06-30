/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.Parent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.ParentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class DtoFactory {

    static IpPrefix subnetPrefix = new IpPrefix(new Ipv4Prefix("10.0.0.0/24"));
    static IpAddress virtRouterIp = new IpAddress(new Ipv4Address("10.0.0.1"));

    public static final ContextId CTX_ID = new ContextId("ctx");
    public static final ContextId L3_CTX = new ContextId("l3ctx");
    public static final ContextId L2BD_CTX = new ContextId("l2bd");
    public static final ContextId L2FD_CTX = new ContextId("l2fd");
    public static final ContractId CONTRACT_ID = new ContractId("contract");
    public static final TenantId TENANT_ID = new TenantId("tenant");
    public static final SubjectName SUBJECT_NAME = new SubjectName("subject");
    public static final RuleName RULE_NAME = new RuleName("rule");

    public static final Name L2_FD_ID = new Name("l2fd");
    public static final Name L3_CTX_ID = new Name("l3ctx");
    public static final Name L2_BD_ID = new Name("l2bd");
    public static final Name SUBNET_ID = new Name("subnetId");

    public static final RuleGroupWithRendererEndpointParticipation RULE_GROUP_WITH_CONSUMER =
            new RuleGroupWithRendererEndpointParticipationBuilder().setContractId(CONTRACT_ID)
                .setTenantId(TENANT_ID)
                .setSubjectName(SUBJECT_NAME)
                .setRendererEndpointParticipation(EndpointPolicyParticipation.CONSUMER)
                .build();
    public static final RuleGroupWithRendererEndpointParticipation RULE_GROUP_WITH_PROVIDER =
            new RuleGroupWithRendererEndpointParticipationBuilder().setContractId(CONTRACT_ID)
                .setTenantId(TENANT_ID)
                .setSubjectName(SUBJECT_NAME)
                .setRendererEndpointParticipation(EndpointPolicyParticipation.PROVIDER)
                .build();
    public final static TopologyKey TOPO_KEY = new TopologyKey(new TopologyId("topology-netconf"));
    public final static InstanceIdentifier<Node> VPP_NODE_1_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, TOPO_KEY)
        .child(Node.class, new NodeKey(new NodeId("node1")))
        .build();
    public final static InstanceIdentifier<Node> VPP_NODE_2_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, TOPO_KEY)
        .child(Node.class, new NodeKey(new NodeId("node2")))
        .build();

    public static Configuration createConfiguration(List<AddressEndpointWithLocation> consumers,
            List<AddressEndpointWithLocation> providers) {
        List<AddressEndpointWithLocation> eps =
                Stream.concat(consumers.stream(), providers.stream()).collect(Collectors.toList());
        Endpoints endpoints = new EndpointsBuilder().setAddressEndpointWithLocation(eps).build();
        List<RendererEndpoint> consumersAsRendererEps = consumers.stream().map(cons -> {
            List<PeerEndpoint> peers = providers.stream()
                .map(web -> new PeerEndpointBuilder().setKey(KeyFactory.peerEndpointKey(web.getKey()))
                    .setRuleGroupWithRendererEndpointParticipation(Arrays.asList(RULE_GROUP_WITH_CONSUMER))
                    .build())
                .collect(Collectors.toList());
            return new RendererEndpointBuilder().setKey(KeyFactory.rendererEndpointKey(cons.getKey()))
                .setPeerEndpoint(peers)
                .build();
        }).collect(Collectors.toList());
        List<RendererEndpoint> providersAsRendererEps = providers.stream().map(prov -> {
            List<PeerEndpoint> peers = consumers.stream()
                .map(client -> new PeerEndpointBuilder().setKey(KeyFactory.peerEndpointKey(client.getKey()))
                    .setRuleGroupWithRendererEndpointParticipation(Arrays.asList(RULE_GROUP_WITH_PROVIDER))
                    .build())
                .collect(Collectors.toList());
            return new RendererEndpointBuilder().setKey(KeyFactory.rendererEndpointKey(prov.getKey()))
                .setPeerEndpoint(peers)
                .build();
        }).collect(Collectors.toList());
        List<RendererEndpoint> rendererEps = Stream
            .concat(consumersAsRendererEps.stream(), providersAsRendererEps.stream()).collect(Collectors.toList());
        return new ConfigurationBuilder().setEndpoints(endpoints)
            .setRendererEndpoints(new RendererEndpointsBuilder().setRendererEndpoint(rendererEps).build())
            .setRuleGroups(new RuleGroupsBuilder(createRuleGroups()).build())
            .setRendererForwarding(new RendererForwardingBuilder()
                .setRendererForwardingByTenant(ImmutableList.<RendererForwardingByTenant>of(createForwarding()))
                .build())
            .build();
    }

    private static RendererForwardingByTenant createForwarding() {
        RendererForwardingContext l2Fd = createRendererForwardingCtx(L2FD_CTX, L2_FD_ID, L2FloodDomain.class).setParent(
                createParent(L2BD_CTX, L2BridgeDomain.class))
            .build();
        RendererForwardingContext l2Bd = createRendererForwardingCtx(L3_CTX, L3_CTX_ID, L3Context.class).setParent(
                createParent(L3_CTX, L3Context.class))
            .build();
        RendererForwardingContext l3Ctx = createRendererForwardingCtx(L2BD_CTX, L2_BD_ID, L2BridgeDomain.class).build();
        RendererNetworkDomain subnet = new RendererNetworkDomainBuilder().setNetworkDomainId(new NetworkDomainId(SUBNET_ID.getValue()))
            .setName(SUBNET_ID)
            .setNetworkDomainType(Subnet.class)
            .setParent(createParent(L2FD_CTX, L2FloodDomain.class))
            .addAugmentation(
                    SubnetAugmentRenderer.class,
                    new SubnetAugmentRendererBuilder().setSubnet(
                            new SubnetBuilder().setIpPrefix(subnetPrefix).setVirtualRouterIp(virtRouterIp).setIsTenant(true).build())
                        .build())
            .build();
        return new RendererForwardingByTenantBuilder().setTenantId(TENANT_ID)
            .setRendererForwardingContext(ImmutableList.<RendererForwardingContext>of(l2Fd, l2Bd, l3Ctx))
            .setRendererNetworkDomain(ImmutableList.<RendererNetworkDomain>of(subnet))
            .build();
    }

    static RendererForwardingContextBuilder createRendererForwardingCtx(ContextId id, Name name,
            Class<? extends ContextType> type) {
        return new RendererForwardingContextBuilder().setName(name).setContextId(id).setContextType(type);
    }

    static Parent createParent(ContextId ctxId, Class<? extends ContextType> type) {
        return new ParentBuilder().setContextId(ctxId).setContextType(type).build();
    }

    public static RuleGroups createRuleGroups() {
        ParameterValue param = new ParameterValueBuilder().setIntValue(EtherTypeClassifierDefinition.IPv4_VALUE)
            .setName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
            .build();
        Classifier classif = new ClassifierBuilder().setClassifierDefinitionId(EtherTypeClassifierDefinition.ID)
            .setDirection(Direction.In)
            .setName(new ClassifierName("cl-name"))
            .setParameterValue(ImmutableList.<ParameterValue>of(param))
            .build();
        Action action = new ActionBuilder().setActionDefinitionId(AllowActionDefinition.ID)
            .setName(new ActionName("Allow"))
            .build();
        ResolvedRuleBuilder rrBuilder = new ResolvedRuleBuilder();
        rrBuilder.setClassifier(ImmutableList.<Classifier>of(classif));
        rrBuilder.setAction(ImmutableList.<Action>of(action));
        rrBuilder.setName(RULE_NAME);
        rrBuilder.setOrder(0);
        RuleGroup ruleGroup = new RuleGroupBuilder().setTenantId(TENANT_ID)
            .setOrder(0)
            .setSubjectName(SUBJECT_NAME)
            .setContractId(CONTRACT_ID)
            .setResolvedRule(ImmutableList.<ResolvedRule>of(rrBuilder.build()))
            .build();
        return new RuleGroupsBuilder().setRuleGroup(ImmutableList.<RuleGroup>of(ruleGroup)).build();
    }

    public static AddressEndpointWithLocation createEndpoint(String ip, String l2FdIdAsNetCont,
            AbsoluteLocation absoluteLocation) {
        AddressEndpointWithLocationKey key =
                new AddressEndpointWithLocationKey(ip, AddressType.class, CTX_ID, L3Context.class);
        NetworkContainment networkContainment =
                new NetworkContainmentBuilder().setContainment(new ForwardingContextContainmentBuilder()
                    .setContextType(L2FloodDomain.class).setContextId(new ContextId(l2FdIdAsNetCont)).build()).build();
        return new AddressEndpointWithLocationBuilder().setKey(key)
            .setNetworkContainment(networkContainment)
            .setAbsoluteLocation(absoluteLocation)
            .setTenant(new TenantId(TENANT_ID))
            .build();
    }

    public static AbsoluteLocation absoluteLocation(InstanceIdentifier<?> mountPoint, String nodeName,
            String nodeConnectorName) {
        ExternalLocationCaseBuilder extLocBuilder =
                new ExternalLocationCaseBuilder().setExternalNodeMountPoint(mountPoint);
        if (!Strings.isNullOrEmpty(nodeName)) {
            extLocBuilder.setExternalNode(VppPathMapper.bridgeDomainToRestPath(nodeName));
        }
        if (!Strings.isNullOrEmpty(nodeConnectorName)) {
            extLocBuilder.setExternalNodeConnector(VppPathMapper.interfaceToRestPath(nodeConnectorName));
        }
        return new AbsoluteLocationBuilder().setLocationType(extLocBuilder.build()).build();
    }
}
