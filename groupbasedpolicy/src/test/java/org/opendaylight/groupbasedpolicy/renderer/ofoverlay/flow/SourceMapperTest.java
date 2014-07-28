/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.HashMap;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowCtx;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import static org.mockito.Matchers.*;

import static org.mockito.Mockito.*;

public class SourceMapperTest extends FlowTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(SourceMapperTest.class);
    @Before
    public void setup() throws Exception {
        initCtx();
        table = new SourceMapper(ctx);
        super.setup();
    }
    
    @Test
    public void testNoPolicy() throws Exception {
        endpointManager.addEndpoint(localEP().build());
        ReadWriteTransaction t = dosync(null);
        verify(t, times(1)).put(any(LogicalDatastoreType.class), 
                                Matchers.<InstanceIdentifier<Flow>>any(), 
                                any(Flow.class));
    }
    
    @Test
    public void testMap() throws Exception {
        Endpoint ep = localEP().build();
        endpointManager.addEndpoint(ep);
        policyResolver.addTenant(baseTenant().build());
        
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, times(2)).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                Matchers.<InstanceIdentifier<Flow>>any(),
                                ac.capture());

        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (f.getMatch() == null) {
                assertEquals(FlowUtils.dropInstructions(),
                             f.getInstructions());
                count += 1;
            } else if (Objects.equals(ep.getMacAddress(),
                               f.getMatch().getEthernetMatch()
                                   .getEthernetSource().getAddress())) {
                // XXX TODO verify register setting in the instructions
                LOG.info("{}", f);
                count += 1;
            }
        }
        assertEquals(2, count);

        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class));
    }
    
}
