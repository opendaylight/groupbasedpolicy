/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.StaticArpCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.EndpointHost;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.IpAddressUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppEndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.routing.RoutingManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.LispUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class FlatOverlayManager {
    private static final Logger LOG = LoggerFactory.getLogger(FlatOverlayManager.class);

    private ConfigManagerHelper overlayHelper;
    private DataBroker dataBroker;

    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    private HostRelatedInfoContainer hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();
    // Node ID, VRF ID, route count
    private Map<String, Map<Long, Long>> vrfsByHostname = new HashMap<>();

    private StaticRoutingHelper staticRoutingHelper;
    private VppEndpointListener vppEndpointListener;

    public FlatOverlayManager(@Nonnull DataBroker dataBroker,
                              @Nonnull MountedDataBrokerProvider mountedDataBrokerProvider,
                              @Nonnull VppEndpointListener vppEndpointListener) {
        this.overlayHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        staticRoutingHelper = new StaticRoutingHelper();
        this.dataBroker = dataBroker;
        this.vppEndpointListener = vppEndpointListener;
    }

    public void configureEndpointForFlatOverlay(AddressEndpointWithLocation addressEp) {
        addStaticRoute(addressEp);
        addStaticArpAndInfcsToVrf(addressEp);
    }

    private void addInterfaceInVrf(String hostName, String interfaceName, long vrf) {
        if (hostRelatedInfoContainer.getPortInterfaceStateOfHost(hostName)
                .isVrfConfiguredForInterface(interfaceName)) {
            return;
        }

        if (!putVrfInInterface(hostName, interfaceName, vrf)) {
            LOG.warn("Failed to put interface {} to vrf {}", interfaceName, vrf);
        } else {
            hostRelatedInfoContainer
                    .getPortInterfaceStateOfHost(hostName)
                    .initializeRoutingContextForInterface(interfaceName, vrf);
            LOG.debug("Added interface {} to vrf {}", interfaceName, vrf);
        }
    }

    private boolean putVrfInInterface(String hostName, String interfaceName, Long vrf) {
        InstanceIdentifier<Routing> iid = VppIidFactory.getRoutingIid(new InterfaceKey(interfaceName));
        RoutingBuilder builder = new RoutingBuilder();
        builder.setIpv4VrfId(vrf);
        return GbpNetconfTransaction.netconfSyncedWrite(LispUtil.HOSTNAME_TO_IID.apply(hostName), iid,
                builder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticArpAndInfcsToVrf(AddressEndpointWithLocation addressEp) {
        if (!addressEp.getAddressType().equals(IpPrefixType.class)) {
            return;
        }
        Map<String, String> intfcsByHostname = resolveIntfcsByHosts(addressEp);
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<Config> cfg = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getVppRendererConfig(), readTx);
        readTx.close();
        intfcsByHostname.entrySet().forEach(intfcByHost -> {
            if (cfg.isPresent() && !cfg.get().getVppEndpoint().isEmpty()) {
                java.util.Optional<VppEndpoint> foundVpp = cfg.get()
                    .getVppEndpoint()
                    .stream()
                    .filter(vpp -> vpp.getVppInterfaceName().equals(intfcByHost.getValue()))
                    .findFirst();
                if (!foundVpp.isPresent()) {
                    return;
                }
                Ipv4Address ipv4 = ConfigManagerHelper.getInterfaceIp(addressEp);
                putStaticArp(intfcByHost.getKey(), new InterfaceKey(intfcByHost.getValue()),
                        new PhysAddress(foundVpp.get().getAddress()), new Ipv4AddressNoZone(ipv4));
                addInterfaceInVrf(intfcByHost.getKey(), intfcByHost.getValue(),
                        getVni(addressEp.getTenant().getValue()));
            }
        });
    }

    private boolean putStaticArp(String hostName, InterfaceKey interfaceKey, PhysAddress physAddress,
        Ipv4AddressNoZone ip) {
        StaticArpCommand.StaticArpCommandBuilder staticArpCommandBuilder =
            new StaticArpCommand.StaticArpCommandBuilder();

        staticArpCommandBuilder.setOperation(General.Operations.PUT);
        staticArpCommandBuilder.setInterfaceKey(interfaceKey);
        staticArpCommandBuilder.setIp(ip);
        staticArpCommandBuilder.setLinkLayerAddress(physAddress);

        return GbpNetconfTransaction.netconfSyncedWrite(LispUtil.HOSTNAME_TO_IID.apply(hostName),
                staticArpCommandBuilder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticRoute(AddressEndpointWithLocation addressEp) {
        //TODO caller method
        Optional<Long> routeId = routeId(addressEp);
        if(!routeId.isPresent()) {
            return;
        }

        //TODO workaround for interfaces that do not exist anymore
        List<ChildEndpoint> childs = addressEp.getChildEndpoint();
        if (childs != null) {
            for (ChildEndpoint child : childs) {
                child.getAddress();
                child.getContextId();
                VppEndpointKey vppEndpointKey = new VppEndpointKey(child.getAddress(), child.getAddressType(),
                        child.getContextId(), child.getContextType());
                ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
                Optional<VppEndpoint> vppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                        VppIidFactory.getVppRendererConfig().child(VppEndpoint.class, vppEndpointKey), rTx);
                rTx.close();
                if (!vppEp.isPresent()) {
                    LOG.warn("Failed to add route for endpoint {}. Interface not created yet.", addressEp);
                    return;
                }
            }
        }

        Map<String, String> hostnamesAndIntfcs = resolveIntfcsByHosts(addressEp);
        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;

        hostnamesAndIntfcs.forEach((hostname, outgoingInterfaceName) -> {

            Ipv4Address ipWithoutPrefix = ConfigManagerHelper.getInterfaceIp(addressEp);

            Ipv4Prefix ipv4Prefix = overlayHelper.getInterfaceIpAsPrefix(addressEp);
            LOG.trace("Adding static route for addressEp: {}", addressEp);
            addStaticRoute(routeId.get(), hostname, vrf, ipWithoutPrefix, ipv4Prefix, outgoingInterfaceName);
        });
    }

    public static Map<String,String> resolveIntfcsByHosts(AddressEndpointWithLocation addressEp) {
        Map<String, String> hostnamesAndIntfcs = new HashMap<>();
        if (addressEp.getRelativeLocations() != null
            && addressEp.getRelativeLocations().getExternalLocation() != null) {
            LOG.trace("deleteStaticRoutingEntry -> addresEp locations: {}", addressEp.getRelativeLocations().getExternalLocation());
            addressEp.getRelativeLocations().getExternalLocation().forEach(externalLocation -> {
                Optional<String> interfaceOptional =
                    VppPathMapper.interfacePathToInterfaceName(externalLocation.getExternalNodeConnector());
                if (interfaceOptional.isPresent()) {
                    hostnamesAndIntfcs.put(
                        externalLocation.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId().getValue(),
                        interfaceOptional.get());
                } else {
                    LOG.warn("Couldn't resolve interface name for addrEP: {}", addressEp);
                }
            });
        } else if (addressEp.getAbsoluteLocation() != null && addressEp.getAbsoluteLocation().getLocationType() != null
            && addressEp.getAbsoluteLocation().getLocationType() instanceof ExternalLocationCase) {
            ExternalLocationCase externalLocationCase =
                (ExternalLocationCase) addressEp.getAbsoluteLocation().getLocationType();
            LOG.trace("deleteStaticRoutingEntry -> addresEp location: {}", externalLocationCase);
            Optional<String> interfaceOptional =
                VppPathMapper.interfacePathToInterfaceName(externalLocationCase.getExternalNodeConnector());
            if (interfaceOptional.isPresent()) {
                hostnamesAndIntfcs.put(
                    externalLocationCase.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId().getValue(),
                    interfaceOptional.get());
            } else {
                LOG.warn("Couldn't resolve interface name for addrEP: {}", addressEp);
            }
        }
        return hostnamesAndIntfcs;
    }

    private boolean addStaticRoute(Long routeId, String hostName, long vrfId, Ipv4Address ipWithoutPrefix,
            Ipv4Prefix ipv4Prefix, String outgoingInterfaceName) {
        if (vrfsByHostname.get(hostName) == null || !vrfsByHostname.get(hostName).keySet().contains(vrfId)) {
            if (staticRoutingHelper.addRoutingProtocolForVrf(LispUtil.HOSTNAME_TO_IID.apply(hostName), vrfId)) {
                addStaticRouteToPublicInterface(hostName, vrfId);
                countPlusPlus(hostName, vrfId);
            }
        }
        if (staticRoutingHelper.addSingleStaticRouteInRoutingProtocol(routeId, hostName, vrfId, ipWithoutPrefix,
                ipv4Prefix, outgoingInterfaceName, null)) {
            countPlusPlus(hostName, vrfId);
            return true;
        }
        return false;
    }

    private void countPlusPlus(String hostName, Long vrfId) {
        if (vrfsByHostname.get(hostName) == null || vrfsByHostname.get(hostName).get(vrfId) == null) {
            HashMap<Long, Long> newEntry = new HashMap<>();
            newEntry.put(vrfId, 1L);
            vrfsByHostname.put(hostName, newEntry);
        } else {
            Long count = vrfsByHostname.get(hostName).get(vrfId);
            HashMap<Long, Long> newEntry = new HashMap<>();
            newEntry.put(vrfId, count + 1);
            vrfsByHostname.put(hostName, newEntry);
        }
    }

    private void countMinusMinus(String hostName, Long vrfId) {
        if (vrfsByHostname.get(hostName) != null && vrfsByHostname.get(hostName).get(vrfId) != null) {
            Long count = vrfsByHostname.get(hostName).get(vrfId);
            HashMap<Long, Long> newEntry = new HashMap<>();
            newEntry.put(vrfId, count - 1);
            vrfsByHostname.put(hostName, newEntry);
        }
    }

    private Long getRouteCount(String hostName, Long vrfId) {
        if (vrfsByHostname.get(hostName) != null && vrfsByHostname.get(hostName).get(vrfId) != null) {
            return vrfsByHostname.get(hostName).get(vrfId);
        }
        return 0L;
    }

    private void addStaticRouteToPublicInterface(String hostName, long vrfId) {
        LOG.trace("addStaticRouteToPublicInterface, hostname: {}, vrfId: {}", hostName, vrfId);
        Ipv4Address physicalInterfaceIp = hostRelatedInfoContainer.getPhysicalInterfaceState(hostName)
            .getIp(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC)
            .getIpv4Address();
        String interfaceName = hostRelatedInfoContainer.getPhysicalInterfaceState(hostName)
            .getName(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC);
        if (interfaceName != null && !interfaceName.isEmpty()) {
            LOG.trace("Adding Public interface route. hostname: {}, VrfId: {}, Ip: {}, Gw: {}, InterfaceName: {}.",
                    hostName, vrfId, physicalInterfaceIp, null, interfaceName);
            if (!staticRoutingHelper.addSingleStaticRouteInRoutingProtocol(0L, hostName, vrfId, physicalInterfaceIp,
                    IpAddressUtil.toIpV4Prefix(physicalInterfaceIp), interfaceName,
                    new VniReference(RoutingManager.DEFAULT_TABLE))) {
                LOG.warn("Failed to add route for physical interface in vrf {} compute host {}", vrfId, hostName);
            } else {
                LOG.debug("addStaticRouteToPublicInterface -> Added route to public intf ({} via {}) in vrf {} in compute host {}",
                        IpAddressUtil.toIpV4Prefix(physicalInterfaceIp), interfaceName, vrfId, hostName);
            }
        }
    }

    private Optional<Long> routeId(AddressEndpointWithLocation addressEp) {
        if (!addressEp.getAddressType().equals(IpPrefixType.class) || !addressEp.getAddress().contains("/")) {
            return Optional.absent();
        }
        String ipAddress = addressEp.getAddress().split("/")[0];
        SubnetUtils subnet = new SubnetUtils(addressEp.getAddress());
        Long routeId = Integer.toUnsignedLong(subnet.getInfo().asInteger(ipAddress));
        return Optional.of(routeId);
    }

    public void deleteStaticRoutingEntry(AddressEndpointWithLocation addressEp) {
        //TODO create
        Optional<Long> routeId = routeId(addressEp);
        if(!routeId.isPresent()) {
            return;
        }
        long vni = getVni(addressEp.getTenant().getValue());
        long vrfId = vni;
        Map<String, String> hostnamesAndIntfcs = resolveIntfcsByHosts(addressEp);
        LOG.trace("deleteStaticRoutingEntry -> addresEp locations: {}", addressEp);
        hostnamesAndIntfcs.entrySet().forEach(intfcsByHost -> {
            LOG.trace("deleteStaticRoutingEntry -> Deleting addresEp: {} for interface: {}, on node: {}", addressEp.getKey(), intfcsByHost.getValue(), intfcsByHost.getKey());
            Ipv4Address ipWithoutPrefix = ConfigManagerHelper.getInterfaceIp(addressEp);
            if (!staticRoutingHelper.deleteSingleStaticRouteFromRoutingProtocol(intfcsByHost.getKey(), vrfId, routeId.get())) {
                LOG.warn("Failed to delete route ({} via {}) from vrf {} from host{}", ipWithoutPrefix,
                    intfcsByHost.getValue(), vrfId, intfcsByHost);
            } else {
                LOG.trace("deletedStaticRoutingEntry -> Deleted addresEp: {} for interface: {}, on node: {}", addressEp.getKey(), intfcsByHost.getValue(), intfcsByHost.getKey());
                countMinusMinus(intfcsByHost.getKey(), vrfId);
                if (getRouteCount(intfcsByHost.getKey(), vrfId) <= 1) {
                    LOG.info("deletedStaticRoutingEntry -> Removing route to public int from VRF {}", vrfId);
                    staticRoutingHelper.deleteSingleStaticRouteFromRoutingProtocol(intfcsByHost.getKey(), vrfId, 0L);
                    InstanceIdentifier<RoutingProtocol> protocol =
                            VppIidFactory.getRoutingInstanceIid(StaticRoutingHelper.getRoutingProtocolName(vrfId));
                    if(GbpNetconfTransaction.netconfSyncedDelete(
                            VppIidFactory.getNetconfNodeIid(new NodeId(intfcsByHost.getKey())), protocol,
                            GbpNetconfTransaction.RETRY_COUNT)) {
                        vrfsByHostname.get(intfcsByHost.getKey()).remove(vrfId);
                    }
                }
                LOG.trace("deleteStaticRoutingEntry -> flushPendingVppEndpoint for addresEp: {}", addressEp);
                hostRelatedInfoContainer.deleteRouteFromIntfc(intfcsByHost.getKey(), intfcsByHost.getValue(), routeId.get());
                vppEndpointListener.flushPendingVppEndpoint(intfcsByHost.getKey(), intfcsByHost.getValue());
                LOG.debug("Delete Static Route ({} via {}) from vrf {} from host {}", ipWithoutPrefix,
                        intfcsByHost.getValue(), vrfId, intfcsByHost);
            }
        });

    }

    public long getVni(String tenantUuid) {
        long vni = neutronTenantToVniMapper.getVni(tenantUuid);
        LOG.debug("Debugging: getVni {} = {}", tenantUuid, vni);
        return vni;
    }
}
