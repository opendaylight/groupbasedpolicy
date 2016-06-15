/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.Containment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.SetMultimap;

public class ForwardingManager {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardingManager.class);

    private final InterfaceManager ifaceManager;
    private final BridgeDomainManager bdManager;

    public ForwardingManager(@Nonnull InterfaceManager ifaceManager, @Nonnull BridgeDomainManager bdManager) {
        this.ifaceManager = Preconditions.checkNotNull(ifaceManager);
        this.bdManager = Preconditions.checkNotNull(bdManager);
    }

    public void createVxlanBridgeDomainsOnNodes(SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Set<NodeId> vppNodes = vppNodesByBridgeDomain.get(bd);
            for (NodeId vppNode : vppNodes) {
                try {
                    bdManager.createVxlanBridgeDomainOnVppNode(bd, null, vppNode).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Bridge domain {} was not created on node {}", bd, vppNode.getValue(), e);
                }
            }
        }
    }

    public void removeVxlanBridgeDomainsOnNodes(SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Set<NodeId> vppNodes = vppNodesByBridgeDomain.get(bd);
            for (NodeId vppNode : vppNodes) {
                try {
                    bdManager.removeBridgeDomainFromVppNode(bd, vppNode).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Bridge domain {} was not removed from node {}", bd, vppNode.getValue(), e);
                }
            }
        }
    }

    public void createForwardingForEndpoint(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        AddressEndpointWithLocation rEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase rEpLoc = resolveAndValidateLocation(rEp);
        if (Strings.isNullOrEmpty(rEpLoc.getExternalNodeConnector())) {
            // TODO add it to the status for renderer manager
            LOG.info("Rednerer endpoint does not have external-node-connector therefore it is ignored {}", rEp);
            return;
        }

        if (Strings.isNullOrEmpty(rEpLoc.getExternalNode())) {
            Optional<String> optL2FloodDomain = resolveL2FloodDomain(rEp.getNetworkContainment());
            if (!optL2FloodDomain.isPresent()) {
                // TODO add it to the status for renderer manager
                LOG.info("Rednerer endpoint does not have l2FloodDomain as network containment {}", rEp);
                return;
            }
            String l2FloodDomain = optL2FloodDomain.get();
            try {
                ifaceManager.addBridgeDomainToInterface(l2FloodDomain, rEp).get();
                LOG.debug("Interface added to bridge-domain {} for endpoint {}", l2FloodDomain, rEp);
            } catch (InterruptedException | ExecutionException e) {
                // TODO add it to the status for renderer manager
                LOG.warn("Interface was not added to bridge-domain {} for endpoint {}", l2FloodDomain, rEp, e);
            }
        }
    }

    public void removeForwardingForEndpoint(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        AddressEndpointWithLocation rEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase rEpLoc = resolveAndValidateLocation(rEp);
        if (Strings.isNullOrEmpty(rEpLoc.getExternalNodeConnector())) {
            // nothing was created for endpoint therefore nothing is removed
            return;
        }

        if (!Strings.isNullOrEmpty(rEpLoc.getExternalNode())) {
            try {
                ifaceManager.deleteBridgeDomainFromInterface(rEp).get();
                LOG.debug("bridge-domain was deleted from interface for endpoint {}", rEp);
            } catch (InterruptedException | ExecutionException e) {
                // TODO add it to the status for renderer manager
                LOG.warn("bridge-domain was not deleted from interface for endpoint {}", rEp, e);
            }
        }
    }

    public static ExternalLocationCase resolveAndValidateLocation(AddressEndpointWithLocation addrEpWithLoc) {
        LocationType locationType = addrEpWithLoc.getAbsoluteLocation().getLocationType();
        if (!(locationType instanceof ExternalLocationCase)) {
            throw new IllegalStateException("Endpoint does not have external location " + addrEpWithLoc);
        }
        ExternalLocationCase result = (ExternalLocationCase) locationType;
        if (result.getExternalNodeMountPoint() == null) {
            throw new IllegalStateException("Endpoint does not have external-node-mount-point " + addrEpWithLoc);
        }
        return result;
    }

    public static Optional<String> resolveL2FloodDomain(@Nullable NetworkContainment netCont) {
        if (netCont == null) {
            return Optional.absent();
        }
        Containment containment = netCont.getContainment();
        if (containment instanceof ForwardingContextContainment) {
            ForwardingContextContainment fwCtxCont = (ForwardingContextContainment) containment;
            if (fwCtxCont.getContextType().isAssignableFrom(L2FloodDomain.class)) {
                return fwCtxCont.getContextId() == null ? null : Optional.of(fwCtxCont.getContextId().getValue());
            }
        }
        if (containment instanceof NetworkDomainContainment) {
            // TODO address missing impl
            LOG.info("Network domain containment in endpoint is not supported yet. {}", netCont);
            return Optional.absent();
        }
        return Optional.absent();
    }

}