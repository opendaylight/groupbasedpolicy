/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Manage the group tables for handling broadcast/multicast
 * @author readams
 */
public class GroupTable extends OfTable {
    private static final Logger LOG = 
            LoggerFactory.getLogger(GroupTable.class);

    public GroupTable(OfTableCtx ctx) {
        super(ctx);
    }
    
    @Override
    public void update(NodeId nodeId, PolicyInfo policyInfo, Dirty dirty)
            throws Exception {
        // there appears to be no way of getting only the existing group
        // tables unfortunately, so we have to get the whole goddamned node.
        // Since this is happening concurrently with other things that are 
        // working in subtrees of nodes, we have to do two transactions
        ReadOnlyTransaction t = ctx.dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Node> niid = createNodePath(nodeId);
        Optional<Node> r =
                t.read(LogicalDatastoreType.CONFIGURATION, niid).get();
        if (!r.isPresent()) return;
        FlowCapableNode fcn = r.get().getAugmentation(FlowCapableNode.class);
        if (fcn == null) return;

        HashMap<GroupId, GroupCtx> groupMap = new HashMap<>();

        if (fcn.getGroup() != null) {
            for (Group g : fcn.getGroup()) {
                GroupCtx gctx = new GroupCtx(g.getGroupId());
                groupMap.put(g.getGroupId(), gctx);

                Buckets bs = g.getBuckets();
                if (bs != null && bs.getBucket() != null)
                for (Bucket b : bs.getBucket()) {
                    gctx.bucketMap.put(b.getBucketId(), new BucketCtx(b));
                }
            }
        }
        
        sync(nodeId, policyInfo, dirty, groupMap);

        WriteTransaction wt = ctx.dataBroker.newWriteOnlyTransaction();
        boolean wrote = syncGroupToStore(wt, nodeId, groupMap);
        if (wrote)
            wt.submit().get();
    }
    
    protected boolean syncGroupToStore(WriteTransaction wt,
                                       NodeId nodeId, 
                                       HashMap<GroupId, GroupCtx> groupMap) {
        boolean wrote = false;
        for (GroupCtx gctx : groupMap.values()) {
            InstanceIdentifier<Group> giid = 
                    createGroupPath(nodeId, gctx.groupId);
            if (!gctx.visited) {
                // Remove group table
                wrote = true;
                wt.delete(LogicalDatastoreType.CONFIGURATION, giid);
            } else {
                ArrayList<Bucket> buckets = new ArrayList<>();
                
                // update group table
                for (BucketCtx bctx : gctx.bucketMap.values()) {
                    BucketId bid;
                    if (bctx.b != null) bid = bctx.b.getBucketId();
                    else bid = bctx.newb.getBucketId();
                    InstanceIdentifier<Bucket> biid = 
                            createBucketPath(nodeId,
                                             gctx.groupId, 
                                             bid);
                    if (!bctx.visited) {
                        // remove bucket
                        wrote = true;
                        wt.delete(LogicalDatastoreType.CONFIGURATION, biid);
                    } else if (bctx.b == null) {
                        // new bucket
                        buckets.add(bctx.newb);
                    } else if (!Objects.equal(bctx.newb.getAction(), 
                                              Ordering.from(ActionComparator.INSTANCE)
                                                  .sortedCopy(bctx.b.getAction()))) {
                        // update bucket
                        buckets.add(bctx.newb);
                    }
                }
                if (buckets.size() > 0) {
                    GroupBuilder gb = new GroupBuilder()
                        .setGroupId(gctx.groupId)
                        .setGroupType(GroupTypes.GroupAll)
                        .setBuckets(new BucketsBuilder()
                        .setBucket(buckets)
                        .build());
                    wrote = true;
                    wt.merge(LogicalDatastoreType.CONFIGURATION, 
                             giid, gb.build());
                }
            }
        }
        return wrote;
    }
    
    protected void sync(NodeId nodeId, PolicyInfo policyInfo, Dirty dirty,
                        HashMap<GroupId, GroupCtx> groupMap) throws Exception {

        for (EgKey epg : ctx.epManager.getGroupsForNode(nodeId)) {
            IndexedTenant it = ctx.policyResolver.getTenant(epg.getTenantId());
            if (it == null) continue;
            EndpointGroup eg = it.getEndpointGroup(epg.getEgId());
            if (eg == null || eg.getNetworkDomain() == null) continue;
            L2FloodDomain fd = it.resolveL2FloodDomain(eg.getNetworkDomain());
            if (fd == null) continue;

            int fdId = ctx.policyManager.getContextOrdinal(epg.getTenantId(),
                                                           fd.getId());
            GroupId gid = new GroupId(Long.valueOf(fdId));
            GroupCtx gctx = groupMap.get(gid);
            if (gctx == null) {
                groupMap.put(gid, gctx = new GroupCtx(gid)); 
            }
            gctx.visited = true;
            
            // we'll use the fdId with the high bit set for remote bucket
            // and just the local port number for local bucket
            for (NodeId destNode : ctx.epManager.getNodesForGroup(epg)) {
                if (nodeId.equals(destNode)) continue;

                long bucketId = (long)ctx.policyManager
                        .getContextOrdinal(destNode.getValue());
                bucketId |= 1L << 31;

                IpAddress tunDst = 
                        ctx.switchManager.getTunnelIP(destNode);
                NodeConnectorId tunPort =
                        ctx.switchManager.getTunnelPort(nodeId);
                if (tunDst == null || tunPort == null) continue;
                Action tundstAction = null;
                if (tunDst.getIpv4Address() != null) {
                    String nextHop = tunDst.getIpv4Address().getValue();
                    tundstAction = nxLoadTunIPv4Action(nextHop, true);
                } else {
                    LOG.error("IPv6 tunnel destination {} for {} not supported",
                              tunDst.getIpv6Address().getValue(),
                              destNode);
                    continue;
                }

                BucketBuilder bb = new BucketBuilder()
                    .setBucketId(new BucketId(Long.valueOf(bucketId)))
                    .setAction(actionList(tundstAction,
                                          outputAction(tunPort)));
                updateBucket(gctx, bb);
            }
            for (Endpoint localEp : ctx.epManager.getEPsForNode(nodeId, epg)) {
                OfOverlayContext ofc = 
                        localEp.getAugmentation(OfOverlayContext.class);
                if (ofc == null || ofc.getNodeConnectorId() == null ||
                    (LocationType.External.equals(ofc.getLocationType())))
                    continue;

                long bucketId;
                try {
                    bucketId = getOfPortNum(ofc.getNodeConnectorId());
                } catch (NumberFormatException e) {
                    LOG.warn("Could not parse port number {}", 
                             ofc.getNodeConnectorId(), e);
                    continue;
                }

                Action output = outputAction(ofc.getNodeConnectorId());
                BucketBuilder bb = new BucketBuilder()
                    .setBucketId(new BucketId(Long.valueOf(bucketId)))
                    .setAction(actionList(output));
                updateBucket(gctx, bb);
            }
        }
    }

    private static void updateBucket(GroupCtx gctx, BucketBuilder bb) {
        BucketCtx bctx = gctx.bucketMap.get(bb.getBucketId());
        if (bctx == null) {
            gctx.bucketMap.put(bb.getBucketId(), 
                               bctx = new BucketCtx(null));
        }
        bctx.visited = true;
        bctx.newb = bb.build();        
    }
    
    protected static class BucketCtx {
        Bucket b;
        Bucket newb;
        boolean visited = false;

        public BucketCtx(Bucket b) {
            super();
            this.b = b;
        }
    }
    
    protected static class GroupCtx {
        GroupId groupId;
        Map<BucketId, BucketCtx> bucketMap = new HashMap<>();
        boolean visited = false;

        public GroupCtx(GroupId groupId) {
            super();
            this.groupId = groupId;
        }
    }
    
}
