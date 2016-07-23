/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangeListenerTester {

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeMock;
    private DataChangeListener listener;

    private Map<InstanceIdentifier<?>, DataObject> testData;
    private Set<InstanceIdentifier<?>> removedPaths;

    @SuppressWarnings("unchecked")
    public DataChangeListenerTester(DataChangeListener listener) {
        changeMock = mock(AsyncDataChangeEvent.class);
        testData = new HashMap<>();
        removedPaths = new HashSet<>();

        this.listener = listener;

        when(changeMock.getCreatedData()).thenReturn(testData);
        when(changeMock.getOriginalData()).thenReturn(testData);
        when(changeMock.getUpdatedData()).thenReturn(testData);
        when(changeMock.getRemovedPaths()).thenReturn(removedPaths);
    }

    public DataChangeListenerTester setDataObject(InstanceIdentifier<DataObject> iid, DataObject dataObject){
        testData.clear();
        return addDataObject(iid, dataObject);
    }

    public DataChangeListenerTester addDataObject(InstanceIdentifier<DataObject> iid, DataObject dataObject){
        testData.put(iid, dataObject);
        return this;
    }

    public DataChangeListenerTester setRemovedPath(InstanceIdentifier<DataObject> iid){
        removedPaths.clear();
        return addRemovedPath(iid);
    }

    public DataChangeListenerTester addRemovedPath(InstanceIdentifier<DataObject> iid){
        removedPaths.add(iid);
        return this;
    }

    public void callOnDataChanged(){
        listener.onDataChanged(changeMock);
    }
}
