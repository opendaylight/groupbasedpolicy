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
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.StaticArpCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.HostVrfRoutingInformationMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
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

    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    private HostVrfRoutingInformationMapper hostVrfInfo = HostVrfRoutingInformationMapper.getInstance();

    private StaticRoutingHelper staticRoutingHelper;

    public FlatOverlayManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.overlayHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        staticRoutingHelper = new StaticRoutingHelper();
    }

    public void configureEndpointForFlatOverlay(AddressEndpointWithLocation addressEp) {
        configureInterfaceForFlatOverlay(addressEp);
        addStaticArp(addressEp);
        addStaticRoute(addressEp);
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

        if (!putVrfInInterface(vppDataBroker, interfaceNameOptional.get(), vrf)) {
            LOG.warn("Failed to put interface {} to vrf {}", interfaceNameOptional.get(), vrf);
        } else {
            LOG.debug("Added interface {} to vrf {}", interfaceNameOptional.get(), vrf);
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
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);

        Ipv4AddressNoZone ip = new Ipv4AddressNoZone(overlayHelper.getInterfaceIp(addressEp));

        if (!putIpv4Neighbor(vppDataBroker, interfaceKey, new PhysAddress(physicalAddress), ip)) {
            LOG.warn("Failed to put static arp with interface {} for ip={} and physical-address={}", interfaceName,
                    ip, physicalAddress);
        } else {
            LOG.debug("Added Static arp ({} {}) in host {} for interface {}", ip, physicalAddress, hostName,
                    interfaceName);
        }
    }

    private boolean putIpv4Neighbor(DataBroker vppDataBroker,
                                 InterfaceKey interfaceKey,
                                 PhysAddress physAddress,
                                 Ipv4AddressNoZone ip) {
        StaticArpCommand.NeighborCommandBuilder neighborCommandBuilder = new StaticArpCommand.NeighborCommandBuilder();

        neighborCommandBuilder.setOperation(General.Operations.PUT);
        neighborCommandBuilder.setInterfaceKey(interfaceKey);
        neighborCommandBuilder.setIp(ip);
        neighborCommandBuilder.setLinkLayerAddress(physAddress);

        return GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                neighborCommandBuilder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addStaticRoute(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = overlayHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostName = overlayHelper.getHostName(addressEp).get();

        long vni = getVni(addressEp.getTenant().getValue());
        long vrf = vni;

        if (!hostVrfInfo.vrfExists(hostName, vrf)) {
            if (!staticRoutingHelper.addRoutingProtocolForVrf(vppDataBroker, hostName, vrf)) {
                LOG.warn("Failed to add Routing protocol for host {} and vrf {}!", hostName, vrf);
            }
        }

        String outgoingInterfaceName = overlayHelper.getInterfaceName(addressEp).get();
        Ipv4Address ipWithoutPrefix = overlayHelper.getInterfaceIp(addressEp);

        if (staticRoutingHelper.endPointRoutingExists(outgoingInterfaceName, ipWithoutPrefix)) {
            return;
        }

        Ipv4Prefix ipv4Prefix = overlayHelper.getInterfaceIpAsPrefix(addressEp);

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
