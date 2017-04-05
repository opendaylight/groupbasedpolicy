/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;

import com.google.common.base.Preconditions;

public class NetworkClient extends ServiceUtil {

    private static final Name NETWORK_CLIENT_EPG_NAME = new Name("NETWORK_CLIENT");
    private static final Description NETWORK_CLIENT_EPG_DESC = new Description("Represents DHCP and DNS clients.");
    /**
     * ID of {@link #EPG}
     */
    public static final EndpointGroupId EPG_ID = new EndpointGroupId("ccc5e444-573c-11e5-885d-feff819cdc9f");
    /**
     * Network-client endpoint-group consuming no contract
     */
    public static final EndpointGroup EPG;

    static {
        EPG = createNetworkClientEpg();
    }

    private static EndpointGroup createNetworkClientEpg() {
        return createEpgBuilder(EPG_ID, NETWORK_CLIENT_EPG_NAME, NETWORK_CLIENT_EPG_DESC).build();
    }

    /**
     * Puts {@link #EPG} to {@link LogicalDatastoreType#CONFIGURATION}
     *
     * @param tenantId location of {@link #EPG}
     * @param wTx transaction where {@link #EPG} is written
     */
    public static void writeNetworkClientEntitiesToTenant(TenantId tenantId, WriteTransaction wTx) {
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, EPG_ID), EPG, true);
    }

    /**
     * Puts consumer-named-selector to {@link #EPG} in {@link LogicalDatastoreType#CONFIGURATION}
     *
     * @param tenantId tenantId location of {@link #EPG}
     * @param consumerNamedSelector is added to {@link #EPG}
     * @param wTx transaction where the given consumer-named-selector is written
     */
    public static void writeConsumerNamedSelector(TenantId tenantId, ConsumerNamedSelector consumerNamedSelector,
            WriteTransaction wTx) {
        Preconditions.checkNotNull(consumerNamedSelector);
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.consumerNamedSelectorIid(tenantId, EPG_ID, consumerNamedSelector.getName()),
                consumerNamedSelector, true);
    }

}
