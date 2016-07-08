/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.IovisorModuleByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.IovisorModuleByTenantByEndpointgroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.iovisor.resolved.endpoint.by.tenant.by.endpointgroup.id.IovisorEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.iovisor.resolved.endpoint.by.tenant.by.endpointgroup.id.IovisorEndpointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class EndpointManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);
    private final DataBroker dataBroker;
    private EndpointListener endpointListener;
    private final IovisorEndpointAug iovisorEndpointAug;
    private IovisorModuleManager iovisorModuleManager;
    private IovisorModuleListener iovisorModuleListener;

    public EndpointManager(DataBroker passedDataBroker, EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        Preconditions.checkNotNull(passedDataBroker, "DataBroker instance must not be null");
        dataBroker = passedDataBroker;
        iovisorEndpointAug = new IovisorEndpointAug(epRendererAugmentationRegistry);
        iovisorModuleManager = new IovisorModuleManager(dataBroker);
        iovisorModuleListener = new IovisorModuleListener(dataBroker);
        LOG.info("Initialized IOVisor EndpointManager");
    }

    public IovisorModuleManager getIovisorModuleManager() {
        return iovisorModuleManager;
    }

    public void processEndpoint(EndpointL3 endpoint) {

        IovisorModuleAugmentation iomAug = endpoint.getAugmentation(IovisorModuleAugmentation.class);
        IovisorModuleId iovisorModuleId = new IovisorModuleId(iomAug.getUri().getValue());

        // See if IovisorModule already exists in OPERATIONAL datastore. If not, for now we will
        // register it.
        IovisorModuleInstance iovisorModuleInstance = iovisorModuleManager.getActiveIovisorModule(iovisorModuleId);

        if (iovisorModuleInstance == null) {
            // In this iteration we will provision the IovisorModule (ie add it to CONF) then
            // activate it (add it to OPER)
            iovisorModuleInstance =
                    new IovisorModuleInstanceBuilder().setId(iovisorModuleId).setUri(iomAug.getUri()).build();
            if (iovisorModuleManager.addProvisionedIovisorModule(iovisorModuleInstance)) {
                LOG.info("Provisioned IovisorModule {}}", iovisorModuleInstance.getId());
            } else {
                LOG.error(
                        "Error provisioning IovisorModule {} as part of processing Endpoint Tenant: {} L3C: {} IPAddress: {}",
                        iovisorModuleInstance.getId(), endpoint.getTenant(), endpoint.getL3Context(),
                        endpoint.getIpAddress());
                return;
            }
            // This could be under the successful if test above but end goal with remote Agent is
            // that it registers so top test should go away
            if (iovisorModuleManager.addActiveIovisorModule(iovisorModuleInstance.getId())) {
                LOG.info("Activated IovisorModule {} as part of processing Endpoint Tenant: {} L3C: {} IPAddress: {}",
                        iovisorModuleInstance.getId().getValue(), endpoint.getTenant().getValue(),
                        endpoint.getL3Context().getValue(), endpoint.getIpAddress().getValue());
            } else {
                LOG.error(
                        "Error provisioning IovisorModule {} as part of processing Endpoint Tenant: {} L3C: {} IPAddress: {}",
                        iovisorModuleInstance.getId().getValue(), endpoint.getTenant().getValue(),
                        endpoint.getL3Context().getValue(), endpoint.getIpAddress().getValue());
                return;
            }
        }

        if (addIovisorResolvedEndpoint(endpoint)) {
            LOG.info("Processed Endpoint Tenant {} L3Context: {} 1st IP Address: {}", endpoint.getTenant().getValue(),
                    endpoint.getL3Context().getValue(), endpoint.getIpAddress().getValue());
            return;
        } else {
            LOG.error(
                    "Could not add to IovisorResolvedEndpoint store following Endpoint Tenant {} L3Context: {} 1st IP Address: {}",
                    endpoint.getTenant().getValue(), endpoint.getL3Context().getValue(),
                    endpoint.getIpAddress().getValue());
            return;
        }
    }

    @VisibleForTesting
    boolean addIovisorResolvedEndpoint(EndpointL3 endpoint) {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx = addToIovisorResolvedEndpointDatastore(endpoint, wTx);
        wTx = addToIovisorResolvedEndpointByTenantByEndpointgroupId(endpoint, wTx);
        wTx = addToIovisorModulesByTenantByEndpointgroupId(endpoint, wTx);
        return DataStoreHelper.submitToDs(wTx);
    }

    @VisibleForTesting
    WriteTransaction addToIovisorModulesByTenantByEndpointgroupId(EndpointL3 endpoint, WriteTransaction wTx) {
        Set<EndpointGroupId> epgIds = new HashSet<>();
        if (endpoint.getEndpointGroup() != null)
            epgIds.add(endpoint.getEndpointGroup());
        if (endpoint.getEndpointGroups() != null)
            epgIds.addAll(endpoint.getEndpointGroups());

        IovisorModuleInstanceId iomId = new IovisorModuleInstanceIdBuilder()
            .setId(new IovisorModuleId(endpoint.getAugmentation(IovisorModuleAugmentation.class).getUri().getValue()))
            .build();

        List<IovisorModuleInstanceId> iomIds = ImmutableList.of(iomId);

        for (EndpointGroupId epg : epgIds) {
            IovisorModuleByTenantByEndpointgroupId data = new IovisorModuleByTenantByEndpointgroupIdBuilder()
                .setTenantId(endpoint.getTenant()).setEndpointgroupId(epg).setIovisorModuleInstanceId(iomIds).build();
            wTx.merge(LogicalDatastoreType.OPERATIONAL,
                    IovisorIidFactory.iovisorModuleByTenantIdByEndpointGroupIdIid(endpoint.getTenant(), epg), data,
                    true);
        }
        return wTx;
    }

    @VisibleForTesting
    WriteTransaction addToIovisorResolvedEndpointByTenantByEndpointgroupId(EndpointL3 endpoint, WriteTransaction wTx) {
        Set<EndpointGroupId> epgIds = new HashSet<>();
        if (endpoint.getEndpointGroup() != null)
            epgIds.add(endpoint.getEndpointGroup());
        if (endpoint.getEndpointGroups() != null)
            epgIds.addAll(endpoint.getEndpointGroups());

        IovisorEndpoint iovisorEndpoint = new IovisorEndpointBuilder().setIpAddress(endpoint.getIpAddress())
            .setL3Context(endpoint.getL3Context())
            .build();
        List<IovisorEndpoint> iovisorEndpoints = ImmutableList.of(iovisorEndpoint);

        for (EndpointGroupId epg : epgIds) {
            IovisorResolvedEndpointByTenantByEndpointgroupId iovisorResolvedEndpointbyTenantByEndpointgroupId =
                    new IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder().setTenantId(endpoint.getTenant())
                        .setEndpointgroupId(epg)
                        .setIovisorEndpoint(iovisorEndpoints)
                        .build();
            wTx.merge(LogicalDatastoreType.OPERATIONAL,
                    IovisorIidFactory.iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(endpoint.getTenant(), epg),
                    iovisorResolvedEndpointbyTenantByEndpointgroupId, true);
            LOG.trace("Added endpoint via endpoingGroups() to ResolvedEndpoints for {} {}",
                    endpoint.getTenant().getValue(), epg.getValue());
        }

        return wTx;
    }

    @VisibleForTesting
    WriteTransaction addToIovisorResolvedEndpointDatastore(EndpointL3 endpoint, WriteTransaction wTx) {
        IovisorResolvedEndpoint iovisorResolvedEndpoint = new IovisorResolvedEndpointBuilder()
            .setIpAddress(endpoint.getIpAddress()).setL3Context(endpoint.getL3Context()).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                IovisorIidFactory.iovisorResolvedEndpointIid(endpoint.getL3Context(), endpoint.getIpAddress()),
                iovisorResolvedEndpoint, true);
        LOG.trace("Added endpoint  to ResolvedEndpoints for {} {}", endpoint.getL3Context().getValue(),
                endpoint.getIpAddress().getValue());
        return wTx;
    }

    @VisibleForTesting
    boolean isResolvedEndpointByTenantByEpg(final L3ContextId l3ContextId, final IpAddress ipAddr, TenantId tenantId,
            EndpointGroupId epgId) {

        Collection<IovisorEndpoint> filter = Collections2.filter(getResolvedEndpointsByTenantByEpg(tenantId, epgId),
                new Predicate<IovisorEndpoint>() {

                    @Override
                    public boolean apply(IovisorEndpoint input) {
                        return (input.getL3Context().equals(l3ContextId) && input.getIpAddress().equals(ipAddr));
                    }
                });
        if (filter.isEmpty())
            return false;
        return true;
    }

    @VisibleForTesting
    Collection<IovisorEndpoint> getResolvedEndpointsByTenantByEpg(TenantId tenantId, EndpointGroupId epgId) {
        Optional<IovisorResolvedEndpointByTenantByEndpointgroupId> returnFromDs =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                        IovisorIidFactory.iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(tenantId, epgId),
                        dataBroker.newReadOnlyTransaction());
        if (returnFromDs.isPresent())
            return returnFromDs.get().getIovisorEndpoint();
        return null;
    }

    @VisibleForTesting
    boolean isResolvedEndpoint(final L3ContextId l3ContextId, final IpAddress ipAddr) {

        Collection<IovisorResolvedEndpoint> filter =
                Collections2.filter(getResolvedEndpoints(), new Predicate<IovisorResolvedEndpoint>() {

                    @Override
                    public boolean apply(IovisorResolvedEndpoint input) {
                        return (input.getL3Context().equals(l3ContextId) && input.getIpAddress().equals(ipAddr));
                    }
                });
        if (filter.isEmpty())
            return false;
        return true;
    }

    @VisibleForTesting
    Collection<IovisorResolvedEndpoint> getResolvedEndpoints() {
        Optional<IovisorResolvedEndpoints> returnFromDs = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IovisorIidFactory.iovisorResolvedEndpointWildcardIid(), dataBroker.newReadOnlyTransaction());
        if (returnFromDs.isPresent())
            return returnFromDs.get().getIovisorResolvedEndpoint();
        return null;
    }

    @Override
    public void close() throws Exception {
        if (iovisorEndpointAug != null)
            iovisorEndpointAug.close();
        if (iovisorModuleListener != null)
            iovisorModuleListener.close();
        if (endpointListener != null)
            endpointListener.close();

    }
}
