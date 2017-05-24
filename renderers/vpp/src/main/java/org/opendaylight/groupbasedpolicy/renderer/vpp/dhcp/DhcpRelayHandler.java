/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp;

import static org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory.getVppRendererConfig;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.DhcpRelayCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;

public class DhcpRelayHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpRelayHandler.class);
    private final DataBroker dataBroker;

    // TODO remove argument
    public DhcpRelayHandler(DataBroker dataBroker, MountedDataBrokerProvider mountDataProvider) {
        this.dataBroker = dataBroker;
    }

    public void createIpv4DhcpRelay(long vni_vrfid, Subnet subnet, SetMultimap<String, NodeId> vppNodesByL2Fd) {
        if (subnet.getDefaultSubnetGatewayIp() == null) {
            LOG.trace("Subnet GW IP is null, skipping processing DhcpRelay for vrfid: {}, subnet: {}, VPP nodes: {}",
                vni_vrfid, subnet, vppNodesByL2Fd);
            return;
        }

        for (String bd : vppNodesByL2Fd.keySet()) {
            Set<NodeId> vppNodes = vppNodesByL2Fd.get(bd);
            for (NodeId vppNode : vppNodes) {
                IpAddress ipAddress = resolveDhcpIpAddress(vppNode, subnet);
                if (ipAddress != null) {
                    DhcpRelayCommand dhcpRelayCommand =
                        getDhcpRelayBuilder(vni_vrfid, subnet, ipAddress, General.Operations.PUT).build();

                    if (!submitDhcpRelay(dhcpRelayCommand, vppNode)) {
                        LOG.warn("DHCP Relay was not configured: {}", dhcpRelayCommand);
                    }
                } else {
                    LOG.warn("DHCP server IP address was not found for node: {}. Skipping processing", vppNode);
                }
            }

        }
    }

    private DhcpRelayCommand.DhcpRelayBuilder getDhcpRelayBuilder(long vni_vrfid, Subnet subnet, IpAddress ipAddress,
        General.Operations operations) {
        return DhcpRelayCommand.builder()
            .setRxVrfId(vni_vrfid)
            .setOperation(operations)
            .setAddressType(Ipv4.class)
            .setGatewayIpAddress(subnet.getDefaultSubnetGatewayIp())
            .setServerIpAddresses(Collections.singletonList(
                new ServerBuilder()
                    .setAddress(ipAddress)
                    .setVrfId(vni_vrfid)
                    .setKey(new ServerKey(ipAddress, vni_vrfid))
                    .build()));
    }

    private IpAddress resolveDhcpIpAddress(NodeId vppNode, Subnet subnet) {
        Optional<Config> vppEndpointOptional =
            DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, getVppRendererConfig(),
                dataBroker.newReadOnlyTransaction());

        if (vppEndpointOptional.isPresent() && !vppEndpointOptional.get().getVppEndpoint().isEmpty()) {
            Stream<VppEndpoint> vppEndpointStream = vppEndpointOptional.get().getVppEndpoint().stream()
                .filter(vppEndpoint -> vppEndpoint.getInterfaceTypeChoice() instanceof TapCase)
                .filter(vppEndpoint -> vppEndpoint.getVppNodeId().equals(vppNode))
                .filter(vppEndpoint -> ((TapCase) vppEndpoint.getInterfaceTypeChoice()).getDhcpServerAddress() != null)
                .filter(vppEndpoint -> NetUtils.isInRange(subnet.getIpPrefix(), ((TapCase) vppEndpoint
                    .getInterfaceTypeChoice()).getDhcpServerAddress().getIpv4Address().getValue()));
            java.util.Optional<VppEndpoint> vppEpOptional = vppEndpointStream.findFirst();
            if (vppEpOptional.isPresent()) {
                IpAddress dhcpServerAddress =
                    ((TapCase) vppEpOptional.get().getInterfaceTypeChoice()).getDhcpServerAddress();
                LOG.trace("Found Dhcp server: {} on VPP node: {}", dhcpServerAddress, vppNode);
                return dhcpServerAddress;
            } else {
                LOG.trace("Dhcp server ip not found in subnet: {}, for node: {},in VPP endpoints: {}",
                    subnet, vppNode, vppEndpointOptional.get().getVppEndpoint());
            }
        }
        return null;
    }

    public void deleteIpv4DhcpRelay(long vni_vrfid, Subnet subnet, SetMultimap<String, NodeId> vppNodesByL2Fd) {
        if (subnet.getDefaultSubnetGatewayIp() == null) {
            LOG.trace("Subnet GW IP is null, skipping processing DhcpRelay for vrfid: {}, subnet: {}, VPP nodes: {}",
                vni_vrfid, subnet, vppNodesByL2Fd);
            return;
        }

        for (String bd : vppNodesByL2Fd.keySet()) {
            Set<NodeId> vppNodes = vppNodesByL2Fd.get(bd);
            for (NodeId vppNode : vppNodes) {
                IpAddress ipAddress = resolveDhcpIpAddress(vppNode, subnet);
                if (ipAddress != null) {
                    DhcpRelayCommand dhcpRelayCommand =
                        getDhcpRelayBuilder(vni_vrfid, subnet, ipAddress, General.Operations.DELETE).build();

                    if (!submitDhcpRelay(dhcpRelayCommand, vppNode)) {
                        LOG.warn("DHCP Relay was not deleted: {}", dhcpRelayCommand);
                    }
                } else {
                    LOG.trace("DHCP server IP address was not found for node: {}. Skipping processing.", vppNode);
                }

            }

        }
    }

    private boolean submitDhcpRelay(DhcpRelayCommand dhcpRelayCommand, NodeId nodeIid) {
        LOG.trace("Submitting DhcpRelay command: {}, nodeId: {}", dhcpRelayCommand, nodeIid);
        if (dhcpRelayCommand.getOperation() == General.Operations.PUT) {
            return GbpNetconfTransaction.netconfSyncedWrite(VppIidFactory.getNetconfNodeIid(nodeIid), dhcpRelayCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        } else if (dhcpRelayCommand.getOperation() == General.Operations.DELETE) {
            return GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(nodeIid), dhcpRelayCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        }
        return false;
    }
}
