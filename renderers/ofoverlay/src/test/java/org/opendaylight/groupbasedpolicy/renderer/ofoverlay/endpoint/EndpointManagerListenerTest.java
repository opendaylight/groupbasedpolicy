package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.DataChangeListenerTester;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointManagerListenerTest {

    private InstanceIdentifier<DataObject> endpointId;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private EndpointManager endpointManager;
    private DataChangeListenerTester tester;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        endpointId = mock(InstanceIdentifier.class);
        endpointManager = mock(EndpointManager.class);
        DataBroker dataProvider = mock(DataBroker.class);

        EndpointManagerListener endpointManagerListener =
                new EndpointManagerListener(dataProvider, endpointManager);
        tester = new DataChangeListenerTester(endpointManagerListener);
        tester.setRemovedPath(endpointId);
    }

    @Test
    public void testOnDataChangeEndpoint() {
        DataObject endpoint = mock(Endpoint.class);
        tester.setDataObject(endpointId, endpoint);

        tester.callOnDataChanged();

        verify(endpointManager, times(3)).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class),
                any(EndpointL3.class));
    }

    @Test
    public void testOnDataChangeEndpointL3() {
        DataObject endpoint = mock(EndpointL3.class);
        tester.setDataObject(endpointId, endpoint);

        tester.callOnDataChanged();

        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, times(3)).processL3Endpoint(any(EndpointL3.class),
                any(EndpointL3.class));
    }

    @Test
    public void testOnDataChangeEndpointL3Prefix() {
        DataObject endpoint = mock(EndpointL3Prefix.class);
        tester.setDataObject(endpointId, endpoint);

        tester.callOnDataChanged();

        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class),
                any(EndpointL3.class));
    }

}
