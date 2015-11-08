/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.LogicalNetworks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.MappedTenantsEntities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetworkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedEntityKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedTenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContractKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasIidFactory {

    public static InstanceIdentifier<LogicalNetworks> logicalNetworksIid() {
        return InstanceIdentifier.builder(LogicalNetworks.class).build();
    }

    public static InstanceIdentifier<LogicalNetwork> logicalNetworkIid(EndpointGroupId consumerEpgId,
            TenantId consumerTenantId, ContractId contractId, EndpointGroupId providerEpgId, TenantId providerTenantId) {
        return InstanceIdentifier.builder(LogicalNetworks.class)
            .child(LogicalNetwork.class,
                    new LogicalNetworkKey(consumerEpgId, consumerTenantId, contractId, providerEpgId, providerTenantId))
            .build();
    }

    public static InstanceIdentifier<MappedTenantsEntities> mappedTenantsEntitiesIid() {
        return InstanceIdentifier.builder(MappedTenantsEntities.class).build();
    }

    public static InstanceIdentifier<MappedSubnet> mappedSubnetIid(TenantId gbpTenantId, SubnetId subnetId) {
        return InstanceIdentifier.builder(MappedTenantsEntities.class)
            .child(MappedEntity.class, new MappedEntityKey(gbpTenantId))
            .child(MappedSubnet.class, new MappedSubnetKey(subnetId))
            .build();
    }

    public static InstanceIdentifier<MappedTenant> mappedTenantIid(TenantId gbpTenantId) {
        return InstanceIdentifier.builder(MappedTenantsEntities.class)
            .child(MappedTenant.class, new MappedTenantKey(gbpTenantId))
            .build();
    }

    public static InstanceIdentifier<MappedEntity> mappedEntityIid(TenantId gbpTenantId) {
        return InstanceIdentifier.builder(MappedTenantsEntities.class)
            .child(MappedEntity.class, new MappedEntityKey(gbpTenantId))
            .build();
    }

    public static InstanceIdentifier<MappedContract> mappedContractIid(TenantId gbpTenantId, ContractId contractId) {
        return InstanceIdentifier.builder(MappedTenantsEntities.class)
            .child(MappedEntity.class, new MappedEntityKey(gbpTenantId))
            .child(MappedContract.class, new MappedContractKey(contractId))
            .build();
    }

    public static InstanceIdentifier<MappedEndpoint> mappedEndpointIid(TenantId gbpTenantId,
            MappedEndpointKey mappedEndpointKey) {
        return InstanceIdentifier.builder(MappedTenantsEntities.class)
            .child(MappedEntity.class, new MappedEntityKey(gbpTenantId))
            .child(MappedEndpoint.class, mappedEndpointKey)
            .build();
    }
}
