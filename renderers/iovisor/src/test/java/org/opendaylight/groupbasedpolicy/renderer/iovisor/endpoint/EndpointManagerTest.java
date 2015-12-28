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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class EndpointManagerTest extends GbpIovisorDataBrokerTest {

    private final TenantId tenant1 = new TenantId("tenant1");
    private final L3ContextId l3c1 = new L3ContextId("l3c1");
    private final IpAddress ipv41 = new IpAddress(new Ipv4Address("10.1.1.10"));

    private final EndpointL3Key ipv41Key = new EndpointL3Key(ipv41, l3c1);

    private final IpAddress ipv61 = new IpAddress(new Ipv6Address("2001:db8::2"));

    List<EndpointGroupId> epgs = ImmutableList.of(new EndpointGroupId("client1"));

    private final List<L3Address> l3Address1 =
            ImmutableList.of(new L3AddressBuilder().setIpAddress(ipv41).setL3Context(l3c1).build(),
                    new L3AddressBuilder().setIpAddress(ipv61).setL3Context(l3c1).build());

    private DataBroker dataBroker;
    private EndpointManager endpointManager;
    private static IovisorModuleManager iovisorModuleManager;
    private EpRendererAugmentationRegistry epRAR;
    private EndpointL3 endpoint1;
    private IovisorModuleAugmentation iomAug1;

    @Before
    public void initialisation() throws Exception {
        dataBroker = getDataBroker();
        epRAR = mock(EpRendererAugmentationRegistry.class);
        endpointManager = new EndpointManager(dataBroker, epRAR);
        iovisorModuleManager = endpointManager.getIovisorModuleManager();

        // Endpoint Setup
        iomAug1 = new IovisorModuleAugmentationBuilder().setUri(new Uri("10.10.10.10:10000")).build();
        endpoint1 = new EndpointL3Builder().setTenant(tenant1)
            .setL3Context(l3c1)
            .setIpAddress(ipv41)
            .setKey(ipv41Key)
            .setEndpointGroups(epgs)
            .setL3Address(l3Address1)
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
        iovisorModule = iovisorModuleManager.getActiveIovisorModule(iovisorModuleId);;
        Assert.assertEquals(iovisorModuleId, iovisorModule.getId());
    }

    @Test
    public void addIovisorResolvedEndpointTest() {
        endpointManager.addIovisorResolvedEndpoint(endpoint1);
        Optional<IovisorResolvedEndpointByTenantByEndpointgroupId> readFromDs =
                DataStoreHelper
                    .readFromDs(LogicalDatastoreType.OPERATIONAL,
                            IovisorIidFactory.iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(
                                    endpoint1.getTenant(), endpoint1.getEndpointGroups().get(0)),
                        dataBroker.newReadOnlyTransaction());
        IovisorResolvedEndpointByTenantByEndpointgroupId iovisorResolvedEndpointByTenantByEndpointgroupId;
        if (readFromDs.isPresent()) {
            iovisorResolvedEndpointByTenantByEndpointgroupId = readFromDs.get();
        } else {
            iovisorResolvedEndpointByTenantByEndpointgroupId = null;
        }
        Assert.assertNotNull(iovisorResolvedEndpointByTenantByEndpointgroupId);
    }
}
