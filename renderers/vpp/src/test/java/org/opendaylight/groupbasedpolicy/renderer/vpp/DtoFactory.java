package org.opendaylight.groupbasedpolicy.renderer.vpp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
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

public class DtoFactory {

    public static final ContextId CTX_ID = new ContextId("ctx");
    public static final ContextId L2FD_CTX = new ContextId("l2fd");
    public static final ContractId CONTRACT_ID = new ContractId("contract");
    public static final TenantId TENANT_ID = new TenantId("tenant");
    public static final SubjectName SUBJECT_NAME = new SubjectName("subject");
    public static final RuleName RULE_NAME = new RuleName("rule");
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
    public static final RuleGroup RULE_GROUP = new RuleGroupBuilder().setContractId(CONTRACT_ID)
        .setTenantId(TENANT_ID)
        .setSubjectName(SUBJECT_NAME)
        .setResolvedRule(Arrays.asList(new ResolvedRuleBuilder().setName(RULE_NAME).build()))
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
            .setRuleGroups(new RuleGroupsBuilder().setRuleGroup(Arrays.asList(RULE_GROUP)).build())
            .build();
    }

    public static AddressEndpointWithLocation createEndpoint(String ip, String l2FdIdAsNetCont,
            AbsoluteLocation absoluteLocation) {
        AddressEndpointWithLocationKey key =
                new AddressEndpointWithLocationKey(ip, AddressType.class, CTX_ID, ContextType.class);
        NetworkContainment networkContainment =
                new NetworkContainmentBuilder().setContainment(new ForwardingContextContainmentBuilder()
                    .setContextType(L2FloodDomain.class).setContextId(new ContextId(l2FdIdAsNetCont)).build()).build();
        return new AddressEndpointWithLocationBuilder().setKey(key)
            .setNetworkContainment(networkContainment)
            .setAbsoluteLocation(absoluteLocation)
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
