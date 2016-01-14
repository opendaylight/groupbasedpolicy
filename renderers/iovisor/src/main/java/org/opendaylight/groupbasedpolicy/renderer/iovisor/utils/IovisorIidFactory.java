/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.utils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModulesByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpointsByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.IovisorModuleByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.IovisorModuleByTenantByEndpointgroupIdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupIdKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IovisorIidFactory {

    private IovisorIidFactory() {}

    private static final InstanceIdentifier<IovisorModuleInstances> IOVISOR_MODULE_INSTANCES_IID =
            InstanceIdentifier.builder(IovisorModuleInstances.class).build();

    /**
     * @return The {@link InstanceIdentifier} of the {@link IovisorModuleInstances}
     */
    public static InstanceIdentifier<IovisorModuleInstances> iovisorModuleInstanceWildCardIid() {
        return IOVISOR_MODULE_INSTANCES_IID;
    }

    /**
     * Return the InstanceIdentifier for a specific IovisorModuleInstance.
     *
     * @param iovisorModuleInstanceKey The key of the {@link IovisorModuleInstance} we want to
     *        retrieve.
     * @return The {@link InstanceIdentifier} of the {@link IovisorModuleInstance}
     */
    public static InstanceIdentifier<IovisorModuleInstance> iovisorModuleInstanceIid(
            IovisorModuleInstanceKey iovisorModuleInstanceKey) {
        return IOVISOR_MODULE_INSTANCES_IID.child(IovisorModuleInstance.class, iovisorModuleInstanceKey);
    }

    public static InstanceIdentifier<IovisorResolvedEndpoints> iovisorResolvedEndpointWildcardIid() {
        return InstanceIdentifier.builder(IovisorResolvedEndpoints.class).build();
    }

    public static InstanceIdentifier<IovisorResolvedEndpoint> iovisorResolvedEndpointIid(L3ContextId l3Context,
            IpAddress ipAddress) {
        return iovisorResolvedEndpointIid(new IovisorResolvedEndpointKey(ipAddress, l3Context));
    }

    public static InstanceIdentifier<IovisorResolvedEndpoint> iovisorResolvedEndpointIid(
            IovisorResolvedEndpointKey iovisorResolvedEndpointKey) {
        return InstanceIdentifier.builder(IovisorResolvedEndpoints.class)
            .child(IovisorResolvedEndpoint.class, iovisorResolvedEndpointKey)
            .build();
    }

    public static InstanceIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> iovisorResolvedEndpointsByTenantIdByEndpointGroupIdWildCardIid() {
        return InstanceIdentifier.builder(IovisorResolvedEndpointsByTenantByEndpointgroupId.class).build();
    }

    public static InstanceIdentifier<IovisorResolvedEndpointByTenantByEndpointgroupId> iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(
            TenantId tenantId, EndpointGroupId epgId) {
        return iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(
                new IovisorResolvedEndpointByTenantByEndpointgroupIdKey(epgId, tenantId));
    }

    public static InstanceIdentifier<IovisorResolvedEndpointByTenantByEndpointgroupId> iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(
            IovisorResolvedEndpointByTenantByEndpointgroupIdKey key) {
        return InstanceIdentifier.builder(IovisorResolvedEndpointsByTenantByEndpointgroupId.class)
            .child(IovisorResolvedEndpointByTenantByEndpointgroupId.class, key)
            .build();
    }

    public static InstanceIdentifier<IovisorModulesByTenantByEndpointgroupId> iovisorModulesByTenantIdByEndpointGroupIdWildCardIid() {
        return InstanceIdentifier.builder(IovisorModulesByTenantByEndpointgroupId.class).build();
    }

    public static InstanceIdentifier<IovisorModuleByTenantByEndpointgroupId> iovisorModuleByTenantIdByEndpointGroupIdIid(
            TenantId tenantId, EndpointGroupId epgId) {
        return iovisorModuleByTenantIdByEndpointGroupIdIid(
                new IovisorModuleByTenantByEndpointgroupIdKey(epgId, tenantId));
    }

    public static InstanceIdentifier<IovisorModuleByTenantByEndpointgroupId> iovisorModuleByTenantIdByEndpointGroupIdIid(
            IovisorModuleByTenantByEndpointgroupIdKey key) {
        return InstanceIdentifier.builder(IovisorModulesByTenantByEndpointgroupId.class)
            .child(IovisorModuleByTenantByEndpointgroupId.class, key)
            .build();
    }

}
