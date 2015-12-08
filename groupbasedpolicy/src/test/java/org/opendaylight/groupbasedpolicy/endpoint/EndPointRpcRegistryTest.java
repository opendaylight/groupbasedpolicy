package org.opendaylight.groupbasedpolicy.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.util.concurrent.CheckedFuture;

public class EndPointRpcRegistryTest {

    private DataBroker dataProvider;
    private RpcProviderRegistry rpcRegistry;
    private RpcRegistration<EndpointService> rpcRegistration;
    private WriteTransaction t;
    private EpRendererAugmentation epRendererAugmentation;

    private EndpointRpcRegistry endpointRpcRegistry;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() {
        dataProvider = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        epRendererAugmentation = mock(EpRendererAugmentation.class);

        t = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(t);
        CheckedFuture<Void, TransactionCommitFailedException> f = mock(CheckedFuture.class);
        when(t.submit()).thenReturn(f);

        rpcRegistration = mock(RpcRegistration.class);
        when(rpcRegistry.addRpcImplementation(any(Class.class), any(RpcService.class))).thenReturn(rpcRegistration);

        endpointRpcRegistry = new EndpointRpcRegistry(dataProvider, rpcRegistry);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registerTest() throws Exception {
        verify(rpcRegistry).addRpcImplementation(any(Class.class), any(RpcService.class));
        verify(t).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Endpoints.class));
        endpointRpcRegistry.register(epRendererAugmentation);
        Assert.assertEquals(1, EndpointRpcRegistry.registeredRenderers.size());

        endpointRpcRegistry.unregister(epRendererAugmentation);
        Assert.assertEquals(0, EndpointRpcRegistry.registeredRenderers.size());
    }

    @Test
    public void registerTestSafelyFail() {
        endpointRpcRegistry.register(epRendererAugmentation);
        endpointRpcRegistry.register(null);
        Assert.assertEquals(1, EndpointRpcRegistry.registeredRenderers.size());
    }

    @Test
    public void registerEndpointTest()throws Exception{
        endpointRpcRegistry.registerEndpoint(setRegisterEndpointVariablesForTest());
        verify(t, times(1)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class), eq(true));

        endpointRpcRegistry.registerEndpoint(setL3AddressVariableForTest());
        verify(t, times(2)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class), eq(true));
    }

    private RegisterEndpointInput setRegisterEndpointVariablesForTest() throws Exception{
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        registerEndpointInputBuilder.setL2Context(new L2BridgeDomainId("l2bdId"));
        registerEndpointInputBuilder.setMacAddress(new MacAddress("5E:83:39:98:4F:1B"));
        return registerEndpointInputBuilder.build();
    }

    private RegisterEndpointInput setL3AddressVariableForTest() {
        RegisterEndpointInputBuilder registerEndpointInputBuilder = new RegisterEndpointInputBuilder();
        List<L3Address> l3AddressList = new ArrayList<>();
        l3AddressList.add(new L3AddressBuilder().setL3Context(new L3ContextId("l3c"))
            .setIpAddress(new IpAddress(new Ipv4Address("1.1.1.1")))
            .build());
        registerEndpointInputBuilder.setL3Address(l3AddressList);
        return registerEndpointInputBuilder.build();
    }

    @Test
    public void registerL3PrefixEndpointTest()throws Exception{
        endpointRpcRegistry.registerL3PrefixEndpoint(setL3PrefixTestVariables());
        verify(t, atLeast(1)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
    }

    private RegisterL3PrefixEndpointInput setL3PrefixTestVariables(){
        RegisterL3PrefixEndpointInputBuilder registerL3PrefixEndpointInputBuilder = new RegisterL3PrefixEndpointInputBuilder();
        registerL3PrefixEndpointInputBuilder.setL3Context(new L3ContextId("l3c"));
        registerL3PrefixEndpointInputBuilder.setIpPrefix(new IpPrefix(new Ipv4Prefix("1.1.1.0/24")));
        registerL3PrefixEndpointInputBuilder.setTenant(new TenantId("t1"));
        return registerL3PrefixEndpointInputBuilder.build();
    }

    @Test
    public void unregisterEndpointTest() throws Exception{
        L2 l2Mock = mock(L2.class);
        List<L2> l2List = new ArrayList<>();
        l2List.add(l2Mock);
        L3 l3Mock = mock(L3.class);
        List<L3> l3List = new ArrayList<>();
        l3List.add(l3Mock);
        UnregisterEndpointInput unregisterEndpointInputMock = mock(UnregisterEndpointInput.class);
        doReturn(l2List).when(unregisterEndpointInputMock).getL2();
        doReturn(l3List).when(unregisterEndpointInputMock).getL3();
        endpointRpcRegistry.unregisterEndpoint(unregisterEndpointInputMock);
        verify(t, times(2)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    @Test
    public void setEndpointGroupConditionsTest()throws Exception{
        EndpointGroupId endpointGroupIdMock = mock(EndpointGroupId.class);

        EndpointGroupCondition endpointGroupConditionMock = mock(EndpointGroupCondition.class);
        List<EndpointGroupCondition> endpointGroupConditionList = new ArrayList();
        endpointGroupConditionList.add(endpointGroupConditionMock);
        SetEndpointGroupConditionsInput setEndpointGroupConditionsInputMock = mock(SetEndpointGroupConditionsInput.class);
        doReturn(endpointGroupIdMock).when(setEndpointGroupConditionsInputMock).getEndpointGroup();
        doReturn(endpointGroupConditionList).when(setEndpointGroupConditionsInputMock).getEndpointGroupCondition();
        endpointRpcRegistry.setEndpointGroupConditions(setEndpointGroupConditionsInputMock);
        verify(t, atLeast(1)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(EndpointGroupCondition.class));
    }

    @Test
    public void unsetEndpointGroupConditionsTest()throws Exception{
        UnsetEndpointGroupConditionsInput unsetEndpointGroupConditionsInputMock = mock(UnsetEndpointGroupConditionsInput.class);
        EndpointGroupCondition endpointGroupConditionMock = mock(EndpointGroupCondition.class);
        List<EndpointGroupCondition> endpointGroupConditionList = new ArrayList();
        endpointGroupConditionList.add(endpointGroupConditionMock);
        doReturn(mock(EndpointGroupId.class)).when(unsetEndpointGroupConditionsInputMock).getEndpointGroup();
        doReturn(endpointGroupConditionList).when(unsetEndpointGroupConditionsInputMock).getEndpointGroupCondition();
        endpointRpcRegistry.unsetEndpointGroupConditions(unsetEndpointGroupConditionsInputMock);
        verify(t, times(1)).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }
}
