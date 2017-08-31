/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class EndpointManagerListenerTest {

    private EndpointManagerListener endpointManagerListener;
    private EndpointManager endpointManager;
    private DataBroker mockBroker;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        endpointManager = mock(EndpointManager.class);
        mockBroker = mock(DataBroker.class);
        endpointManagerListener = new EndpointManagerListener(mockBroker, endpointManager);

        doReturn(mock(ListenerRegistration.class)).when(mockBroker).registerDataTreeChangeListener(
                any(DataTreeIdentifier.class), any(DataTreeChangeListener.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointCreated() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(Endpoint.class))),
                dtclCaptor.capture());

        Endpoint endpoint = mock(Endpoint.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(null, endpoint,
                DataObjectModification.ModificationType.WRITE));

        verify(endpointManager).processEndpoint(null, endpoint);
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointUpdated() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(Endpoint.class))),
                dtclCaptor.capture());

        Endpoint endpoint = mock(Endpoint.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(endpoint, endpoint,
                DataObjectModification.ModificationType.WRITE));

        verify(endpointManager).processEndpoint(endpoint, endpoint);
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointDeleted() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(Endpoint.class))),
                dtclCaptor.capture());

        Endpoint endpoint = mock(Endpoint.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(endpoint, null,
                DataObjectModification.ModificationType.DELETE));

        verify(endpointManager).processEndpoint(endpoint, null);
        verify(endpointManager, never()).processL3Endpoint(any(EndpointL3.class), any(EndpointL3.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointL3Created() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(EndpointL3.class))),
                dtclCaptor.capture());

        EndpointL3 endpoint = mock(EndpointL3.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(null, endpoint,
                DataObjectModification.ModificationType.WRITE));

        verify(endpointManager).processL3Endpoint(null, endpoint);
        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointL3Updated() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(EndpointL3.class))),
                dtclCaptor.capture());

        EndpointL3 endpoint = mock(EndpointL3.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(endpoint, endpoint,
                DataObjectModification.ModificationType.WRITE));

        verify(endpointManager).processL3Endpoint(endpoint, endpoint);
        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEndpointL3Deleted() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(mockBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(EndpointL3.class))),
                dtclCaptor.capture());

        EndpointL3 endpoint = mock(EndpointL3.class);

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(endpoint, null,
                DataObjectModification.ModificationType.DELETE));

        verify(endpointManager).processL3Endpoint(endpoint, null);
        verify(endpointManager, never()).processEndpoint(any(Endpoint.class), any(Endpoint.class));
    }

    @SuppressWarnings("unchecked")
    private static <T extends DataObject> Collection<DataTreeModification<T>> newMockDataTreeModification(T dataBefore,
            T dataAfter, DataObjectModification.ModificationType type) {
        DataTreeModification<T> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<T> mockModification = mock(DataObjectModification.class);
        doReturn(type).when(mockModification).getModificationType();
        doReturn(dataBefore).when(mockModification).getDataBefore();
        doReturn(dataAfter).when(mockModification).getDataAfter();
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();

        return Collections.singletonList(mockDataTreeModification);
    }
}
