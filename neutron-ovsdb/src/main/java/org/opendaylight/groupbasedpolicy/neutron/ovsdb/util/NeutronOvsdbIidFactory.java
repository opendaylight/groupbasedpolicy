/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.FloatingIpAssociationMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.InternalPortsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.floating.ip.ports.EndpointByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronOvsdbIidFactory {

    public static InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeAugmentationIid(TopologyId ovsdbTopologyId) {
        return InstanceIdentifier.create(NetworkTopology.class) .child(Topology.class, new TopologyKey(ovsdbTopologyId))
        .child(Node.class)
        .augmentation(OvsdbNodeAugmentation.class);
    }

    public static InstanceIdentifier<ExternalGatewayAsL3Endpoint> neutronGbpExternalGatewayIidWildcard() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(NeutronByGbpMappings.class)
                .child(ExternalGatewaysAsL3Endpoints.class)
                .child(ExternalGatewayAsL3Endpoint.class)
                .build();
    }

    public static InstanceIdentifier<EndpointByFloatingIpPort> neutronGbpFloatingIpIidWildcard() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(GbpByNeutronMappings.class)
                .child(EndpointsByFloatingIpPorts.class)
                .child(EndpointByFloatingIpPort.class)
                .build();
    }

    public static InstanceIdentifier<EndpointByPort> endpointByPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(Mappings.class)
                .child(GbpByNeutronMappings.class)
                .child(EndpointsByPorts.class)
                .child(EndpointByPort.class, new EndpointByPortKey(portId))
                .build();
    }

    public static InstanceIdentifier<InternalPortByFloatingIpPort> internalPortByFloatingIpIid(UniqueId floatingIpPortId) {
        return InstanceIdentifier.builder(Mappings.class)
                .child(FloatingIpAssociationMappings.class)
                .child(InternalPortsByFloatingIpPorts.class)
                .child(InternalPortByFloatingIpPort.class, new InternalPortByFloatingIpPortKey(floatingIpPortId))
                .build();
    }

    public static InstanceIdentifier<InternalPortByFloatingIpPort> neutronGbpInternalPortByFloatingIpIidWildcard() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(FloatingIpAssociationMappings.class)
                .child(InternalPortsByFloatingIpPorts.class)
                .child(InternalPortByFloatingIpPort.class)
                .build();
    }


    public static InstanceIdentifier<NeutronByGbpMappings> neutronGbpMappingsIidWildcard() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(NeutronByGbpMappings.class)
                .build();
    }
}
