/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.ContainmentEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.NetworkDomain;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class BaseEndpointServiceImplTest extends CustomDataBrokerTest {

    private static final String MAC_ADDRESS = "01:23:45:67:89:AB";
    private static final String IP_ADDRESS = "192.168.100.1/24";
    private static final String TENANT = "admin";
    private static final String DOMAIN = "test.domain";
    private static final String CONTEXT_ID = "testContext";
    private static final long timestamp = 1234567890L;
    private static final NetworkDomainContainment networkDomainContainment = new NetworkDomainContainmentBuilder()
        .setNetworkDomainId(new NetworkDomainId(DOMAIN)).setNetworkDomainType(NetworkDomain.class).build();
    private static final ParentEndpoint BASE_L3_PARENT = new ParentEndpointBuilder().setAddress(IP_ADDRESS)
        .setAddressType(AddressType.class)
        .setContextId(new ContextId(CONTEXT_ID))
        .setContextType(ContextType.class)
        .build();
    private static final ChildEndpoint BASE_L2_CHILD = new ChildEndpointBuilder().setAddress(MAC_ADDRESS)
        .setAddressType(AddressType.class)
        .setContextId(new ContextId(CONTEXT_ID))
        .setContextType(ContextType.class)
        .build();
    private static final ParentContainmentEndpoint BASE_CONT_PARENT = new ParentContainmentEndpointBuilder()
        .setContextId(new ContextId(CONTEXT_ID)).setContextType(ContextType.class).build();

    private DataBroker dataProvider;
    private BaseEndpointServiceImpl baseEndpointRpcRegistry;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.of(Endpoints.class, AddressEndpoints.class, ContainmentEndpoints.class);
    }

    @Before
    public void init() {
        dataProvider = getDataBroker();

        baseEndpointRpcRegistry =
                new BaseEndpointServiceImpl(dataProvider, new EndpointAugmentorRegistryImpl());
    }

    @Test
    public void testConstructor() throws Exception {
        BaseEndpointServiceImpl registry =
                new BaseEndpointServiceImpl(dataProvider, new EndpointAugmentorRegistryImpl());
        registry.close();
    }

    @Test
    public void testRegisterEndpoint() throws Exception {
        AddressEndpoint l2EpWithL3Parent =
                createBaseL2EpBuilder()
                    .setParentEndpointChoice(
                            new ParentEndpointCaseBuilder().setParentEndpoint(Arrays.asList(BASE_L3_PARENT)).build())
                    .build();
        AddressEndpoint l3EpWithL2Child =
                createBaseL3EpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        ContainmentEndpoint contEp = createBaseContEpBuilder().build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l2EpWithL3Parent, l3EpWithL2Child);
        setContEpsToBuilder(inputBuilder, contEp);

        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(l2EpWithL3Parent.getKey()), rTx);
        Assert.assertTrue(addressEndpointL2.isPresent());
        Assert.assertEquals(l2EpWithL3Parent, addressEndpointL2.get());

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(l3EpWithL2Child.getKey()), rTx);
        Assert.assertTrue(addressEndpointL3.isPresent());
        Assert.assertEquals(l3EpWithL2Child, addressEndpointL3.get());

        Optional<ContainmentEndpoint> ContainmentEndpoint = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.containmentEndpointIid(contEp.getKey()), rTx);
        Assert.assertTrue(ContainmentEndpoint.isPresent());
        Assert.assertEquals(contEp, ContainmentEndpoint.get());
    }

    @Test
    public void testRegisterEndpoint_withParentUpdate() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        AddressEndpoint baseL3Ep = createBaseL3EpBuilder().build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(baseL3Ep.getKey()), baseL3Ep);
        wTx.submit().get();
        AddressEndpoint l2EpWithL3Parent =
                createBaseL2EpBuilder()
                    .setParentEndpointChoice(
                            new ParentEndpointCaseBuilder().setParentEndpoint(Arrays.asList(BASE_L3_PARENT)).build())
                    .build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l2EpWithL3Parent);

        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(l2EpWithL3Parent.getKey()), rTx);
        Assert.assertTrue(addressEndpointL2.isPresent());
        List<ParentEndpoint> parentEndpoints =
                EndpointUtils.getParentEndpoints(addressEndpointL2.get().getParentEndpointChoice());
        Assert.assertEquals(1, parentEndpoints.size());

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(baseL3Ep.getKey()), rTx);
        Assert.assertTrue(addressEndpointL3.isPresent());
        Assert.assertEquals(1, addressEndpointL3.get().getChildEndpoint().size());
    }

    @Test
    public void testRegisterEndpoint_withChildUpdate() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        AddressEndpoint baseL2Ep = createBaseL2EpBuilder().build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(baseL2Ep.getKey()), baseL2Ep);
        wTx.submit().get();
        AddressEndpoint l3EpWithL2Child =
                createBaseL3EpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l3EpWithL2Child);

        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<AddressEndpoint> addressEndpointL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(baseL2Ep.getKey()), rTx);
        Assert.assertTrue(addressEndpointL2.isPresent());
        List<ParentEndpoint> parentEndpoints =
                EndpointUtils.getParentEndpoints(addressEndpointL2.get().getParentEndpointChoice());
        Assert.assertEquals(1, parentEndpoints.size());

        Optional<AddressEndpoint> addressEndpointL3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(l3EpWithL2Child.getKey()), rTx);
        Assert.assertTrue(addressEndpointL3.isPresent());
        Assert.assertEquals(1, addressEndpointL3.get().getChildEndpoint().size());
    }

    @Test
    public void testRegisterEndpoint_rpcInput_missing_child() throws Exception {
        AddressEndpoint l2EpWithL3Parent =
                createBaseL2EpBuilder()
                    .setParentEndpointChoice(
                            new ParentEndpointCaseBuilder().setParentEndpoint(Arrays.asList(BASE_L3_PARENT)).build())
                    .build();
        AddressEndpoint l3EpWithL2Child = createBaseL3EpBuilder().setChildEndpoint(Arrays.asList()).build();
        ContainmentEndpoint contEp = createBaseContEpBuilder().build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l2EpWithL3Parent, l3EpWithL2Child);
        setContEpsToBuilder(inputBuilder, contEp);

        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNotNull(rpcResult.getErrors());
        Assert.assertEquals(1, rpcResult.getErrors().size());
    }

    @Test
    public void testRegisterEndpoint_rpcInput_missingParent() throws Exception {
        AddressEndpoint l2EpWithL3Parent = createBaseL2EpBuilder().build();
        AddressEndpoint l3EpWithL2Child =
                createBaseL3EpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        ContainmentEndpoint contEp = createBaseContEpBuilder().build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l2EpWithL3Parent, l3EpWithL2Child);
        setContEpsToBuilder(inputBuilder, contEp);

        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNotNull(rpcResult.getErrors());
        Assert.assertEquals(1, rpcResult.getErrors().size());
    }

    @Test
    public void testUnregisterEndpoint_withParent() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        AddressEndpoint l2EpWithL3EpParent =
                createBaseL2EpBuilder()
                    .setParentEndpointChoice(
                            new ParentEndpointCaseBuilder().setParentEndpoint(Arrays.asList(BASE_L3_PARENT)).build())
                    .build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithL3EpParent.getKey()),
                l2EpWithL3EpParent);
        AddressEndpoint l3EpWithL2EpChild =
                createBaseL3EpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l3EpWithL2EpChild.getKey()),
                l3EpWithL2EpChild);
        wTx.submit().get();

        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l2EpWithL3EpParent);
        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.unregisterEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<AddressEndpoint> l2EpWithL3EpParentFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithL3EpParent.getKey()), rTx);
        Assert.assertFalse(l2EpWithL3EpParentFromDsOptional.isPresent());

        Optional<AddressEndpoint> l3EpWithL2EpChildFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l3EpWithL2EpChild.getKey()), rTx);
        Assert.assertTrue(l3EpWithL2EpChildFromDsOptional.isPresent());
        Assert.assertEquals(0, l3EpWithL2EpChildFromDsOptional.get().getChildEndpoint().size());
    }

    @Test
    public void testUnregisterEndpoint_withChild() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        AddressEndpoint l2EpWithL3EpParent =
                createBaseL2EpBuilder()
                    .setParentEndpointChoice(
                            new ParentEndpointCaseBuilder().setParentEndpoint(Arrays.asList(BASE_L3_PARENT)).build())
                    .build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithL3EpParent.getKey()),
                l2EpWithL3EpParent);
        AddressEndpoint l3EpWithL2EpChild =
                createBaseL3EpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l3EpWithL2EpChild.getKey()),
                l3EpWithL2EpChild);
        wTx.submit().get();

        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        setAddrEpsToBuilder(inputBuilder, l3EpWithL2EpChild);
        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.unregisterEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<AddressEndpoint> l3EpWithL2EpChildFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l3EpWithL2EpChild.getKey()), rTx);
        Assert.assertFalse(l3EpWithL2EpChildFromDsOptional.isPresent());

        Optional<AddressEndpoint> l2EpWithL3EpParentFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithL3EpParent.getKey()), rTx);
        Assert.assertTrue(l2EpWithL3EpParentFromDsOptional.isPresent());
        Assert.assertEquals(0, EndpointUtils
            .getParentEndpoints(l2EpWithL3EpParentFromDsOptional.get().getParentEndpointChoice()).size());
    }

    @Test
    public void testRegisterEndpoint_containmentEndpointWithChildFail() throws Exception {
        ContainmentEndpoint contEpWithChild =
                createBaseContEpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder();
        setContEpsToBuilder(inputBuilder, contEpWithChild);
        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.registerEndpoint(inputBuilder.build()).get();
        Assert.assertFalse(rpcResult.isSuccessful());
        Assert.assertNull(rpcResult.getResult());
        Assert.assertEquals(1, rpcResult.getErrors().size());
    }

    @Test
    public void testUnregisterEndpoint_containmentEndpointWithChild() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        ContainmentEndpoint contEpWithL2EpChild =
                createBaseContEpBuilder().setChildEndpoint(Arrays.asList(BASE_L2_CHILD)).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(contEpWithL2EpChild.getKey()),
                contEpWithL2EpChild);
        AddressEndpoint l2EpWithContEpParent =
                createBaseL2EpBuilder().setParentEndpointChoice(new ParentContainmentEndpointCaseBuilder()
                    .setParentContainmentEndpoint(Arrays.asList(BASE_CONT_PARENT)).build()).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithContEpParent.getKey()),
                l2EpWithContEpParent);
        wTx.submit().get();

        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        setContEpsToBuilder(inputBuilder, contEpWithL2EpChild);
        RpcResult<Void> rpcResult = baseEndpointRpcRegistry.unregisterEndpoint(inputBuilder.build()).get();
        Assert.assertTrue(rpcResult.isSuccessful());

        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<ContainmentEndpoint> contEpWithL2EpChildFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(contEpWithL2EpChild.getKey()), rTx);
        Assert.assertFalse(contEpWithL2EpChildFromDsOptional.isPresent());

        Optional<AddressEndpoint> l2EpWithContEpParentFromDsOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(l2EpWithContEpParent.getKey()), rTx);
        Assert.assertTrue(l2EpWithContEpParentFromDsOptional.isPresent());
        Assert.assertEquals(0, EndpointUtils
            .getParentEndpoints(l2EpWithContEpParentFromDsOptional.get().getParentEndpointChoice()).size());
    }

    private void setAddrEpsToBuilder(RegisterEndpointInputBuilder builder, AddressEndpoint... addrEndpoints) {
        List<AddressEndpointReg> addrEpRegs = new ArrayList<>();
        for (AddressEndpoint addrEp : addrEndpoints) {
            addrEpRegs.add(new AddressEndpointRegBuilder().setTimestamp(timestamp)
                .setContextId(addrEp.getContextId())
                .setContextType(addrEp.getContextType())
                .setTenant(addrEp.getTenant())
                .setAddress(addrEp.getAddress())
                .setAddressType(addrEp.getAddressType())
                .setNetworkContainment(addrEp.getNetworkContainment())
                .setTimestamp(addrEp.getTimestamp())
                .setParentEndpointChoice(addrEp.getParentEndpointChoice())
                .setChildEndpoint(addrEp.getChildEndpoint())
                .setEndpointGroup(addrEp.getEndpointGroup())
                .build());
        }
        builder.setAddressEndpointReg(addrEpRegs);
    }

    private void setContEpsToBuilder(RegisterEndpointInputBuilder builder, ContainmentEndpoint... contEndpoints) {
        List<ContainmentEndpointReg> contEpRegs = new ArrayList<>();
        for (ContainmentEndpoint contEp : contEndpoints) {
            contEpRegs.add(new ContainmentEndpointRegBuilder().setTimestamp(contEp.getTimestamp())
                .setContextId(contEp.getContextId())
                .setContextType(contEp.getContextType())
                .setTenant(contEp.getTenant())
                .setNetworkContainment(contEp.getNetworkContainment())
                .setChildEndpoint(contEp.getChildEndpoint())
                .setEndpointGroup(contEp.getEndpointGroup())
                .build());
        }
        builder.setContainmentEndpointReg(contEpRegs);
    }

    private void setAddrEpsToBuilder(UnregisterEndpointInputBuilder builder, AddressEndpoint... addrEndpoints) {
        List<AddressEndpointUnreg> addrEpUnregs = new ArrayList<>();
        for (AddressEndpoint addrEp : addrEndpoints) {
            addrEpUnregs.add(new AddressEndpointUnregBuilder().setContextId(addrEp.getContextId())
                .setContextType(addrEp.getContextType())
                .setAddress(addrEp.getAddress())
                .setAddressType(addrEp.getAddressType())
                .build());
        }
        builder.setAddressEndpointUnreg(addrEpUnregs);
    }

    private void setContEpsToBuilder(UnregisterEndpointInputBuilder builder, ContainmentEndpoint... contEndpoints) {
        List<ContainmentEndpointUnreg> contEpUnregs = new ArrayList<>();
        for (ContainmentEndpoint contEp : contEndpoints) {
            contEpUnregs.add(new ContainmentEndpointUnregBuilder().setContextId(contEp.getContextId())
                .setContextType(contEp.getContextType())
                .build());
        }
        builder.setContainmentEndpointUnreg(contEpUnregs);
    }

    private AddressEndpointBuilder createBaseL2EpBuilder() {
        return new AddressEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(ContextType.class)
            .setTenant(new TenantId(TENANT))
            .setAddress(MAC_ADDRESS)
            .setAddressType(AddressType.class)
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setTimestamp(timestamp)
            .setEndpointGroup(Collections.singletonList(new EndpointGroupId("testEPGID")));
    }

    private AddressEndpointBuilder createBaseL3EpBuilder() {
        return new AddressEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(ContextType.class)
            .setTenant(new TenantId(TENANT))
            .setAddress(IP_ADDRESS)
            .setAddressType(AddressType.class)
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setTimestamp(timestamp)
            .setEndpointGroup(Collections.singletonList(new EndpointGroupId("testEPGID")));
    }

    private ContainmentEndpointBuilder createBaseContEpBuilder() {
        return new ContainmentEndpointBuilder().setTimestamp(timestamp)
            .setContextId(new ContextId(CONTEXT_ID))
            .setContextType(ContextType.class)
            .setTenant(new TenantId(TENANT))
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(networkDomainContainment).build())
            .setEndpointGroup(Collections.singletonList(new EndpointGroupId("testEPGID")));
    }
}
