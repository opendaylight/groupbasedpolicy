package org.opendaylight.groupbasedpolicy.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.util.concurrent.CheckedFuture;

public class EndPointRpcRegistryTest {

    private DataBroker dataProvider;
    private RpcProviderRegistry rpcRegistry;
    private RpcRegistration<EndpointService> rpcRegistration;
    private WriteTransaction t;
    private EpRendererAugmentation epRendererAugmentation;

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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registerTest() throws Exception {
        EndpointRpcRegistry.register(dataProvider, rpcRegistry, epRendererAugmentation);
        verify(rpcRegistry).addRpcImplementation(any(Class.class), any(RpcService.class));
        verify(t).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Endpoints.class));

        EndpointRpcRegistry.unregister(epRendererAugmentation);
        verify(rpcRegistration).close();
    }

    @Test
    public void registerTestSafelyFail() {
        EndpointRpcRegistry.register(null, rpcRegistry, epRendererAugmentation);
        EndpointRpcRegistry.register(dataProvider, null, epRendererAugmentation);
        EndpointRpcRegistry.register(null, rpcRegistry, null);
        EndpointRpcRegistry.register(dataProvider, null, null);
        EndpointRpcRegistry.register(dataProvider, rpcRegistry, null);
    }

    @Test
    public void unregisterTestFail() throws Exception {
        EndpointRpcRegistry.unregister(null);
        verify(rpcRegistration, never()).close();

        EpRendererAugmentation epRendererAugmentation = mock(EpRendererAugmentation.class);
        EndpointRpcRegistry.unregister(epRendererAugmentation);
        verify(rpcRegistration, never()).close();
    }
}
