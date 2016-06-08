/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class PortHandler implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(MappingProvider.class);

    private static final String[] COMPUTE_OWNER = {"compute"};
    private static final String VHOST_USER = "vhostuser";
    private static final String NETCONF_TOPOLOGY_ID = "topology-netconf";

    private BindingTransactionChain transactionChain;
    PortAware portByBaseEpListener;
    DataBroker dataBroker;
    SocketInfo socketInfo;

    PortHandler(DataBroker dataBroker, SocketInfo socketInfo) {
        this.dataBroker = dataBroker;
        this.socketInfo = socketInfo;
        transactionChain = this.dataBroker.createTransactionChain(this);
    }

    void processCreated(Port port) {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<BaseEndpointByPort> optBaseEpByPort = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                createBaseEpByPortIid(port.getUuid()), rTx);
        if (!optBaseEpByPort.isPresent()) {
            return;
        }
        processCreatedData(port, optBaseEpByPort.get());
    }

    void processCreated(BaseEndpointByPort bebp) {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Port> optPort = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                createPortIid(bebp.getPortId()), rTx);
        if (!optPort.isPresent()) {
            return;
        }
        processCreatedData(optPort.get(), bebp);
    }

    @VisibleForTesting
    void processCreatedData(Port port, BaseEndpointByPort bebp) {
        if (isValidVhostUser(port)) {
            VppEndpoint vppEp = buildVhostUserEndpoint(port, bebp);
            writeVppEndpoint(createVppEndpointIid(vppEp.getKey()), vppEp);
            LOG.debug("Created vpp-endpoint {}", vppEp);
        }
    }

    private boolean isValidVhostUser(Port port) {
        PortBindingExtension portBindingExt = port.getAugmentation(PortBindingExtension.class);
        if (portBindingExt != null) {
            String vifType = portBindingExt.getVifType();
            String deviceOwner = port.getDeviceOwner();
            if (vifType != null && deviceOwner != null) {
                if (vifType.contains(VHOST_USER)) {
                    for (String computeOwner : COMPUTE_OWNER) {
                        if (deviceOwner.contains(computeOwner)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void processUpdated(Port original, Port delta) {
        if (isValidVhostUser(original)) {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<BaseEndpointByPort> optBebp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    createBaseEpByPortIid(original.getUuid()), rTx);
            rTx.close();
            if (!optBebp.isPresent()) {
                return;
            }
            processDeleted(optBebp.get());
        }
        processCreated(delta);
    }

    void processDeleted(BaseEndpointByPort bebp) {
        VppEndpointKey vppEpKey = new VppEndpointKey(bebp.getAddress(), bebp.getAddressType(), bebp.getContextId(),
                bebp.getContextType());
        InstanceIdentifier<VppEndpoint> vppEpIid = createVppEndpointIid(vppEpKey);
        ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
        Optional<VppEndpoint> readVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, vppEpIid, rTx);
        rTx.close();
        if (readVppEp.isPresent()) {
            writeVppEndpoint(vppEpIid, null);
            LOG.debug("Deleted vpp-endpoint {}", vppEpKey);
        }
    }

    private void writeVppEndpoint(InstanceIdentifier<VppEndpoint> vppEpIid, VppEndpoint vppEp) {
        WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
        if (vppEp != null) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, vppEp, true);
        } else {
            wTx.delete(LogicalDatastoreType.CONFIGURATION, vppEpIid);
        }
        try {
            wTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction chain commit failed. {}", e);
            transactionChain.close();
            transactionChain = dataBroker.createTransactionChain(this);
        }
    }

    @VisibleForTesting
    VppEndpoint buildVhostUserEndpoint(Port port, BaseEndpointByPort bebp) {
        PortBindingExtension portBinding = port.getAugmentation(PortBindingExtension.class);
        String socket = socketInfo.getSocketPath() + socketInfo.getSocketPrefix() + bebp.getPortId().getValue();
        return new VppEndpointBuilder().setDescription("neutron port")
            .setContextId(bebp.getContextId())
            .setContextType(bebp.getContextType())
            .setAddress(bebp.getAddress())
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(socket).build())
            .setAddressType(bebp.getAddressType())
            .setVppInterfaceName(bebp.getPortId().getValue())
            .setVppNodePath(createNodeIid(new NodeId(portBinding.getHostId())))
            .build();
    }

    private InstanceIdentifier<Node> createNodeIid(NodeId nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(NETCONF_TOPOLOGY_ID)))
            .child(Node.class, new NodeKey(nodeId))
            .build();
    }

    private InstanceIdentifier<VppEndpoint> createVppEndpointIid(VppEndpointKey vppEpKey) {
        return InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEpKey).build();
    }

    private InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(Uuid uuid) {
        return createBaseEpByPortIid(new UniqueId(uuid.getValue()));
    }

    private InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(UniqueId uuid) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(BaseEndpointsByPorts.class)
            .child(BaseEndpointByPort.class, new BaseEndpointByPortKey(uuid))
            .build();
    }

    InstanceIdentifier<Port> createWildcartedPortIid() {
        return portsIid().child(Port.class).build();
    }

    private InstanceIdentifier<Port> createPortIid(UniqueId uuid) {
        return portsIid().child(Port.class, new PortKey(new Uuid(uuid.getValue()))).build();
    }

    private InstanceIdentifierBuilder<Ports> portsIid() {
        return InstanceIdentifier.builder(Neutron.class).child(Ports.class);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        LOG.error("Transaction chain failed. {}", cause.getMessage());
        transactionChain.close();
        transactionChain = dataBroker.createTransactionChain(this);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.trace("Transaction chain was successfull. {}", chain);
    }
}
