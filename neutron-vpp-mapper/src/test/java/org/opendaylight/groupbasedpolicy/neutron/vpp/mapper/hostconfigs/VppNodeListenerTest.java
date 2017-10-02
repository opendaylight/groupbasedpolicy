/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.hostconfigs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppNodeListenerTest extends TestResources {

    private TopologyId topologyId = new TopologyId("topology1");
    private NodeId nodeId = new NodeId("node1");
    private InstanceIdentifier<Node> nodeIid = createNodeIid(topologyId, nodeId);
    private VppNodeListener vppNodeListener;

    @Before
    public void init() throws InterruptedException, ExecutionException {
        String socketPath = "/tmp/";
        String socketPrefix = "socket_";
        setDataBroker();
        vppNodeListener = new VppNodeListener(dataBroker, new SocketInfo(socketPath, socketPrefix));
        writeTopologyNode(topologyId, nodeId);
        writeRendererNode(createNodeIid(topologyId, nodeId));
    }

    @Test
    public void writeDataTest() throws InterruptedException, ExecutionException {
        assertTrue(readHostconfig(nodeId).isPresent());
    }

    @Test
    public void deleteDataTest() throws InterruptedException, ExecutionException {
        deleteRendererNode(nodeIid);
        assertFalse(readHostconfig(nodeId).isPresent());
    }

    @After
    public void after() {
        vppNodeListener.close();
    }
}
