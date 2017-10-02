/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.sflow.values.SflowClientSettings;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SflowClientSettingsListenerTest {

    private SflowClientSettingsListener listener;
    private DataObjectModification<SflowClientSettings> rootNode;
    private Set<DataTreeModification<SflowClientSettings>> changes;

    private InstanceIdentifier<SflowClientSettings> rootIdentifier;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        DataBroker dataProvider = mock(DataBroker.class);

        StatisticsManager ofStatisticsManager = mock(StatisticsManager.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        listener = spy(new SflowClientSettingsListener(dataProvider, executor, ofStatisticsManager));

        SflowClientSettings sflowClientSettings = mock(SflowClientSettings.class);

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = InstanceIdentifier.builder(OfOverlayConfig.class).child(SflowClientSettings.class).build();
        DataTreeIdentifier<SflowClientSettings> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);

        DataTreeModification<SflowClientSettings> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        when(rootNode.getDataBefore()).thenReturn(sflowClientSettings);
        when(rootNode.getDataAfter()).thenReturn(sflowClientSettings);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        listener.onDataTreeChanged(changes);

        verify(listener).onWrite(rootNode, rootIdentifier);
    }

    @Test
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        listener.onDataTreeChanged(changes);

        verify(listener).onDelete(rootNode, rootIdentifier);
    }

    @Test
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        // first call will initialize uninitialized dependencies;
        // second call will call #close on them
        listener.onDataTreeChanged(changes);
        listener.onDataTreeChanged(changes);
        verify(listener, times(2)).onSubtreeModified(rootNode, rootIdentifier);
    }

}
