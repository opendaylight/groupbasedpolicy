package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.SegmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

import java.util.ArrayList;
import java.util.List;

public abstract class MapperUtilsTest {

    protected static final MacAddress MAC_0 = new MacAddress("00:00:00:00:00:00");
    protected static final MacAddress MAC_1 = new MacAddress("00:00:00:00:00:01");
    protected static final MacAddress MAC_2 = new MacAddress("00:00:00:00:00:02");
    protected static final Ipv4Address IPV4_0 = new Ipv4Address("170.0.0.1");
    protected static final Ipv4Address IPV4_1 = new Ipv4Address("190.0.0.1");
    protected static final Ipv4Address IPV4_2 = new Ipv4Address("210.0.0.1");
    protected static final Ipv6Address IPV6_1 = new Ipv6Address("2000:db80:85a3:08ba:0947:8a2e:3a70:7334");
    protected static final Ipv6Address IPV6_2 = new Ipv6Address("0947:db80:3a70:7334:85a3:8a2e:2000:08ba");
    protected static final NodeConnectorId CONNECTOR_0 = new NodeConnectorId("0");
    protected static final NodeConnectorId CONNECTOR_1 = new NodeConnectorId("1");
    protected static final NodeConnectorId CONNECTOR_2 = new NodeConnectorId("2");
    protected static final SubnetId SUBNET_0 = new SubnetId("subnet0");
    protected static final SubnetId SUBNET_1 = new SubnetId("subnet1");
    protected static final SubnetId SUBNET_2 = new SubnetId("subnet2");
    protected static final SubnetId SUBNET_EXT = new SubnetId("externalSubnet");
    protected static final String IP_PREFIX_32 = "/32";
    protected static final String IP_PREFIX_128 = "/128";
    protected static final TenantId TENANT_ID = new TenantId("tenantId");
    protected static final NodeId NODE_ID = new NodeId("nodeId");
    protected static final EndpointGroupId ENDPOINT_GROUP_0 = new EndpointGroupId("eg0");
    protected static final EndpointGroupId ENDPOINT_GROUP_1 = new EndpointGroupId("eg1");
    protected static final EndpointGroupId ENDPOINT_GROUP_2 = new EndpointGroupId("eg2");
    protected static final NetworkDomainId NET_DOMAIN_ID = new NetworkDomainId("ndId");
    protected static final L2BridgeDomainId L2BD_ID = new L2BridgeDomainId("l2bdId");
    protected static final L2FloodDomainId L2FD_ID = new L2FloodDomainId("l2fdId");
    protected static final L2FloodDomainId L2_FD_ID_EXT = new L2FloodDomainId("externalL2fdId");
    protected static final L3ContextId L3C_ID = new L3ContextId("l3cId");
    protected static final ContractId CONTRACT_ID = new ContractId("contractId");
    protected static final ContextId CONTEXT_ID = new L3ContextId("ctxId");
    // Often used strings
    protected static final String ALLOW = "allow";
    protected static final String L2 = "L2";
    protected static final String OPENFLOW = "openflow:";
    protected static final String DROP_ALL = "dropAll";
    protected static final String DROP = "drop";
    protected static final String TCP_SRC = "tcp_src_80";
    // Mock variables
    protected Short tableId;
    protected OfContext ctx;
    protected OfWriter ofWriter;
    protected SwitchManager switchManager;
    protected PolicyManager policyManager;
    protected EndpointManager endpointManager;
    protected PolicyInfo policyInfo;
    protected FlowTable table;

    protected FlowBuilder buildFlow(FlowId flowId, short tableId, Integer priority, Match match, Instructions instructions) {
        FlowBuilder flowBuilder = FlowUtils.base(tableId);
        flowBuilder.setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(instructions);
        return flowBuilder;
    }

    protected EndpointL3Builder buildL3Endpoint(Ipv4Address natIp, Ipv4Address ip, MacAddress mac, String l2bd) {
        Preconditions.checkNotNull(natIp);
        Preconditions.checkNotNull(ip);
        Preconditions.checkNotNull(mac);
        EndpointL3Builder endpointL3Builder = new EndpointL3Builder();
        NatAddressBuilder natAddressBuilder = new NatAddressBuilder();
        natAddressBuilder.setNatAddress(new IpAddress(new Ipv4Address(natIp)));
        endpointL3Builder.addAugmentation(NatAddress.class, natAddressBuilder.build());
        endpointL3Builder.setIpAddress(new IpAddress(ip));
        endpointL3Builder.setMacAddress(new MacAddress(mac));
        if (l2bd != null) {
            endpointL3Builder.setL2Context(new L2BridgeDomainId(l2bd));
        }
        return endpointL3Builder;
    }

    protected EndpointL3Builder buildL3Endpoint(Ipv6Address natIp, Ipv6Address ip, MacAddress mac, String l2bd) {
        Preconditions.checkNotNull(natIp);
        Preconditions.checkNotNull(ip);
        Preconditions.checkNotNull(mac);
        EndpointL3Builder endpointL3Builder = new EndpointL3Builder();
        NatAddressBuilder natAddressBuilder = new NatAddressBuilder();
        natAddressBuilder.setNatAddress(new IpAddress(new Ipv6Address(natIp)));
        endpointL3Builder.addAugmentation(NatAddress.class, natAddressBuilder.build());
        endpointL3Builder.setIpAddress(new IpAddress(ip));

        endpointL3Builder.setMacAddress(new MacAddress(mac));
        if (l2bd != null) {
            endpointL3Builder.setL2Context(new L2BridgeDomainId(l2bd));
        }
        return endpointL3Builder;
    }

    public SegmentationBuilder buildSegmentation() {
        SegmentationBuilder segmentationBuilder = new SegmentationBuilder();
        segmentationBuilder.setSegmentationId(1);
        return  segmentationBuilder;
    }

    protected TenantBuilder buildTenant() {
        TenantBuilder tenantBuilder = new TenantBuilder();
        tenantBuilder.setId(TENANT_ID);
        tenantBuilder.setForwardingContext(buildForwardingContext().build());
        PolicyBuilder policyBuilder = new PolicyBuilder();
        policyBuilder.setEndpointGroup(getEndpointGroups());
        policyBuilder.setSubjectFeatureInstances(getSubjectFeatureInstances());
        tenantBuilder.setPolicy(policyBuilder.build());
        return tenantBuilder;
    }

    protected IndexedTenant getTestIndexedTenant() {
        return new IndexedTenant(buildTenant().build());
    }

    protected ForwardingContextBuilder buildForwardingContext() {
        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setL2FloodDomain(getL2FloodDomainList(false));
        forwardingContextBuilder.setL2BridgeDomain(getL2BridgeDomainList());
        forwardingContextBuilder.setL3Context(getL3ContextList());
        forwardingContextBuilder.setSubnet(getSubnetList());
        return forwardingContextBuilder;
    }

    protected List<L3Context> getL3ContextList() {
        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());
        return l3Contexts;
    }

    protected List<L2BridgeDomain> getL2BridgeDomainList() {
        List<L2BridgeDomain> l2BridgeDomains = new ArrayList<>();
        L2BridgeDomainBuilder l2BridgeDomainBuilder = new L2BridgeDomainBuilder();
        l2BridgeDomainBuilder.setId(L2BD_ID);
        l2BridgeDomainBuilder.setParent(L3C_ID);
        l2BridgeDomains.add(l2BridgeDomainBuilder.build());
        return l2BridgeDomains;
    }

    protected List<L2FloodDomain> getL2FloodDomainList(boolean external) {
        List<L2FloodDomain> l2FloodDomains = new ArrayList<>();
        L2FloodDomainBuilder l2FloodDomainBuilder = new L2FloodDomainBuilder();
        l2FloodDomainBuilder.setId(L2FD_ID);
        if (external) {
            l2FloodDomainBuilder.setId(L2_FD_ID_EXT);
        }
        l2FloodDomainBuilder.setParent(new L2BridgeDomainId(L2BD_ID));
        l2FloodDomainBuilder.addAugmentation(Segmentation.class, buildSegmentation().build());
        l2FloodDomains.add(l2FloodDomainBuilder.build());
        return l2FloodDomains;
    }

    protected List<L3Address> getL3AddressList(Ipv4Address l3IpAddress, L3ContextId l3ContextId) {
        List<L3Address> l3Addresses = new ArrayList<>();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(l3IpAddress));
        l3AddressBuilder.setL3Context(new L3ContextId(l3ContextId));
        l3Addresses.add(l3AddressBuilder.build());
        return l3Addresses;
    }

    protected List<L3Address> getL3AddressList(Ipv6Address l3IpAddress) {
        List<L3Address> l3Addresses = new ArrayList<>();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(l3IpAddress));
        l3Addresses.add(l3AddressBuilder.build());
        return l3Addresses;
    }

    protected OfOverlayContextBuilder getOfOverlayContext(NodeConnectorId connector) {
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(connector);
        ofOverlayContextBuilder.setNodeId(NODE_ID);
        return ofOverlayContextBuilder;
    }

    protected EndpointBuilder buildEndpoint(Ipv4Address l3IpAddress, MacAddress mac, NodeConnectorId connector) {
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        endpointBuilder.setTenant(TENANT_ID);
        endpointBuilder.setL3Address(getL3AddressList(l3IpAddress, L3C_ID));
        endpointBuilder.setMacAddress(new MacAddress(mac));
        endpointBuilder.setL2Context(new L2BridgeDomainId(L2BD_ID));
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);
        endpointBuilder.addAugmentation(OfOverlayContext.class, getOfOverlayContext(connector).build());
        endpointBuilder.setNetworkContainment(NET_DOMAIN_ID);
        return endpointBuilder;
    }

    protected EndpointBuilder buildEndpoint(Ipv6Address l3IpAddress, MacAddress mac, NodeConnectorId connector) {
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        endpointBuilder.setTenant(TENANT_ID);
        endpointBuilder.setL3Address(getL3AddressList(l3IpAddress));
        endpointBuilder.setMacAddress(new MacAddress(mac));
        endpointBuilder.setL2Context(new L2BridgeDomainId(L2BD_ID));
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);
        endpointBuilder.addAugmentation(OfOverlayContext.class, getOfOverlayContext(connector).build());
        endpointBuilder.setNetworkContainment(NET_DOMAIN_ID);
        return endpointBuilder;
    }

    public List<EndpointGroup> getEndpointGroups() {
        return ImmutableList.of(
                new EndpointGroupBuilder().setId(ENDPOINT_GROUP_0)
                        .setNetworkDomain(SUBNET_0)
                        .setConsumerNamedSelector(ImmutableList.of(new ConsumerNamedSelectorBuilder()
                                .setName(new SelectorName("cns1")).setContract(ImmutableList.of(CONTRACT_ID)).build()))
                        .build(),
                new EndpointGroupBuilder().setId(ENDPOINT_GROUP_1)
                        .setNetworkDomain(SUBNET_1)
                        .setProviderNamedSelector(ImmutableList.of(new ProviderNamedSelectorBuilder()
                                .setName(new SelectorName("pns1")).setContract(ImmutableList.of(CONTRACT_ID)).build()))
                        .build());
    }

    protected SubjectFeatureInstances getSubjectFeatureInstances() {
        SubjectFeatureInstancesBuilder builder = new SubjectFeatureInstancesBuilder();
        return builder.setClassifierInstance(ImmutableList.of(new ClassifierInstanceBuilder()
                .setName(new ClassifierName("tcp_dst_80"))
                .setClassifierDefinitionId(L4ClassifierDefinition.DEFINITION.getId())
                .setParameterValue(ImmutableList.of(new ParameterValueBuilder().setName(new ParameterName("destport"))
                        .setIntValue(80L)        // Endpoint

                        .build(), new ParameterValueBuilder().setName(new ParameterName("proto"))
                        .setIntValue(6L)
                        .build()))
                .build(), new ClassifierInstanceBuilder().setName(new ClassifierName(TCP_SRC))
                .setClassifierDefinitionId(Classifier.L4_CL.getId())
                .setParameterValue(ImmutableList.of(new ParameterValueBuilder().setName(new ParameterName("sourceport"))
                        .setIntValue(80L)
                        .build(), new ParameterValueBuilder().setName(new ParameterName("proto"))
                        .setIntValue(6L)
                        .build()))
                .build(), new ClassifierInstanceBuilder().setName(new ClassifierName("ether_type"))
                .setClassifierDefinitionId(Classifier.ETHER_TYPE_CL.getId())
                .setParameterValue(ImmutableList.of(new ParameterValueBuilder()
                        .setName(new ParameterName("ethertype"))
                        .setIntValue(FlowUtils.IPv4)
                        .build()))
                .build()))
                .setActionInstance(ImmutableList.of(new ActionInstanceBuilder().setName(new ActionName("allow"))
                        .setActionDefinitionId(new AllowAction().getId())
                        .build()))
                .build();
    }

    protected List<Subnet> getSubnetList() {
        return ImmutableList.of(
                new SubnetBuilder().setId(SUBNET_0)
                        .setParent(L2FD_ID)
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.1.0/24")))
                        .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.1.1")))
                        .build(),
                new SubnetBuilder().setId(SUBNET_1)
                        .setParent(L2FD_ID)
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/24")))
                        .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.0.1")))
                        .build(),
                new SubnetBuilder().setId(SUBNET_2)
                        .setParent(L2BD_ID)
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.2.0/24")))
                        .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.2.1")))
                        .build(),
                new SubnetBuilder().setId(SUBNET_EXT)
                        .setParent(L2_FD_ID_EXT)
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("192.168.111.0/24")))
                        .build());
    }
}
