/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.TapPortCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand.VhostUserCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.AddInterfaceToBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateInterfaceOnNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DelInterfaceFromBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.ExpandVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.bridge.domain.attributes.tunnel.type.Vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.bridge.domain.attributes.tunnel.type.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.TunnelTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.network.topology.topology.tunnel.parameters.VlanNetworkParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.network.topology.topology.tunnel.parameters.VxlanTunnelParameters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppRpcServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(VppRpcServiceImpl.class);

    private final DataBroker dataBroker;
    private final BridgeDomainManager bridgeDomainManager;
    private final InterfaceManager interfaceManager;
    private final MountedDataBrokerProvider mountDataProvider;

    public VppRpcServiceImpl(@Nonnull DataBroker dataBroker, @Nonnull MountedDataBrokerProvider mountDataProvider,
            BridgeDomainManager bridgeDomainManager, InterfaceManager interfaceManager) {
        this.dataBroker = dataBroker;
        this.bridgeDomainManager = bridgeDomainManager;
        this.interfaceManager = interfaceManager;
        this.mountDataProvider = mountDataProvider;
    }

    public Future<RpcResult<Void>> createVirtualBridgeDomain(CreateVirtualBridgeDomainOnNodesInput input) {
        if (input.getTunnelType() == null) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC,
                        "Failed to create bridge domain" + input.getId() + "." + "Tunnel type not specified")
                .build());
        }
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        List<NodeId> nodeIds = input.getPhysicalLocationRef()
            .stream()
            .map(locationRef -> locationRef.getNodeId())
            .collect(Collectors.toList());
        if (input.getTunnelType() instanceof Vxlan) {
            Vxlan tunnelType = (Vxlan) input.getTunnelType();
            VxlanVni vxlanVni = new VxlanVni(tunnelType.getVni().getValue());
            nodeIds.forEach(nodeId -> {
                futures.add(bridgeDomainManager.createVxlanBridgeDomainOnVppNode(input.getId(), vxlanVni, nodeId));
            });
        } else if (input.getTunnelType() instanceof Vlan) {
            Vlan vlan = (Vlan) input.getTunnelType();
            VlanId vlanId = new VlanId(vlan.getVlanId().getValue());
            nodeIds.forEach(nodeId -> {
                futures.add(bridgeDomainManager.createVlanBridgeDomainOnVppNode(input.getId(), vlanId, nodeId));
            });
        }
        return Futures.transform(Futures.allAsList(futures), voidsToRpcResult());
    }

    public Future<RpcResult<Void>> deleteVirtualBridgeDomain(DeleteVirtualBridgeDomainOnNodesInput input) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        input.getBridgeDomainNode().forEach(nodeId -> {
            futures.add(bridgeDomainManager.removeBridgeDomainFromVppNode(input.getBridgeDomainId(), nodeId));
        });
        return Futures.transform(Futures.allAsList(futures), voidsToRpcResult());
    }

    public ListenableFuture<RpcResult<Void>> expandVirtualBridgeDomainOnNode(ExpandVirtualBridgeDomainOnNodesInput input) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Topology> topologyIid = VppIidFactory.getTopologyIid(new TopologyKey(new TopologyId(
                input.getBridgeDomainId())));
        return Futures.transform(rTx.read(LogicalDatastoreType.OPERATIONAL, topologyIid),
                new AsyncFunction<Optional<Topology>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Topology> optTopology) throws Exception {
                        if (!optTopology.isPresent()) {

                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Failed to expand bridge domain. Bridge domain " + input.getBridgeDomainId()
                                                + " does not exist.")
                                .build());
                        }
                        TopologyTypes topologyTypes = optTopology.get().getTopologyTypes();
                        if (topologyTypes == null
                                || topologyTypes.getAugmentation(TopologyTypesVbridgeAugment.class) == null
                                || optTopology.get().getAugmentation(TopologyVbridgeAugment.class) == null) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Failed to expand bridge domain. Topology " + input.getBridgeDomainId()
                                                + " is not bridge domain type.")
                                .build());
                        }
                        TopologyVbridgeAugment vBridge = optTopology.get()
                            .getAugmentation(TopologyVbridgeAugment.class);
                        if (vBridge.getTunnelParameters() instanceof TunnelTypeVxlan) {
                            VxlanTunnelParameters vxlanTunnelParams = (VxlanTunnelParameters) vBridge.getTunnelParameters();
                            VxlanVni vni = vxlanTunnelParams.getVni();
                            input.getBridgeDomainNode().forEach(
                                    nodeId -> {
                                        futures.add(bridgeDomainManager.createVxlanBridgeDomainOnVppNode(
                                                input.getBridgeDomainId(), vni, nodeId));
                                    });
                        } else if (vBridge.getTunnelParameters() instanceof TunnelTypeVlan) {
                            VlanNetworkParameters vlanTunnelParams = (VlanNetworkParameters) vBridge.getTunnelParameters();
                            VlanId vlanId = vlanTunnelParams.getVlanId();
                            input.getBridgeDomainNode().forEach(
                                    nodeId -> {
                                        futures.add(bridgeDomainManager.createVlanBridgeDomainOnVppNode(
                                                input.getBridgeDomainId(), vlanId, nodeId));
                                    });
                        }
                        return Futures.transform(Futures.allAsList(futures), voidsToRpcResult());
                    }
                });
    }

    public ListenableFuture<RpcResult<Void>> createInterfaceOnNode(CreateInterfaceOnNodeInput input) {
        InterfaceTypeChoice interfaceType = input.getInterfaceTypeChoice();
        ConfigCommand ifaceCommand = null;
        if (interfaceType instanceof VhostUserCase) {
            VhostUserCommandBuilder vhostBuilder = VhostUserCommand.builder();
            vhostBuilder.setName(input.getVppInterfaceName());
            VhostUserCase vhostCase = (VhostUserCase) input.getInterfaceTypeChoice();
            vhostBuilder.setSocket(vhostCase.getSocket());
            vhostBuilder.setRole(VhostUserRole.Client);
            vhostBuilder.setDescription(input.getDescription());
            vhostBuilder.setOperation(Operations.PUT);
            ifaceCommand = vhostBuilder.build();
        }
        if (interfaceType instanceof TapCase) {
            TapPortCommand.TapPortCommandBuilder tapBuilder = TapPortCommand.builder();
            TapCase tapIface = (TapCase) input.getInterfaceTypeChoice();
            tapBuilder.setTapName(tapIface.getName());
            tapBuilder.setPhysAddress(tapIface.getPhysicalAddress());
            tapBuilder.setInterfaceName(input.getVppInterfaceName());
            tapBuilder.setDescription(input.getDescription());
            tapBuilder.setOperation(Operations.PUT);
            ifaceCommand = tapBuilder.build();
        }
        Optional<DataBroker> optDataBroker = mountDataProvider.getDataBrokerForMountPoint(input.getVppNodePath());
        if (!optDataBroker.isPresent()) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC, "Cannot find data broker for mount point " + input.getVppNodePath())
                .build());
        }
        return Futures.transform(interfaceManager.createInterfaceOnVpp(ifaceCommand, optDataBroker.get()),
                voidToRpcResult());
    }

    public ListenableFuture<RpcResult<Void>> deleteInterfaceOnNode(DeleteInterfaceInput input) {
        return Futures.transform(readInterface(input.getVppNodePath(), input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        InterfaceKey iKey = new InterfaceKey(input.getVppInterfaceName());
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot delete interface " + iKey + " on node " + input.getVppNodePath()
                                                + ". Not found or already deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(input.getVppNodePath());
                        WriteTransaction wTx = dataBroker.get().newWriteOnlyTransaction();
                        wTx.delete(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(iKey));
                        return Futures.transform(wTx.submit(), voidToRpcResult());
                    }
                });
    }

    public ListenableFuture<RpcResult<Void>> addInterfaceToBridgeDomain(AddInterfaceToBridgeDomainInput input) {
        return Futures.transform(readInterface(input.getVppNodePath(), input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        InterfaceKey iKey = new InterfaceKey(input.getVppInterfaceName());
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot add interface " + iKey + " to bridge domain on node "
                                                + input.getVppNodePath() + ". Not found or deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(input.getVppNodePath());
                        return Futures.transform(interfaceManager.configureInterface(dataBroker.get(), iKey,
                                input.getBridgeDomainId(), null), voidToRpcResult());
                    }
                });
    }

    public ListenableFuture<RpcResult<Void>> delInterfaceFromBridgeDomain(DelInterfaceFromBridgeDomainInput input) {
        return Futures.transform(readInterface(input.getVppNodePath(), input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot remove interface " + input.getVppInterfaceName()
                                                + " from bridge domain on node " + input.getVppNodePath()
                                                + ". Not found or deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(input.getVppNodePath());
                        return Futures.transform(interfaceManager.removeInterfaceFromBridgeDomain(dataBroker.get(),
                                optIface.get().getKey()), voidToRpcResult());
                    }
                });
    }

    private CheckedFuture<Optional<Interface>, ReadFailedException> readInterface(InstanceIdentifier<?> nodeIid,
            String interfaceName) {
        Optional<DataBroker> optDataBroker = mountDataProvider.getDataBrokerForMountPoint(nodeIid);
        if (!optDataBroker.isPresent()) {
            LOG.error("Cannot find data broker for node {}", nodeIid);
            return Futures.immediateCheckedFuture(Optional.absent());
        }
        ReadOnlyTransaction rwTx = optDataBroker.get().newReadOnlyTransaction();
        InterfaceKey iKey = new InterfaceKey(interfaceName);
        InstanceIdentifier<Interface> interfaceIID = VppIidFactory.getInterfaceIID(iKey);
        CheckedFuture<Optional<Interface>, ReadFailedException> readInterface = rwTx.read(
                LogicalDatastoreType.CONFIGURATION, interfaceIID);
        rwTx.close();
        return readInterface;
    }

    private AsyncFunction<Void, RpcResult<Void>> voidToRpcResult() {
        return new AsyncFunction<Void, RpcResult<Void>>() {

            @Override
            public ListenableFuture<RpcResult<Void>> apply(Void input) throws Exception {
                return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
            }
        };
    }

    private AsyncFunction<List<Void>, RpcResult<Void>> voidsToRpcResult() {
        return new AsyncFunction<List<Void>, RpcResult<Void>>() {

            @Override
            public ListenableFuture<RpcResult<Void>> apply(List<Void> input) throws Exception {
                return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
            }
        };
    }
}
