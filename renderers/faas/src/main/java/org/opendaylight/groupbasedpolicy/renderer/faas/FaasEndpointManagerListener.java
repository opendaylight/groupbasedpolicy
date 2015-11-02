/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.faas;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaasEndpointManagerListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FaasEndpointManagerListener.class);

    private final ListenerRegistration<DataChangeListener> registerListener;

    public FaasEndpointManagerListener(DataBroker dataProvider) {
        this.registerListener = checkNotNull(dataProvider).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointsIidWildcard(), this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // Create
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint) {
                LOG.debug("Created Endpoint {}", (Endpoint) dao);
            } else if (dao instanceof EndpointL3) {
                LOG.debug("Created EndpointL3 {}", (EndpointL3) dao);
            } else if (dao instanceof EndpointL3Prefix) {
                LOG.debug("Created EndpointL3Prefix {}", (EndpointL3Prefix) dao);
            }
        }
        // Update
        Map<InstanceIdentifier<?>, DataObject> dao = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dao.entrySet()) {
            if (entry.getValue() instanceof Endpoint) {
                LOG.debug("Updated Endpoint {}", (Endpoint) dao);
            } else if (entry.getValue() instanceof EndpointL3) {
                LOG.debug("Updated EndpointL3 {}", (EndpointL3) dao);
            } else if (entry.getValue() instanceof EndpointL3Prefix) {
                LOG.debug("Updated EndpointL3Prefix {}", (EndpointL3Prefix) dao);
            }
        }
        // Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Endpoint) {
                LOG.debug("Removed Endpoint {}", (Endpoint) old);
            } else if (old instanceof EndpointL3) {
                LOG.debug("Removed EndpointL3 {}", (EndpointL3) old);
            } else if (old instanceof EndpointL3Prefix) {
                LOG.debug("Removed EndpointL3Prefix {}", (EndpointL3Prefix) old);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }
}
