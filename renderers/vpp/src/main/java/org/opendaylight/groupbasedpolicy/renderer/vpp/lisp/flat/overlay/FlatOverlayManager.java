/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.StaticArpCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.EndpointHost;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortRouteState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.SubnetState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfHolder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.IpAddressUtil;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlatOverlayManager {
    private static final Logger LOG = LoggerFactory.getLogger(FlatOverlayManager.class);

    private ConfigManagerHelper overlayHelper;
    private DataBroker dataBroker;

    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    private HostRelatedInfoContainer hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();

    private StaticRoutingHelper staticRoutingHelper;

    public FlatOverlayManager(@Nonnull DataBroker dataBroker,
                              @Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.overlayHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        staticRoutingHelper = new StaticRoutingHelper();
        this.dataBroker = dataBroker;
    }

    public void configureEndpointForFlatOverlay(AddressEndpointWithLocation addressEp) {
        if (!overlayHelper.hasRelativeLocations(addressEp)) {
            configureInterfaceForFlatOverlay(addressEp);
            addStaticArp(addressEp);
            addStaticRoute(addressEp);
        } else {
            Ipv4Address metadataIp = overlayHelper.getInterfaceIp(addressEp);
            Ipv4Prefix metadataIpPrefix = overlayHelper.getInterfaceIpAsPrefix(addressEp);
            addressEp.getRelativeLocations().getExternalLocation().forEach(externalLocation -> {
                String hostName = overlayHelper.getHostName(externalLocation).get();
                String metadataInterfaceName = overlayHelper.getInterfaceName(externalLocation).get();

                long vrf = getVni(addressEp.getTenant().getValue());

                String metadataSubnetUuid = Constants.METADATA_SUBNET_UUID;

                PortInterfaces portInterfacesOfHost = hostRelatedInfoContainer.getPortInterfaceStateOfHost(hostName);

                if (!portInterfacesOfHost.isInterfaceConfiguredForMetadata(metadataInterfaceName)) {
                    addInterfaceInVrf(hostName, metadataInterfaceName, vrf);
                    String physicalAddress = resolvePhysicalAddress(hostName, metadataInterfaceName);
                    addStaticArp(hostName, metadataInterfaceName, physicalAddress, metadataIp);
                    addStaticRoute(hostName, vrf, metadataSubnetUuid, metadataIp, metadataIpPrefix,
                            metadataInterfaceName);
                    portInterfacesOfHost.addInterfaceInMetadataInterfaceSet(metadataInterfaceName);
                }
            });
        }
    }

    private String resolvePhysicalAddress(String hostName, String metadataInterfaceName) {
        String physAddress = null;
        Optional<Config> configOptional =
            DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getVppRendererConfig(),
                dataBroker.newReadOnlyTransaction());
        if (configOptional.isPresent() && configOptional.get().getVppEndpoint() != null) {
            java.util.Optional<VppEndpoint> vppEndpointOptional = configOptional.get().getVppEndpoint().stream()
                    .filter(vppEndpoint -> vppEndpoint.getVppNodeId().getValue().equals(hostName))
                    .filter(vppEndpoint -> vppEndpoint.getVppInterfaceName().equals(metadataInterfaceName))
                    .findFirst();
            if (vppEndpointOptional.isPresent() && vppEndpointOptional.get()
                .getInterfaceTypeChoice() instanceof TapCase) {
                TapCase tapCase = (TapCase) vppEndpointOptional.get().getInterfaceTypeChoice();
                physAddress = tapCase.getPhysicalAddress().getValue();
                LOG.trace("Resolved PhysicalAddress : {} for metadataInterfaceName: {}, on node: {}", physAddress
                    , metadataInterfaceName, hostName);
            } else {
                LOG.warn("PhysicalAddress was not resolved for metadataInterfaceName: {}, on node: {}",
                    metadataInterfaceName, hostName);
            }
        }

        return physAddress;
    }

    public void handleEndpointDeleteForFlatOverlay(AddressEndpointWithLocation addressEp) {
        if (overlayHelper.hasRelativeLocations(addressEp)) {
            // okay to ignore
            // routes will be deleted on interface delete
            return;
        }
        deleteStaticRoute(addressEp);
    }

    public void handleInterfaceDeleteForFlatOverlay(VppEndpoint vppEndpoint) {
        String hostName = vppEndpoint.getVppNodeId().getValue();
        String interfaceName = vppEndpoint.getVppInterfaceName();
        LOG.trace("handleInterfaceDeleteForFlatOverlay: hostname: {}, interfaceName: {}", hostName,interfaceName);

        Preconditions.checkNotNull(hostName, "Hostname cannot be null when deleting Interface");
        Preconditions.checkNotNull(interfaceName, "InterfaceName cannot be null when deleting Interface");

        staticRoutingHelper.deleteAllRoutesThroughInterface(hostName, interfaceName);

        PortInterfaces portInterfaces = hostRelatedInfoContainer.getPortInterfaceStateOfHost(hostName);
        portInterfaces.removePortInterface(interfaceName);
    }

    private void configureInterfaceForFlatOverlay(AddressEndpointWithLocation addressEp) {
        addInterfaceInVrf(addressEp);
    }

    private void addInterfaceInVrf(AddressEndpointWithLocation addressEp) {
        EndpointHost endpointHost = overlayHelper.getEndpointHostInformation(addressEp);
        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;
        Optional<String> interfaceNameOptional = overlayHelper.getInterfaceName(addressEp);

        Preconditions.checkArgument(interfaceNameOptional.isPresent());

        addInterfaceInVrf(endpointHost.getHostName(), interfaceNameOptional.get(), vrf);
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

    private boolean putVrfInInterface(String hostName,
                                  String interfaceName,
                                  Long vrf) {
        InstanceIdentifier<Routing> iid = VppIidFactory.getRoutingIid(new InterfaceKey(interfaceName));
        RoutingBuilder builder = new RoutingBuilder();
        builder.setIpv4VrfId(vrf);
        return GbpNetconfTransaction.netconfSyncedWrite(LispUtil.HOSTNAME_TO_IID.apply(hostName), iid,
                builder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticArp(AddressEndpointWithLocation addressEp) {
        String hostName = overlayHelper.getHostName(addressEp).get();
        String physicalAddress = overlayHelper.getPhysicalAddress(addressEp);
        Optional<String> interfaceNameOptional = overlayHelper.getInterfaceName(addressEp);

        Preconditions.checkArgument(interfaceNameOptional.isPresent());

        String interfaceName = interfaceNameOptional.get();

        addStaticArp(hostName, interfaceName, physicalAddress, overlayHelper.getInterfaceIp(addressEp));
    }

    private void addStaticArp(String hostName,
                              String interfaceName,
                              String physicalAddress,
                              Ipv4Address ipv4Address) {
        Ipv4AddressNoZone ip = new Ipv4AddressNoZone(ipv4Address);
        if (physicalAddress == null || physicalAddress.isEmpty()) {
            LOG.warn("Cannot add static arp for interface {}, ip={}, because physical address is null or empty",
                interfaceName, ip, physicalAddress);
            return;
        }
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        if (!putStaticArp(hostName,
                             interfaceKey,
                             new PhysAddress(physicalAddress),
                             ip)) {
            LOG.warn("Failed to put static arp with interface {} for ip={} and physical-address={}",
                    interfaceName, ip, physicalAddress);
        } else {
            LOG.debug("Added Static arp ({} {}) in host {} for interface {}", ip, physicalAddress, hostName,
                    interfaceName);
        }
    }

    private boolean putStaticArp(String hostName,
                                 InterfaceKey interfaceKey,
                                 PhysAddress physAddress,
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
        String hostName = overlayHelper.getHostName(addressEp).get();
        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;

        String portSubnetUuid = overlayHelper.getSubnet(addressEp);

        String outgoingInterfaceName = overlayHelper.getInterfaceName(addressEp).get();
        Ipv4Address ipWithoutPrefix = overlayHelper.getInterfaceIp(addressEp);

        if (overlayHelper.isMetadataPort(addressEp)) {
            //override original port subnet to handle the Absolute location case of address endpoint
            portSubnetUuid = Constants.METADATA_SUBNET_UUID;
        }

        Ipv4Prefix ipv4Prefix = overlayHelper.getInterfaceIpAsPrefix(addressEp);

        addStaticRoute(hostName, vrf, portSubnetUuid, ipWithoutPrefix, ipv4Prefix, outgoingInterfaceName);
    }

    private void addStaticRoute(String hostName, long vrfId, String hostIpSubnetUuid,
                                Ipv4Address ipWithoutPrefix, Ipv4Prefix ipv4Prefix, String outgoingInterfaceName) {

        VrfHolder vrfHolderOfHost = hostRelatedInfoContainer.getVrfStateOfHost(hostName);

        if (!vrfHolderOfHost.hasVrf(vrfId)) {
            if (!staticRoutingHelper.addRoutingProtocolForVrf(LispUtil.HOSTNAME_TO_IID.apply(hostName), vrfId,
                    vrfHolderOfHost)) {
                LOG.warn("Failed to add Routing protocol for host {} and vrf {}!", hostName, vrfId);
            } else {
                addStaticRouteToPublicInterface(hostName, vrfId);
            }
        }

        VrfState vrfStateOfVrfId = vrfHolderOfHost.getVrfState(vrfId);

        if (vrfStateOfVrfId.getSubnetHolder().getSubnetState(hostIpSubnetUuid).isIpPresent(ipWithoutPrefix)) {
            LOG.info("Ip already exists in host {} vrf {} ip {}", hostName, vrfId, ipWithoutPrefix);
            return;
        }

        if (!staticRoutingHelper.addSingleStaticRouteInRoutingProtocol(hostName, vrfId, hostIpSubnetUuid,
                ipWithoutPrefix, ipv4Prefix, outgoingInterfaceName, null)) {
            LOG.warn("Failed to add routing ({} via {}) in vrf {} in compute host {}!",
                    ipv4Prefix, outgoingInterfaceName, vrfId, hostName);
        } else {
            LOG.debug("Added route ({} via {}) in vrf {} in compute host {}",
                    ipv4Prefix, outgoingInterfaceName, vrfId, hostName);
        }
    }

    private void addStaticRouteToPublicInterface(String hostName, long vrfId) {
        LOG.trace("addStaticRouteToPublicInterface, hostname: {}, vrfId: {}", hostName, vrfId);
        Ipv4Address physicalInterfaceIp = hostRelatedInfoContainer
                .getPhysicalInterfaceState(hostName)
                .getIp(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC).getIpv4Address();
        String interfaceName = hostRelatedInfoContainer
                .getPhysicalInterfaceState(hostName)
                .getName(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC);

        if (interfaceName != null && !interfaceName.isEmpty()) {
            LOG.trace("Adding Public interface route. hostname: {}, VrfId: {}, Ip: {}, Gw: {}, InterfaceName: {}.",
                hostName, vrfId, physicalInterfaceIp, null, interfaceName);
            if (!staticRoutingHelper.addSingleStaticRouteInRoutingProtocol(hostName, vrfId,
                Constants.PUBLIC_SUBNET_UUID, null, IpAddressUtil.toIpV4Prefix(physicalInterfaceIp),
                interfaceName, new VniReference(RoutingManager.DEFAULT_TABLE))) {
                LOG.warn("Failed to add route for physical interface in vrf {} compute host {}", vrfId, hostName);
            } else {
                LOG.debug("Added route for physical interface {} in vrf {}", interfaceName, vrfId);
            }
        }
    }

    private void deleteStaticRoute(AddressEndpointWithLocation addressEp) {
        String hostName = overlayHelper.getHostName(addressEp).get();
        String interfaceName = overlayHelper.getInterfaceName(addressEp).get();

        long vni = getVni(addressEp.getTenant().getValue());
        long vrfId = vni;

        String ipSubnetUuid = overlayHelper.getSubnet(addressEp);

        if (overlayHelper.isMetadataPort(addressEp)) {
            //override original port subnet to handle the Absolute location case of address endpoint
            ipSubnetUuid = Constants.METADATA_SUBNET_UUID;
        }

        Ipv4Address ipWithoutPrefix = overlayHelper.getInterfaceIp(addressEp);

        PortRouteState portRouteState = hostRelatedInfoContainer
                                            .getPortInterfaceStateOfHost(hostName)
                                            .getPortRouteState(interfaceName);
        if (portRouteState == null) {
            LOG.warn("Port route state is null, it has been deleted already. Skip delete for addresEp: {}.", addressEp);
            return;
        }

        SubnetState subnetState = hostRelatedInfoContainer
                                    .getVrfStateOfHost(hostName)
                                    .getVrfState(vrfId)
                                    .getSubnetHolder()
                                    .getSubnetState(ipSubnetUuid);

        if (!subnetState.isIpPresent(ipWithoutPrefix)) {
            LOG.debug("Route {} already deleted from vrf {} of host {}", ipWithoutPrefix, vrfId, hostName);
            return;
        }

        long targetRouteId = portRouteState.getRouteIdOfIp(ipWithoutPrefix);

        if (!staticRoutingHelper.deleteSingleStaticRouteFromRoutingProtocol(hostName,
                                                                            vrfId,
                                                                            interfaceName,
                                                                            targetRouteId)) {
            LOG.warn("Failed to delete route ({} via {}) from vrf {} from host{}",
                    ipWithoutPrefix, interfaceName, vrfId, hostName);

        } else {
            portRouteState.removeIp(ipWithoutPrefix);
            subnetState.removeIp(ipWithoutPrefix);
            LOG.debug("Delete Static Route ({} via {}) from vrf {} from host {}",
                    ipWithoutPrefix, interfaceName, vrfId, hostName);
        }
    }

    public long getVni(String tenantUuid) {
        long vni = neutronTenantToVniMapper.getVni(tenantUuid);
        LOG.debug("Debugging: getVni {} = {}", tenantUuid, vni);
        return vni;
    }
}
