/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ProxyRangeCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.UnnumberedInterfaceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.LispCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.LispStateCommandExecutor;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispHelperArgumentException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.FlatOverlayManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class LoopbackManager {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackManager.class);

    private ConfigManagerHelper loopbackManagerHelper;
    private Table<NodeKey, String, List<String>> unnumberedCache = HashBasedTable.create();

    private NeutronTenantToVniMapper neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();


    Map<String, GbpSubnet> GbpSubnetCache = new HashMap<>();
    Map<String, List<LoopBackDetails>> loopBackHostnames = new HashMap<>();

    private class LoopBackDetails {
        LoopbackCommand loopbackCommand;
        String hostName;

        public LoopbackCommand getLoopbackCommand() {
            return loopbackCommand;
        }

        public void setLoopbackCommand(LoopbackCommand loopbackCommand) {
            this.loopbackCommand = loopbackCommand;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }
    }


    public LoopbackManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        this.loopbackManagerHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
    }

    public void createSimpleLoopbackIfNeeded(AddressEndpointWithLocation addressEp) {
        if (!addressEp.getAddressType().equals(IpPrefixType.class)) {
            return;
        }
        Map<String, String> intfcsByHostname = FlatOverlayManager.resolveIntfcsByHosts(addressEp);

        intfcsByHostname.forEach((hostname, interfaceName) -> {
            try {
                long vni = getVni(addressEp.getTenant().getValue());
                long vrfId = vni;
                String subnetUuid = loopbackManagerHelper.getSubnet(addressEp);
                GbpSubnet gbpSubnetInfo = GbpSubnetCache.get(subnetUuid);
                String loopIntfcName = Constants.GW_NAME_PREFIX + subnetUuid;

                if (gbpSubnetInfo != null) {
                    Optional<Interface> optionalLoopback =
                        GbpNetconfTransaction.read(VppIidFactory.getNetconfNodeIid(new NodeId(hostname)),
                            LogicalDatastoreType.CONFIGURATION,
                            VppIidFactory.getInterfaceIID(new InterfaceKey(loopIntfcName)),
                            GbpNetconfTransaction.RETRY_COUNT);
                    if (!optionalLoopback.isPresent()) {
                        LoopbackCommand simpleLoopbackCommand =
                            LoopbackCommandWrapper.simpleLoopbackPutCommand(loopIntfcName, vrfId, gbpSubnetInfo.getGatewayIp(),
                                gbpSubnetInfo.getCidr());
                        if (createLoopbackInterface(hostname, simpleLoopbackCommand)) {
                            addGpeEntry(VppIidFactory.getNetconfNodeIid(new NodeId(hostname)), gbpSubnetInfo, vni);
                            addProxyArpRange(hostname, vrfId, gbpSubnetInfo);
                            if(loopBackHostnames.get(loopIntfcName) == null) {
                                LoopBackDetails loopBackDetails = new LoopBackDetails();
                                loopBackDetails.setHostName(hostname);
                                loopBackDetails.setLoopbackCommand(simpleLoopbackCommand);
                                loopBackHostnames.put(loopIntfcName, Lists.newArrayList(loopBackDetails));
                            } else {
                                LoopBackDetails loopBackDetails = new LoopBackDetails();
                                loopBackDetails.setHostName(hostname);
                                loopBackDetails.setLoopbackCommand(simpleLoopbackCommand);
                                loopBackHostnames.get(loopIntfcName).add(loopBackDetails);
                            }
                        }
                    } else {
                        LOG.trace("Loopback already present on host: {} skip update for: {} - {} in vrf: {}", hostname, loopIntfcName, gbpSubnetInfo.getGatewayIp(), vrfId);
                    }
                }


                if (!addUnnumberedInterface(hostname, interfaceName, loopIntfcName)){
                    LOG.warn("Failed to add unnumbered for addressEp : {}", addressEp);
                }
            } catch (LispConfigCommandFailedException e) {
                LOG.warn("LISP couldn't be configured: {}", e.getMessage());
            }
        });



    }

    private boolean createLoopbackInterface(String hostName, LoopbackCommand loopbackCommand){

        return GbpNetconfTransaction.netconfSyncedWrite(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)),
                loopbackCommand, GbpNetconfTransaction.RETRY_COUNT);
    }

    private boolean deleteSpecificLoopback(InstanceIdentifier<Node> nodeIid, String loopbackName) {
        LOG.trace("deleteSpecificLoopback -> nodeiid: {}, loopbackInterface: {}", nodeIid, loopbackName);
        if (unnumberedCache.get(new NodeKey(nodeIid.firstKeyOf(Node.class)), loopbackName) != null) {
            unnumberedCache.get(new NodeKey(nodeIid.firstKeyOf(Node.class)), loopbackName).forEach(intfc -> {
                if (GbpNetconfTransaction.netconfSyncedDelete(nodeIid,
                    VppIidFactory.getUnnumberedIid(new InterfaceKey(intfc)), GbpNetconfTransaction.RETRY_COUNT)) {
                    unnumberedCache.remove(new NodeKey(nodeIid.firstKeyOf(Node.class)), loopbackName);
                }
            });
        }
        return GbpNetconfTransaction.netconfSyncedDelete(nodeIid,
                VppIidFactory.getInterfaceIID(new InterfaceKey(loopbackName)), GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addProxyArpRange(String hostName,
                                  long vrf,
                                  GbpSubnet gbpSubnetInfo) throws LispConfigCommandFailedException {
        Ipv4Prefix subnetPrefix = gbpSubnetInfo.getCidr().getIpv4Prefix();

        Preconditions.checkNotNull(subnetPrefix, "Subnet CIDR found to be null for "
                + "subnet uuid =" +  gbpSubnetInfo.getId() + "!");

        Pair<Ipv4Address, Ipv4Address> startAndEndAddress = IpAddressUtil.getStartAndEndIp(subnetPrefix);

        if (!putArpRangesCommand(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), vrf,
            startAndEndAddress.getLeft(), startAndEndAddress.getRight())) {
            throw new LispConfigCommandFailedException("Proxy arp configuration failed for subnet uuid: " +
                    gbpSubnetInfo.getId() + "!");
        } else {
            LOG.debug("Configured proxy arp for range {} to {} on node : {}!", startAndEndAddress.getLeft(),
                    startAndEndAddress.getRight(), hostName);
        }
    }

    private boolean deleteProxyArpRange(String hostName, long vrf, GbpSubnet gbpSubnetInfo) {
        Ipv4Prefix subnetPrefix = gbpSubnetInfo.getCidr().getIpv4Prefix();

        Preconditions.checkNotNull(subnetPrefix, "Subnet CIDR found to be null for "
                + "subnet uuid =" +  gbpSubnetInfo.getId() + "!");

        Pair<Ipv4Address, Ipv4Address> startAndEndAddress = IpAddressUtil.getStartAndEndIp(subnetPrefix);

        return (deleteArpRangesCommand(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), vrf,
            startAndEndAddress.getLeft(), startAndEndAddress.getRight()));
    }

    private boolean putArpRangesCommand(InstanceIdentifier<Node> iid, long vrf, Ipv4Address start, Ipv4Address end) {
        ProxyRangeCommand.ProxyRangeCommandBuilder builder = new ProxyRangeCommand.ProxyRangeCommandBuilder();
        builder.setOperation(General.Operations.PUT);
        builder.setVrf(vrf);
        builder.setStartAddress(start);
        builder.setEndAddress(end);
        return GbpNetconfTransaction.netconfSyncedWrite(iid, builder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private boolean deleteArpRangesCommand(InstanceIdentifier<Node> iid,
                                           long vrf,
                                           Ipv4Address start,
                                           Ipv4Address end) {
        ProxyRangeCommand.ProxyRangeCommandBuilder builder = new ProxyRangeCommand.ProxyRangeCommandBuilder();
        builder.setOperation(General.Operations.DELETE);
        builder.setVrf(vrf);
        builder.setStartAddress(start);
        builder.setEndAddress(end);
        return GbpNetconfTransaction.netconfSyncedDelete(iid, builder.build(), GbpNetconfTransaction.RETRY_COUNT);
    }

    private boolean addUnnumberedInterface(String hostname, String neutronInterfaceName, String loopbackName) {
        InstanceIdentifier<Node> nodeIid = VppIidFactory.getNetconfNodeIid(new NodeId(hostname));
        if (neutronInterfaceName.equalsIgnoreCase(loopbackName)) {
            LOG.trace("No need to configure unnumbered for loopback: {} on host: {}. skip processing.", loopbackName, neutronInterfaceName);
            return true;
        }
        LOG.trace("Adding unnumbered configuration hostname: {}, interface: {} use : {}", hostname, neutronInterfaceName, loopbackName);
        boolean unnumberWritten = putUnnumberedInterface(nodeIid, neutronInterfaceName, loopbackName);
        if (unnumberWritten) {
            if (unnumberedCache.get(nodeIid.firstKeyOf(Node.class), loopbackName) != null) {
                unnumberedCache.get(nodeIid.firstKeyOf(Node.class), loopbackName).add(neutronInterfaceName);
            } else {
                unnumberedCache.put(nodeIid.firstKeyOf(Node.class), loopbackName, Lists.newArrayList(neutronInterfaceName));
            }
            LOG.debug("Added Interface {} as unnumbered for {}", loopbackName, neutronInterfaceName);
        }
        return unnumberWritten;
    }

    private boolean putUnnumberedInterface(InstanceIdentifier<Node> iid, String interfaceName, String useInterface) {
        UnnumberedInterfaceCommand unnumberedCommand =
            UnnumberedInterfaceCommand.builder()
                .setOperation(General.Operations.MERGE)
                .setUseInterface(useInterface)
                .setInterfaceName(interfaceName)
                .build();
        return GbpNetconfTransaction.netconfSyncedWrite(iid, unnumberedCommand, GbpNetconfTransaction.RETRY_COUNT);
    }

    private void addGpeEntry(InstanceIdentifier<Node> iid, GbpSubnet gbpSubnetInfo, long vni) {
        LOG.trace("addGpeEntry called. iid: {}, GbpSubnet: {}, vni: {}", iid, gbpSubnetInfo, vni);
        try {
            Pair<Ipv4Prefix, Ipv4Prefix> delegatingSubnets =
                IpAddressUtil.getSmallerSubnet(gbpSubnetInfo.getCidr().getIpv4Prefix());

            RemoteEid firstREid =
                LispUtil.toRemoteEid(LispUtil.toLispIpv4Prefix(delegatingSubnets.getLeft()), vni, Ipv4PrefixAfi.class);
            if (!putGpeEntry(iid, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1", firstREid, vni, vni)) {
                LOG.warn("Failed to write GPE entry: {}", Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_1");
            }

            if (delegatingSubnets.getLeft().equals(delegatingSubnets.getRight())) {
                return;
            }

            RemoteEid secondREid =
                LispUtil.toRemoteEid(LispUtil.toLispIpv4Prefix(delegatingSubnets.getRight()), vni, Ipv4PrefixAfi.class);

            if (!putGpeEntry(iid, Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2", secondREid, vni, vni)) {
                LOG.warn("Failed to write GPE entry: {}", Constants.GPE_ENTRY_PREFIX + gbpSubnetInfo.getId() + "_2");
            }
        } catch (LispHelperArgumentException e) {
            e.printStackTrace();
        }
    }

    private boolean putGpeEntry(InstanceIdentifier<Node> iid, String id, RemoteEid rEid, long vni, long vrf) {
        AbstractLispCommand<GpeEntry> gpeEntryCommand = LispCommandWrapper
                .addGpeSendMapregisterAction(id, rEid, vni, vrf);
        return LispStateCommandExecutor.executePutCommand(iid, gpeEntryCommand);
    }

    private boolean deleteGpeEntry(InstanceIdentifier<Node> iid, String id) {
        AbstractLispCommand<GpeEntry> gpeEntryDeletionCommand = LispCommandWrapper
                .deleteGpeEntry(id);
        return LispStateCommandExecutor.executeDeleteCommand(iid, gpeEntryDeletionCommand);
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }


    public void gbpSubnetCreated(String subnetUuid, GbpSubnet subnetInfo) {
        GbpSubnetCache.put(subnetUuid, subnetInfo);
    }

    public void gbpSubnetdeleted(String subnetUuid) {
        LOG.trace("gbpSubnetdeleted -> subnetUuid:{}", subnetUuid);
        GbpSubnet gbpSubnet = GbpSubnetCache.get(subnetUuid);
        String loopIntfcName = Constants.GW_NAME_PREFIX + subnetUuid;
        List<LoopBackDetails> loopBackDetails = loopBackHostnames.get(loopIntfcName);
        if (loopBackDetails != null) {
            loopBackDetails.forEach(loopbackDetail -> {

                InstanceIdentifier<Node> iid = VppIidFactory.getNetconfNodeIid(new NodeId(loopbackDetail.getHostName()));

                if (deleteSpecificLoopback(iid, loopIntfcName)) {
                    if (!deleteProxyArpRange(loopbackDetail.getHostName(), loopbackDetail.getLoopbackCommand().getVrfId(), gbpSubnet)) {
                        LOG.warn("Failed to delete ProxyArpRange: {} on host: {}", gbpSubnet.getAllocationPools(), loopbackDetail.getHostName());
                    }

                    if (!deleteGpeEntry(iid, Constants.GPE_ENTRY_PREFIX + gbpSubnet.getId() + "_1")) {
                        LOG.warn("Failed to delete gpeEntry: {} on host: {}", Constants.GPE_ENTRY_PREFIX + gbpSubnet.getId() + "_1", loopbackDetail.getHostName());
                    }
                    if (!deleteGpeEntry(iid, Constants.GPE_ENTRY_PREFIX + gbpSubnet.getId() + "_2")) {
                        LOG.warn("Failed to delete gpeEntry: {} on host: {}", Constants.GPE_ENTRY_PREFIX + gbpSubnet.getId() + "_2", loopbackDetail.getHostName());
                    }
                    if (!deleteGpeFeatureData(loopbackDetail.getHostName())) {
                        LOG.warn("Failed to delete gpe configuration: {} on host: {}", loopbackDetail.getHostName());
                    }

                } else {
                    LOG.warn("Failed to delete loopback: {} on host: {}", loopIntfcName, loopbackDetail.getHostName());
                }
            });
        }
        GbpSubnetCache.remove(subnetUuid);
    }

    private boolean deleteGpeFeatureData(String hostname) {
        return GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(new NodeId(hostname)),
            VppIidFactory.getGpeFeatureDataIid(), GbpNetconfTransaction.RETRY_COUNT);
    }
}
