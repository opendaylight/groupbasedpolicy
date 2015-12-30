/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.ResolvedPolicyListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceIdBuilder;

import com.google.common.collect.ImmutableList;

public class EndpointManagerTest extends GbpIovisorDataBrokerTest {

    private EndpointL3 endpoint1;
    private IovisorModuleAugmentation iomAug1;
    private final TenantId tenant1 = new TenantId("tenant1");
    private final EndpointGroupId epgId1 = new EndpointGroupId("client");
    private final L3ContextId l3c1 = new L3ContextId("l3c1");
    private final IpAddress ipv41 = new IpAddress(new Ipv4Address("10.1.1.10"));
    private final EndpointL3Key ipv41Key = new EndpointL3Key(ipv41, l3c1);
    private final IpAddress ipv61 = new IpAddress(new Ipv6Address("2001:db8::2"));
    List<EndpointGroupId> epgs1 = ImmutableList.of(epgId1);
    private final List<L3Address> l3Address1 =
            ImmutableList.of(new L3AddressBuilder().setIpAddress(ipv41).setL3Context(l3c1).build(),
                    new L3AddressBuilder().setIpAddress(ipv61).setL3Context(l3c1).build());

    private EndpointL3 endpoint2;
    private final EndpointGroupId epgId2 = new EndpointGroupId("webserver");
    private final IpAddress ipv42 = new IpAddress(new Ipv4Address("10.1.1.11"));
    private final EndpointL3Key ipv42Key = new EndpointL3Key(ipv42, l3c1);
    private final IpAddress ipv62 = new IpAddress(new Ipv6Address("2001:db8::3"));
    List<EndpointGroupId> epgs2 = ImmutableList.of(epgId2);
    private final List<L3Address> l3Address2 =
            ImmutableList.of(new L3AddressBuilder().setIpAddress(ipv42).setL3Context(l3c1).build(),
                    new L3AddressBuilder().setIpAddress(ipv62).setL3Context(l3c1).build());

    private IovisorModuleId iom1Id;
    private IovisorModuleInstanceId iom1InstanceId;

    private DataBroker dataBroker;
    private EndpointManager endpointManager;
    private IovisorModuleManager iovisorModuleManager;
    private ResolvedPolicyListener resolvedPolicyListener;
    private EpRendererAugmentationRegistry epRAR;

    @Before
    public void initialisation() throws Exception {
        dataBroker = getDataBroker();
        epRAR = mock(EpRendererAugmentationRegistry.class);
        endpointManager = new EndpointManager(dataBroker, epRAR);
        iovisorModuleManager = endpointManager.getIovisorModuleManager();
        resolvedPolicyListener = new ResolvedPolicyListener(dataBroker, iovisorModuleManager);

        iom1Id = new IovisorModuleId("10.10.10.10:10000");
        iom1InstanceId = new IovisorModuleInstanceIdBuilder().setId(iom1Id).build();

        // Endpoint Setup
        iomAug1 = new IovisorModuleAugmentationBuilder().setUri(new Uri(iom1Id.getValue())).build();

        endpoint1 = new EndpointL3Builder().setTenant(tenant1)
            .setL3Context(l3c1)
            .setIpAddress(ipv41)
            .setKey(ipv41Key)
            .setEndpointGroups(epgs1)
            .setL3Address(l3Address1)
            .addAugmentation(IovisorModuleAugmentation.class, iomAug1)
            .build();

        endpoint2 = new EndpointL3Builder().setTenant(tenant1)
            .setL3Context(l3c1)
            .setIpAddress(ipv42)
            .setKey(ipv42Key)
            .setEndpointGroups(epgs2)
            .setL3Address(l3Address2)
            .addAugmentation(IovisorModuleAugmentation.class, iomAug1)
            .build();

    }

    @Test
    public void processEndpointTest() {
        // Test if endpoint IovisorModule URI Augmentation does not already exist in datastore
        IovisorModuleId iovisorModuleId =
                new IovisorModuleId(endpoint1.getAugmentation(IovisorModuleAugmentation.class).getUri().getValue());
        IovisorModuleInstance iovisorModule = iovisorModuleManager.getActiveIovisorModule(iovisorModuleId);
        Assert.assertNull(iovisorModule);
        endpointManager.processEndpoint(endpoint1);
        iovisorModule = iovisorModuleManager.getActiveIovisorModule(iovisorModuleId);
        Assert.assertEquals(iovisorModuleId, iovisorModule.getId());
    }

    @Test
    public void addIovisorResolvedEndpointTest() {
        // Ensure Empty
        Assert.assertNull(endpointManager.getResolvedEndpoints());
        Assert.assertNull(endpointManager.getResolvedEndpointsByTenantByEpg(endpoint1.getTenant(), epgId1));
        Assert.assertNull(iovisorModuleManager.getIovisorModulesByTenantByEpg(endpoint1.getTenant(), epgId1));

        // add one Endpoint to various datastore lists/indexes
        endpointManager.addIovisorResolvedEndpoint(endpoint1);

        Assert.assertTrue(endpointManager.isResolvedEndpoint(endpoint1.getL3Context(), endpoint1.getIpAddress()));
        Assert.assertFalse(endpointManager.isResolvedEndpoint(endpoint2.getL3Context(), endpoint2.getIpAddress()));
        Assert.assertTrue(endpointManager.getResolvedEndpoints().size() == 1);

        Assert.assertTrue(endpointManager.getResolvedEndpointsByTenantByEpg(endpoint1.getTenant(), epgId1).size() == 1);
        Assert.assertTrue(endpointManager.isResolvedEndpointByTenantByEpg(endpoint1.getL3Context(),
                endpoint1.getIpAddress(), endpoint1.getTenant(), epgId1));
        Assert
            .assertTrue(iovisorModuleManager.getIovisorModulesByTenantByEpg(endpoint1.getTenant(), epgId1).size() == 1);
        Assert.assertTrue(iovisorModuleManager.getIovisorModulesByTenantByEpg(endpoint1.getTenant(), epgId1)
            .contains(iom1InstanceId));

        endpointManager.addIovisorResolvedEndpoint(endpoint1);
        Assert.assertTrue(endpointManager.getResolvedEndpoints().size() == 1);

        Assert.assertTrue(endpointManager.getResolvedEndpointsByTenantByEpg(endpoint1.getTenant(), epgId1).size() == 1);
        Assert
            .assertTrue(iovisorModuleManager.getIovisorModulesByTenantByEpg(endpoint1.getTenant(), epgId1).size() == 1);

    }
}
