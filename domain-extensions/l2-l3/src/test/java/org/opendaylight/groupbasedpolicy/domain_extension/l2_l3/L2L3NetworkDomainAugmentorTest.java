package org.opendaylight.groupbasedpolicy.domain_extension.l2_l3;

import java.util.Arrays;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.gateways.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yangtools.yang.binding.Augmentation;

@RunWith(MockitoJUnitRunner.class)
public class L2L3NetworkDomainAugmentorTest {

    private static final IpPrefix IP_PREFIX = new IpPrefix(new Ipv4Prefix("1.1.1.1/24"));
    private static final IpPrefix IP_PREFIX_2 = new IpPrefix(new Ipv4Prefix("2.2.2.2/24"));
    private static final IpAddress IP_ADDRESS = new IpAddress(new Ipv4Address("1.1.1.1"));
    private static final IpAddress IP_ADDRESS_2 = new IpAddress(new Ipv4Address("2.2.2.2"));
    private static final NetworkDomainId NET_DOMAIN = new NetworkDomainId("netDomain1");

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    private NetworkDomainAugmentorRegistry networkDomainAugmentorRegistry;

    private L2L3NetworkDomainAugmentor l2l3Augmentor;

    @Before
    public void init() {
        l2l3Augmentor = new L2L3NetworkDomainAugmentor(networkDomainAugmentorRegistry);
    }

    @Test
    public void testConstructor() {
        L2L3NetworkDomainAugmentor l2l3NetworkDomainAugmentor =
                new L2L3NetworkDomainAugmentor(networkDomainAugmentorRegistry);
        Mockito.verify(networkDomainAugmentorRegistry).register(Mockito.eq(l2l3NetworkDomainAugmentor));
    }

    @Test
    public void testConstructor_npe_exception() {
        exception.expect(NullPointerException.class);
        new L2L3NetworkDomainAugmentor(null);
    }

    @Test
    public void testClose() throws Exception {
        l2l3Augmentor.close();
        Mockito.verify(networkDomainAugmentorRegistry).register(Mockito.eq(l2l3Augmentor));
    }

    @Test
    public void testBuildRendererNetworkDomainAugmentation() {
        Gateways gateways = new GatewaysBuilder().setGateway(IP_ADDRESS)
            .setPrefixes(Arrays.asList(new PrefixesBuilder().setPrefix(IP_PREFIX_2).build()))
            .build();
        SubnetAugmentForwarding subnetAugmentForwarding =
                new SubnetAugmentForwardingBuilder().setSubnet(new SubnetBuilder().setIpPrefix(IP_PREFIX)
                    .setGateways(Arrays.asList(gateways))
                    .setVirtualRouterIp(IP_ADDRESS_2)
                    .build()).build();
        NetworkDomain networkDomain = new NetworkDomainBuilder()
            .setNetworkDomainType(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.NetworkDomain.class)
            .setNetworkDomainId(NET_DOMAIN)
            .addAugmentation(SubnetAugmentForwarding.class, subnetAugmentForwarding)
            .build();

        Entry<Class<? extends Augmentation<RendererNetworkDomain>>, Augmentation<RendererNetworkDomain>> rendererNetworkDomainAugmentation =
                l2l3Augmentor.buildRendererNetworkDomainAugmentation(networkDomain);

        Assert.assertNotNull(rendererNetworkDomainAugmentation);
        Assert.assertEquals(SubnetAugmentRenderer.class, rendererNetworkDomainAugmentation.getKey());
        Augmentation<RendererNetworkDomain> rendererNetDomainAug = rendererNetworkDomainAugmentation.getValue();
        Assert.assertTrue(rendererNetDomainAug instanceof SubnetAugmentRenderer);
        SubnetAugmentRenderer subnetAugmentRenderer = (SubnetAugmentRenderer) rendererNetDomainAug;
        Assert.assertEquals(subnetAugmentForwarding.getSubnet(), subnetAugmentRenderer.getSubnet());
    }

    @Test
    public void testBuildRendererNetworkDomainAugmentation_nullAugmentation() {
        NetworkDomain networkDomain = new NetworkDomainBuilder()
            .setNetworkDomainType(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.NetworkDomain.class)
            .setNetworkDomainId(NET_DOMAIN)
            .build();

        Entry<Class<? extends Augmentation<RendererNetworkDomain>>, Augmentation<RendererNetworkDomain>> rendererNetworkDomainAugmentation =
                l2l3Augmentor.buildRendererNetworkDomainAugmentation(networkDomain);

        Assert.assertNull(rendererNetworkDomainAugmentation);
    }

}
