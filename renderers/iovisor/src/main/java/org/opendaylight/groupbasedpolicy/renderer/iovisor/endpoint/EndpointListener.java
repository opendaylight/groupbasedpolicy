/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorModuleUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class EndpointListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointListener.class);

    private final ListenerRegistration<DataChangeListener> registerListener;

    private DataBroker dataBroker;

    public EndpointListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.registerListener = dataBroker.registerDataChangeListener(
                                                LogicalDatastoreType.OPERATIONAL,
                                                IidFactory.endpointsIidWildcard().child(Endpoint.class),
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
            Endpoint endpoint = fromMd(newEndpoint.getKey(), (Endpoint) newEndpoint.getValue());
            LOG.debug("Endpoint CREATED {}", endpoint);

            // Validate the IOVisorModuleInstance
            IovisorModuleAugmentation iovisorModuleAugmentation = endpoint.getAugmentation(IovisorModuleAugmentation.class);
            Preconditions.checkNotNull(iovisorModuleAugmentation.getUri(), "At this point, the Endpoint should be provided with a IovisorModuleInstance");
            if (IovisorModuleUtils.validateIovisorModuleInstance(dataBroker, iovisorModuleAugmentation.getUri())) {
                LOG.debug("This Endpoint {} provides a valid IovisorModuleInstance {}", endpoint, iovisorModuleAugmentation.getUri());
                // TODO process validated endpoint
            }
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
            Endpoint endpoint = fromMd(updatedEndpoint.getKey(), (Endpoint) updatedEndpoint.getValue());
            LOG.debug("Endpoint UPDATED {}", endpoint);
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
            Endpoint endpoint = fromMd(deletedEndpointPath, (Endpoint) changes.getOriginalData().get(deletedEndpointPath));
            LOG.debug("Endpoint REMOVED {}", endpoint);
            // TODO process removed event
        }
    }

    /**
     * Get the object from MD-SAL based on the instance identifier.
     *
     * @param iid
     *            {@link InstanceIdentifier} of the related event
     * @param endpoint
     *            Endpoint from the related event
     * @return Endpoint constructed from the one gathered in the related event
     */
    private Endpoint fromMd(InstanceIdentifier<?> iid, Endpoint endpoint) {
        EndpointBuilder result = new EndpointBuilder();

        final EndpointKey endpointKey = iid.firstKeyOf(Endpoint.class);
        if (endpointKey != null) {
            result.setKey(endpointKey);
        }

        result.setCondition(endpoint.getCondition());
        result.setEndpointGroup(endpoint.getEndpointGroup());
        result.setEndpointGroups(endpoint.getEndpointGroups());
        result.setL2Context(endpoint.getL2Context());
        result.setL3Address(endpoint.getL3Address());
        result.setMacAddress(endpoint.getMacAddress());
        result.setNetworkContainment(endpoint.getNetworkContainment());
        result.setTenant(endpoint.getTenant());
        result.setTimestamp(endpoint.getTimestamp());

        return result.build();
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }
}
