/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Sets;

public class AclManager {

    private static final Logger LOG = LoggerFactory.getLogger(AclManager.class);
    private final MountedDataBrokerProvider mountDataProvider;

    private static ImmutableTable<NodeId, InterfaceKey, ImmutableSet<AddressEndpointKey>> multipleEndpointsOnInterface;

    public AclManager(@Nonnull MountedDataBrokerProvider mountDataProvider) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
    }

    public List<AccessListWrapper> resolveAclsOnInterface(RendererEndpointKey rEpKey, PolicyContext ctx) {
        List<AccessListWrapper> aclWrappers = new ArrayList<>();
        for (ACE_DIRECTION dir : new ACE_DIRECTION[] {ACE_DIRECTION.INGRESS, ACE_DIRECTION.EGRESS}) {
            aclWrappers.add(buildAccessListWrappers(dir, ctx, rEpKey));
        }
        return aclWrappers;
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

    public void updateAclsForPeers(PolicyContext policyCtx, RendererEndpointKey rEpKey) {
        ImmutableSet<PeerEndpointKey> peers = policyCtx.getPolicyTable().row(rEpKey).keySet();
        for (RendererEndpointKey peerRendEp : peers.stream()
            .map(AddressEndpointUtils::fromPeerEpKey)
            .collect(Collectors.toList())
            .stream()
            .map(AddressEndpointUtils::toRendererEpKey)
            .collect(Collectors.toList())) {
            updateAclsForRendEp(peerRendEp, policyCtx);
        }
    }

    public void updateAclsForRendEp(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        LOG.info("Updating policy for endpoint {}", rEpKey);
        AddressEndpointWithLocation peerAddrEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase epLoc;
        try {
            epLoc = InterfaceManager.resolveAndValidateLocation(peerAddrEp);
        } catch (NullPointerException | IllegalArgumentException e) {
            //TODO investigate, don't just move on.
            LOG.warn("Peer {} has no location. Moving on...", peerAddrEp, e.getMessage());
            return;
        }
        InstanceIdentifier<?> vppNodeIid = epLoc.getExternalNodeMountPoint();
        Optional<InstanceIdentifier<Interface>> optInterfaceIid =
                VppPathMapper.interfaceToInstanceIdentifier(epLoc.getExternalNodeConnector());
        if (!optInterfaceIid.isPresent()) {
            LOG.warn("Cannot  find interface for endpoint {}. ACLs for endpoint not updated {}. ", rEpKey);
            return;
        }
        Optional<DataBroker> optMountPoint = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        resolveAclsOnInterface(rEpKey, policyCtx).forEach(aclWrapper -> aclWrapper
            .writeAcl(optMountPoint.get(), optInterfaceIid.get().firstKeyOf(Interface.class)));
    }

    /**
     * Cache end-points accessible via a single interface for further processing.
     */
    public void cacheMultiInterfaces(@Nonnull PolicyContext ctx) {
        Builder<NodeId, InterfaceKey, ImmutableSet<AddressEndpointKey>> resultBuilder = new Builder<>();
        resolveEndpointsOnMultipleInterface(ImmutableList.copyOf(ctx.getAddrEpByKey().values()), resultBuilder);
        multipleEndpointsOnInterface = resultBuilder.build();
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
        if (multipleEndpointsOnInterface != null) {
            for (InterfaceKey ifaceKey : multipleEndpointsOnInterface.columnKeySet()) {
                for (NodeId nodeId : multipleEndpointsOnInterface.column(ifaceKey).keySet()) {
                    ImmutableSet<AddressEndpointKey> addrEps = multipleEndpointsOnInterface.get(nodeId, ifaceKey);
                    if (addrEps != null && addrEps.contains(key) && addrEps.size() > 1) {
                        return multipleEndpointsOnInterface.get(nodeId, ifaceKey);
                    }
                }
            }
        }
        return ImmutableSet.copyOf(Sets.newHashSet());
    }
}
