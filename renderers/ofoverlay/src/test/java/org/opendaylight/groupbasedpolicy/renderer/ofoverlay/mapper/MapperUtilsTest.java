package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper;

import java.util.ArrayList;
import java.util.List;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

import com.google.common.collect.ImmutableList;

public abstract class MapperUtilsTest {

    protected static final String IPV4_1 = "170.0.0.1";
    protected static final String IPV4_2 = "190.0.0.1";
    protected static final String MAC_0 = "00:00:00:00:00:00";
    protected static final String MAC_1 = "00:00:00:00:00:01";
    protected static final String CONNECTOR_0 = "0";
    protected static final String CONNECTOR_1 = "1";
    protected static final String IP_PREFIX_32 = "/32";
    protected static final String IP_PREFIX_128 = "/128";
    protected static final String IPV6_1 = "2000:db80:85a3:08ba:0947:8a2e:3a70:7334";
    protected static final String IPV6_2 = "0947:db80:3a70:7334:85a3:8a2e:2000:08ba";
    protected static final String DHCP_IP = "255.255.255.255";
    protected static final String TENANT_ID = "dummy tenant";
    protected static final String NODE_ID = "dummy node";
    protected static final String CONTRACT_ID = "dummy contract";
    protected static final String L2 = "L2";
    protected static final String DOMAIN_ID = "dummy id";
    protected final NodeId nodeId = new NodeId(NODE_ID);
    protected NodeConnectorId nodeConnectorId =
            new NodeConnectorId(nodeId.getValue() + CONNECTOR_0);
    protected L3ContextId l3c = new L3ContextId("2cf51ee4-e996-467e-a277-2d380334a91d");
    protected L2BridgeDomainId bd = new L2BridgeDomainId("c95182ba-7807-43f8-98f7-6c7c720b7639");
    protected L2FloodDomainId fd = new L2FloodDomainId("98e1439e-52d2-46f8-bd69-5136e6088771");
    protected L2FloodDomainId ext_fd = new L2FloodDomainId("d8024f7a-b83e-11e5-9912-ba0be0483c18");
    protected SubnetId sub = new SubnetId("4fcf8dfc-53b5-4aef-84d3-6b5586992fcb");
    protected SubnetId sub2 = new SubnetId("c285a59f-fcb8-42e6-bf29-87ea522fd626");
    protected SubnetId sub3 = new SubnetId("a0380d52-2a25-48ef-882c-a4d4cd9e00ec");
    protected SubnetId ext_sub = new SubnetId("8da17ad9-3261-4dc9-bcff-928a2f73cce7");
    protected TenantId tid = new TenantId(TENANT_ID);
    protected EndpointGroupId eg = new EndpointGroupId("36dec84a-08c7-497b-80b6-a0035af72a12");
    protected EndpointGroupId eg2 = new EndpointGroupId("632e5e11-7988-4eb5-8fe6-6c182d890276");
    protected ContractId cid = new ContractId("a5874893-bcd5-46de-96af-3c8d99bedf9f");
    protected Short tableId;
    protected OfContext ctx;
    protected OfWriter ofWriter;
    protected SwitchManager switchManager;
    protected PolicyManager policyManager;
    protected EndpointManager endpointManager;
    protected PolicyInfo policyInfo;
    protected FlowTable table;

    protected Flow flowCreator(FlowId flowId, short tableId, Integer priority, Match match, Instructions instructions) {
        FlowBuilder flowBuilder = FlowUtils.base(tableId);
        flowBuilder.setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(instructions);

        return flowBuilder.build();
    }

    protected Endpoint endpointCreator(IpAddress ip, MacAddress mac, NodeConnectorId nodeConnectorId) {
        EndpointBuilder endpointBuilder = new EndpointBuilder();

        // Set tenant
        endpointBuilder.setTenant(new TenantId(TENANT_ID));

        // Set L3 address
        if (ip != null) {
            List<L3Address> l3Addresses = new ArrayList<>();
            L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
            l3AddressBuilder.setIpAddress(ip);
            l3Addresses.add(l3AddressBuilder.build());
            endpointBuilder.setL3Address(l3Addresses);
        }

        // Set Mac address
        endpointBuilder.setMacAddress(new MacAddress(mac));

        // Augment node connector
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(nodeConnectorId));
        ofOverlayContextBuilder.setNodeId(nodeId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());

        // Set network containment
        endpointBuilder.setNetworkContainment(new NetworkDomainId(DOMAIN_ID));

        return endpointBuilder.build();
    }

    protected EndpointL3 endpointL3Creator(String ip, String insideIp, String mac, String l2, boolean ipv6) {
        EndpointL3Builder endpointL3Builder = new EndpointL3Builder();

        // Set NAT address augmentation
        if (ip != null) {
            if (ipv6) {
                NatAddressBuilder natAddressBuilder = new NatAddressBuilder();
                natAddressBuilder.setNatAddress(new IpAddress(new Ipv6Address(ip)));
                endpointL3Builder.addAugmentation(NatAddress.class, natAddressBuilder.build());
            } else {
                NatAddressBuilder natAddressBuilder = new NatAddressBuilder();
                natAddressBuilder.setNatAddress(new IpAddress(new Ipv4Address(ip)));
                endpointL3Builder.addAugmentation(NatAddress.class, natAddressBuilder.build());
            }
        }

        // Set IP address
        if (insideIp != null) {
            if (ipv6) {
                endpointL3Builder.setIpAddress(new IpAddress(new Ipv6Address(insideIp)));
            } else {
                endpointL3Builder.setIpAddress(new IpAddress(new Ipv4Address(insideIp)));
            }
        }

        // Set MAC
        if (mac != null) {
            endpointL3Builder.setMacAddress(new MacAddress(mac));
        }

        // Set L2 context
        if (l2 != null) {
            endpointL3Builder.setL2Context(new L2BridgeDomainId(l2));
        }

        return endpointL3Builder.build();

    }

    protected IndexedTenant tenantCreator() {
        TenantBuilder tenantBuilder = new TenantBuilder();
        tenantBuilder.setId(new TenantId(TENANT_ID));

        // Set forwarding context
        SegmentationBuilder segmentationBuilder = new SegmentationBuilder();
        segmentationBuilder.setSegmentationId(1);
        List<L2FloodDomain> l2FloodDomains = new ArrayList<>();
        L2FloodDomainBuilder l2FloodDomainBuilder = new L2FloodDomainBuilder();
        l2FloodDomainBuilder.setId(new L2FloodDomainId("l2id"));
        l2FloodDomainBuilder.addAugmentation(Segmentation.class, segmentationBuilder.build());
        l2FloodDomains.add(l2FloodDomainBuilder.build());
        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setL2FloodDomain(l2FloodDomains);
        tenantBuilder.setForwardingContext(forwardingContextBuilder.build());

        return new IndexedTenant(tenantBuilder.build());
    }

    protected List<L2FloodDomain> l2FloodDomainsCreator() {
        SegmentationBuilder segmentationBuilder = new SegmentationBuilder();
        segmentationBuilder.setSegmentationId(1);
        List<L2FloodDomain> l2FloodDomains = new ArrayList<>();
        L2FloodDomainBuilder l2FloodDomainBuilder = new L2FloodDomainBuilder();
        l2FloodDomainBuilder.setId(new L2FloodDomainId("l2id"));
        l2FloodDomainBuilder.addAugmentation(Segmentation.class, segmentationBuilder.build());
        l2FloodDomains.add(l2FloodDomainBuilder.build());
        return l2FloodDomains;
    }

    protected EndpointBuilder endpointBuilder(IpAddress ip, MacAddress mac, NodeConnectorId nodeConnectorId,
            EndpointGroupId epg, L2BridgeDomainId l2) {
        EndpointBuilder endpointBuilder = new EndpointBuilder();

        // Set tenant
        endpointBuilder.setTenant(new TenantId(TENANT_ID));

        // Set L3 address
        if (ip != null) {
            List<L3Address> l3Addresses = new ArrayList<>();
            L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
            l3AddressBuilder.setIpAddress(ip);
            l3Addresses.add(l3AddressBuilder.build());
            endpointBuilder.setL3Address(l3Addresses);
        }

        // Set Mac address
        endpointBuilder.setMacAddress(new MacAddress(mac));
        endpointBuilder.setL2Context(l2);
        endpointBuilder.setEndpointGroup(epg);

        // Augment node connector
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(nodeConnectorId));
        ofOverlayContextBuilder.setNodeId(nodeId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());

        // Set network containment
        endpointBuilder.setNetworkContainment(new NetworkDomainId(DOMAIN_ID));

        return endpointBuilder;
    }

    protected TenantBuilder baseTenant() {
        return new TenantBuilder().setId(tid)
            .setPolicy(new PolicyBuilder()
                .setEndpointGroup(ImmutableList.of(
                        new EndpointGroupBuilder().setId(eg)
                            .setNetworkDomain(sub)
                            .setConsumerNamedSelector(ImmutableList.of(new ConsumerNamedSelectorBuilder()
                                .setName(new SelectorName("cns1")).setContract(ImmutableList.of(cid)).build()))
                            .build(),
                        new EndpointGroupBuilder().setId(eg2)
                            .setNetworkDomain(sub2)
                            .setProviderNamedSelector(ImmutableList.of(new ProviderNamedSelectorBuilder()
                                .setName(new SelectorName("pns1")).setContract(ImmutableList.of(cid)).build()))
                            .build()))
                .setSubjectFeatureInstances(
                        new SubjectFeatureInstancesBuilder()
                            .setClassifierInstance(
                                    ImmutableList
                                        .of(new ClassifierInstanceBuilder().setName(new ClassifierName("tcp_dst_80"))
                                            .setClassifierDefinitionId(L4ClassifierDefinition.DEFINITION.getId())
                                            .setParameterValue(ImmutableList.of(
                                                    new ParameterValueBuilder().setName(new ParameterName("destport"))
                                                        .setIntValue(Long.valueOf(80))
                                                        .build(),
                                                    new ParameterValueBuilder().setName(new ParameterName("proto"))
                                                        .setIntValue(Long.valueOf(6))
                                                        .build()))
                                            .build(), new ClassifierInstanceBuilder()
                                                .setName(new ClassifierName("tcp_src_80"))
                                                .setClassifierDefinitionId(Classifier.L4_CL.getId())
                                                .setParameterValue(ImmutableList.of(
                                                        new ParameterValueBuilder()
                                                            .setName(new ParameterName("sourceport"))
                                                            .setIntValue(Long.valueOf(80))
                                                            .build(),
                                                        new ParameterValueBuilder().setName(new ParameterName("proto"))
                                                            .setIntValue(Long.valueOf(6))
                                                            .build()))
                                                .build(),
                                                new ClassifierInstanceBuilder()
                                                    .setName(new ClassifierName("ether_type"))
                                                    .setClassifierDefinitionId(Classifier.ETHER_TYPE_CL.getId())
                                                    .setParameterValue(ImmutableList.of(new ParameterValueBuilder()
                                                        .setName(new ParameterName("ethertype"))
                                                        .setIntValue(Long.valueOf(FlowUtils.IPv4))
                                                        .build()))
                                                    .build()))
                            .setActionInstance(
                                    ImmutableList.of(new ActionInstanceBuilder().setName(new ActionName("allow"))
                                        .setActionDefinitionId(new AllowAction().getId())
                                        .build()))
                            .build())
                .build())
            .setForwardingContext(
                    new ForwardingContextBuilder()
                        .setL3Context(ImmutableList.of(new L3ContextBuilder().setId(l3c).build()))
                        .setL2BridgeDomain(
                                ImmutableList.of(new L2BridgeDomainBuilder().setId(bd).setParent(l3c).build()))
                        .setL2FloodDomain(ImmutableList.of(
                                new L2FloodDomainBuilder()
                                    .setId(fd)
                                    .setParent(bd)
                                    .addAugmentation(Segmentation.class,
                                        new SegmentationBuilder()
                                        .setSegmentationId(Integer.valueOf(216))
                                        .build())
                                    .build(),
                                new L2FloodDomainBuilder()
                                    .setId(ext_fd)
                                    .addAugmentation(Segmentation.class,
                                        new SegmentationBuilder()
                                        .setSegmentationId(Integer.valueOf(2016))
                                        .build())
                                    .build()))
                        .setSubnet(ImmutableList.of(
                                new SubnetBuilder().setId(sub2)
                                    .setParent(fd)
                                    .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.1.0/24")))
                                    .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.1.1")))
                                    .build(),
                                new SubnetBuilder().setId(sub)
                                    .setParent(fd)
                                    .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/24")))
                                    .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.0.1")))
                                    .build(),
                                new SubnetBuilder().setId(sub3)
                                    .setParent(bd)
                                    .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.2.0/24")))
                                    .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.2.1")))
                                    .build(),
                                new SubnetBuilder()
                                    .setId(ext_sub)
                                    .setIpPrefix(new IpPrefix(new Ipv4Prefix("192.168.111.0/24")))
                                    .setParent(ext_fd)
                                    .build()))
                       .build());
    }
}
