/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.AclManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.Containment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.Parent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.NatAddressRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererForwardingContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VlanNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.LoopbackCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;

public final class ForwardingManager {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardingManager.class);
    @VisibleForTesting
    private byte WAIT_FOR_BD_PROCESSING = 60; // seconds
    private long lastVxlanVni = 1L;
    private final Map<String, VxlanVni> vxlanVniByBridgeDomain = new HashMap<>();
    private final InterfaceManager ifaceManager;
    private final AclManager aclManager;
    private final BridgeDomainManager bdManager;
    private final NatManager natManager;
    private final DataBroker dataBroker;

    public ForwardingManager(@Nonnull InterfaceManager ifaceManager, @Nonnull AclManager aclManager,
        @Nonnull NatManager natManager, @Nonnull BridgeDomainManager bdManager, @Nonnull DataBroker dataBroker) {
        this.ifaceManager = Preconditions.checkNotNull(ifaceManager);
        this.bdManager = Preconditions.checkNotNull(bdManager);
        this.natManager = Preconditions.checkNotNull(natManager);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.aclManager = Preconditions.checkNotNull(aclManager);
    }

    public Optional<GbpBridgeDomain> readGbpBridgeDomainConfig(String name) {
        InstanceIdentifier<GbpBridgeDomain> bdIid = InstanceIdentifier.builder(Config.class)
            .child(GbpBridgeDomain.class, new GbpBridgeDomainKey(name))
            .build();
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<GbpBridgeDomain> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, bdIid, rTx);
        rTx.close();
        return optBd;
    }

    public void createBridgeDomainOnNodes(SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Optional<GbpBridgeDomain> bdConfig = readGbpBridgeDomainConfig(bd);
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

    private void createVxlanBridgeDomains(final String bd, final VxlanVni vni, final Set<NodeId> vppNodes) {
        for (NodeId vppNode : vppNodes) {
            try {
                LOG.debug("Creating VXLAN bridge-domain {} on node {} with VNI {}", bd, vppNode.getValue(),
                        vni);
                bdManager.createVxlanBridgeDomainOnVppNode(bd, vni, vppNode).get(WAIT_FOR_BD_PROCESSING,
                        TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("VXLAN Bridge domain {} was not created on node {}", bd, vppNode.getValue(), e);
            } catch (TimeoutException e) {
                LOG.warn("Probably, VXLAN Bridge domain {} was not created on node {} because BridgeDomainManager "
                        + "did not respond by {} seconds. Check VBD log for more details",
                        bd, vppNode.getValue(), WAIT_FOR_BD_PROCESSING, e);
            }
        }
    }

    private void createVlanBridgeDomains(final String bd, final VlanId vlanId, final Set<NodeId> vppNodes) {
        for (NodeId vppNode : vppNodes) {
            try {
                LOG.debug("Creating VLAN bridge-domain {} on node {} with VLAN ID {}", bd, vppNode.getValue(),
                        vlanId.getValue());
                bdManager.createVlanBridgeDomainOnVppNode(bd, vlanId, vppNode).get(WAIT_FOR_BD_PROCESSING,
                        TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("VLAN Bridge domain {} was not created on node {}", bd, vppNode.getValue(), e);
            } catch (TimeoutException e) {
                LOG.warn("Probably, VLAN Bridge domain {} was not created on node {} because BridgeDomainManager "
                        + "did not respond by {} seconds. Check VBD log for more details",
                        bd, vppNode.getValue(), WAIT_FOR_BD_PROCESSING, e);
            }
        }
    }

    public void removeBridgeDomainOnNodes(final SetMultimap<String, NodeId> vppNodesByBridgeDomain) {
        for (String bd : vppNodesByBridgeDomain.keySet()) {
            Set<NodeId> vppNodes = vppNodesByBridgeDomain.get(bd);
            for (NodeId vppNode : vppNodes) {
                try {
                    bdManager.removeBridgeDomainFromVppNode(bd, vppNode).get(WAIT_FOR_BD_PROCESSING,
                            TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Bridge domain {} was not removed from node {}", bd, vppNode.getValue(), e);
                } catch (TimeoutException e) {
                    LOG.warn("Probably, bridge domain {} was not removed from node {} because BridgeDomainManager "
                            + "did not respond by {} seconds. Check VBD log for more details",
                            bd, vppNode.getValue(), WAIT_FOR_BD_PROCESSING, e);
                }
            }
        }
    }

    public void createForwardingForEndpoint(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        AddressEndpointWithLocation rEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase rEpLoc = resolveAndValidateLocation(rEp);
        if (Strings.isNullOrEmpty(rEpLoc.getExternalNodeConnector())) {
            // TODO add it to the status for renderer manager
            LOG.info("Renderer endpoint does not have external-node-connector therefore it is ignored {}", rEp);
            return;
        }

        if (Strings.isNullOrEmpty(rEpLoc.getExternalNode())) {
            java.util.Optional<String> optL2FloodDomain = resolveL2FloodDomain(rEp, policyCtx);
            if (!optL2FloodDomain.isPresent()) {
                // TODO add it to the status for renderer manager
                LOG.info("Renderer endpoint does not have l2FloodDomain as network containment {}", rEp);
                return;
            }
            String l2FloodDomain = optL2FloodDomain.get();
            try {
                ifaceManager.addBridgeDomainToInterface(l2FloodDomain, rEp, AccessListUtil.resolveAclsOnInterface(
                        rEpKey, policyCtx), isBviForEndpoint(rEp)).get();
                aclManager.updateAclsForPeers(policyCtx, rEpKey);
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

    private boolean isBviForEndpoint(AddressEndpointWithLocation rEp) {
        VppEndpointKey vppEndpointKey =
            new VppEndpointKey(rEp.getAddress(), rEp.getAddressType(), rEp.getContextId(), rEp.getContextType());
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> vppEndpointOptional =
            DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEndpointKey).build(), rTx);
        if (vppEndpointOptional.isPresent()) {
            InterfaceTypeChoice interfaceTypeChoice = vppEndpointOptional.get().getInterfaceTypeChoice();
            if (interfaceTypeChoice instanceof LoopbackCase) {
                LOG.trace("Vpp renderer endpoint {} IS a BVI interface.", rEp.getKey());
                return ((LoopbackCase) interfaceTypeChoice).isBvi();
            }
        }
        rTx.close();
        LOG.trace("Vpp renderer endpoint {} IS NOT a BVI interface.", rEp.getKey());
        return false;
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

    public static java.util.Optional<String> resolveL2FloodDomain(@Nonnull AddressEndpointWithLocation ep,
            @Nonnull PolicyContext policyCtx) {
        NetworkContainment netCont = ep.getNetworkContainment();
        if (netCont == null) {
            return java.util.Optional.empty();
        }
        Containment containment = netCont.getContainment();
        if (containment instanceof ForwardingContextContainment) {
            ForwardingContextContainment fwCtxCont = (ForwardingContextContainment) containment;
            if (L2FloodDomain.class.equals(fwCtxCont.getContextType())) {
                return fwCtxCont.getContextId() == null ? java.util.Optional.empty() : java.util.Optional
                    .of(fwCtxCont.getContextId().getValue());
            }
        }
        if (containment instanceof NetworkDomainContainment) {
            final NetworkDomainContainment netDomainCont = (NetworkDomainContainment) containment;
            final TenantId tenantId = ep.getTenant();
            final NetworkDomainId domainId = netDomainCont.getNetworkDomainId();
            final Class<? extends NetworkDomain> domainKey = netDomainCont.getNetworkDomainType();
            final RendererNetworkDomainKey rendererNetworkDomainKey = new RendererNetworkDomainKey(domainId, domainKey);
            final RendererNetworkDomain rendererNetworkDomain =
                    policyCtx.getNetworkDomainTable().get(tenantId, rendererNetworkDomainKey);
            if (rendererNetworkDomain == null) {
                LOG.debug("Network domain not found. Containment: {}", containment);
                return java.util.Optional.empty();
            }
            java.util.Optional<String> optL2Fd = getForwardingCtxForParent(ep.getTenant(),
                    rendererNetworkDomain.getParent(), policyCtx.getForwardingCtxTable())
                        .filter(fwdCtx -> L2FloodDomain.class.equals(fwdCtx.getContextType()))
                        .map(RendererForwardingContext::getContextId)
                        .map(ContextId::getValue);
            if (!optL2Fd.isPresent()) {
                LOG.debug("network-domain-containment in endpoint does not have L2-flood-domain as parent. "
                        + "This case is not supported in VPP renderer. {}", ep);
            }
            return optL2Fd;
        }
        return java.util.Optional.empty();
    }

    private static @Nonnull java.util.Optional<RendererForwardingContext> getForwardingCtxForParent(
            @Nullable TenantId tenant, @Nullable Parent parent,
            Table<TenantId, RendererForwardingContextKey, RendererForwardingContext> forwardingCtxTable) {
        if (tenant == null || parent == null) {
            return java.util.Optional.empty();
        }
        if (parent.getContextId() != null && parent.getContextType() != null) {
            return java.util.Optional.ofNullable(forwardingCtxTable.get(tenant,
                    new RendererForwardingContextKey(parent.getContextId(), parent.getContextType())));
        }
        return java.util.Optional.empty();
    }

    public void syncNatEntries(PolicyContext policyCtx) {
        Configuration cfg = policyCtx.getPolicy().getConfiguration();
        if(cfg != null) {
            List<MappingEntryBuilder> natEntries = resolveNatTableEntries(cfg.getEndpoints());
            if (natEntries.isEmpty()) {
                LOG.trace("NAT entries are empty, skipping processing.");
                return;
            }
            LOG.trace("Syncing NAT entries {}", natEntries);
            if (cfg.getRendererForwarding() != null) {
                for (RendererForwardingByTenant fwd : cfg.getRendererForwarding().getRendererForwardingByTenant()) {
                    List<InstanceIdentifier<PhysicalInterface>> physIfacesIid =
                        resolvePhysicalInterfacesForNat(fwd.getRendererNetworkDomain());
                    natManager.submitNatChanges(physIfacesIid, natEntries, true);
                }
            }
        }
    }

    public void deleteNatEntries(PolicyContext policyCtx) {
        Configuration cfg = policyCtx.getPolicy().getConfiguration();
        if(cfg != null) {
            List<MappingEntryBuilder> natEntries = resolveNatTableEntries(cfg.getEndpoints());
            if (natEntries.isEmpty()) {
                LOG.trace("NAT entries are empty,nothing to delete, skipping processing.");
                return;
            }
            LOG.trace("Deleting NAT entries {}", natEntries);
            if (cfg.getRendererForwarding() != null) {
                for (RendererForwardingByTenant fwd : cfg.getRendererForwarding().getRendererForwardingByTenant()) {
                    List<InstanceIdentifier<PhysicalInterface>> physIfacesIid =
                        resolvePhysicalInterfacesForNat(fwd.getRendererNetworkDomain());
                    natManager.submitNatChanges(physIfacesIid, natEntries, false);
                }
            }
        }
    }

    public List<InstanceIdentifier<PhysicalInterface>> resolvePhysicalInterfacesForNat(
            List<RendererNetworkDomain> rendNetDomains) {
        List<InstanceIdentifier<PhysicalInterface>> physIfaces = new ArrayList<>();
        for (RendererNetworkDomain rendDomain : rendNetDomains) {
            Optional<IpPrefix> resolvedIpPrefix = resolveIpPrefix(rendDomain);
            if (resolvedIpPrefix.isPresent()) {
                Optional<InstanceIdentifier<PhysicalInterface>> resPhIface = natManager.resolvePhysicalInterface(resolvedIpPrefix.get());
                if (resPhIface.isPresent()) {
                    physIfaces.add(resPhIface.get());
                }
            }
        }
        return physIfaces;
    }

    private Optional<IpPrefix> resolveIpPrefix(RendererNetworkDomain rendDomain) {
        SubnetAugmentRenderer subnetAug = rendDomain.getAugmentation(SubnetAugmentRenderer.class);
        if (subnetAug.getSubnet() != null) {
            return Optional.of(subnetAug.getSubnet().getIpPrefix());
        }
        return Optional.absent();
    }

    public List<MappingEntryBuilder> resolveNatTableEntries(Endpoints endpoints) {
        List<MappingEntryBuilder> sNatEntries = new ArrayList<>();
        for (AddressEndpointWithLocation addrEp : endpoints.getAddressEndpointWithLocation()) {
            if (addrEp.getAugmentation(NatAddressRenderer.class) == null) {
                continue;
            }
            NatAddressRenderer natAddr = addrEp.getAugmentation(NatAddressRenderer.class);
            if (natAddr.getNatAddress() == null && natAddr.getNatAddress().getIpv4Address() == null) {
                LOG.warn("Only Ipv4 SNAT is currently supported. Cannot apply SNAT for [{},{}]", addrEp.getAddress(),
                        natAddr.getNatAddress());
                continue;
            }
            Optional<MappingEntryBuilder> entry = natManager.resolveSnatEntry(addrEp.getAddress(), natAddr.getNatAddress()
                .getIpv4Address());
            if (entry.isPresent()) {
                sNatEntries.add(entry.get());
            }
        }
        return sNatEntries;
    }

    @VisibleForTesting
    void setTimer(byte time) {
        WAIT_FOR_BD_PROCESSING = time;
    }
}
