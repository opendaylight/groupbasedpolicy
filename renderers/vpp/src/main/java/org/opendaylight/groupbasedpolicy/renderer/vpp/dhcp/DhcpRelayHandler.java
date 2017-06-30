/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.DhcpRelayCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.subnet.DhcpServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DhcpRelayHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpRelayHandler.class);
    private final MountedDataBrokerProvider mountDataProvider;

    public DhcpRelayHandler(MountedDataBrokerProvider mountDataProvider) {
        this.mountDataProvider = mountDataProvider;
    }

    public void createIpv4DhcpRelay(long vni_vrfid, Subnet subnet, SetMultimap<String, NodeId> vppNodesByL2Fd) {
        if (subnet.getDhcpServers() == null) {
            LOG.trace("Dhcp Server IP is null, skipping processing DhcpRelay for vrfid: {}, subnet: {}, VPP nodes: {}",
                vni_vrfid, subnet, vppNodesByL2Fd);
            return;
        }

        for (String bd : vppNodesByL2Fd.keySet()) {
            Set<NodeId> vppNodes = vppNodesByL2Fd.get(bd);
            for (NodeId vppNode : vppNodes) {
                IpAddress ipAddress = resolveDhcpIpAddress(subnet.getDhcpServers(), vppNode);
                if (ipAddress != null) {
                    DhcpRelayCommand dhcpRelayCommand =
                        DhcpRelayCommand.builder()
                            .setOperation(General.Operations.PUT)
                            .setRxVrfId(vni_vrfid)
                            .setAddressType(Ipv4.class)
                            .setGatewayIpAddress(subnet.getDefaultSubnetGatewayIp())
                            .setServerIpAddresses(Collections.singletonList(
                                new ServerBuilder()
                                    .setAddress(ipAddress)
                                    .setVrfId(vni_vrfid)
                                    .setKey(new ServerKey(ipAddress, vni_vrfid))
                                    .build()))
                            .build();

                    if (!submitDhcpRelay(dhcpRelayCommand, vppNode)) {
                        LOG.warn("DHCP Relay was not configured: {}", dhcpRelayCommand);
                    }
                } else {
                    LOG.warn("DHCP Relay not configured for node: {}, DHCP server IP addres was not found.", vppNode);
                }
            }

        }
    }

    private IpAddress resolveDhcpIpAddress(List<DhcpServers> dhcpServers, NodeId vppNode) {
        java.util.Optional<DhcpServers> dhcpServerOptional = dhcpServers.stream()
            .filter(dhcpServer -> dhcpServer.getNode().equalsIgnoreCase(vppNode.getValue()))
            .findFirst();
        return dhcpServerOptional.map(DhcpServers::getDhcpServerIp).orElse(null);
    }

    public void deleteIpv4DhcpRelay(long vni_vrfid, Subnet subnet, SetMultimap<String, NodeId> vppNodesByL2Fd) {
        if (subnet.getDhcpServers() == null) {
            LOG.trace("Dhcp Server IP is null, skipping processing DhcpRelay for vrfid: {}, subnet: {}, VPP nodes: {}",
                vni_vrfid, subnet, vppNodesByL2Fd);
            return;
        }

        for (String bd : vppNodesByL2Fd.keySet()) {
            Set<NodeId> vppNodes = vppNodesByL2Fd.get(bd);
            for (NodeId vppNode : vppNodes) {
                IpAddress ipAddress = resolveDhcpIpAddress(subnet.getDhcpServers(), vppNode);
                if (ipAddress != null) {
                    DhcpRelayCommand dhcpRelayCommand =
                        DhcpRelayCommand.builder()
                            .setOperation(General.Operations.DELETE)
                            .setRxVrfId(vni_vrfid)
                            .setAddressType(Ipv4.class)
                            .setGatewayIpAddress(subnet.getDefaultSubnetGatewayIp())
                            .setServerIpAddresses(Collections.singletonList(
                                new ServerBuilder()
                                    .setAddress(ipAddress)
                                    .setVrfId(vni_vrfid)
                                    .setKey(new ServerKey(ipAddress, vni_vrfid))
                                    .build()))
                            .build();

                    if (!submitDhcpRelay(dhcpRelayCommand, vppNode)) {
                        LOG.warn("DHCP Relay was not deleted: {}", dhcpRelayCommand);
                    }
                } else {
                    LOG.warn("DHCP Relay not deleted for node: {}, DHCP server IP address was not found.", vppNode);
                }

            }

        }
    }

    private boolean submitDhcpRelay(DhcpRelayCommand dhcpRelayCommand, NodeId nodeIid) {
        LOG.trace("Submitting DhcpRelay command: {}, nodeId: {}", dhcpRelayCommand, nodeIid);

        Optional<DataBroker> mountPointDataBroker =
            mountDataProvider.getDataBrokerForMountPoint(VppIidFactory.getNetconfNodeIid(nodeIid));
        if (!mountPointDataBroker.isPresent()) {
            throw new IllegalStateException("Cannot find data broker for mount point " + nodeIid);
        }
        if (dhcpRelayCommand.getOperation() == General.Operations.PUT) {
            return GbpNetconfTransaction.netconfSyncedWrite(mountPointDataBroker.get(), dhcpRelayCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        } else if (dhcpRelayCommand.getOperation() == General.Operations.DELETE) {
            return GbpNetconfTransaction.netconfSyncedDelete(mountPointDataBroker.get(), dhcpRelayCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        }
        return false;
    }
}
