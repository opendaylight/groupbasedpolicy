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
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.LoopbackHostSpecificInfoMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.SubnetUuidToGbpSubnetMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
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
import java.util.List;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class LoopbackManager {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackManager.class);

    private ConfigManagerHelper loopbackManagerHelper;

    private LoopbackHostSpecificInfoMapper subnetHostSpecificInfo;
    private NeutronTenantToVniMapper neutronTenantToVniMapper;
    private SubnetUuidToGbpSubnetMapper subnetUuidToGbpSubnetMapper;

    private static final String LOOP_NAME_PREFIX = "loop-";
    private static final String GPE_ENTRY_PREFIX = "gpe-entry-";

    public LoopbackManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.loopbackManagerHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        this.subnetHostSpecificInfo = new LoopbackHostSpecificInfoMapper();
        this.neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
        this.subnetUuidToGbpSubnetMapper = SubnetUuidToGbpSubnetMapper.getInstance();
    }

    public void createBviLoopbackIfNeeded(AddressEndpointWithLocation addressEp,
                                          String bridgeDomainName) {
        try {
            DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = loopbackManagerHelper.getHostName(addressEp).get();
            String subnetUuid = loopbackManagerHelper.getSubnet(addressEp);

            if (subnetHostSpecificInfo.loopbackAlreadyExists(hostName, subnetUuid)) {
                subnetHostSpecificInfo.addNewPortInHostSubnet(hostName, subnetUuid);
                return;
            }

            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(getSubnetInfo(subnetUuid),
                    "Subnet UUID {} hasn't been created yet!", subnetUuid);

            String interfaceName = LOOP_NAME_PREFIX + subnetHostSpecificInfo.getLoopbackCount(hostName);
            long vni = getVni(addressEp.getTenant().getValue());

            LoopbackCommand bviLoopbackCommand = LoopbackCommandWrapper
                    .bviLoopbackPutCommand(interfaceName, vni, gbpSubnetInfo.getGatewayIp(), gbpSubnetInfo.getCidr(),
                            bridgeDomainName);
            createLoopbackInterface(hostName, subnetUuid, vppDataBroker, bviLoopbackCommand);
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("LISP couldn't be configured: {}", e.getMessage());
        }
    }

    public void createSimpleLoopbackIfNeeded(AddressEndpointWithLocation addressEp) {
        try {
            DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = loopbackManagerHelper.getHostName(addressEp).get();
            String subnetUuid = loopbackManagerHelper.getSubnet(addressEp);

            if (subnetHostSpecificInfo.loopbackAlreadyExists(hostName, subnetUuid)) {
                subnetHostSpecificInfo.addNewPortInHostSubnet(hostName, subnetUuid);
                String loopbackInterfaceName = subnetHostSpecificInfo
                        .getInterfaceNameForLoopbackInHost(hostName, subnetUuid);
                addUnnumberedInterface(addressEp, loopbackInterfaceName);
                return;
            }

            String interfaceName = LOOP_NAME_PREFIX + subnetHostSpecificInfo.getLoopbackCount(hostName);
            long vni = getVni(addressEp.getTenant().getValue());
            long vrf = vni;

            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(getSubnetInfo(subnetUuid),
                    "Subnet UUID {} hasn't been created yet!", subnetUuid);

            LoopbackCommand simpleLoopbackCommand = LoopbackCommandWrapper
                    .simpleLoopbackPutCommand(interfaceName, vrf, gbpSubnetInfo.getGatewayIp(),
                            gbpSubnetInfo.getCidr());

            createLoopbackInterface(hostName, subnetUuid, vppDataBroker, simpleLoopbackCommand);
            addProxyArpRange(vppDataBroker, vrf, gbpSubnetInfo, hostName);
            addGpeEntry(vppDataBroker, gbpSubnetInfo, vni);
            addUnnumberedInterface(addressEp, interfaceName);
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("LISP couldn't be configured: {}", e.getMessage());
        }
    }

    private void createLoopbackInterface(String hostName, String subnetUuid, DataBroker vppDataBroker,
                                        LoopbackCommand loopbackCommand) throws LispConfigCommandFailedException {

        if (GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                                                     loopbackCommand,
                                                     GbpNetconfTransaction.RETRY_COUNT)) {
            subnetHostSpecificInfo.addLoopbackForHost(hostName, subnetUuid, loopbackCommand.getName(),
                    loopbackCommand.getVrfId());
            subnetHostSpecificInfo.addNewPortInHostSubnet(hostName, subnetUuid);
        } else {
            throw new LispConfigCommandFailedException("BVI could not be created for "
                    + hostName + " and bridge domain " + loopbackCommand.getBridgeDomain());
        }
    }

    public void deleteLoopbackIfExists(String subnetUuid) {

        List<String> hostsWithSubnet = subnetHostSpecificInfo.getHostsWithSubnet(subnetUuid);

        hostsWithSubnet.forEach(host -> {
            DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(host).get();
            String interfaceName = subnetHostSpecificInfo.getInterfaceNameForLoopbackInHost(host, subnetUuid);

            try {
                deleteSpecificLoopback(vppDataBroker, interfaceName);
            } catch (LispConfigCommandFailedException e) {
                e.printStackTrace();
            }
        });

        subnetHostSpecificInfo.clearSubnet(subnetUuid);
    }

    public void handleEndpointDelete(AddressEndpointWithLocation addressEp) {
        DataBroker vppDataBroker = loopbackManagerHelper.getPotentialExternalDataBroker(addressEp).get();
        String hostId = loopbackManagerHelper.getHostName(addressEp).get();
        String portSubnetUuid = loopbackManagerHelper.getSubnet(addressEp);
        String interfaceName = subnetHostSpecificInfo.getInterfaceNameForLoopbackInHost(hostId, portSubnetUuid);
        if (subnetHostSpecificInfo.deletePortFromHostSubnetAndTriggerLoopbackDelete(hostId, portSubnetUuid)) {
            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(subnetUuidToGbpSubnetMapper.getSubnetInfo(portSubnetUuid),
                    "Invalid port!");
            long vni = getVni(addressEp.getTenant().getValue());
            try {
                deleteSpecificLoopback(vppDataBroker, interfaceName);
                deleteProxyArpRange(vppDataBroker, vni, gbpSubnetInfo, hostId);
                deleteGpeEntry(vppDataBroker, GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1");
                deleteGpeEntry(vppDataBroker, GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2");
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
                                  long vrf,
                                  GbpSubnet gbpSubnetInfo,
                                  String hostName) throws LispConfigCommandFailedException {
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
                                long vrf,
                                GbpSubnet gbpSubnetInfo,
                                String hostName) throws LispConfigCommandFailedException {
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
            putGpeEntry(vppDataBroker, GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1", firstREid, vni, vni);

            if (delegatingSubnets.getLeft().equals(delegatingSubnets.getRight())) {
                return;
            }

            RemoteEid secondREid = LispUtil.toRemoteEid(LispUtil.toLispIpv4Prefix(delegatingSubnets.getRight()),
                                                        vni,
                                                        Ipv4PrefixAfi.class);

            putGpeEntry(vppDataBroker, GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2", secondREid, vni, vni);
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
