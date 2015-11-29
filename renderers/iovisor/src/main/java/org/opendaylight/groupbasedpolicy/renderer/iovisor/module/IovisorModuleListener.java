/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.module;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IovisorModuleListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorModuleListener.class);

    private final ListenerRegistration<DataChangeListener> registerListener;

    public IovisorModuleListener(DataBroker dataBroker) {
        this.registerListener = dataBroker.registerDataChangeListener(
                                                LogicalDatastoreType.OPERATIONAL,
                                                IovisorIidFactory.iovisorModuleInstancesIid().child(IovisorModuleInstance.class),
                                                this,
                                                AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        created(changes.getCreatedData());
        updated(changes.getUpdatedData());
        removed(changes);
    }

    /**
     * Process created events.
     *
     * @param created
     *            Created data
     */
    private void created(Map<InstanceIdentifier<?>, DataObject> created) {
        for (Entry<InstanceIdentifier<?>, DataObject> newEndpoint : created.entrySet()) {
            IovisorModuleInstance iovisorModuleInstance = fromMd(newEndpoint.getKey(), (IovisorModuleInstance) newEndpoint.getValue());
            LOG.debug("IovisorModuleInstance CREATED {}", iovisorModuleInstance);
            //TODO process created event
        }
    }

    /**
     * Process updated events.
     *
     * @param updated
     *            updated data
     */
    private void updated(Map<InstanceIdentifier<?>, DataObject> updated) {
        for (Entry<InstanceIdentifier<?>, DataObject> updatedEndpoint : updated.entrySet()) {
            IovisorModuleInstance iovisorModuleInstance = fromMd(updatedEndpoint.getKey(), (IovisorModuleInstance) updatedEndpoint.getValue());
            LOG.debug("IovisorModuleInstance UPDATED {}", iovisorModuleInstance);
            //TODO process updated event

        }
    }

    /**
     * Process REMOVED data.
     *
     * @param changes
     *            Changes data
     */
    private void removed(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (InstanceIdentifier<?> deletedEndpointPath : changes.getRemovedPaths()) {
            IovisorModuleInstance iovisorModuleInstance = fromMd(deletedEndpointPath, (IovisorModuleInstance) changes.getOriginalData().get(deletedEndpointPath));
            LOG.debug("IovisorModuleInstance REMOVED {}", iovisorModuleInstance);
            // TODO process removed event
        }
    }

    /**
     * Get the object from MD-SAL based on the instance identifier.
     *
     * @param iid
     *            {@link InstanceIdentifier} of the related event
     * @param iovisorModuleInstance
     *            {@link IovisorModuleInstance} from the related event
     * @return IovisorModuleInstance constructed from the one gathered in the related event
     */
    private IovisorModuleInstance fromMd(InstanceIdentifier<?> iid, IovisorModuleInstance iovisorModuleInstance) {
        IovisorModuleInstanceBuilder result = new IovisorModuleInstanceBuilder();

        final IovisorModuleInstanceKey key = iid.firstKeyOf(IovisorModuleInstance.class);
        if (key != null) {
            result.setKey(key);
        }

        result.setUri(iovisorModuleInstance.getUri());

        return result.build();
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }
}
