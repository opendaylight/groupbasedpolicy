/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.Containment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VlanNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.SetMultimap;

public final class ForwardingManager {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardingManager.class);
    @VisibleForTesting
    static long WAIT_FOR_BD_CREATION = 10; // seconds
    private long lastVxlanVni = 1L;
    private final Map<String, VxlanVni> vxlanVniByBridgeDomain = new HashMap<>();
    private final InterfaceManager ifaceManager;
    private final BridgeDomainManager bdManager;
    private final DataBroker dataBroker;
    public ForwardingManager(@Nonnull InterfaceManager ifaceManager, @Nonnull BridgeDomainManager bdManager, @Nonnull DataBroker dataBroker) {
        this.ifaceManager = Preconditions.checkNotNull(ifaceManager);
        this.bdManager = Preconditions.checkNotNull(bdManager);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    public Optional<BridgeDomain> readBridgeDomainConfig(String name) {
        InstanceIdentifier<BridgeDomain> bdIid = InstanceIdentifier.builder(Config.class)
            .child(BridgeDomain.class, new BridgeDomainKey(name))
            .build();
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, bdIid, rTx);
    }

    public void createBridgeDomainOnNodes(SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Optional<BridgeDomain> bdConfig = readBridgeDomainConfig(bd);
            Set<NodeId> vppNodes = vppNodesByBridgeDomain.get(bd);
            if (bdConfig.isPresent()) {
                if (bdConfig.get().getType().equals(VlanNetwork.class)) {
                    createVlanBridgeDomains(bd, bdConfig.get().getVlan(), vppNodes);
                }
            } else {
                VxlanVni vxlanVni = vxlanVniByBridgeDomain.get(bd);
                if (vxlanVni == null) {
                    vxlanVni = new VxlanVni(lastVxlanVni++);
                    vxlanVniByBridgeDomain.put(bd, vxlanVni);
                }
                createVxlanBridgeDomains(bd, vxlanVni, vppNodes);
            }
        }
    }

    private void createVxlanBridgeDomains(String bd, VxlanVni vni, Set<NodeId> vppNodes) {
        for (NodeId vppNode : vppNodes) {
            try {
                LOG.trace("Creating VXLAN bridge-domain {} on node {} with VNI {}", bd, vppNode.getValue(),
                        vni);
                // TODO think about propagating ListenableFuture - timeout set as workaround
                bdManager.createVxlanBridgeDomainOnVppNode(bd, vni, vppNode).get(WAIT_FOR_BD_CREATION,
                        TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("VXLAN Bridge domain {} was not created on node {}", bd, vppNode.getValue(), e);
            } catch (TimeoutException e) {
                LOG.warn("Probably, VXLAN Bridge domain {} was not created on node {} because BridgeDomainManager "
                        + "did not respond by {} seconds.", bd, vppNode.getValue(), WAIT_FOR_BD_CREATION, e);
            }
        }
    }

    private void createVlanBridgeDomains(String bd, VlanId vlanId, Set<NodeId> vppNodes) {
        for (NodeId vppNode : vppNodes) {
            try {
                LOG.trace("Creating VLAN bridge-domain {} on node {} with VLAN ID {}", bd, vppNode.getValue(),
                        vlanId.getValue());
                // TODO think about propagating ListenableFuture - timeout set as workaround
                bdManager.createVlanBridgeDomainOnVppNode(bd, vlanId, vppNode).get(WAIT_FOR_BD_CREATION,
                        TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("VLAN Bridge domain {} was not created on node {}", bd, vppNode.getValue(), e);
            } catch (TimeoutException e) {
                LOG.warn("Probably, VLAN Bridge domain {} was not created on node {} because BridgeDomainManager "
                        + "did not respond by {} seconds.", bd, vppNode.getValue(), WAIT_FOR_BD_CREATION, e);
            }
        }
    }

    public void removeBridgeDomainOnNodes(SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Set<NodeId> vppNodes = vppNodesByBridgeDomain.get(bd);
            for (NodeId vppNode : vppNodes) {
                try {
                    bdManager.removeBridgeDomainFromVppNode(bd, vppNode).get(WAIT_FOR_BD_CREATION,
                            TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Bridge domain {} was not removed from node {}", bd, vppNode.getValue(), e);
                } catch (TimeoutException e) {
                    LOG.warn("Probably, bridge domain {} was not removed from node {} because BridgeDomainManager "
                            + "did not respond by {} seconds.", bd, vppNode.getValue(), WAIT_FOR_BD_CREATION, e);
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
        } else {
            LOG.debug("Forwarding is not created - Location of renderer endpoint contains "
                    + "external-node therefore VPP renderer assumes that interface for endpoint is "
                    + "already assigned in bridge-domain representing external-node. {}", rEp);
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
        } else {
            LOG.debug("Forwarding is not removed - Location of renderer endpoint does not contain "
                    + "external-node therefore VPP renderer assumes that interface for endpoint is not "
                    + "assigned to bridge-domain representing external-node. {}", rEp);
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
