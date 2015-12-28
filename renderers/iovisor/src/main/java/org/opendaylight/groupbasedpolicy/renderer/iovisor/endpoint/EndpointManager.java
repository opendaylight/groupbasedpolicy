/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.IovisorResolvedEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.iovisor.resolved.endpoint.by.tenant.by.endpointgroup.id.IovisorEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.iovisor.resolved.endpoint.by.tenant.by.endpointgroup.id.IovisorEndpointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

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

    @VisibleForTesting
    IovisorModuleManager getIovisorModuleManager() {
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
        IovisorResolvedEndpoint iovisorResolvedEndpoint = new IovisorResolvedEndpointBuilder()
            .setIpAddress(endpoint.getIpAddress()).setL3Context(endpoint.getL3Context()).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                IovisorIidFactory.iovisorResolvedEndpointIid(endpoint.getL3Context(), endpoint.getIpAddress()),
                iovisorResolvedEndpoint, true);

        IovisorEndpoint iovisorEndpoint = new IovisorEndpointBuilder().setIpAddress(endpoint.getIpAddress())
            .setL3Context(endpoint.getL3Context())
            .build();
        List<IovisorEndpoint> iovisorEndpoints = new ArrayList<>();
        iovisorEndpoints.add(iovisorEndpoint);

        IovisorResolvedEndpointByTenantByEndpointgroupId iovisorResolvedEndpointbyTenantByEndpointgroupId;
        if (endpoint.getEndpointGroup() != null) {
            iovisorResolvedEndpointbyTenantByEndpointgroupId =
                    new IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder().setTenantId(endpoint.getTenant())
                        .setEndpointgroupId(endpoint.getEndpointGroup())
                        .setIovisorEndpoint(iovisorEndpoints)
                        .build();
            wTx.merge(LogicalDatastoreType.OPERATIONAL,
                    IovisorIidFactory.iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(endpoint.getTenant(),
                            endpoint.getEndpointGroup()),
                    iovisorResolvedEndpointbyTenantByEndpointgroupId, true);
            LOG.trace("Added endpoint via endpointGroup to ResolvedEndpoints for {} {}",
                    endpoint.getTenant().getValue(), endpoint.getEndpointGroup().getValue());
        }
        if (endpoint.getEndpointGroups() != null) {
            for (EndpointGroupId epg : endpoint.getEndpointGroups()) {
                iovisorResolvedEndpointbyTenantByEndpointgroupId =
                        new IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder().setTenantId(endpoint.getTenant())
                            .setEndpointgroupId(epg)
                            .setIovisorEndpoint(iovisorEndpoints)
                            .build();
                wTx.merge(
                        LogicalDatastoreType.OPERATIONAL, IovisorIidFactory
                            .iovisorResolvedEndpointByTenantIdByEndpointGroupIdIid(endpoint.getTenant(), epg),
                        iovisorResolvedEndpointbyTenantByEndpointgroupId, true);
                LOG.trace("Added endpoint via endpoingGroups() to ResolvedEndpoints for {} {}",
                        endpoint.getTenant().getValue(), epg.getValue());
            }
        }
        return DataStoreHelper.submitToDs(wTx);

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
