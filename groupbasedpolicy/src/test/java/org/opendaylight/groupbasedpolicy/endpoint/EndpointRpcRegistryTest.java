package org.opendaylight.groupbasedpolicy.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3PrefixBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointRpcRegistryTest {

    private static final String L2_BD_ID = "l2bdId";
    private static final String MAC_ADDRESS = "5E:83:39:98:4F:1B";
    private static final String L3_CX_ID = "l3c";
    private static final String IPv4_ADDRESS = "1.1.1.1";
    private static final String IPv4_PREFIX = "1.1.1.0/24";
    private static final String TENANT_ID = "t1";
    private static final String EPG_ID = "epg_1";
    private static final String CONDITION_NAME = "condition_1";

    private DataBroker dataProvider;
    private EpRendererAugmentation epRendererAugmentation;
    private EndpointRpcRegistry endpointRpcRegistry;

    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        epRendererAugmentation = mock(EpRendererAugmentation.class);
        WriteTransaction wt = newWriteTransactionMock();
        RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);

        endpointRpcRegistry = new EndpointRpcRegistry(dataProvider, rpcRegistry);
    }

    @Test
    public void testConstructor() throws Exception {
        RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);
        EndpointRpcRegistry other = new EndpointRpcRegistry(dataProvider, rpcRegistry);
        other.close();
    }

    @Test
    public void testRegister() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        endpointRpcRegistry.register(epRendererAugmentation);
        Assert.assertEquals(1, EndpointRpcRegistry.registeredRenderers.size());

        endpointRpcRegistry.unregister(epRendererAugmentation);
        Assert.assertEquals(0, EndpointRpcRegistry.registeredRenderers.size());
    }

    @Test
    public void testRegister_SafelyFail() {
        endpointRpcRegistry.register(epRendererAugmentation);
        endpointRpcRegistry.register(null);

        Assert.assertEquals(1, EndpointRpcRegistry.registeredRenderers.size());
    }

    @Test
    public void testRegisterEndpoint() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        endpointRpcRegistry.registerEndpoint(setRegisterEndpointVariablesForTest());

        verify(wt, times(1)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(DataObject.class), eq(true));

        endpointRpcRegistry.registerEndpoint(setL3AddressVariableForTest());

        verify(wt, times(2)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(DataObject.class), eq(true));
    }

    private RegisterEndpointInput setRegisterEndpointVariablesForTest() throws Exception {
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        registerEndpointInputBuilder.setL2Context(new L2BridgeDomainId(L2_BD_ID));
        registerEndpointInputBuilder.setMacAddress(new MacAddress(MAC_ADDRESS));
        return registerEndpointInputBuilder.build();
    }

    private RegisterEndpointInput setL3AddressVariableForTest() {
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        List<L3Address> l3AddressList = new ArrayList<>();
        l3AddressList.add(new L3AddressBuilder().setL3Context(new L3ContextId(L3_CX_ID))
            .setIpAddress(new IpAddress(new Ipv4Address(IPv4_ADDRESS)))
            .build());
        registerEndpointInputBuilder.setL3Address(l3AddressList);
        return registerEndpointInputBuilder.build();
    }

    @Test
    public void testRegisterL3PrefixEndpoint() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        endpointRpcRegistry.registerL3PrefixEndpoint(setL3PrefixTestVariables());

        verify(wt, times(1)).put(eq(LogicalDatastoreType.OPERATIONAL),
                Mockito.<InstanceIdentifier<EndpointL3Prefix>>any(), any(EndpointL3Prefix.class));
    }

    @Test
    public void testRegisterL3PrefixEndpoint_BadData() {
        WriteTransaction wt = newWriteTransactionMock();

        endpointRpcRegistry.registerL3PrefixEndpoint(setL3PrefixTestVariables_NoIpPrefix());
        endpointRpcRegistry.registerL3PrefixEndpoint(setL3PrefixTestVariables_NoL3Context());
        endpointRpcRegistry.registerL3PrefixEndpoint(setL3PrefixTestVariables_NoTenant());

        verify(wt, never()).put(eq(LogicalDatastoreType.OPERATIONAL),
                Mockito.<InstanceIdentifier<EndpointL3Prefix>>any(), any(EndpointL3Prefix.class));
    }

    private RegisterL3PrefixEndpointInput setL3PrefixTestVariables() {
        RegisterL3PrefixEndpointInputBuilder registerL3PrefixEndpointInputBuilder =
                new RegisterL3PrefixEndpointInputBuilder();
        registerL3PrefixEndpointInputBuilder.setL3Context(new L3ContextId(L3_CX_ID));
        registerL3PrefixEndpointInputBuilder.setIpPrefix(new IpPrefix(new Ipv4Prefix(IPv4_PREFIX)));
        registerL3PrefixEndpointInputBuilder.setTenant(new TenantId(TENANT_ID));
        return registerL3PrefixEndpointInputBuilder.build();
    }

    private RegisterL3PrefixEndpointInput setL3PrefixTestVariables_NoL3Context() {
        RegisterL3PrefixEndpointInputBuilder registerL3PrefixEndpointInputBuilder =
                new RegisterL3PrefixEndpointInputBuilder();
        registerL3PrefixEndpointInputBuilder.setIpPrefix(new IpPrefix(new Ipv4Prefix(IPv4_PREFIX)));
        registerL3PrefixEndpointInputBuilder.setTenant(new TenantId(TENANT_ID));
        return registerL3PrefixEndpointInputBuilder.build();
    }

    private RegisterL3PrefixEndpointInput setL3PrefixTestVariables_NoIpPrefix() {
        RegisterL3PrefixEndpointInputBuilder registerL3PrefixEndpointInputBuilder =
                new RegisterL3PrefixEndpointInputBuilder();
        registerL3PrefixEndpointInputBuilder.setL3Context(new L3ContextId(L3_CX_ID));
        registerL3PrefixEndpointInputBuilder.setTenant(new TenantId(TENANT_ID));
        return registerL3PrefixEndpointInputBuilder.build();
    }

    private RegisterL3PrefixEndpointInput setL3PrefixTestVariables_NoTenant() {
        RegisterL3PrefixEndpointInputBuilder registerL3PrefixEndpointInputBuilder =
                new RegisterL3PrefixEndpointInputBuilder();
        registerL3PrefixEndpointInputBuilder.setL3Context(new L3ContextId(L3_CX_ID));
        registerL3PrefixEndpointInputBuilder.setIpPrefix(new IpPrefix(new Ipv4Prefix(IPv4_PREFIX)));
        return registerL3PrefixEndpointInputBuilder.build();
    }

    @Test
    public void testUnregisterEndpoint() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        UnregisterEndpointInput unregisterEndpointInput = unregisterEndpointInput();

        endpointRpcRegistry.unregisterEndpoint(unregisterEndpointInput);

        verify(wt, times(2)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    private UnregisterEndpointInput unregisterEndpointInput() {
        L2 l2 = new L2Builder().setL2Context(new L2BridgeDomainId(L2_BD_ID))
            .setMacAddress(new MacAddress(MAC_ADDRESS))
            .build();
        List<L2> l2List = new ArrayList<>();
        l2List.add(l2);
        L3 l3 = new L3Builder().setL3Context(new L3ContextId(L3_CX_ID))
            .setIpAddress(new IpAddress(new Ipv4Address(IPv4_ADDRESS)))
            .build();
        List<L3> l3List = new ArrayList<>();
        l3List.add(l3);
        List<L3Prefix> l3PrefixList = new ArrayList<>();
        l3PrefixList.add(new L3PrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix(IPv4_PREFIX))).build());
        return new UnregisterEndpointInputBuilder().setL2(l2List).setL3(l3List).setL3Prefix(l3PrefixList).build();
    }

    @Test
    public void testSetEndpointGroupConditions() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        EndpointGroupId endpointGroupId = new EndpointGroupId(EPG_ID);
        EndpointGroupCondition endpointGroupCondition =
                new EndpointGroupConditionBuilder().setCondition(new ConditionName(CONDITION_NAME)).build();
        List<EndpointGroupCondition> endpointGroupConditionList = new ArrayList<>();
        endpointGroupConditionList.add(endpointGroupCondition);

        SetEndpointGroupConditionsInput setEndpointGroupConditionsInput = new SetEndpointGroupConditionsInputBuilder()
            .setEndpointGroup(endpointGroupId).setEndpointGroupCondition(endpointGroupConditionList).build();

        endpointRpcRegistry.setEndpointGroupConditions(setEndpointGroupConditionsInput);

        verify(wt, times(1)).put(eq(LogicalDatastoreType.OPERATIONAL),
                Mockito.<InstanceIdentifier<EndpointGroupCondition>>any(), any(EndpointGroupCondition.class));
    }

    @Test
    public void testUnsetEndpointGroupConditions() throws Exception {
        WriteTransaction wt = newWriteTransactionMock();

        EndpointGroupId endpointGroupId = new EndpointGroupId(EPG_ID);
        EndpointGroupCondition endpointGroupCondition =
                new EndpointGroupConditionBuilder().setCondition(new ConditionName(CONDITION_NAME)).build();
        List<EndpointGroupCondition> endpointGroupConditionList = new ArrayList<>();
        endpointGroupConditionList.add(endpointGroupCondition);

        UnsetEndpointGroupConditionsInput unsetEndpointGroupConditionsInput =
                new UnsetEndpointGroupConditionsInputBuilder().setEndpointGroup(endpointGroupId)
                    .setEndpointGroupCondition(endpointGroupConditionList)
                    .build();

        endpointRpcRegistry.unsetEndpointGroupConditions(unsetEndpointGroupConditionsInput);

        verify(wt, times(1)).delete(eq(LogicalDatastoreType.OPERATIONAL),
                Mockito.<InstanceIdentifier<EndpointGroupCondition>>any());
    }

    private WriteTransaction newWriteTransactionMock() {
        WriteTransaction wt = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> f = mock(CheckedFuture.class);

        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wt);
        when(wt.submit()).thenReturn(f);
        return wt;
    }
}
