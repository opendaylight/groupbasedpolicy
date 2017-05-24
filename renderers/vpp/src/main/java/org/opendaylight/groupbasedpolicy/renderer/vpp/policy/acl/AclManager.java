/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.RendererResolvedPolicy;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.ResolvedRuleGroup;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

public class AclManager {

    private static final Logger LOG = LoggerFactory.getLogger(AclManager.class);
    private final MountedDataBrokerProvider mountDataProvider;

    private static ImmutableTable<NodeId, InterfaceKey, ImmutableSet<AddressEndpointKey>> endpointsByInterface;
    private final InterfaceManager interfaceManager;
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(30));

    public AclManager(@Nonnull MountedDataBrokerProvider mountDataProvider, InterfaceManager interfaceManager) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
        this.interfaceManager = Preconditions.checkNotNull(interfaceManager);
    }

    public ListenableFuture<List<AccessListWrapper>> resolveAclsOnInterface(RendererEndpointKey rEpKey,
            PolicyContext ctx) {
        Callable<List<AccessListWrapper>> aclBuildExecutor = new Callable<List<AccessListWrapper>>() {

            @Override
            public List<AccessListWrapper> call() throws Exception {
                LOG.info("Resolving ACL for renderer endpoint {}", rEpKey);
                List<AccessListWrapper> aclWrappers = new ArrayList<>();
                for (ACE_DIRECTION dir : new ACE_DIRECTION[] {ACE_DIRECTION.INGRESS, ACE_DIRECTION.EGRESS}) {
                    aclWrappers.add(buildAccessListWrappers(dir, ctx, rEpKey));
                }
                return aclWrappers;
            }
        };
        ListenableFuture<List<AccessListWrapper>> accessListFuture = executor.submit(aclBuildExecutor);
        return accessListFuture;
    }

    /**
     * @param policyDirection direction for which policy should be resolved. EP -> VPP = OUTBOUND, EP <- VPP = INBOUND
     * @param ctx with cached data
     * @param rEpKey key of EP for which to create ACLs.
     * @return synchronization futures, so that INGRESS and EGRESS ACLS can be resolved in parallel.
     */
    private static AccessListWrapper buildAccessListWrappers(ACE_DIRECTION policyDirection, PolicyContext ctx,
            RendererEndpointKey rEpKey) {
        LOG.trace("Resolving policy for VPP renderer endpoint {} in a separate thread in {} direction.", rEpKey,
                policyDirection);
        AccessListWrapper aclWrapper = AccessListUtil.ACE_DIRECTION.INGRESS
            .equals(policyDirection) ? new IngressAccessListWrapper() : new EgressAccessListWrapper();
            AccessListUtil.configureLocalRules(ctx, rEpKey, policyDirection, aclWrapper);
        // we support multiple IP end-points on a same interface
       for (AddressEndpointKey aek : otherEndpointsOnTheSameInterface(ctx,
                AddressEndpointUtils.fromRendererEpKey(rEpKey))) {
           AccessListUtil.configureLocalRules(ctx, AddressEndpointUtils.toRendererEpKey(aek), policyDirection, aclWrapper);
        }
        // resolve peers with no location
        aclWrapper.writeRules(AccessListUtil.denyDomainSubnets(ctx, policyDirection));
        // TODO currently any traffic heading to/from outside of managed domain is
        // permitted for demonstration purposes
        if (rEpKey.getContextType().isAssignableFrom(L2BridgeDomain.class) && AccessListUtil.findAddrEp(ctx, rEpKey) != null) {
            Optional<GbpAceBuilder> allowExtAccess =
                    AccessListUtil.allowExternalNetworksForEp(AccessListUtil.findAddrEp(ctx, rEpKey), policyDirection);
            if (allowExtAccess.isPresent()) {
                aclWrapper.writeRule(allowExtAccess.get());
            }
        }
        return aclWrapper;
    }

    public void resolveRulesToConfigure(@Nonnull PolicyContext policyCtx,
            @Nonnull SetView<RendererEndpointKey> changedEndpoints, @Nonnull SetView<RuleGroupKey> changedRules, boolean write) {
        Table<NodeId, AclKey, List<Ace>> aceTable = HashBasedTable.<NodeId, AclKey, List<Ace>>create();
        Map<RendererEndpointKey, Set<RuleGroupKey>> endpointsToUpdate = new HashMap<>();
        // rules changed
        changedRules.forEach(changedGroupRuleKey -> {
            policyCtx.getEndpointsByRuleGroups().get(changedGroupRuleKey).forEach(rendEp -> {
                endpointsToUpdate.put(rendEp, changedRules);
            });
        });
        // end-points changed
        for (RendererEndpointKey rendEp : changedEndpoints) {
            policyCtx.getPolicyTable().row(rendEp).keySet().forEach(peer -> {
                ImmutableSortedSet<RendererResolvedPolicy> resolvedPolicy =
                        policyCtx.getPolicyTable().get(rendEp, peer);
                if (resolvedPolicy == null) {
                    return;
                }
                Set<RuleGroupKey> ruleGroupsToUpdate = resolvedPolicy.stream()
                    .map(policy -> policy.getRuleGroup().getRelatedRuleGroupKey())
                    .collect(Collectors.toSet());
                if (endpointsToUpdate.get(rendEp) != null) {
                    endpointsToUpdate.put(rendEp,
                            Stream.concat(endpointsToUpdate.get(rendEp).stream(), ruleGroupsToUpdate.stream())
                                .collect(Collectors.toSet()));
                } else {
                    endpointsToUpdate.put(rendEp, ruleGroupsToUpdate);
                }
            });
        }
        for (ACE_DIRECTION aceDirection : new ACE_DIRECTION[] {ACE_DIRECTION.INGRESS, ACE_DIRECTION.EGRESS}) {
            endpointsToUpdate.forEach((endpointKey, rulesToUpdate) -> {
                java.util.Optional<RendererEndpoint> endpointToUpdate = policyCtx.getPolicy()
                    .getConfiguration()
                    .getRendererEndpoints()
                    .getRendererEndpoint()
                    .stream()
                    .filter(rendEpInPolicy -> rendEpInPolicy.getKey().equals(endpointKey))
                    .findAny();
                Builder<RendererEndpointKey, PeerEndpointKey, List<RendererResolvedPolicy>> updateTreeBuilder = new Builder<>();
                endpointToUpdate.get()
                    .getPeerEndpoint()
                    .stream()
                    .filter(peer -> policyCtx.getPolicyTable().get(endpointKey, peer.getKey()) != null)
                    .forEach(peer -> {updateTreeBuilder.put(endpointKey, peer.getKey(),
                            policyCtx.getPolicyTable()
                                .get(endpointKey, peer.getKey())
                                .stream()
                                .filter(rrp -> rulesToUpdate.contains(rrp.getRuleGroup().getRelatedRuleGroupKey()))
                                .collect(Collectors.toList()));
                                });
                ImmutableTable<RendererEndpointKey, PeerEndpointKey, List<RendererResolvedPolicy>> updateTree = updateTreeBuilder.build();
                List<GbpAceBuilder> aceBuilders = new ArrayList<>();
                updateTree.columnKeySet().stream().filter(p -> updateTree.get(endpointKey, p) != null).forEach(peer -> {
                    updateTree.get(endpointKey, peer).stream().forEach(rendererResolvedPolicy -> {
                        if (write) {
                            aceBuilders.addAll(generateRulesForEndpointPair(policyCtx, endpointKey, peer,
                                    rendererResolvedPolicy, aceDirection));
                        } else {
                            // we only need to resolve rule names when removing ACE
                            aceBuilders.addAll(rendererResolvedPolicy.getRuleGroup()
                                .getRules()
                                .stream()
                                .map(rule -> new GbpAceBuilder(
                                        AccessListUtil.resolveAceName(rule.getName(), endpointKey, peer)))
                                .collect(Collectors.toList()));
                        }
                    });
                });
                ImmutableSetMultimap<NodeId, InterfaceKey> interfacesForEndpoint =
                        getInterfacesForEndpoint(policyCtx, KeyFactory.addressEndpointKey(endpointKey));
                interfacesForEndpoint.keySet().forEach(nodeId -> {
                    ImmutableSet<InterfaceKey> intfcsOnNode = interfacesForEndpoint.get(nodeId);
                    intfcsOnNode.forEach(intf -> {
                        if (aceTable.get(nodeId, intf) != null) {
                            List<Ace> aces = Stream
                                .concat(aceTable.get(nodeId, intf).stream(),
                                        aceBuilders.stream().map(aceBuilder -> aceBuilder.build()))
                                .collect(Collectors.toList());
                            aceTable.put(nodeId, new AclKey(intf.getName() + aceDirection, VppAcl.class), aces);
                        } else {
                            aceTable.put(nodeId, new AclKey(intf.getName() + aceDirection, VppAcl.class),
                                    aceBuilders.stream()
                                        .map(aceBuilder -> aceBuilder.build())
                                        .collect(Collectors.toList()));
                        }
                    });
                });
            });
        }
        updateRules(ImmutableTable.copyOf(aceTable), write);
    }

    private void updateRules(ImmutableTable<NodeId, AclKey, List<Ace>> rulesToUpdate, boolean write) {
        List<ListenableFuture<Void>> sync = new ArrayList<>();
        rulesToUpdate.rowKeySet().forEach(nodeId -> {
            Callable<Void> syncExecutor = new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    LOG.debug("Updating node {}", nodeId);
                    InstanceIdentifier<Node> vppIid = VppIidFactory.getNetconfNodeIid(nodeId);
                    DataBroker dataBroker =
                        mountDataProvider.resolveDataBrokerForMountPoint(vppIid);
                    if (dataBroker == null) {
                        LOG.error("Failed to update ACLs for endpoints on node {}. Mount point does not exist.",
                                nodeId);
                    }
                    ImmutableMap<AclKey, List<Ace>> row = rulesToUpdate.row(nodeId);
                    row.keySet().forEach(aclKey -> {
                        Map<InstanceIdentifier<Ace>, Ace> entries = new HashMap<>();
                        row.get(aclKey).forEach(ace -> {
                            entries.put(VppIidFactory.getVppAcl(aclKey)
                                .builder()
                                .child(AccessListEntries.class)
                                .child(Ace.class, ace.getKey())
                                .build(), ace);
                        });
                        if (entries.isEmpty()) {
                            return;
                        }
                        LOG.info("Updating ACL: Action={}, Node={}, ACL={}", write, nodeId.getValue(),
                                aclKey.getAclName());
                        boolean result = (write) ? GbpNetconfTransaction.netconfSyncedWrite(vppIid, entries,
                                GbpNetconfTransaction.RETRY_COUNT) : GbpNetconfTransaction.netconfSyncedDelete(
                                        vppIid, entries.keySet(), GbpNetconfTransaction.RETRY_COUNT);
                        if (!result) {
                            LOG.error("Failed to remove rules from ACL {} on mount point {}", aclKey,
                                    nodeId.getValue());
                        }
                    });
                    return null;
                }
            };
            sync.add(executor.submit(syncExecutor));
        });
        try {
            Futures.allAsList(sync).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to sync ACLs on VPP nodes. {}", e);
        }
    }

    private List<GbpAceBuilder> generateRulesForEndpointPair(PolicyContext ctx, RendererEndpointKey r,
            PeerEndpointKey p, RendererResolvedPolicy rrp, ACE_DIRECTION aceDirection) {
        List<GbpAceBuilder> rules = new ArrayList<>();
        Direction direction =
                AccessListUtil.calculateClassifDirection(rrp.getRendererEndpointParticipation(), aceDirection);
        ResolvedRuleGroup resolvedRuleGroup = ctx.getRuleGroupByKey().get(rrp.getRuleGroup().getRelatedRuleGroupKey());
        resolvedRuleGroup.getRules().forEach(rule -> {
            Optional<GbpAceBuilder> ace = AccessListUtil.resolveAceClassifersAndAction(rule, direction,
                    AccessListUtil.resolveAceName(rule.getName(), r, p));
            if (ace.isPresent()) {
                rules.add(ace.get());
            }
        });
        AccessListUtil.updateAddressesInRules(rules, r, p, ctx, aceDirection, true);
        return rules;
    }

    //TODO remove
    public void updateAclsForPeers(PolicyContext policyCtx, RendererEndpointKey rEpKey) {
        ImmutableSet<PeerEndpointKey> peers = policyCtx.getPolicyTable().row(rEpKey).keySet();
        List<ListenableFuture<Void>> sync = new ArrayList<>();
        for (RendererEndpointKey peerRendEp : peers.stream()
            .map(AddressEndpointUtils::fromPeerEpKey)
            .collect(Collectors.toList())
            .stream()
            .map(AddressEndpointUtils::toRendererEpKey)
            .collect(Collectors.toList())) {
            sync.add(updateAclsForRendEp(peerRendEp, policyCtx));
        }
        try {
            Futures.allAsList(sync).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to update ACLs for peers of {}. {}", rEpKey, e);
        }
    }

    public ListenableFuture<Void> updateAclsForRendEp(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        SettableFuture<Void> sf = SettableFuture.create();
        AddressEndpointWithLocation peerAddrEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase epLoc;
        try {
            epLoc = InterfaceManager.resolveAndValidateLocation(peerAddrEp);
        } catch (NullPointerException | IllegalArgumentException e) {
            //TODO investigate, don't just move on.
            LOG.warn("Peer {} has no location. Moving on...", peerAddrEp, e.getMessage());
            return Futures.immediateFuture(null);
        }
        InstanceIdentifier<Node> vppNodeIid = (InstanceIdentifier<Node>) epLoc.getExternalNodeMountPoint();
        Optional<InstanceIdentifier<Interface>> optInterfaceIid =
                VppPathMapper.interfaceToInstanceIdentifier(epLoc.getExternalNodeConnector());
        if (!optInterfaceIid.isPresent()) {
            LOG.warn("Cannot  find interface for endpoint {}. ACLs for endpoint not updated {}. ", rEpKey);
            return Futures.immediateFuture(null);
        }
        DataBroker optMountPoint = mountDataProvider.resolveDataBrokerForMountPoint(vppNodeIid);
        if (interfaceManager.isExcludedFromPolicy(vppNodeIid.firstKeyOf(Node.class).getNodeId(),
                optInterfaceIid.get().firstKeyOf(Interface.class).getName())) {
            return Futures.immediateFuture(null);
        }
        LOG.info("Updating policy for endpoint {}", rEpKey);
        ListenableFuture<List<AccessListWrapper>> future = resolveAclsOnInterface(rEpKey, policyCtx);
        Futures.addCallback(future, new FutureCallback<List<AccessListWrapper>>() {

            @Override
            public void onSuccess(List<AccessListWrapper> result) {
                result.forEach(
                        acl -> acl.writeAcl(vppNodeIid, optInterfaceIid.get().firstKeyOf(Interface.class)));
                sf.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to update ACL for interface {} on node {}",
                        optInterfaceIid.get().firstKeyOf(Interface.class), vppNodeIid.firstKeyOf(Node.class));
                sf.set(null);
            }
        });
        return sf;
    }

    /**
     * Cache end-points accessible via a single interface for further processing.
     */
    public void cacheMultiInterfaces(@Nonnull PolicyContext ctx) {
        Builder<NodeId, InterfaceKey, ImmutableSet<AddressEndpointKey>> resultBuilder = new Builder<>();
        resolveEndpointsOnMultipleInterface(ImmutableList.copyOf(ctx.getAddrEpByKey().values()), resultBuilder);
        endpointsByInterface = resultBuilder.build();
    }

    /**
     *  Recursively grouping interfaces behind the same port
     */
    private void resolveEndpointsOnMultipleInterface(@Nullable List<AddressEndpointWithLocation> eps,
            @Nonnull Builder<NodeId, InterfaceKey, ImmutableSet<AddressEndpointKey>> builder) {
        if (eps == null || eps.isEmpty()) {
            return;
        }
        // look for any end-point with absolute location as reference end-point in this cycle;
        java.util.Optional<AddressEndpointWithLocation> refEndpoint =
                eps.stream().filter(ep -> EndpointUtils.getExternalLocationFrom(ep).isPresent()).findAny();
        if (!refEndpoint.isPresent()) {
            return;
        }
        Predicate<AddressEndpointWithLocation> sameLocation = new Predicate<AddressEndpointWithLocation>() {
            @Override
            public boolean test(AddressEndpointWithLocation addrEp) {
                return AddressEndpointUtils.sameExternalLocationCase(refEndpoint.get(), addrEp);
            }
        };
        Optional<ExternalLocationCase> extLoc = EndpointUtils.getExternalLocationFrom(refEndpoint.get());
        Set<AddressEndpointKey> sameLocations = eps.stream()
            .filter(sameLocation)
            .map(addrEp -> AddressEndpointUtils.fromAddressEndpointWithLocationKey(addrEp.getKey()))
            .collect(Collectors.toSet());
        builder.put(extLoc.get().getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId(),
                new InterfaceKey(extLoc.get().getExternalNodeConnector()),
                ImmutableSet.<AddressEndpointKey>copyOf(sameLocations));
        List<AddressEndpointWithLocation> differentLocations = eps.stream()
            //  keep end-points with different location and end-points with relative location in loop
            .filter(sameLocation.negate().or(p -> !EndpointUtils.getExternalLocationFrom(p).isPresent()))
            .collect(Collectors.toList());
        if (!differentLocations.isEmpty()) {
            resolveEndpointsOnMultipleInterface(differentLocations, builder);
        }
    }

    public @Nonnull static ImmutableSet<AddressEndpointKey> otherEndpointsOnTheSameInterface(@Nonnull PolicyContext ctx,
            @Nonnull AddressEndpointKey key) {
        if (endpointsByInterface != null) {
            for (InterfaceKey ifaceKey : endpointsByInterface.columnKeySet()) {
                for (NodeId nodeId : endpointsByInterface.column(ifaceKey).keySet()) {
                    ImmutableSet<AddressEndpointKey> addrEps = endpointsByInterface.get(nodeId, ifaceKey);
                    if (addrEps != null && addrEps.contains(key) && addrEps.size() > 1) {
                        return endpointsByInterface.get(nodeId, ifaceKey);
                    }
                }
            }
        }
        return ImmutableSet.copyOf(Sets.newHashSet());
    }

    public @Nonnull static ImmutableSetMultimap<NodeId, InterfaceKey> getInterfacesForEndpoint(@Nonnull PolicyContext ctx,
            @Nonnull AddressEndpointKey key) {
        SetMultimap<NodeId, InterfaceKey> interfaces = HashMultimap.create();
        if (endpointsByInterface != null) {
            for (InterfaceKey ifaceKey : endpointsByInterface.columnKeySet()) {
                for (NodeId nodeId : endpointsByInterface.column(ifaceKey).keySet()) {
                    ImmutableSet<AddressEndpointKey> addrEps = endpointsByInterface.get(nodeId, ifaceKey);
                    if (addrEps != null && addrEps.contains(key)) {
                        interfaces.put(nodeId, ifaceKey);
                    }
                }
            }
        }
        return ImmutableSetMultimap.copyOf(interfaces);
    }
}
