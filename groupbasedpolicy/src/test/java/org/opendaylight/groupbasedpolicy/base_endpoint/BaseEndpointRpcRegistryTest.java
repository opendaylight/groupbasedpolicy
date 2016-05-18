/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import com.google.common.util.concurrent.CheckedFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentation;
import org.opendaylight.groupbasedpolicy.base_endpoint.BaseEndpointRpcRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class BaseEndpointRpcRegistryTest {

    private static final String MAC_ADDRESS = "01:23:45:67:89:AB";
    private static final String TENANT = "admin";
    private static final String DOMAIN = "test.domain";
    private static final String CONTEXT_ID = "testContext";
    private static final String FLOOD_DOMAIN = "testFloodDomain";

    private DataBroker dataProvider;
    private BaseEndpointRendererAugmentation baseEndpointRendererAugmentation;
    private BaseEndpointRpcRegistry baseEndpointRpcRegistry;

    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        baseEndpointRendererAugmentation = mock(BaseEndpointRendererAugmentation.class);
        WriteTransaction wt = newWriteTransactionMock();
        RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);

        baseEndpointRpcRegistry = new BaseEndpointRpcRegistry(dataProvider, rpcRegistry);
    }

    @Test
    public void testConstructor() throws Exception {
        RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);
        BaseEndpointRpcRegistry registry = new BaseEndpointRpcRegistry(dataProvider, rpcRegistry);
        registry.close();
    }

    @Test
    public void testRegister() throws Exception {
        baseEndpointRpcRegistry.register(baseEndpointRendererAugmentation);
        Assert.assertEquals(1, BaseEndpointRpcRegistry.registeredRenderers.size());

        baseEndpointRpcRegistry.unregister(baseEndpointRendererAugmentation);
        Assert.assertEquals(0, BaseEndpointRpcRegistry.registeredRenderers.size());
    }

    @Test
    public void testRegisterEndpoint() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        baseEndpointRpcRegistry.registerEndpoint(createRegisterEndpointInputVariablesForTest());

        verify(wt, times(2)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(DataObject.class), eq(true));
    }

    private RegisterEndpointInput createRegisterEndpointInputVariablesForTest() throws Exception {
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        long timestamp = System.currentTimeMillis();

        registerEndpointInputBuilder.setAddressEndpointReg(new ArrayList<>());
        registerEndpointInputBuilder.setContainmentEndpointReg(new ArrayList<>());

        registerEndpointInputBuilder.getAddressEndpointReg().add(
                new AddressEndpointRegBuilder().setTimestamp(timestamp)
                    .setContextId(new ContextId(CONTEXT_ID))
                    .setContextType(L2FloodDomain.class)
                    .setTenant(new TenantId(TENANT))
                    .setAddress(MAC_ADDRESS)
                    .setAddressType(MacAddressType.class)
                    .setAddressType(AddressType.class)
                    .setNetworkContainment(new NetworkContainmentBuilder().setContainment(new NetworkDomainContainmentBuilder()
                        .setNetworkDomainId(new NetworkDomainId(DOMAIN)).setNetworkDomainType(Subnet.class).build()).build())
                    .setKey(new AddressEndpointRegKey(MAC_ADDRESS,MacAddressType.class, new ContextId(CONTEXT_ID), L2FloodDomain.class))
                    .setTimestamp(timestamp).build());

        registerEndpointInputBuilder.getContainmentEndpointReg().add(
                new ContainmentEndpointRegBuilder()
                    .setTimestamp(timestamp)
                    .setContextId(new L2FloodDomainId(FLOOD_DOMAIN))
                    .setContextType(L2FloodDomain.class)
                    .setTenant(new TenantId(TENANT))
                    .setNetworkContainment(new NetworkContainmentBuilder().setContainment(new NetworkDomainContainmentBuilder()
                        .setNetworkDomainId(new NetworkDomainId(DOMAIN)).setNetworkDomainType(Subnet.class).build()).build())
                    .setKey(new ContainmentEndpointRegKey(new L2FloodDomainId(FLOOD_DOMAIN),L2FloodDomain.class))
                    .build());

        return registerEndpointInputBuilder.build();
    }

    @Test
    public void testUnregisterEndpoint() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        UnregisterEndpointInput unregisterEndpointInput = unregisterEndpointInput();

        baseEndpointRpcRegistry.unregisterEndpoint(unregisterEndpointInput);

        verify(wt, times(2)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    private UnregisterEndpointInput unregisterEndpointInput() {
        UnregisterEndpointInputBuilder builder = new UnregisterEndpointInputBuilder();

        builder.setAddressEndpointUnreg(new ArrayList<>());
        builder.setContainmentEndpointUnreg(new ArrayList<>());

        builder.getAddressEndpointUnreg().add(new AddressEndpointUnregBuilder().setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L2FloodDomain.class)
            .setAddress(MAC_ADDRESS)
            .setAddressType(MacAddressType.class)
            .build());

        builder.getContainmentEndpointUnreg().add(new ContainmentEndpointUnregBuilder()
            .setContextId(new ContextId(CONTEXT_ID)).setContextType(L2FloodDomain.class).build());

        return builder.build();
    }

    private WriteTransaction newWriteTransactionMock() {
        WriteTransaction wt = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> f = mock(CheckedFuture.class);

        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wt);
        when(wt.submit()).thenReturn(f);
        return wt;
    }

}
