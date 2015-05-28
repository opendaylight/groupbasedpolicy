/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
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

    public static InstanceIdentifier<ExternalGatewaysAsL3Endpoints> neutronGbpExternalGatewaysIid() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(NeutronByGbpMappings.class)
                .child(ExternalGatewaysAsL3Endpoints.class)
                .build();
    }

    public static InstanceIdentifier<NeutronByGbpMappings> neutronGbpMappingsIid() {
        return InstanceIdentifier.builder(Mappings.class)
                .child(NeutronByGbpMappings.class)
                .build();
    }
}
