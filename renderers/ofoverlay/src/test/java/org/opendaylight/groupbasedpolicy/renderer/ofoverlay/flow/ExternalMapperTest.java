/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

public class ExternalMapperTest {

    private ExternalMapper mapper;

    private OfContext ctx;
    private short tableId;
    private NodeId nodeId;
    private OfWriter ofWriter;
    private SwitchManager switchManager;

    @Before
    public void initialisation() {
        ctx = mock(OfContext.class);
        tableId = 5;
        nodeId = mock(NodeId.class);
        ofWriter = mock(OfWriter.class);
        switchManager = mock(SwitchManager.class);
        when(ctx.getSwitchManager()).thenReturn(switchManager);

        mapper = new ExternalMapper(ctx, tableId);
    }

    @Test
    public void consturctorTest() {
        Assert.assertEquals(tableId, mapper.getTableId());
    }

    @Test
    public void syncTest() throws Exception {
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        Set<NodeConnectorId> externalPorts = new HashSet<NodeConnectorId>(Arrays.asList(nodeConnectorId));
        when(switchManager.getExternalPorts(nodeId)).thenReturn(externalPorts);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(2)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestNoExternalPorts() throws Exception {
        when(switchManager.getExternalPorts(nodeId)).thenReturn(null);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, never()).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }
}
