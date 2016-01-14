/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.actionList;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.createNodePath;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.getOfPortNum;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.antlr.v4.runtime.misc.Array2DHashSet;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Manage the group tables for handling broadcast/multicast
 */

public class GroupTable extends OfTable {

    private static final Logger LOG = LoggerFactory.getLogger(GroupTable.class);

    public GroupTable(OfContext ctx) {
        super(ctx);
    }

    FlowCapableNode getFCNodeFromDatastore(NodeId nodeId)
            throws ExecutionException, InterruptedException {
        FlowCapableNode fcn = null;
        ReadOnlyTransaction t = ctx.getDataBroker().newReadOnlyTransaction();
        InstanceIdentifier<FlowCapableNode> fcniid = createNodePath(nodeId).builder()
                .augmentation(FlowCapableNode.class).build();

        Optional<FlowCapableNode> r = t.read(LogicalDatastoreType.OPERATIONAL, fcniid).get();
        if (!r.isPresent()) {
            LOG.warn("Node {} is not present", fcniid);
            return null;
        }
        fcn = r.get();
        t.close();
        return fcn;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {
        // there appears to be no way of getting only the existing group
        // tables unfortunately, so we have to get the whole goddamned node.
        // Since this is happening concurrently with other things that are
        // working in subtrees of nodes, we have to do two transactions
        FlowCapableNode fcn = getFCNodeFromDatastore(nodeId);
        if (fcn == null)
            return;

        for (Endpoint localEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            EndpointFwdCtxOrdinals localEpFwdCtxOrds =
                    OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, localEp);
            if (localEpFwdCtxOrds == null) {
                LOG.info("getEndpointFwdCtxOrdinals is null for EP {}", localEp);
                continue;
            }

            GroupId gid = new GroupId(Long.valueOf(localEpFwdCtxOrds.getFdId()));
            if (!ofWriter.groupExists(nodeId, gid.getValue())) {
                LOG.info("createGroup {} {}", nodeId, gid);
                ofWriter.writeGroup(nodeId, gid);
            }

            for (EgKey epg : ctx.getEndpointManager().getGroupsForNode(nodeId)) {

                // we'll use the fdId with the high bit set for remote bucket
                // and just the local port number for local bucket
                for (NodeId destNode : findPeerNodesForGroup(epg)) {
                    if (nodeId.equals(destNode))
                        continue;

                    if(isFloodDomainOnNode(localEpFwdCtxOrds.getFdId(), destNode)) {
                        long bucketId = OrdinalFactory.getContextOrdinal(destNode);
                        bucketId |= 1L << 31;

                        IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(destNode, TunnelTypeVxlan.class);
                        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
                        if (tunDst == null || tunPort == null)
                            continue;
                        Action tundstAction = null;
                        if (tunDst.getIpv4Address() != null) {
                            String nextHop = tunDst.getIpv4Address().getValue();
                            tundstAction = nxLoadTunIPv4Action(nextHop, true);
                        } else {
                            LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(),
                                    destNode);
                            continue;
                        }
                        BucketBuilder bb = new BucketBuilder().setBucketId(new BucketId(Long.valueOf(bucketId)))
                                .setAction(actionList(tundstAction, outputAction(tunPort)));
                        ofWriter.writeBucket(nodeId, gid, bb.build());
                    }
                }
                // TODO broadcasts are not separated by EPG between endpoints on the same node
                OfOverlayContext ofc = localEp.getAugmentation(OfOverlayContext.class);
                if (EndpointManager.isExternal(localEp, ctx.getTenant(localEp.getTenant()).getExternalImplicitGroups()))
                    continue;

                long bucketId;
                try {
                    bucketId = getOfPortNum(ofc.getNodeConnectorId());
                } catch (NumberFormatException e) {
                    LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), e);
                    continue;
                }
                Action output = outputAction(ofc.getNodeConnectorId());
                BucketBuilder bb = new BucketBuilder().setBucketId(new BucketId(Long.valueOf(bucketId))).setAction(
                        FlowUtils.actionList(output));
                ofWriter.writeBucket(nodeId, gid, bb.build());

                // if boradcast exceeds internal domain
                for (Endpoint extEp : ctx.getEndpointManager().getExtEpsNoLocForGroup(epg)) {
                    if (extEp.getNetworkContainment() != null
                            && extEp.getNetworkContainment().equals(localEp.getNetworkContainment())) {
                        L2FloodDomain l2Fd = ctx.getTenant(extEp.getTenant()).resolveL2FloodDomain(
                                extEp.getNetworkContainment());
                        if (l2Fd != null) {
                            Segmentation segmentation = l2Fd.getAugmentation(Segmentation.class);
                            // external endpoints do not have location augmentation
                            // however they are beyond external ports
                            for (NodeConnectorId extNcId : ctx.getSwitchManager().getExternalPorts(nodeId)) {
                                try {
                                    bucketId = getOfPortNum(extNcId);
                                } catch (NumberFormatException e) {
                                    LOG.warn("Could not parse external port number {}", extNcId, e);
                                    continue;
                                }
                                ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder>
                                    actionList = new ArrayList<>();
                                if (segmentation != null) {
                                    Integer vlanId = segmentation.getSegmentationId();
                                    actionList.addAll(FlowUtils.pushVlanActions(vlanId));
                                    actionList.add(new ActionBuilder().setOrder(2).setAction(outputAction(extNcId)));
                                } else {
                                    actionList.add(new ActionBuilder().setOrder(0).setAction(outputAction(extNcId)));
                                }
                                bb.setBucketId(new BucketId(Long.valueOf(bucketId))).setAction(
                                        FlowUtils.actionList(actionList));
                                ofWriter.writeBucket(nodeId, gid, bb.build());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param sourceEpgKey a key of source group
     * @return all the nodes on which endpoints are either in groups that have policy with source
     *         group, or are in the source group
     */
    private Set<NodeId> findPeerNodesForGroup(EgKey sourceEpgKey) {
        Set<NodeId> nodes = new HashSet<NodeId>();
        nodes.addAll(ctx.getEndpointManager().getNodesForGroup(sourceEpgKey));
        for (EgKey dstEpgs : ctx.getCurrentPolicy().getPeers(sourceEpgKey)) {
            nodes.addAll(ctx.getEndpointManager().getNodesForGroup(dstEpgs));
        }
        return nodes;
    }

    private boolean isFloodDomainOnNode(int fdId, NodeId node) throws Exception {
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(node)) {
            int epFdId = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, ep).getFdId();
            if (fdId == epFdId) {
                return true;
            }
        }
        return false;
    }
}
