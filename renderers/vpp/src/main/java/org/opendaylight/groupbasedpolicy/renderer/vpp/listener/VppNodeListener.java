/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class VppNodeListener implements DataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeListener.class);
    private static final TopologyId TOPOLOGY_NETCONF = new TopologyId("topology-netconf");

    private final ListenerRegistration<VppNodeListener> listenerRegistration;
    private final VppNodeManager nodeManager;
    private final EventBus eventBus;

    public VppNodeListener(DataBroker dataBroker, VppNodeManager nodeManager, EventBus eventBus) {
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        this.eventBus = Preconditions.checkNotNull(eventBus);
        // Register listener
        final DataTreeIdentifier<Node> networkTopologyPath = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(TOPOLOGY_NETCONF))
                    .child(Node.class)
                    .build());
        listenerRegistration =
                Preconditions.checkNotNull(dataBroker.registerDataTreeChangeListener(networkTopologyPath, this));
        LOG.info("Network-Topology VppNodelistener registered");
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        LOG.debug("Topology Node changed. Changes {}", changes);

        for (DataTreeModification<Node> modification : changes) {
            DataObjectModification<Node> rootNode = modification.getRootNode();
            Node dataAfter = rootNode.getDataAfter();
            Node dataBefore = rootNode.getDataBefore();
            NodeOperEvent event =
                    new NodeOperEvent(modification.getRootPath().getRootIdentifier(), dataBefore, dataAfter);
            eventBus.post(event);
            nodeManager.syncNodes(dataAfter, dataBefore);
        }
    }

    public ListenerRegistration<VppNodeListener> getRegistrationObject() {
        return listenerRegistration;
    }

    @Override
    public void close() throws Exception {
        listenerRegistration.close();
    }
}
