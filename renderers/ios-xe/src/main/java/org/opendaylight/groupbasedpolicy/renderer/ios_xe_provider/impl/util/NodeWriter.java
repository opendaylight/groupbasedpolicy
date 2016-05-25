/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(NodeWriter.class);
    private List<RendererNode> rendererNodesCache;

    public NodeWriter() {
        rendererNodesCache = new ArrayList<>();
    }

    public void write(RendererNode node) {
        rendererNodesCache.add(node);
    }

    public void commitToDatastore(DataBroker dataBroker) {
        RendererNodes rendererNodes = buildRendererNodes();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<RendererNodes> Iid = InstanceIdentifier.builder(Renderers.class)
                .child(Renderer.class, new RendererKey(new RendererName("ios-xe-renderer"))) // TODO unify renderer name
                .child(RendererNodes.class)
                .build();
        try {
            wtx.merge(LogicalDatastoreType.OPERATIONAL, Iid, rendererNodes, true);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wtx.submit();
            submitFuture.checkedGet();
            // Clear cache
            rendererNodesCache.clear();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
    }

    private RendererNodes buildRendererNodes() {
        RendererNodesBuilder rendererNodesBuilder = new RendererNodesBuilder();
        rendererNodesBuilder.setRendererNode(new ArrayList<>(rendererNodesCache));
        return rendererNodesBuilder.build();
    }

}
