/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronListenerTest extends AbstractDataBrokerTest implements DataTreeChangeListener<VppEndpoint> {

    private DataBroker dataBroker;
    private Integer eventCount;
    private static final LogicalDatastoreType CONFIG = LogicalDatastoreType.CONFIGURATION;
    private static final LogicalDatastoreType OPER = LogicalDatastoreType.OPERATIONAL;

    private final Port port = new PortBuilder().setUuid(new Uuid("00000000-1111-2222-3333-444444444444")).build();
    private final BaseEndpointByPort bebp = new BaseEndpointByPortBuilder().setContextId(
            new ContextId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        .setAddress("00:11:11:11:11:11")
        .setPortId(new UniqueId("00000000-1111-2222-3333-444444444444"))
        .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
        .setAddressType(MacAddressType.class)
        .build();

    @Before
    public void init() {
        dataBroker = getDataBroker();
    }

    @Test
    public void constructorTest() {
        dataBroker = Mockito.spy(dataBroker);
        NeutronListener neutronListener = new NeutronListener(dataBroker);
        verify(dataBroker).registerDataTreeChangeListener(
                eq(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Neutron.class)
                            .build())), any(NeutronListener.class));
        verify(dataBroker).registerDataTreeChangeListener(
                eq(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Mappings.class)
                        .child(GbpByNeutronMappings.class)
                        .child(BaseEndpointsByPorts.class)
                        .child(BaseEndpointByPort.class)
                        .build())), any(BaseEndpointByPortListener.class));
        neutronListener.close();
    }

    @Test
    public void createAndDeleteTest() throws Exception {
        eventCount = 0;
        ListenerRegistration<NeutronListenerTest> registerDataTreeChangeListener = registerVppEpListener();
        NeutronListener neutronListener = new NeutronListener(dataBroker);
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(CONFIG, TestUtils.createPortIid(port.getKey()), port);
        wTx.put(OPER, TestUtils.createBaseEpByPortIid(port.getUuid()), bebp);
        wTx.submit().get();
        wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(CONFIG, TestUtils.createPortIid(port.getKey()));
        wTx.delete(OPER, TestUtils.createBaseEpByPortIid(port.getUuid()));
        DataStoreHelper.submitToDs(wTx);
        // manual delay for max 5s
        for (int i = 0; i < 50; i++) {
            if (eventCount >= 2) {
                break;
            }
            Thread.sleep(100);
        }
        assertEquals(Integer.valueOf(2), eventCount);
        registerDataTreeChangeListener.close();
        neutronListener.close();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<VppEndpoint>> changes) {
        for (DataTreeModification<VppEndpoint> change : changes) {
            DataObjectModification<VppEndpoint> vppEpChange = change.getRootNode();
            ModificationType modType = vppEpChange.getModificationType();
            if (modType.equals(ModificationType.WRITE)) {
                assertNull(vppEpChange.getDataBefore());
                assertNotNull(vppEpChange.getDataAfter());
                eventCount++;
            } else if (modType.equals(ModificationType.DELETE)) {
                assertNotNull(vppEpChange.getDataBefore());
                assertNull(vppEpChange.getDataAfter());
                eventCount++;
            }
        }
    }

    private ListenerRegistration<NeutronListenerTest> registerVppEpListener() {
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class).build()), this);
    }
}
