/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ProxyRangeCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.LispCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.LispStateCommandExecutor;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispHelperArgumentException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.EndpointHost;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.SubnetState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfHolder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.SubnetUuidToGbpSubnetMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.IpAddressUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.LispUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.InterfaceUnnumberedAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class LoopbackManager {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackManager.class);

    private ConfigManagerHelper loopbackManagerHelper;

    private HostRelatedInfoContainer hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();
    private SubnetUuidToGbpSubnetMapper subnetUuidToGbpSubnetMapper = SubnetUuidToGbpSubnetMapper.getInstance();
    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();

    public LoopbackManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.loopbackManagerHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
    }

    public void createBviLoopbackIfNeeded(AddressEndpointWithLocation addressEp,
                                          String bridgeDomainName) {
        try {
            EndpointHost endpointHost = loopbackManagerHelper.getEndpointHostInformation(addressEp);
            long vni = getVni(addressEp.getTenant().getValue());
            long vrfId = vni;
            String subnetUuid = loopbackManagerHelper.getSubnet(addressEp);

            VrfHolder hostVrfHolder = hostRelatedInfoContainer.getVrfStateOfHost(endpointHost.getHostName());

            if (!hostVrfHolder.hasVrf(vrfId)) {
                //dummy init for bridge domain case
                hostVrfHolder.initializeVrfState(vrfId, Constants.DUMMY_PROTOCOL_BRIDGE_DOMAIN);
            }

            SubnetState subnetState = hostVrfHolder.getVrfState(vni)
                                                                            .getSubnetHolder()
                                                                            .getSubnetState(subnetUuid);

            if (!subnetState.isGwConfigured()) {
                return;
            }

            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(getSubnetInfo(subnetUuid),
                    "Subnet UUID {} hasn't been created yet!", subnetUuid);

            String gwInterfaceName = loopbackManagerHelper.getGatewayInterfaceName(Constants.GW_NAME_PREFIX, subnetUuid);

            LoopbackCommand bviLoopbackCommand = LoopbackCommandWrapper
                    .bviLoopbackPutCommand(gwInterfaceName, vni, gbpSubnetInfo.getGatewayIp(), gbpSubnetInfo.getCidr(),
                            bridgeDomainName);
            createLoopbackInterface(endpointHost.getHostDataBroker(), endpointHost.getHostName(),
                    subnetState, bviLoopbackCommand);
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("LISP couldn't be configured: {}", e.getMessage());
        }
    }

    public void createSimpleLoopbackIfNeeded(AddressEndpointWithLocation addressEp) {
        try {

            if (loopbackManagerHelper.isMetadataPort(addressEp)) {
                return;
            }

            DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = loopbackManagerHelper.getHostName(addressEp).get();
            long vni = getVni(addressEp.getTenant().getValue());
            long vrfId = vni;
            String subnetUuid = loopbackManagerHelper.getSubnet(addressEp);

            SubnetState stateOfSubnetUuid = hostRelatedInfoContainer
                                                            .getVrfStateOfHost(hostName)
                                                            .getVrfState(vrfId).getSubnetHolder()
                                                            .getSubnetState(subnetUuid);

            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(getSubnetInfo(subnetUuid),
                    "Subnet UUID {} hasn't been created yet!", subnetUuid);

            if (!stateOfSubnetUuid.isGwConfigured()) {
                String interfaceName = loopbackManagerHelper.getGatewayInterfaceName(Constants.GW_NAME_PREFIX,
                        subnetUuid);
                LoopbackCommand simpleLoopbackCommand = LoopbackCommandWrapper
                        .simpleLoopbackPutCommand(interfaceName, vrfId, gbpSubnetInfo.getGatewayIp(),
                                gbpSubnetInfo.getCidr());
                createLoopbackInterface(vppDataBroker, hostName, stateOfSubnetUuid, simpleLoopbackCommand);
                addProxyArpRange(vppDataBroker, hostName, vrfId, gbpSubnetInfo);
                addGpeEntry(vppDataBroker, gbpSubnetInfo, vni);
            }

            String gwInterfaceName = stateOfSubnetUuid.getGwInterfaceName();
            addUnnumberedInterface(addressEp, gwInterfaceName);
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("LISP couldn't be configured: {}", e.getMessage());
        }
    }

    private void createLoopbackInterface(DataBroker vppDataBroker, String hostName,
                                         SubnetState subnetState,
                                         LoopbackCommand loopbackCommand) throws LispConfigCommandFailedException {

        if (GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                loopbackCommand,
                GbpNetconfTransaction.RETRY_COUNT)) {
            subnetState.setGwInterfaceName(loopbackCommand.getName());
        } else {
            throw new LispConfigCommandFailedException("BVI could not be created for "
                    + hostName + " and bridge domain " + loopbackCommand.getBridgeDomain());
        }
    }

    public void handleEndpointDelete(AddressEndpointWithLocation addressEp) {

        if (loopbackManagerHelper.isMetadataPort(addressEp)) {
            return;
        }

        DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostName = loopbackManagerHelper.getHostName(addressEp).get();
        String portSubnetUuid = loopbackManagerHelper.getSubnet(addressEp);
        long vrfId = getVni(addressEp.getTenant().getValue());
        SubnetState subnetStateForSubnetUuid = hostRelatedInfoContainer
                                                        .getVrfStateOfHost(hostName)
                                                        .getVrfState(vrfId)
                                                        .getSubnetHolder()
                                                        .getSubnetState(portSubnetUuid);

        if (!subnetStateForSubnetUuid.hasIpsInSubnet()) {
            String gwInterfaceName = subnetStateForSubnetUuid.getGwInterfaceName();
            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(subnetUuidToGbpSubnetMapper.getSubnetInfo(portSubnetUuid),
                    "Invalid port!");
            long vni = getVni(addressEp.getTenant().getValue());
            try {
                deleteSpecificLoopback(vppDataBroker, gwInterfaceName);
                deleteProxyArpRange(vppDataBroker, hostName, vni, gbpSubnetInfo);
                deleteGpeEntry(vppDataBroker, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1");
                deleteGpeEntry(vppDataBroker, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2");
                hostRelatedInfoContainer.getVrfStateOfHost(hostName)
                        .getVrfState(vrfId)
                        .getSubnetHolder()
                        .removeSubnetState(portSubnetUuid);
            } catch (LispConfigCommandFailedException e) {
                LOG.warn("Loopback not deleted properly: {}", e.getMessage());
            }
        }
    }

    private void deleteSpecificLoopback(DataBroker vppDataBroker, String interfaceName) throws LispConfigCommandFailedException {
        if (!GbpNetconfTransaction.netconfSyncedDelete(vppDataBroker,
                VppIidFactory.getInterfaceIID(new InterfaceKey(interfaceName)), GbpNetconfTransaction.RETRY_COUNT)) {
            throw new LispConfigCommandFailedException("Failed to delete Loopback interface!");
        } else {
            LOG.debug("Deleted loopback interface!");
        }
    }

    private void addProxyArpRange(DataBroker vppDataBroker,
                                  String hostName,
                                  long vrf,
                                  GbpSubnet gbpSubnetInfo) throws LispConfigCommandFailedException {
        Ipv4Prefix subnetPrefix = gbpSubnetInfo.getCidr().getIpv4Prefix();

        Preconditions.checkNotNull(subnetPrefix, "Subnet CIDR found to be null for "
                + "subnet uuid =" +  gbpSubnetInfo.getId() + "!");

        Pair<Ipv4Address, Ipv4Address> startAndEndAddress = IpAddressUtil.getStartAndEndIp(subnetPrefix);

        if (!putArpRangesCommand(vppDataBroker,
                vrf,
                startAndEndAddress.getLeft(),
                startAndEndAddress.getRight())) {
            throw new LispConfigCommandFailedException("Proxy arp configuration failed for subnet uuid: " +
                    gbpSubnetInfo.getId() + "!");
        } else {
            LOG.debug("Configured proxy arp for range {} to {} on node : {}!", startAndEndAddress.getLeft(),
                    startAndEndAddress.getRight(), hostName);
        }
    }

    private void deleteProxyArpRange(DataBroker vppDataBroker,
                                     String hostName,
                                     long vrf,
                                     GbpSubnet gbpSubnetInfo) throws LispConfigCommandFailedException {
        Ipv4Prefix subnetPrefix = gbpSubnetInfo.getCidr().getIpv4Prefix();

        Preconditions.checkNotNull(subnetPrefix, "Subnet CIDR found to be null for "
                + "subnet uuid =" +  gbpSubnetInfo.getId() + "!");

        Pair<Ipv4Address, Ipv4Address> startAndEndAddress = IpAddressUtil.getStartAndEndIp(subnetPrefix);

        if (!deleteArpRangesCommand(vppDataBroker,
                vrf,
                startAndEndAddress.getLeft(),
                startAndEndAddress.getRight())) {
            throw new LispConfigCommandFailedException("Proxy arp configuration failed for subnet uuid: " +
                    gbpSubnetInfo.getId() + "!");
        } else {
            LOG.debug("Removed proxy arp for range {} to {} on node : {}!", startAndEndAddress.getLeft(),
                    startAndEndAddress.getRight(), hostName);
        }
    }

    private boolean putArpRangesCommand(DataBroker vppDataBroker, long vrf, Ipv4Address start, Ipv4Address end) {
        ProxyRangeCommand.ProxyRangeCommandBuilder builder = new ProxyRangeCommand.ProxyRangeCommandBuilder();
        builder.setOperation(General.Operations.PUT);
        builder.setVrf(vrf);
        builder.setStartAddress(start);
        builder.setEndAddress(end);

        return GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                builder.build(),
                GbpNetconfTransaction.RETRY_COUNT);
    }

    private boolean deleteArpRangesCommand(DataBroker vppDataBroker,
                                           long vrf,
                                           Ipv4Address start,
                                           Ipv4Address end) {
        ProxyRangeCommand.ProxyRangeCommandBuilder builder = new ProxyRangeCommand.ProxyRangeCommandBuilder();
        builder.setOperation(General.Operations.DELETE);
        builder.setVrf(vrf);
        builder.setStartAddress(start);
        builder.setEndAddress(end);

        return GbpNetconfTransaction.netconfSyncedDelete(vppDataBroker,
                builder.build(),
                GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addUnnumberedInterface(AddressEndpointWithLocation addressEp, String loopbackName) throws LispConfigCommandFailedException {
        DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
        String neutronInterfaceName = loopbackManagerHelper.getInterfaceName(addressEp).get();

        if (putUnnumberedInterface(vppDataBroker, neutronInterfaceName, loopbackName)) {
            LOG.debug("Added Interface {} as unnumberd for {}", loopbackName, neutronInterfaceName);
        } else {
            throw new LispConfigCommandFailedException("Unnumbered configuration failed for " +
                    neutronInterfaceName + " - " + loopbackName);
        }
    }

    private boolean putUnnumberedInterface(DataBroker vppDataBroker, String interfaceFor, String interfaceWith) {
        UnnumberedBuilder unnumberedBuilder = new UnnumberedBuilder();
        unnumberedBuilder.setUse(interfaceWith);
        InstanceIdentifier<Interface> interfaceIid = VppIidFactory.getInterfaceIID(new InterfaceKey(interfaceFor));
        InterfaceUnnumberedAugmentationBuilder augBuilder = new InterfaceUnnumberedAugmentationBuilder();
        augBuilder.setUnnumbered(unnumberedBuilder.build());
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setKey(new InterfaceKey(interfaceFor));
        interfaceBuilder.addAugmentation(InterfaceUnnumberedAugmentation.class, augBuilder.build());
        return GbpNetconfTransaction.netconfSyncedMerge(vppDataBroker,
                interfaceIid,
                interfaceBuilder.build(),
                GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addGpeEntry(DataBroker vppDataBroker, GbpSubnet gbpSubnetInfo, long vni) {
        try {
            Pair<Ipv4Prefix, Ipv4Prefix> delegatingSubnets = IpAddressUtil
                    .getSmallerSubnet(gbpSubnetInfo.getCidr().getIpv4Prefix());

            RemoteEid firstREid = LispUtil.toRemoteEid(LispUtil.toLispIpv4Prefix(delegatingSubnets.getLeft()),
                    vni,
                    Ipv4PrefixAfi.class);
            putGpeEntry(vppDataBroker, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1", firstREid, vni, vni);

            if (delegatingSubnets.getLeft().equals(delegatingSubnets.getRight())) {
                return;
            }

            RemoteEid secondREid = LispUtil.toRemoteEid(LispUtil.toLispIpv4Prefix(delegatingSubnets.getRight()),
                    vni,
                    Ipv4PrefixAfi.class);

            putGpeEntry(vppDataBroker, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2", secondREid, vni, vni);
        } catch (LispHelperArgumentException e) {
            e.printStackTrace();
        }
    }

    private boolean putGpeEntry(DataBroker vppDataBroker, String id, RemoteEid rEid, long vni, long vrf) {
        AbstractLispCommand<GpeEntry> gpeEntryCommand = LispCommandWrapper
                .addGpeSendMapregisterAction(id, rEid, vni, vrf);
        return LispStateCommandExecutor.executePutCommand(vppDataBroker, gpeEntryCommand);
    }

    private boolean deleteGpeEntry(DataBroker vppDataBroker, String id) {
        AbstractLispCommand<GpeEntry> gpeEntryDeletionCommand = LispCommandWrapper
                .deleteGpeEntry(id);
        return LispStateCommandExecutor.executeDeleteCommand(vppDataBroker, gpeEntryDeletionCommand);
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }

    private GbpSubnet getSubnetInfo(String subnetUuid) {
        return subnetUuidToGbpSubnetMapper.getSubnetInfo(subnetUuid);
    }
}
