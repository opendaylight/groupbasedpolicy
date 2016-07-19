/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.FlatNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.NetworkTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VlanNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.bridge.domain.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.bridge.domain.PhysicalLocationRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.NetworkTypeFlat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.NetworkTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class NetworkAware implements MappingProvider<Network> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkAware.class);

    private final DataBroker dataBroker;

    public NetworkAware(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public InstanceIdentifier<Network> getNeutronDtoIid() {
        return InstanceIdentifier.builder(Neutron.class).child(Networks.class).child(Network.class).build();
    }

    @Override
    public void processCreatedNeutronDto(Network network) {
        BridgeDomain bridgeDomain = createBridgeDomain(network);
        if (bridgeDomain != null) {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, getBridgeDomainIid(bridgeDomain.getId()), bridgeDomain, true);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    @VisibleForTesting
    BridgeDomain createBridgeDomain(Network network) {
        BridgeDomainBuilder bridgeDomainBuilder = new BridgeDomainBuilder();
        String description = (network.getName() != null) ? network.getName() : "Neutron network";
        bridgeDomainBuilder.setDescription(description);
        bridgeDomainBuilder.setId(network.getUuid().getValue());
        NetworkProviderExtension providerAug = network.getAugmentation(NetworkProviderExtension.class);
        if (providerAug == null || providerAug.getNetworkType() == null) {
            LOG.error("Cannot create VPP bridge domain. Network type not specified in neutron network: {}", network);
            return null;
        }
        Class<? extends NetworkTypeBase> netType = convertNetworkType(providerAug.getNetworkType());
        if (netType == null) {
            return null;
        }
        bridgeDomainBuilder.setPhysicalLocationRef(resolveDomainLocations(providerAug));
        bridgeDomainBuilder.setType(netType);
        if (providerAug.getNetworkType().isAssignableFrom(NetworkTypeVlan.class)
                && providerAug.getSegmentationId() != null) {
            try {
                bridgeDomainBuilder.setVlan(new VlanId(Integer.valueOf(providerAug.getSegmentationId())));
            } catch (NumberFormatException e) {
                LOG.error("Neutron network {}. Cannot create VLAN ID from segmentation-id: {}. {}",
                        providerAug.getSegmentationId(), network.getUuid(), e);
                return null;
            }
        }
        return bridgeDomainBuilder.build();
    }

    @VisibleForTesting
    List<PhysicalLocationRef> resolveDomainLocations(NetworkProviderExtension providerAug) {
        List<PhysicalLocationRef> locationRefs = new ArrayList<>();
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        if (providerAug.getPhysicalNetwork() == null) {
            return null;
        }
        Optional<Topology> readTopology = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                getTopologyIid(new TopologyId(providerAug.getPhysicalNetwork())), rTx);
        if (readTopology.isPresent()) {
            Topology topology = readTopology.get();
            for (Node node : topology.getNode()) {
                PhysicalLocationRefBuilder location = new PhysicalLocationRefBuilder();
                location.setNodeId(node.getNodeId());
                location.setInterface(Lists.transform(node.getTerminationPoint(),
                        new Function<TerminationPoint, String>() {

                            @Override
                            public String apply(TerminationPoint input) {
                                return input.getTpId().getValue();
                            }
                        }));
                locationRefs.add(location.build());
            }
        }
        return locationRefs;
    }

    public static Class<? extends NetworkTypeBase> convertNetworkType(
            Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.NetworkTypeBase> base) {
        if (base.isAssignableFrom(NetworkTypeFlat.class)) {
            return FlatNetwork.class;
        }
        if (base.isAssignableFrom(NetworkTypeVlan.class)) {
            return VlanNetwork.class;
        }
        return null;
    }

    InstanceIdentifier<Topology> getTopologyIid(TopologyId topologyId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(topologyId))
            .build();
    }

    InstanceIdentifier<BridgeDomain> getBridgeDomainIid(String id) {
        return InstanceIdentifier.builder(Config.class).child(BridgeDomain.class, new BridgeDomainKey(id)).build();
    }

    @Override
    public void processUpdatedNeutronDto(Network originalNetwork, Network updatedNetwork) {
        InstanceIdentifier<BridgeDomain> bdIid = getBridgeDomainIid(originalNetwork.getUuid().getValue());
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        deleteBridgeDomainIfPresent(rwTx, bdIid);
        BridgeDomain updatedBridgeDomain = createBridgeDomain(updatedNetwork);
        if (updatedBridgeDomain != null) {
            rwTx.put(LogicalDatastoreType.CONFIGURATION, bdIid, updatedBridgeDomain, true);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    private void deleteBridgeDomainIfPresent(ReadWriteTransaction rwTx, InstanceIdentifier<BridgeDomain> bdIid) {
        Optional<BridgeDomain> readFromDs = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, bdIid, rwTx);
        if (readFromDs.isPresent()) {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, bdIid);
        }
    }

    @Override
    public void processDeletedNeutronDto(Network network) {
        InstanceIdentifier<BridgeDomain> bdIid = getBridgeDomainIid(network.getUuid().getValue());
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        deleteBridgeDomainIfPresent(rwTx, bdIid);
        DataStoreHelper.submitToDs(rwTx);
    }
}
