/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BaseEndpointByPortListener extends DataTreeChangeHandler<BaseEndpointByPort> implements
        MappingProvider<Port> {

    private final PortHandler portHandler;

    protected BaseEndpointByPortListener(PortHandler portHandler, DataBroker dataProvider) {
        super(dataProvider);
        this.portHandler = portHandler;
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Mappings.class)
                    .child(GbpByNeutronMappings.class)
                    .child(BaseEndpointsByPorts.class)
                    .child(BaseEndpointByPort.class)
                    .build()));
    }

    @Override
    public InstanceIdentifier<Port> getNeutronDtoIid() {
        return portHandler.createWildcartedPortIid();
    }

    @Override
    public void processCreatedNeutronDto(Port port) {
        portHandler.processCreated(port);
    }

    @Override
    public void processUpdatedNeutronDto(Port original, Port delta) {
        portHandler.processUpdated(original, delta);
    }

    @Override
    public void processDeletedNeutronDto(Port port) {
        // handled by BaseEndpointByPort removal
    }

    @Override
    protected void onWrite(DataObjectModification<BaseEndpointByPort> rootNode,
            InstanceIdentifier<BaseEndpointByPort> rootIdentifier) {
        if (rootNode.getDataBefore() == null) {
            portHandler.processCreated(rootNode.getDataAfter());
        }
    }

    @Override
    protected void onDelete(DataObjectModification<BaseEndpointByPort> rootNode,
            InstanceIdentifier<BaseEndpointByPort> rootIdentifier) {
        portHandler.processDeleted(rootNode.getDataBefore());
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<BaseEndpointByPort> rootNode,
            InstanceIdentifier<BaseEndpointByPort> rootIdentifier) {
        // update should not happen
    }
}
