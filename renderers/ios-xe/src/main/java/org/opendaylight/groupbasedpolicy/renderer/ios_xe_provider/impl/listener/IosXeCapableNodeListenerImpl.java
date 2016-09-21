/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.NodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class IosXeCapableNodeListenerImpl implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeCapableNodeListenerImpl.class);
    private final NodeManager nodeManager;

    private final ListenerRegistration listenerRegistration;


    public IosXeCapableNodeListenerImpl(final DataBroker dataBroker, final NodeManager nodeManager) {
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        final DataTreeIdentifier<Node> networkTopologyPath = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class).build());
        listenerRegistration = Preconditions.checkNotNull(dataBroker
                .registerDataTreeChangeListener(networkTopologyPath, this));
        LOG.info("network-topology listener registered");
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<Node> modification : changes) {
            DataObjectModification<Node> rootNode = modification.getRootNode();
            Node dataAfter = rootNode.getDataAfter();
            Node dataBefore = rootNode.getDataBefore();
            nodeManager.syncNodes(dataAfter, dataBefore);
        }
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }
}
