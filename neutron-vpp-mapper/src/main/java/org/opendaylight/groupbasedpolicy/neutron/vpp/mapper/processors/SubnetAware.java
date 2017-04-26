/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/3/17.
 */
public class SubnetAware implements MappingProvider<Subnet>{
    private static final Logger LOG = LoggerFactory.getLogger(SubnetAware.class);

    private SubnetHandler subnetHandler;

    public SubnetAware(DataBroker dataBroker) {
        subnetHandler = new SubnetHandler(dataBroker);
    }

    @Override
    public InstanceIdentifier<Subnet> getNeutronDtoIid() {
        return InstanceIdentifier.builder(Neutron.class).child(Subnets.class).child(Subnet.class).build();
    }

    @Override
    public void processCreatedNeutronDto(Subnet subnet) {
        LOG.debug("Got create for subnet: {}", subnet.getUuid().getValue());
        subnetHandler.processCreatedNeutronDto(subnet);
    }

    @Override
    public void processUpdatedNeutronDto(Subnet original, Subnet delta) {
        LOG.debug("Got update of subnet: {}", original.getUuid().getValue());
        subnetHandler.processUpdatedNeutronDto(original, delta);
    }

    @Override
    public void processDeletedNeutronDto(Subnet subnet) {
        LOG.debug("Got delete of subnet: {}", subnet.getUuid().getValue());
        subnetHandler.processDeletedNeutronDto(subnet);
    }
}