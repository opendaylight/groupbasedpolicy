/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable.BucketCtx;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable.GroupCtx;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import static org.junit.Assert.*;

public class GroupTableTest extends OfTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(GroupTableTest.class);

    GroupTable table;

    NodeConnectorId tunnelId = 
            new NodeConnectorId(nodeId.getValue() + ":42");
    NodeConnectorId remoteTunnelId = 
            new NodeConnectorId(remoteNodeId.getValue() + ":101");

    @Before
    public void setup() throws Exception {
        initCtx();
        table = new GroupTable(ctx);
    }
    
    @Test
    public void testGroup() throws Exception {
        Endpoint localEp = localEP().build();
        endpointManager.addEndpoint(localEp);
        Endpoint remoteEp = remoteEP(remoteNodeId).build();
        endpointManager.addEndpoint(remoteEp);
        
        switchManager.addSwitch(nodeId, tunnelId, 
                                Collections.<NodeConnectorId>emptySet(),
                                new OfOverlayNodeConfigBuilder()
                                    .setTunnelIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                                    .build());
        switchManager.addSwitch(remoteNodeId, remoteTunnelId, 
                                Collections.<NodeConnectorId>emptySet(),
                                new OfOverlayNodeConfigBuilder()
                                    .setTunnelIp(new IpAddress(new Ipv4Address("1.2.3.5")))
                                    .build());

        policyResolver.addTenant(baseTenant().build());

        HashMap<GroupId, GroupCtx> groupMap = new HashMap<>();
        table.sync(nodeId, ctx.policyResolver.getCurrentPolicy(), 
                   null, groupMap);
        
        assertEquals(1, groupMap.size());
        int fdId = ctx.policyManager.getContextOrdinal(tid, fd);
        GroupCtx ctx = groupMap.get(new GroupId(Long.valueOf(fdId)));
        assertNotNull(ctx);
        long tunBucketId = 
                (long)policyManager.getContextOrdinal(remoteNodeId.getValue());
        tunBucketId |= 1L << 31;
 
        int count = 0;
        for (BucketCtx bctx : ctx.bucketMap.values()) {
            if (Objects.equal(Long.valueOf(4),
                              bctx.newb.getBucketId().getValue())) {
                count += 1;
            } else if (Objects.equal(Long.valueOf(tunBucketId),
                                     bctx.newb.getBucketId().getValue())) {
                
                count += 1;
            }
        }
        assertEquals(2, count);
    }
}
