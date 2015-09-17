package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointManagerListenerTest {

    private InstanceIdentifier<DataObject> endpointId;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private EndpointManager endpointManager;
    private EndpointManagerListener endpointManagerListener;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        endpointId = mock(InstanceIdentifier.class);
        change = mock(AsyncDataChangeEvent.class);
        endpointManager = mock(EndpointManager.class);
        endpointId = mock(InstanceIdentifier.class);
        DataBroker dataProvider = mock(DataBroker.class);
        endpointManagerListener = new EndpointManagerListener(dataProvider, endpointManager);
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<>();
        removedPaths.add(endpointId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
    }

    @Test
    public void testOnDataChangeEndpoint() {
        DataObject endpoint = mock(Endpoint.class);

        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(endpointId, endpoint);
        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);

        endpointManagerListener.onDataChanged(change);
        verify(endpointManager, times(3)).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

    @Test
    public void testOnDataChangeEndpointL3() {
        DataObject endpoint = mock(EndpointL3.class);
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(endpointId, endpoint);

        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);

        endpointManagerListener.onDataChanged(change);
        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, times(3)).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

    @Test
    public void testOnDataChangeEndpointL3Prefix() {
        DataObject endpoint = mock(EndpointL3Prefix.class);
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(endpointId, endpoint);

        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);

        endpointManagerListener.onDataChanged(change);
        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

}