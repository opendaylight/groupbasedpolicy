/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class NeutronHelper {

    /**
     * This looks up the Endpoint L2 key from an
     * operational data store kept in neutron-mapper
     *
     * @param externalId The neutron port UUID
     * @param dataBroker {@link DataBroker} to use for the transaction
     * @return {@link EndpointKey} of the matching Endpoint, null if not found
     */
    public static EndpointKey getEpKeyFromNeutronMapper(Uuid externalId, DataBroker dataBroker) {

        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<EndpointByPort> iid = InstanceIdentifier
                .create(Mappings.class)
                .child(GbpByNeutronMappings.class)
                .child(EndpointsByPorts.class)
                .child(EndpointByPort.class, new EndpointByPortKey(new UniqueId(externalId)));
        Optional<EndpointByPort> optionalEp = readFromDs(LogicalDatastoreType.OPERATIONAL, iid, transaction );
        if (optionalEp.isPresent()) {
            EndpointByPort epByPort = optionalEp.get();
            return new EndpointKey(epByPort.getL2Context(), epByPort.getMacAddress());
        }
        return null;
    }


}
