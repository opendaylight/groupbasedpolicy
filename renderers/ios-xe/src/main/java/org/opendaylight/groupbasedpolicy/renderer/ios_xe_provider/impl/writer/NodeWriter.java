/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public ListenableFuture<Boolean> commitToDatastore(final DataBroker dataBroker) {
        if (rendererNodesCache.isEmpty()) {
            return Futures.immediateFuture(true);
        }

        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        final ArrayList<RendererNode> rendererNodes = new ArrayList<>(rendererNodesCache);
        // Clear cache
        rendererNodesCache.clear();

        boolean createParents = true;
        for (RendererNode rendererNode : rendererNodes) {
            final InstanceIdentifier<RendererNode> iid = buildRendererNodeIid(rendererNode);
            writeTransaction.put(LogicalDatastoreType.OPERATIONAL, iid, rendererNode, createParents);
            createParents = false;
        }
        try {
            final boolean result = DataStoreHelper.submitToDs(writeTransaction);
            return Futures.immediateFuture(result);
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return Futures.immediateFuture(false);
    }

    /**
     * Removes all cached items from data store
     *
     * @param dataBroker appropriate data provider
     */
    public ListenableFuture<Boolean> removeFromDatastore(final DataBroker dataBroker) {
        boolean result = true;
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        for (RendererNode nodeToRemove : rendererNodesCache) {
            final InstanceIdentifier<RendererNode> iid = buildRendererNodeIid(nodeToRemove);
            try {
                writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, iid);
                final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTransaction.submit();
                submitFuture.checkedGet();
                // Clear cache
            } catch (TransactionCommitFailedException e) {
                LOG.error("Write transaction failed to {}", e.getMessage());
                result = false;
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
                result = false;
            }
        }
        // Clear cache
        rendererNodesCache.clear();
        return Futures.immediateFuture(result);
    }

    private InstanceIdentifier<RendererNode> buildRendererNodeIid(final RendererNode rendererNode) {
        return InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(new RendererName(PolicyManagerImpl.IOS_XE_RENDERER)))
                .child(RendererNodes.class)
                .child(RendererNode.class, new RendererNodeKey(rendererNode.getNodePath()))
                .build();
    }
}
