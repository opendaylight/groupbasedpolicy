/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.module;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.IovisorModuleByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.HostSpecifier;

public class IovisorModuleManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorModuleManager.class);
    private DataBroker dataBroker;

    public IovisorModuleManager(DataBroker passedDataBroker) {
        Preconditions.checkNotNull(passedDataBroker, "DataBroker instance must not be null");
        dataBroker = passedDataBroker;
        LOG.info("Initialized IOVisor IovisorModuleManager");
    }

    public boolean addProvisionedIovisorModule(IovisorModuleInstance iovisorModuleInstance) {
        return addIovisorModule(iovisorModuleInstance, LogicalDatastoreType.CONFIGURATION);
    }

    public boolean addActiveIovisorModule(IovisorModuleId iovisorModuleId) {
        IovisorModuleInstance iovisorModuleInstance = getProvisionedIovisorModule(iovisorModuleId);
        if (iovisorModuleInstance == null) {
            LOG.error("Cannot Activate IovisorModule {}, no provisioned IovisorModule found.",
                    iovisorModuleId.getValue());
            return false;
        }
        return addIovisorModule(iovisorModuleInstance, LogicalDatastoreType.OPERATIONAL);
    }

    public boolean addIovisorModule(IovisorModuleInstance iovisorModuleInstance, LogicalDatastoreType dataStoreType) {
        if (!isValidIovisorModuleInstance(iovisorModuleInstance))
            return false;
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(dataStoreType,
                IovisorIidFactory.iovisorModuleInstanceIid(new IovisorModuleInstanceKey(iovisorModuleInstance.getId())),
                iovisorModuleInstance);
        return DataStoreHelper.submitToDs(wTx);
    }

    public IovisorModuleInstance getActiveIovisorModule(IovisorModuleId iovisorModuleId) {
        if (!isValidIovisorModuleId(iovisorModuleId))
            return null;
        return getIovisorModule(iovisorModuleId, LogicalDatastoreType.OPERATIONAL);
    }

    public IovisorModuleInstance getProvisionedIovisorModule(IovisorModuleId iovisorModuleId) {
        return getIovisorModule(iovisorModuleId, LogicalDatastoreType.CONFIGURATION);
    }

    private IovisorModuleInstance getIovisorModule(IovisorModuleId iovisorModuleId,
            LogicalDatastoreType dataStoreType) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<IovisorModuleInstance> readFromDs = DataStoreHelper.readFromDs(dataStoreType,
                IovisorIidFactory.iovisorModuleInstanceIid(new IovisorModuleInstanceKey(iovisorModuleId)), rTx);
        if (readFromDs.isPresent()) {
            return readFromDs.get();
        }
        return null;
    }

    public IovisorModuleInstances getIovisorModules(ReadOnlyTransaction rTx, LogicalDatastoreType dataStoreType) {
        Optional<IovisorModuleInstances> readFromDs =
                DataStoreHelper.readFromDs(dataStoreType, IovisorIidFactory.iovisorModuleInstanceWildCardIid(), rTx);
        if (readFromDs.isPresent()) {
            return readFromDs.get();
        }
        return null;
    }

    private boolean isValidIovisorModuleInstance(IovisorModuleInstance iovisorModuleInstance) {
        if (iovisorModuleInstance == null || iovisorModuleInstance.getId() == null
                || iovisorModuleInstance.getUri() == null) {
            LOG.info("IovisorModuleInstance was not valid. {} contained null.", iovisorModuleInstance.toString());
            return false;
        }
        if (!isValidIovisorModuleId(iovisorModuleInstance.getId())) {
            return false;
        }
        if (!isValidIovisorModuleUri(iovisorModuleInstance.getUri())) {
            return false;
        }
        return true;
    }

    private boolean isValidIovisorModuleUri(Uri uri) {
        /*
         * TODO Still deciding if want to take IovisorModuleId in host:port form and if host
         * is form name.something.org:port convert to IpAddress:port for putting in Uri. For now
         * assuming Id==Uri
         */
        IovisorModuleId idFromUri = new IovisorModuleId(uri.getValue());
        if (!isValidIovisorModuleId(idFromUri)) {
            LOG.error("IovisorModule Uri is not of form host-specifier:port {}", uri.getValue());
            return false;
        }
        return true;
    }

    private boolean isValidIovisorModuleId(IovisorModuleId id) {
        String[] idParts = id.getValue().split(":");
        if (idParts.length != 2) {
            LOG.error("IovisorModuleId is not of form host-specifier:port {}", id.getValue());
            return false;
        }
        String nameOrIp = idParts[0];
        if (!HostSpecifier.isValid(nameOrIp)) {
            LOG.error("IovisorModuleId host specifier is incorrect format: {}", nameOrIp);
            return false;
        }
        Integer port = Integer.valueOf(idParts[1]);
        if (port < 0 || port > 65535) {
            LOG.error("IovisorModuleId port specifier is incorrect format: {}", port);
            return false;
        }
        return true;
    }

    public List<IovisorModuleInstanceId> getIovisorModulesByTenantByEpg(TenantId tenantId, EndpointGroupId epgId) {
        Optional<IovisorModuleByTenantByEndpointgroupId> returnFromDs =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                        IovisorIidFactory.iovisorModuleByTenantIdByEndpointGroupIdIid(tenantId, epgId),
                        dataBroker.newReadOnlyTransaction());
        if (returnFromDs.isPresent())
            return returnFromDs.get().getIovisorModuleInstanceId();
        return null;
    }

    @Override
    public void close() throws Exception {}

}
