package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper;

import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

import java.util.ArrayList;
import java.util.List;

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
    protected static final String L2 = "L2";
    private static final String DOMAIN_ID = "dummy id";
    protected final NodeId nodeId = new NodeId(NODE_ID);
    protected Short tableId;
    protected OfContext ctx;
    protected OfWriter ofWriter;
    protected SwitchManager switchManager;
    protected PolicyManager policyManager;
    protected EndpointManager endpointManager;
    protected PolicyInfo policyInfo;

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
}
