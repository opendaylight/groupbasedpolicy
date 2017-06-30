/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.binding.attributes.VifDetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.binding.attributes.VifDetailsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class TestUtils {
    static String TEST_SOCKET = "/tmp/socket_testsocket";
    static final String NODE_1 = "devstack-control";
    static final String DUMMY_UUID = "00000000-1111-2222-3333-444444444444";

    public static Port createValidVppPort() {
        PortBindingExtension portBindingExt = new PortBindingExtensionBuilder().setHostId(NODE_1)
            .setVifType("vhostuser")
            .setVifDetails(Collections.singletonList(
                new VifDetailsBuilder().setKey(new VifDetailsKey("vhostuser_socket"))
                    .setValue(TEST_SOCKET)
                    .build()))
            .build();
        return new PortBuilder().setUuid(new Uuid(DUMMY_UUID))
            .setDeviceOwner("compute")
            .setDeviceId(DUMMY_UUID)
            .setMacAddress(new MacAddress("00:11:00:00:11:11"))
            .addAugmentation(PortBindingExtension.class, portBindingExt)
            .build();
    }

    public static Port createNonVppPort() {
        return new PortBuilder().setUuid(new Uuid(DUMMY_UUID))
            .setDeviceOwner("owner1")
            .build();
    }

    public static BaseEndpointByPort createBaseEndpointByPortForPort() {
        return new BaseEndpointByPortBuilder().setContextId(new ContextId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
            .setAddress("00:11:11:11:11:11")
            .setPortId(new UniqueId(DUMMY_UUID))
            .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
            .setAddressType(MacAddressType.class)
            .build();
    }

    static void writeQrouter(@Nonnull DataBroker dataBroker, @Nonnull RouterKey routerKey) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, createNeutronRouterIid(routerKey),
                new RouterBuilder().setKey(routerKey).build(), true);
        DataStoreHelper.submitToDs(tx);
    }

    public static VppEndpointKey createVppEndpointKey(BaseEndpointByPort bebp) {
        return new VppEndpointKey(bebp.getAddress(), bebp.getAddressType(), bebp.getContextId(), bebp.getContextType());
    }

    public static InstanceIdentifier<VppEndpoint> createVppEpIid(VppEndpointKey key) {
        return InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, key).build();
    }

    public static InstanceIdentifier<VppEndpoint> createVppEpIid(String addr, Class<? extends AddressType> addrType,
            ContextId ctxId, Class<? extends ContextType> ctxType) {
        return createVppEpIid(new VppEndpointKey(addr, addrType, ctxId, ctxType));
    }

    public static InstanceIdentifier<Port> createPortIid(PortKey portKey) {
        return InstanceIdentifier.builder(Neutron.class).child(Ports.class).child(Port.class, portKey).build();
    }

    public static InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(Uuid uuid) {
        return createBaseEpByPortIid(new UniqueId(uuid.getValue()));
    }

    public static InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(UniqueId uuid) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(BaseEndpointsByPorts.class)
            .child(BaseEndpointByPort.class, new BaseEndpointByPortKey(uuid))
            .build();
    }

    public static InstanceIdentifier<Router> createNeutronRouterIid(RouterKey routerKey) {
        return InstanceIdentifier.builder(Neutron.class).child(Routers.class).child(Router.class, routerKey).build();
    }

    public static void assertPathArgumentTypes(Iterable<PathArgument> pathArguments, Class<?>[] expectedTypes) {
        assertNotNull(pathArguments);
        Iterator<PathArgument> it = pathArguments.iterator();
        for (int i = 0; i < expectedTypes.length; ++i) {
            assertTrue("Next path argument expected.", it.hasNext());
            assertEquals("Unexpected path argument type.", expectedTypes[i], it.next().getType());
        }
    }
}
