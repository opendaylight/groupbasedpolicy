/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointListenerCovrgTest {

    private EndpointListener listener;
    private DataObjectModification<EndpointL3> rootNode;
    private Set<DataTreeModification<EndpointL3>> changes;

    private InstanceIdentifier<EndpointL3> rootIdentifier;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        DataBroker dataProvider = mock(DataBroker.class);

        EndpointManager endpointManager = mock(EndpointManager.class);
        listener = spy(new EndpointListener(dataProvider, endpointManager));

        EndpointL3 endpointL3 = mock(EndpointL3.class);

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = IidFactory.l3EndpointsIidWildcard();
        DataTreeIdentifier<EndpointL3> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, rootIdentifier);

        DataTreeModification<EndpointL3> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        when(rootNode.getDataBefore()).thenReturn(endpointL3);
        when(rootNode.getDataAfter()).thenReturn(endpointL3);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        listener.onDataTreeChanged(changes);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        listener.onDataTreeChanged(changes);
    }

    @Test
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        listener.onDataTreeChanged(changes);
    }

}
