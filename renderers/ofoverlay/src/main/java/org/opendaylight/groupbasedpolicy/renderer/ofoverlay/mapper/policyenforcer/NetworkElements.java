/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer;

import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkElements {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkElements.class);
    private final Endpoint srcEp;
    private final Endpoint dstEp;
    private final EgKey srcEpg;
    private final EgKey dstEpg;
    private NodeId srcNodeId;
    private NodeId dstNodeId;
    private final NodeId localNodeId;
    private EndpointFwdCtxOrdinals srcEpOrdinals;
    private EndpointFwdCtxOrdinals dstEpOrdinals;

    public NetworkElements(Endpoint srcEp, Endpoint dstEp, EgKey srcEpg, EgKey dstEpg, NodeId nodeId, OfContext ctx) throws Exception {
        this.srcEp = srcEp;
        this.dstEp = dstEp;
        this.srcEpg = srcEpg;
        this.dstEpg = dstEpg;
        this.localNodeId = nodeId;
        this.srcEpOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, srcEp);
        if (this.srcEpOrdinals == null) {
            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", srcEp);
            return;
        }
        this.dstEpOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, dstEp);
        if (this.dstEpOrdinals == null) {
            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", dstEp);
            return;
        }
        if (dstEp.getAugmentation(OfOverlayContext.class) != null) {
            this.dstNodeId = dstEp.getAugmentation(OfOverlayContext.class).getNodeId();
        }
        if (srcEp.getAugmentation(OfOverlayContext.class) != null) {
            this.srcNodeId = srcEp.getAugmentation(OfOverlayContext.class).getNodeId();
        }
    }


    public Endpoint getSrcEp() {
        return srcEp;
    }


    public Endpoint getDstEp() {
        return dstEp;
    }

    public EgKey getSrcEpg() {
        return srcEpg;
    }

    public EgKey getDstEpg() {
        return dstEpg;
    }

    public NodeId getSrcNodeId() {
        return srcNodeId;
    }


    public NodeId getDstNodeId() {
        return dstNodeId;
    }


    public NodeId getLocalNodeId() {
        return localNodeId;
    }


    public EndpointFwdCtxOrdinals getSrcEpOrdinals() {
        return srcEpOrdinals;
    }


    public EndpointFwdCtxOrdinals getDstEpOrdinals() {
        return dstEpOrdinals;
    }

}