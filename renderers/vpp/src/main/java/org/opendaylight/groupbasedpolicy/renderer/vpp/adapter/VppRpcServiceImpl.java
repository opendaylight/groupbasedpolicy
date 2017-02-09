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

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteInterfaceFromNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteVirtualBridgeDomainFromNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CloneVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.VppAdapterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.bridge.domain.attributes.tunnel.type.Vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.bridge.domain.attributes.tunnel.type.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.network.topology.topology.tunnel.parameters.VlanNetworkParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.network.topology.topology.tunnel.parameters.VxlanTunnelParameters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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

public class VppRpcServiceImpl implements VppAdapterService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppRpcServiceImpl.class);

    private final DataBroker dataBroker;
    private final BridgeDomainManager bridgeDomainManager;
    private final InterfaceManager interfaceManager;
    private final MountedDataBrokerProvider mountDataProvider;

    public VppRpcServiceImpl(@Nonnull DataBroker dataBroker, @Nonnull VppRenderer renderer) {
        this.dataBroker = dataBroker;
        this.bridgeDomainManager = renderer.getBridgeDomainManager();
        this.interfaceManager = renderer.getInterfaceManager();
        this.mountDataProvider = renderer.getMountedDataBroker();
    }

    public Future<RpcResult<Void>> createVirtualBridgeDomainOnNodes(CreateVirtualBridgeDomainOnNodesInput input) {
        LOG.info("Processing a remote call for creating bridge domain {}", input.getId());
        if (input.getTunnelType() == null) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC,
                        "Failed to create bridge domain" + input.getId() + "." + "Tunnel type not specified")
                .build());
        }
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        List<NodeId> nodeIds = (input.getPhysicalLocationRef() == null) ? new ArrayList<>() : input
            .getPhysicalLocationRef().stream().map(locationRef -> locationRef.getNodeId()).collect(Collectors.toList());
        LOG.trace("Corresponding nodes for bridge-domain {}", input.getPhysicalLocationRef());
        if (input.getTunnelType() instanceof Vxlan) {
            LOG.trace("Detected VXLAN type for bridge domain {}", input.getId());
            Vxlan tunnelType = (Vxlan) input.getTunnelType();
            VxlanVni vxlanVni = new VxlanVni(tunnelType.getVni().getValue());
            nodeIds.forEach(nodeId -> {
                futures.add(bridgeDomainManager.createVxlanBridgeDomainOnVppNode(input.getId(), vxlanVni, nodeId));
            });
        } else if (input.getTunnelType() instanceof Vlan) {
            LOG.trace("Detected VLAN type for bridge domain {}", input.getId());
            Vlan vlan = (Vlan) input.getTunnelType();
            VlanId vlanId = new VlanId(vlan.getVlanId().getValue());
            nodeIds.forEach(nodeId -> {
                futures.add(bridgeDomainManager.createVlanBridgeDomainOnVppNode(input.getId(), vlanId, nodeId));
            });
        }
        return Futures.transform(Futures.allAsList(futures), voidsToRpcResult());
    }

    public Future<RpcResult<Void>> deleteVirtualBridgeDomainFromNodes(DeleteVirtualBridgeDomainFromNodesInput input) {
        LOG.info("Processing a remote call for removing bridge domain {}", input.getBridgeDomainId());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        input.getBridgeDomainNode().forEach(nodeId -> {
            futures.add(bridgeDomainManager.removeBridgeDomainFromVppNode(input.getBridgeDomainId(), nodeId));
        });
        return Futures.transform(Futures.allAsList(futures), voidsToRpcResult());
    }

    public ListenableFuture<RpcResult<Void>> cloneVirtualBridgeDomainOnNodes(CloneVirtualBridgeDomainOnNodesInput input) {
        LOG.info("Processing a remote call for clonning  bridge domain {}", input.getBridgeDomainId());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Topology> topologyIid = VppIidFactory.getTopologyIid(new TopologyKey(new TopologyId(
                input.getBridgeDomainId())));
        return Futures.transform(rTx.read(LogicalDatastoreType.CONFIGURATION, topologyIid),
                new AsyncFunction<Optional<Topology>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Topology> optTopology) throws Exception {
                        if (!optTopology.isPresent()) {

                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Failed to clone bridge domain. Bridge domain " + input.getBridgeDomainId()
                                                + " does not exist.")
                                .build());
                        }
                        TopologyVbridgeAugment vBridgeAug = optTopology.get().getAugmentation(TopologyVbridgeAugment.class);
                        if (vBridgeAug == null) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Failed to clone bridge domain. Topology " + input.getBridgeDomainId()
                                                + " is not bridge domain type.")
                                .build());
                        }
                        if (vBridgeAug.getTunnelParameters() instanceof VxlanTunnelParameters) {
                            LOG.debug("Clonning VXLAN type bridge domain {} on nodes {}", input.getBridgeDomainId(),
                                    input.getBridgeDomainNode());
                            VxlanTunnelParameters vxlanTunnelParams = (VxlanTunnelParameters) vBridgeAug.getTunnelParameters();
                            VxlanVni vni = vxlanTunnelParams.getVni();
                            input.getBridgeDomainNode().forEach(
                                    nodeId -> {
                                        futures.add(bridgeDomainManager.createVxlanBridgeDomainOnVppNode(
                                                input.getBridgeDomainId(), vni, nodeId));
                                    });
                        } else if (vBridgeAug.getTunnelParameters() instanceof VlanNetworkParameters) {
                            LOG.debug("Clonning VLAN type bridge domain {} on nodes {}", input.getBridgeDomainId(),
                                    input.getBridgeDomainNode());
                            VlanNetworkParameters vlanTunnelParams = (VlanNetworkParameters) vBridgeAug.getTunnelParameters();
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
        LOG.info("Processing a remote call for creating interface {} on node {}", input.getVppInterfaceName(),
                input.getVppNodeId());
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
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(input.getVppNodeId());
        Optional<DataBroker> optDataBroker = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!optDataBroker.isPresent()) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC, "Cannot find data broker for mount point " + vppNodeIid)
                .build());
        }
        return Futures.transform(interfaceManager.createInterfaceOnVpp(ifaceCommand, optDataBroker.get()),
                voidToRpcResult());
    }

    public ListenableFuture<RpcResult<Void>> deleteInterfaceFromNode(DeleteInterfaceFromNodeInput input) {
        LOG.info("Processing a remote call for removing interface {} from node {}", input.getVppInterfaceName(),
                input.getVppNodeId());
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(input.getVppNodeId());
        return Futures.transform(readInterface(vppNodeIid, input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        InterfaceKey iKey = new InterfaceKey(input.getVppInterfaceName());
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot delete interface " + iKey + " on node " + vppNodeIid
                                                + ". Not found or already deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
                        WriteTransaction wTx = dataBroker.get().newWriteOnlyTransaction();
                        wTx.delete(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(iKey));
                        return Futures.transform(wTx.submit(), voidToRpcResult());
                    }
                });
    }

    public ListenableFuture<RpcResult<Void>> addInterfaceToBridgeDomain(AddInterfaceToBridgeDomainInput input) {
        LOG.info("Processing a remote call for adding interface {} to bridge domain {}", input.getVppInterfaceName(),
                input.getBridgeDomainId());
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(input.getVppNodeId());
        return Futures.transform(readInterface(vppNodeIid, input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        InterfaceKey iKey = new InterfaceKey(input.getVppInterfaceName());
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot add interface " + iKey + " to bridge domain on node "
                                                + vppNodeIid + ". Not found or deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
                        return Futures.transform(interfaceManager.configureInterface(dataBroker.get(), iKey,
                                input.getBridgeDomainId(), null), voidToRpcResult());
                    }
                });
    }

    public ListenableFuture<RpcResult<Void>> delInterfaceFromBridgeDomain(DelInterfaceFromBridgeDomainInput input) {
        LOG.info("Processing a remote call for removing interface {} from bridge domain.", input.getVppInterfaceName());
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(input.getVppNodeId());
        return Futures.transform(readInterface(vppNodeIid, input.getVppInterfaceName()),
                new AsyncFunction<Optional<Interface>, RpcResult<Void>>() {

                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(Optional<Interface> optIface) throws Exception {
                        if (!optIface.isPresent()) {
                            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                                .withError(
                                        ErrorType.RPC,
                                        "Cannot remove interface " + input.getVppInterfaceName()
                                                + " from bridge domain on node " + vppNodeIid
                                                + ". Not found or deleted.")
                                .build());
                        }
                        Optional<DataBroker> dataBroker = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
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

    @Override
    public void close() throws Exception {
        // NOOP
    }
}
