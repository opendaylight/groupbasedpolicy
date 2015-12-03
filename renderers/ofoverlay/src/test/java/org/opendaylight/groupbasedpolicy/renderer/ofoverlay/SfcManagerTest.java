/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcManagerTest {

    private SfcManager manager;

    private DataBroker dataBroker;
    private RpcProviderRegistry rpcRegistry;
    private ExecutorService executor;
    private ListenerRegistration<DataChangeListener> actionListener;

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> actionInstanceNotification;
    private InstanceIdentifier<DataObject> pathIdentifier;
    private ActionInstance dataObject;
    private HashMap<InstanceIdentifier<?>, DataObject> dataMap;
    private Set<InstanceIdentifier<?>> dataSet;

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() {
        dataBroker = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        executor = mock(ExecutorService.class);
        actionListener = mock(ListenerRegistration.class);
        actionInstanceNotification = mock(AsyncDataChangeEvent.class);
        pathIdentifier = mock(InstanceIdentifier.class);
        dataObject = mock(ActionInstance.class);

        when(
                dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(actionListener);
        dataMap = new HashMap<InstanceIdentifier<?>, DataObject>();
        dataMap.put(pathIdentifier, dataObject);
        dataSet = new HashSet<InstanceIdentifier<?>>(Arrays.asList(pathIdentifier));

        manager = new SfcManager(dataBroker, rpcRegistry, executor);
    }

    @Test
    public void constructorTest() throws Exception {
        manager.close();
        verify(actionListener).close();
    }

    @Test
    public void onDataChangedTestAdd() {
        when(actionInstanceNotification.getCreatedData()).thenReturn(dataMap);
        manager.onDataChanged(actionInstanceNotification);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void onDataChangedTestDelete() {
        when(actionInstanceNotification.getRemovedPaths()).thenReturn(dataSet);
        when(actionInstanceNotification.getOriginalData()).thenReturn(dataMap);
        manager.onDataChanged(actionInstanceNotification);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void onDataChangedTestChange() {
        when(actionInstanceNotification.getOriginalData()).thenReturn(dataMap);
        when(actionInstanceNotification.getUpdatedData()).thenReturn(dataMap);
        manager.onDataChanged(actionInstanceNotification);
        verify(executor).execute(any(Runnable.class));
    }
}
