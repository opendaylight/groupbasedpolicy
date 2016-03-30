/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Manage the group tables for handling broadcast/multicast
 */

public class GroupTable extends OfTable {

    private static final Logger LOG = LoggerFactory.getLogger(GroupTable.class);

    public GroupTable(OfContext ctx) {
        super(ctx);
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {
        NodeId endpointNodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        if (endpointNodeId == null) {
            LOG.warn("Endpoint {} has no location specified, skipped", endpoint);
            return;
        }

        // there appears to be no way of getting only the existing group
        // tables unfortunately, so we have to get the whole node.
        // Since this is happening concurrently with other things that are
        // working in subtrees of nodes, we have to do two transactions
        FlowCapableNode fcn = getFCNodeFromDatastore(endpointNodeId);
        if (fcn == null)
            return;
        EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        if (ordinals == null) {
            LOG.info("getEndpointFwdCtxOrdinals is null for EP {}", endpoint);
            return;
        }
        GroupId groupId = new GroupId(Long.valueOf(ordinals.getFdId()));
        if (!ofWriter.groupExists(endpointNodeId, groupId.getValue())) {
            LOG.info("createGroup {} {}", endpointNodeId, groupId);
            ofWriter.writeGroup(endpointNodeId, groupId);
        }
        syncGroups(endpointNodeId, ordinals, endpoint, groupId, ofWriter);
    }

    @VisibleForTesting
    void syncGroups(NodeId nodeId, EndpointFwdCtxOrdinals ordinals, Endpoint endpoint, GroupId groupId,
                            OfWriter ofWriter) throws Exception {
        for (EgKey endpointGroupKey : ctx.getEndpointManager().getGroupsForNode(nodeId)) {
            // we'll use the fdId with the high bit set for remote bucket
            // and just the local port number for local bucket
            for (NodeId destinationNode : findPeerNodesForGroup(endpointGroupKey)) {
                if (nodeId.equals(destinationNode))
                    continue;
                if (isFloodDomainOnNode(ordinals.getFdId(), destinationNode)) {
                    Long bucketId;
                    try {
                        bucketId = (long) OrdinalFactory.getContextOrdinal(destinationNode);
                    } catch (Exception e) {
                        LOG.error("Error during getting of context ordinal, node: {}", destinationNode);
                        continue;
                    }
                    bucketId |= 1L << 31;
                    IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(destinationNode, TunnelTypeVxlan.class);
                    NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
                    if (tunDst == null || tunPort == null)
                        continue;
                    Action tunDstAction;
                    if (tunDst.getIpv4Address() != null) {
                        String nextHop = tunDst.getIpv4Address().getValue();
                        tunDstAction = nxLoadTunIPv4Action(nextHop, true);
                    } else {
                        LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(),
                                destinationNode);
                        continue;
                    }
                    BucketBuilder bucketBuilder = new BucketBuilder().setBucketId(new BucketId(bucketId))
                            .setAction(actionList(tunDstAction, outputAction(tunPort)));
                    ofWriter.writeBucket(nodeId, groupId, bucketBuilder.build());
                }
            }
            // TODO broadcasts are not separated by EPG between endpoints on the same node
            OfOverlayContext ofc = endpoint.getAugmentation(OfOverlayContext.class);
            if (EndpointManager.isExternal(endpoint, ctx.getTenant(endpoint.getTenant()).getExternalImplicitGroups()))
                continue;
            long bucketId;
            try {
                bucketId = getOfPortNum(ofc.getNodeConnectorId());
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), e);
                continue;
            }
            Action output = outputAction(ofc.getNodeConnectorId());
            BucketBuilder bb = new BucketBuilder().setBucketId(new BucketId(bucketId)).setAction(
                    FlowUtils.actionList(output));
            ofWriter.writeBucket(nodeId, groupId, bb.build());
            // if broadcast exceeds internal domain
            for (Endpoint extEp : ctx.getEndpointManager().getExtEpsNoLocForGroup(endpointGroupKey)) {
                if (extEp.getNetworkContainment() != null
                        && extEp.getNetworkContainment().equals(endpoint.getNetworkContainment())) {
                    L2FloodDomain l2Fd = ctx.getTenant(extEp.getTenant())
                            .resolveL2FloodDomain(extEp.getNetworkContainment());
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
                            bb.setBucketId(new BucketId(bucketId)).setAction(
                                    FlowUtils.actionList(actionList));
                            ofWriter.writeBucket(nodeId, groupId, bb.build());
                        }
                    }
                }
            }
        }

    }

    /**
     * @param sourceEpgKey a key of source group
     * @return all the nodes on which endpoints are either in groups that have policy with source
     * group, or are in the source group
     */
    private Set<NodeId> findPeerNodesForGroup(EgKey sourceEpgKey) {
        Set<NodeId> nodes = new HashSet<>();
        nodes.addAll(ctx.getEndpointManager().getNodesForGroup(sourceEpgKey));
        for (EgKey dstEpGroups : ctx.getCurrentPolicy().getPeers(sourceEpgKey)) {
            nodes.addAll(ctx.getEndpointManager().getNodesForGroup(dstEpGroups));
        }
        return nodes;
    }

    private boolean isFloodDomainOnNode(int fdId, NodeId node) throws Exception {
        for (Endpoint endpoint : ctx.getEndpointManager().getEndpointsForNode(node)) {
            EndpointFwdCtxOrdinals endpointFwdCtxOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
            if (endpointFwdCtxOrdinals == null) {
                continue;
            }
            int epFdId = endpointFwdCtxOrdinals.getFdId();
            if (fdId == epFdId) {
                return true;
            }
        }
        return false;
    }

    private FlowCapableNode getFCNodeFromDatastore(NodeId nodeId)
            throws ExecutionException, InterruptedException {
        ReadOnlyTransaction t = ctx.getDataBroker().newReadOnlyTransaction();
        InstanceIdentifier<FlowCapableNode> fcnIid = createNodePath(nodeId).builder()
                .augmentation(FlowCapableNode.class).build();

        Optional<FlowCapableNode> r = t.read(LogicalDatastoreType.OPERATIONAL, fcnIid).get();
        if (!r.isPresent()) {
            LOG.warn("Node {} is not present", fcnIid);
            return null;
        }
        FlowCapableNode fcn = r.get();
        t.close();
        return fcn;
    }
}
