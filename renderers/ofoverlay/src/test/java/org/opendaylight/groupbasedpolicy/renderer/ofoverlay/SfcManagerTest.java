/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class SfcManagerTest {

    private SfcManager manager;

    private DataBroker dataBroker;
    private RpcProviderRegistry rpcRegistry;
    private ExecutorService executor;
    private ListenerRegistration<?> actionListener;

    private DataObjectModification<ActionInstance> mockModification;
    private Collection<DataTreeModification<ActionInstance>> changeEvent;
    private ActionInstance dataObject;

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() {
        dataBroker = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        executor = mock(ExecutorService.class);
        actionListener = mock(ListenerRegistration.class);
        dataObject = mock(ActionInstance.class);

        doReturn(actionListener).when(dataBroker).registerDataTreeChangeListener(
                any(DataTreeIdentifier.class), any(DataTreeChangeListener.class));

        manager = new SfcManager(dataBroker, rpcRegistry, executor);

        DataTreeModification<ActionInstance> mockDataTreeModification = mock(DataTreeModification.class);
        mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        changeEvent = Collections.singletonList(mockDataTreeModification);
    }

    @Test
    public void constructorTest() throws Exception {
        manager.close();
        verify(actionListener).close();
    }

    @Test
    public void onDataChangedTestAdd() {
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(dataObject).when(mockModification).getDataAfter();

        manager.onDataTreeChanged(changeEvent);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void onDataChangedTestDelete() {
        doReturn(DataObjectModification.ModificationType.DELETE).when(mockModification).getModificationType();
        doReturn(dataObject).when(mockModification).getDataBefore();

        manager.onDataTreeChanged(changeEvent);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void onDataChangedTestChange() {
        doReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED).when(mockModification).getModificationType();
        doReturn(dataObject).when(mockModification).getDataBefore();
        doReturn(dataObject).when(mockModification).getDataAfter();

        manager.onDataTreeChanged(changeEvent);
        verify(executor).execute(any(Runnable.class));
    }
}
