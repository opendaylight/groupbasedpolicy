/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentation;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.ContainmentEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

public class BaseEndpointRpcRegistryTest extends CustomDataBrokerTest {

    private static final String MAC_ADDRESS = "01:23:45:67:89:AB";
    private static final String IP_ADDRESS = "192.168.100.1/24";
    private static final String TENANT = "admin";
    private static final String DOMAIN = "test.domain";
    private static final String CONTEXT_ID = "testContext";
    private static final long timestamp = 1234567890L;

    private enum AddressEndpointRegistration {
        CHILD, PARENT, BOTH, NONE
    }
    private enum AddressEndpointType {
        L2, L3, BOTH, NONE
    }

    private DataBroker dataProvider;
    private BaseEndpointRendererAugmentation baseEndpointRendererAugmentation;
    private BaseEndpointRpcRegistry baseEndpointRpcRegistry;
    private AddressEndpoint basel2Ep;
    private AddressEndpoint basel3Ep;
    private ContainmentEndpoint baseContainmentEp;
    private ParentEndpoint basel3Parent;
    private ChildEndpoint basel2Child;

    @Nonnull
    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.of(Endpoints.class, AddressEndpoints.class, ContainmentEndpoints.class,
                MacAddressType.class, IpPrefixType.class);
    }

    @Before
    public void init() {
        dataProvider = getDataBroker();
        baseEndpointRendererAugmentation = mock(BaseEndpointRendererAugmentation.class);
        RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);

        baseEndpointRpcRegistry = new BaseEndpointRpcRegistry(dataProvider, rpcRegistry);

        NetworkDomainContainment
            networkDomainContainment =
            new NetworkDomainContainmentBuilder().setNetworkDomainId(new NetworkDomainId(DOMAIN)).setNetworkDomainType(
                Subnet.class).build();

        basel3Parent = new ParentEndpointBuilder().setAddress(IP_ADDRESS)
            .setAddressType(IpPrefixType.class)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L3Context.class)
            .build();

        basel2Child = new ChildEndpointBuilder().setAddress(MAC_ADDRESS)
            .setAddressType(MacAddressType.class)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L2FloodDomain.class)
            .build();

        basel2Ep = new AddressEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L2FloodDomain.class)
            .setTenant(new TenantId(TENANT))
            .setAddress(MAC_ADDRESS)
            .setAddressType(MacAddressType.class)
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setTimestamp(timestamp)
            .setParentEndpointChoice(
                    new ParentEndpointCaseBuilder().setParentEndpoint(Collections.singletonList(basel3Parent)).build())
            .build();

        basel3Ep = new AddressEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L3Context.class)
            .setTenant(new TenantId(TENANT))
            .setAddress(IP_ADDRESS)
            .setAddressType(IpPrefixType.class)
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setTimestamp(timestamp)
            .setChildEndpoint(Collections.singletonList(basel2Child))
            .build();

        baseContainmentEp = new ContainmentEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L2FloodDomain.class)
            .setTenant(new TenantId(TENANT))
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setChildEndpoint(Collections.singletonList(basel2Child))
            .build();
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
        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH, AddressEndpointType.BOTH, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        ReadOnlyTransaction transaction = dataProvider.newReadOnlyTransaction();

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
                L2FloodDomain.class);
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), transaction);

        Assert.assertTrue(addressEndpointL2.isPresent());

        if (addressEndpointL2.isPresent()) {
            Assert.assertEquals(basel2Ep, addressEndpointL2.get());
        }

        key = new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, new ContextId(CONTEXT_ID), L3Context.class);

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), transaction);

        Assert.assertTrue(addressEndpointL3.isPresent());

        if (addressEndpointL3.isPresent()) {
            Assert.assertEquals(basel3Ep, addressEndpointL3.get());
        }

        ContainmentEndpointKey containmentEndpointKey =
                new ContainmentEndpointKey(new ContextId(CONTEXT_ID), L2FloodDomain.class);

        Optional<ContainmentEndpoint> ContainmentEndpoint = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.containmentEndpointIid(containmentEndpointKey), transaction);

        Assert.assertTrue(ContainmentEndpoint.isPresent());

        if (ContainmentEndpoint.isPresent()) {
            Assert.assertEquals(baseContainmentEp, ContainmentEndpoint.get());
        }
    }

    @Test
    public void testRegisterEndpointWithParentUpdate() throws Exception {
        setupBasicDataStore();
        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH, AddressEndpointType.L2, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
                L2FloodDomain.class);
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());

        if (addressEndpointL2.isPresent()) {
            ParentEndpointCase parentEndpointCase =
                    (ParentEndpointCase) addressEndpointL2.get().getParentEndpointChoice();
            List<ParentEndpoint> parentEndpoints = parentEndpointCase.getParentEndpoint();
            Assert.assertEquals(parentEndpoints.size(), 1);
        }

        key = new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, new ContextId(CONTEXT_ID), L3Context.class);

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL3.isPresent());

        if (addressEndpointL3.isPresent()) {
            Assert.assertEquals(addressEndpointL3.get().getChildEndpoint().size(), 1);
        }
    }

    @Test
    public void testRegisterEndpointWithParentUpdateFail() throws Exception {
        RegisterEndpointInput input = createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH,
                AddressEndpointType.L2, true);

        Future<RpcResult<Void>> rpcResultFuture = baseEndpointRpcRegistry.registerEndpoint(input);

        Assert.assertFalse(rpcResultFuture.get().isSuccessful());
        Assert.assertNotNull(rpcResultFuture.get().getErrors());
        Assert.assertEquals(rpcResultFuture.get().getErrors().size(), 1);
    }

    @Test
    public void testRegisterEndpointWithChildUpdate() throws Exception {
        setupBasicDataStore();
        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH, AddressEndpointType.L3, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
                L2FloodDomain.class);
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());

        if (addressEndpointL2.isPresent()) {
            ParentEndpointCase parentEndpointCase =
                    (ParentEndpointCase) addressEndpointL2.get().getParentEndpointChoice();
            List<ParentEndpoint> parentEndpoints = parentEndpointCase.getParentEndpoint();
            Assert.assertEquals(parentEndpoints.size(), 1);
        }

        key = new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, new ContextId(CONTEXT_ID), L3Context.class);

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL3.isPresent());

        if (addressEndpointL3.isPresent()) {
            Assert.assertEquals(addressEndpointL3.get().getChildEndpoint().size(), 1);
        }
    }

    @Test
    public void testRegisterEndpointWithChildUpdateFail() throws Exception {
        RegisterEndpointInput input = createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH,
                AddressEndpointType.L3, true);

        Future<RpcResult<Void>> rpcResultFuture = baseEndpointRpcRegistry.registerEndpoint(input);

        Assert.assertFalse(rpcResultFuture.get().isSuccessful());
        Assert.assertNotNull(rpcResultFuture.get().getErrors());
        Assert.assertEquals(rpcResultFuture.get().getErrors().size(), 1);
    }

    private void setupBasicDataStore() throws Exception {
        InstanceIdentifier<Endpoints> id = InstanceIdentifier.builder(Endpoints.class).build();
        dataProvider.newWriteOnlyTransaction().delete(LogicalDatastoreType.OPERATIONAL, id);

        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.NONE, AddressEndpointType.BOTH, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
                L2FloodDomain.class);
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());

        if (addressEndpointL2.isPresent()) {
            ParentEndpointCase parentEndpointCase =
                    (ParentEndpointCase) addressEndpointL2.get().getParentEndpointChoice();
            Assert.assertEquals(parentEndpointCase.getParentEndpoint().size(), 0);
        }

        key = new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, new ContextId(CONTEXT_ID), L3Context.class);

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL3.isPresent());

        if (addressEndpointL3.isPresent()) {
            Assert.assertEquals(addressEndpointL3.get().getChildEndpoint().size(), 0);
        }
    }

    @Test
    public void testRegisterEndpointParentFail() throws Exception {
        Future<RpcResult<Void>> rpcResultFuture =
                baseEndpointRpcRegistry.registerEndpoint(createRegisterEndpointInputVariablesForTest(
                        AddressEndpointRegistration.PARENT, AddressEndpointType.BOTH, true));

        RpcResult<Void> rpcResult = rpcResultFuture.get();

        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNull(rpcResult.getResult());
        Assert.assertEquals(rpcResult.getErrors().size(), 1);

    }

    @Test
    public void testRegisterEndpointChildFail() throws Exception {
        Future<RpcResult<Void>> rpcResultFuture =
                baseEndpointRpcRegistry.registerEndpoint(createRegisterEndpointInputVariablesForTest(
                        AddressEndpointRegistration.CHILD, AddressEndpointType.BOTH, true));

        RpcResult<Void> rpcResult = rpcResultFuture.get();

        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNull(rpcResult.getResult());
        Assert.assertEquals(rpcResult.getErrors().size(), 1);
    }

    @Test
    public void testUnregisterEndpointWithParent() throws Exception {
        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH, AddressEndpointType.BOTH, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
                L2FloodDomain.class);
        ContainmentEndpointKey cKey = new ContainmentEndpointKey(new ContextId(CONTEXT_ID), L2FloodDomain.class);

        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());

        if (addressEndpointL2.isPresent()) {
            Assert.assertEquals(basel2Ep, addressEndpointL2.get());
        }

        Optional<ContainmentEndpoint> ContainmentEndpoint = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.containmentEndpointIid(cKey), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(ContainmentEndpoint.isPresent());

        if (ContainmentEndpoint.isPresent()) {
            Assert.assertEquals(baseContainmentEp, ContainmentEndpoint.get());
        }

        baseEndpointRpcRegistry.unregisterEndpoint(unregisterEndpointInputParent());
        Optional<AddressEndpoint> endpointOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertFalse(endpointOptional.isPresent());

        Optional<ContainmentEndpoint> containmentEndpointOptional =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(cKey),
                        dataProvider.newReadOnlyTransaction());

        Assert.assertFalse(containmentEndpointOptional.isPresent());
    }

    @Test
    public void testUnregisterEndpointWithChild() throws Exception {
        RegisterEndpointInput input =
                createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.BOTH, AddressEndpointType.BOTH, true);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key =
                new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, new ContextId(CONTEXT_ID), L3Context.class);

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL3.isPresent());

        if (addressEndpointL3.isPresent()) {
            Assert.assertEquals(basel3Ep, addressEndpointL3.get());
        }

        baseEndpointRpcRegistry.unregisterEndpoint(unregisterEndpointInputChild());
        Optional<AddressEndpoint> endpointOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertFalse(endpointOptional.isPresent());

    }

    @Test
    public void testRegisterContainmentEndpointWithChildFail() throws Exception {
        Future<RpcResult<Void>> rpcResultFuture =
                baseEndpointRpcRegistry.registerEndpoint(createRegisterEndpointInputVariablesForTest(
                        AddressEndpointRegistration.NONE, AddressEndpointType.NONE, true));

        RpcResult<Void> rpcResult = rpcResultFuture.get();

        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNull(rpcResult.getResult());
        Assert.assertEquals(rpcResult.getErrors().size(), 1);
    }

    @Test
    public void testUnregisterContainmentEndpointWithChild() throws Exception {
        InstanceIdentifier<Endpoints> id = InstanceIdentifier.builder(Endpoints.class).build();
        dataProvider.newWriteOnlyTransaction().delete(LogicalDatastoreType.OPERATIONAL, id);

        RegisterEndpointInput input =
            createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration.NONE, AddressEndpointType.L2, false);

        baseEndpointRpcRegistry.registerEndpoint(input);

        AddressEndpointKey key = new AddressEndpointKey(MAC_ADDRESS, MacAddressType.class, new ContextId(CONTEXT_ID),
            L2FloodDomain.class);

        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
            IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());
        if (addressEndpointL2.isPresent()){
            ParentEndpointChoice parentEndpointChoice = addressEndpointL2.get().getParentEndpointChoice();
            if(parentEndpointChoice instanceof ParentContainmentEndpointCase){
                ParentContainmentEndpointCase
                    parentEndpointCase = (ParentContainmentEndpointCase) parentEndpointChoice;
                Assert.assertNull(parentEndpointCase.getParentContainmentEndpoint());
            }
        }

        ContainmentEndpointReg containmentEndpointReg = new ContainmentEndpointRegBuilder().setTimestamp(baseContainmentEp.getTimestamp())
            .setContextId(baseContainmentEp.getContextId())
            .setContextType(baseContainmentEp.getContextType())
            .setTenant(baseContainmentEp.getTenant())
            .setNetworkContainment(baseContainmentEp.getNetworkContainment())
            .setChildEndpoint(Collections.singletonList(basel2Child))
            .build();

        baseEndpointRpcRegistry.registerEndpoint(new RegisterEndpointInputBuilder().setContainmentEndpointReg(Collections.singletonList(containmentEndpointReg)).build());

        addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
            IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());
        if (addressEndpointL2.isPresent()) {
            ParentEndpointChoice parentEndpointChoice = addressEndpointL2.get().getParentEndpointChoice();
            if(parentEndpointChoice instanceof ParentContainmentEndpointCase){
                ParentContainmentEndpointCase
                    parentEndpointCase = (ParentContainmentEndpointCase) parentEndpointChoice;
                Assert.assertNotNull(parentEndpointCase.getParentContainmentEndpoint());
                Assert.assertEquals(parentEndpointCase.getParentContainmentEndpoint().size(),1);
            }
        }

        ContainmentEndpointUnreg containmentEndpointUnreg = new ContainmentEndpointUnregBuilder()
            .setContextId(baseContainmentEp.getContextId())
            .setContextType(baseContainmentEp.getContextType())
            .build();

        baseEndpointRpcRegistry.unregisterEndpoint(new UnregisterEndpointInputBuilder().setContainmentEndpointUnreg(Collections.singletonList(containmentEndpointUnreg)).build());

        addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
            IidFactory.addressEndpointIid(key), dataProvider.newReadOnlyTransaction());

        Assert.assertTrue(addressEndpointL2.isPresent());
        if (addressEndpointL2.isPresent()) {
            ParentEndpointChoice parentEndpointChoice = addressEndpointL2.get().getParentEndpointChoice();
            if(parentEndpointChoice instanceof ParentContainmentEndpointCase){
                ParentContainmentEndpointCase
                    parentEndpointCase = (ParentContainmentEndpointCase) parentEndpointChoice;
                Assert.assertNotNull(parentEndpointCase.getParentContainmentEndpoint());
                Assert.assertEquals(parentEndpointCase.getParentContainmentEndpoint().size(),0);
            }
        }

    }

    private UnregisterEndpointInput unregisterEndpointInputParent() {
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

    private UnregisterEndpointInput unregisterEndpointInputChild() {
        UnregisterEndpointInputBuilder builder = new UnregisterEndpointInputBuilder();

        builder.setAddressEndpointUnreg(new ArrayList<>());
        builder.setContainmentEndpointUnreg(new ArrayList<>());

        builder.getAddressEndpointUnreg().add(new AddressEndpointUnregBuilder().setContextId(new ContextId(CONTEXT_ID))
            .setContextType(L3Context.class)
            .setAddress(IP_ADDRESS)
            .setAddressType(IpPrefixType.class)
            .build());

        return builder.build();
    }

    private RegisterEndpointInput createRegisterEndpointInputVariablesForTest(AddressEndpointRegistration registration,
            AddressEndpointType type, boolean containmentEpPresent) throws Exception {
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        long timestamp = System.currentTimeMillis();

        List<ParentEndpoint> parentEndpoints = new ArrayList<>();
        if (registration == AddressEndpointRegistration.BOTH || registration == AddressEndpointRegistration.PARENT) {
            parentEndpoints.add(basel3Parent);
        }

        List<ChildEndpoint> childEndpoints = new ArrayList<>();
        if (registration == AddressEndpointRegistration.BOTH || registration == AddressEndpointRegistration.CHILD) {
            childEndpoints.add(basel2Child);
        }

        registerEndpointInputBuilder.setAddressEndpointReg(new ArrayList<>());
        registerEndpointInputBuilder.setContainmentEndpointReg(new ArrayList<>());

        if (type == AddressEndpointType.BOTH || type == AddressEndpointType.L2) {
            registerEndpointInputBuilder.getAddressEndpointReg()
                .add(new AddressEndpointRegBuilder().setTimestamp(timestamp)
                    .setContextId(basel2Ep.getContextId())
                    .setContextType(basel2Ep.getContextType())
                    .setTenant(basel2Ep.getTenant())
                    .setAddress(basel2Ep.getAddress())
                    .setAddressType(basel2Ep.getAddressType())
                    .setNetworkContainment(basel2Ep.getNetworkContainment())
                    .setTimestamp(basel2Ep.getTimestamp())
                    .setParentEndpointChoice(new ParentEndpointCaseBuilder().setParentEndpoint(parentEndpoints).build())
                    .build());
        }

        if (type == AddressEndpointType.BOTH || type == AddressEndpointType.L3) {
            registerEndpointInputBuilder.getAddressEndpointReg()
                .add(new AddressEndpointRegBuilder().setContextId(basel3Ep.getContextId())
                    .setContextType(basel3Ep.getContextType())
                    .setTenant(basel3Ep.getTenant())
                    .setAddress(basel3Ep.getAddress())
                    .setAddressType(basel3Ep.getAddressType())
                    .setNetworkContainment(basel3Ep.getNetworkContainment())
                    .setTimestamp(basel3Ep.getTimestamp())
                    .setChildEndpoint(childEndpoints)
                    .build());
        }

        if(containmentEpPresent) {
            registerEndpointInputBuilder.getContainmentEndpointReg()
                .add(new ContainmentEndpointRegBuilder().setTimestamp(baseContainmentEp.getTimestamp())
                    .setContextId(baseContainmentEp.getContextId())
                    .setContextType(baseContainmentEp.getContextType())
                    .setTenant(baseContainmentEp.getTenant())
                    .setNetworkContainment(baseContainmentEp.getNetworkContainment())
                    .setChildEndpoint(Collections.singletonList(basel2Child))
                    .build());
        }

        return registerEndpointInputBuilder.build();
    }
}
