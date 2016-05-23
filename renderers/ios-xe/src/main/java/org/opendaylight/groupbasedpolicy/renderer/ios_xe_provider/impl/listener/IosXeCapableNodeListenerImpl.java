/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.NodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;

public class IosXeCapableNodeListenerImpl implements DataTreeChangeListener<NetworkTopology>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeCapableNodeListenerImpl.class);
    private final NodeManager nodeManager;

    private final ListenerRegistration listenerRegistration;


    public IosXeCapableNodeListenerImpl(final DataBroker dataBroker, final NodeManager nodeManager) {
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        final DataTreeIdentifier<NetworkTopology> networkTopologyPath = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(NetworkTopology.class).build());
        listenerRegistration = Preconditions.checkNotNull(dataBroker
                .registerDataTreeChangeListener(networkTopologyPath, this));
        LOG.info("network-topology listener registered");
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<NetworkTopology>> changes) {
        printTraceStart(LOG);
        for (DataTreeModification<NetworkTopology> modification : changes) {
            DataObjectModification<NetworkTopology> rootNode = modification.getRootNode();
            NetworkTopology dataAfter = rootNode.getDataAfter();
            NetworkTopology dataBefore = rootNode.getDataBefore();
            if (dataAfter != null && dataBefore == null) {
                nodeManager.syncNodes(dataAfter.getTopology(), null);
            }
            else if (dataAfter == null && dataBefore != null) {
                nodeManager.syncNodes(null, dataBefore.getTopology());
            }
            else if (dataAfter != null) {
                nodeManager.syncNodes(dataAfter.getTopology(), dataBefore.getTopology());
            }
        }
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }
}
