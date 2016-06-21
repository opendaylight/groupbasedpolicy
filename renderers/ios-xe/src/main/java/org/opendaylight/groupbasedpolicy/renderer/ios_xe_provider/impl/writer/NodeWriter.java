/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.NodeManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(NodeWriter.class);
    private final List<RendererNode> rendererNodesCache;

    public NodeWriter() {
        rendererNodesCache = new ArrayList<>();
    }

    public void cache(RendererNode node) {
        rendererNodesCache.add(node);
    }

    /**
     * Put all cached items to data store
     *
     * @param dataBroker appropriate data provider
     */
    public void commitToDatastore(DataBroker dataBroker) {
        RendererNodes rendererNodes = buildRendererNodes();
        final Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(dataBroker);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create transaction, mountpoint: {}", dataBroker);
            return;
        }
        final WriteTransaction writeTransaction = optionalWriteTransaction.get();
        final InstanceIdentifier<RendererNodes> iid = buildRendererNodesIid();
        try {
            writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, iid, rendererNodes, true);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTransaction.submit();
            submitFuture.checkedGet();
            // Clear cache
            rendererNodesCache.clear();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
    }

    /**
     * Removes all cached items from data store
     *
     * @param dataBroker appropriate data provider
     */
    public void removeFromDatastore(DataBroker dataBroker) {
        final Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(dataBroker);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create transaction, mountpoint: {}", dataBroker);
            return;
        }
        final WriteTransaction writeTransaction = optionalWriteTransaction.get();
        for (RendererNode nodeToRemove : rendererNodesCache) {
            InstanceIdentifier<RendererNode> iid = buildRendererNodeIid(nodeToRemove);
            try {
                writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, iid);
                CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTransaction.submit();
                submitFuture.checkedGet();
                // Clear cache
            } catch (TransactionCommitFailedException e) {
                LOG.error("Write transaction failed to {}", e.getMessage());
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
            }
        }
        rendererNodesCache.clear();
    }

    private InstanceIdentifier<RendererNodes> buildRendererNodesIid() {
        return InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(new RendererName(NodeManager.iosXeRenderer))) // TODO unify renderer name
                .child(RendererNodes.class)
                .build();
    }

    private InstanceIdentifier<RendererNode> buildRendererNodeIid(RendererNode rendererNode) {
        return InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(new RendererName(NodeManager.iosXeRenderer))) // TODO unify renderer name
                .child(RendererNodes.class)
                .child(RendererNode.class, new RendererNodeKey(rendererNode.getNodePath()))
                .build();
    }

    private RendererNodes buildRendererNodes() {
        RendererNodesBuilder rendererNodesBuilder = new RendererNodesBuilder();
        rendererNodesBuilder.setRendererNode(new ArrayList<>(rendererNodesCache));
        return rendererNodesBuilder.build();
    }
}
