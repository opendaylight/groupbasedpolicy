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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.StaticArpCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.HostIdToMetadataInterfaceMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.HostVrfRoutingInformationMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.InterfaceNameToStaticInfoMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Created by Shakib Ahmed on 5/2/17.
 */
public class FlatOverlayManager {
    private static final Logger LOG = LoggerFactory.getLogger(FlatOverlayManager.class);

    private ConfigManagerHelper overlayHelper;
    private DataBroker dataBroker;

    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    private HostVrfRoutingInformationMapper hostVrfInfo = HostVrfRoutingInformationMapper.getInstance();
    private HostIdToMetadataInterfaceMapper
            hostIdToMetadataInterfaceMapper = HostIdToMetadataInterfaceMapper.getInstance();

    private InterfaceNameToStaticInfoMapper interfaceNameToStaticInfoMapper;

    private StaticRoutingHelper staticRoutingHelper;

    public FlatOverlayManager(@Nonnull DataBroker dataBroker, @Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.overlayHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        this.interfaceNameToStaticInfoMapper = InterfaceNameToStaticInfoMapper.getInstance();
        staticRoutingHelper = new StaticRoutingHelper(interfaceNameToStaticInfoMapper);
        this.dataBroker = dataBroker;
    }

    public void configureEndpointForFlatOverlay(AddressEndpointWithLocation addressEp) {
        if (!overlayHelper.isMetadataPort(addressEp)) {
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

                if (!hostIdToMetadataInterfaceMapper.isMetadataInterfaceConfigured(hostName, metadataInterfaceName)) {
                    DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(externalLocation).get();
                    addInterfaceInVrf(vppDataBroker, metadataInterfaceName, vrf);
                    String physicalAddress = resolvePhysicalAddress(hostName, metadataInterfaceName);
                    addStaticArp(vppDataBroker, hostName, metadataInterfaceName, physicalAddress, metadataIp);
                    addStaticRoute(vppDataBroker, hostName, vrf, metadataIp, metadataIpPrefix, metadataInterfaceName);
                    hostIdToMetadataInterfaceMapper.addMetadataInterfaceInHost(hostName, metadataInterfaceName);
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
        deleteStaticRoute(addressEp);
    }

    private void configureInterfaceForFlatOverlay(AddressEndpointWithLocation addressEp) {
        addInterfaceInVrf(addressEp);
    }

    private void addInterfaceInVrf(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(addressEp).get();
        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;
        Optional<String> interfaceNameOptional = overlayHelper.getInterfaceName(addressEp);

        Preconditions.checkArgument(interfaceNameOptional.isPresent());

        addInterfaceInVrf(vppDataBroker, interfaceNameOptional.get(), vrf);
    }

    private void addInterfaceInVrf(DataBroker vppDataBroker, String interfaceName, long vrf) {
        if (!putVrfInInterface(vppDataBroker, interfaceName, vrf)) {
            LOG.warn("Failed to put interface {} to vrf {}", interfaceName, vrf);
        } else {
            LOG.debug("Added interface {} to vrf {}", interfaceName, vrf);
        }
    }

    private boolean putVrfInInterface(DataBroker vppDataBroker,
                                  String interfaceName,
                                  Long vrf) {
        InstanceIdentifier<Routing> iid = VppIidFactory.getRoutingIid(new InterfaceKey(interfaceName));
        RoutingBuilder builder = new RoutingBuilder();
        builder.setIpv4VrfId(vrf);
        return GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker, iid, builder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticArp(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostName = overlayHelper.getHostName(addressEp).get();
        String physicalAddress = overlayHelper.getPhysicalAddress(addressEp);
        Optional<String> interfaceNameOptional = overlayHelper.getInterfaceName(addressEp);

        Preconditions.checkArgument(interfaceNameOptional.isPresent());

        String interfaceName = interfaceNameOptional.get();

        addStaticArp(vppDataBroker, hostName, interfaceName, physicalAddress, overlayHelper.getInterfaceIp(addressEp));
    }

    private void addStaticArp(DataBroker vppDataBroker,
                              String hostName,
                              String interfaceName,
                              String physicalAddress,
                              Ipv4Address ipv4Address) {
        Ipv4AddressNoZone ip = new Ipv4AddressNoZone(ipv4Address);
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        if (!putStaticArp(vppDataBroker,
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

    private boolean putStaticArp(DataBroker vppDataBroker,
                                 InterfaceKey interfaceKey,
                                 PhysAddress physAddress,
                                 Ipv4AddressNoZone ip) {
        StaticArpCommand.StaticArpCommandBuilder staticArpCommandBuilder = new StaticArpCommand.StaticArpCommandBuilder();

        staticArpCommandBuilder.setOperation(General.Operations.PUT);
        staticArpCommandBuilder.setInterfaceKey(interfaceKey);
        staticArpCommandBuilder.setIp(ip);
        staticArpCommandBuilder.setLinkLayerAddress(physAddress);

        return GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                staticArpCommandBuilder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticRoute(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostName = overlayHelper.getHostName(addressEp).get();

        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;

        String outgoingInterfaceName = overlayHelper.getInterfaceName(addressEp).get();
        Ipv4Address ipWithoutPrefix = overlayHelper.getInterfaceIp(addressEp);
        Ipv4Prefix ipv4Prefix = overlayHelper.getInterfaceIpAsPrefix(addressEp);

        addStaticRoute(vppDataBroker, hostName, vrf, ipWithoutPrefix, ipv4Prefix, outgoingInterfaceName);
    }

    private void addStaticRoute(DataBroker vppDataBroker, String hostName, long vrf, Ipv4Address ipWithoutPrefix,
                                Ipv4Prefix ipv4Prefix, String outgoingInterfaceName) {

        if (!hostVrfInfo.vrfExists(hostName, vrf)) {
            if (!staticRoutingHelper.addRoutingProtocolForVrf(vppDataBroker, hostName, vrf)) {
                LOG.warn("Failed to add Routing protocol for host {} and vrf {}!", hostName, vrf);
            }
        }

        if (staticRoutingHelper.endPointRoutingExists(outgoingInterfaceName, ipWithoutPrefix)) {
            return;
        }

        if (staticRoutingHelper.routeAlreadyExistsInHostVrf(hostName, vrf, ipWithoutPrefix)) {
            LOG.warn("Ip already exists in host {} vrf {} ip {}", hostName, vrf, ipWithoutPrefix);
            return;
        }

        if (!staticRoutingHelper.addSingleStaticRouteInRoutingProtocol(vppDataBroker,
                hostName,
                vrf,
                ipWithoutPrefix,
                ipv4Prefix,
                outgoingInterfaceName)) {
            LOG.warn("Failed to add routing ({} via {}) in vrf {} in compute host {}!",
                    ipv4Prefix, outgoingInterfaceName, vrf, hostName);
        } else {
            LOG.debug("Added route ({} via {}) in vrf {} in compute host {}",
                    ipv4Prefix, outgoingInterfaceName, vrf, hostName);
        }
    }

    private void deleteStaticRoute(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostName = overlayHelper.getHostName(addressEp).get();
        String interfaceName = overlayHelper.getInterfaceName(addressEp).get();

        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;

        Ipv4Address ipWithoutPrefix = overlayHelper.getInterfaceIp(addressEp);

        if (!staticRoutingHelper.deleteSingleStaticRouteFromRoutingProtocol(vppDataBroker,
                                                                       hostName,
                                                                       vrf,
                                                                       interfaceName)) {
            LOG.warn("Failed to delete route ({} via {}) from vrf {} from host{}",
                    ipWithoutPrefix, interfaceName, vrf, hostName);

        } else {
            LOG.debug("Delete Static Route ({} via {}) from vrf {} from host {}",
                    ipWithoutPrefix, interfaceName, vrf, hostName);
        }
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }
}
