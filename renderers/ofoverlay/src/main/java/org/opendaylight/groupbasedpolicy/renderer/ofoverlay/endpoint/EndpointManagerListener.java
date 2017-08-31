/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class EndpointManagerListener implements AutoCloseable {

    private final List<ListenerRegistration<?>> listenerRegistrations = new ArrayList<>();
    private final EndpointManager endpointManager;

    public EndpointManagerListener(DataBroker dataProvider, EndpointManager endpointManager) {
        this.endpointManager = checkNotNull(endpointManager);
        checkNotNull(dataProvider);

        listenerRegistrations.add(dataProvider.registerDataTreeChangeListener(new DataTreeIdentifier<>(
            LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(Endpoint.class)),
            changes -> onEndpointChanged(changes)));

        listenerRegistrations.add(dataProvider.registerDataTreeChangeListener(new DataTreeIdentifier<>(
            LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(EndpointL3.class)),
            changes -> onEndpointL3Changed(changes)));
    }

    private void onEndpointChanged(Collection<DataTreeModification<Endpoint>> changes) {
        for (DataTreeModification<Endpoint> change: changes) {
            DataObjectModification<Endpoint> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    endpointManager.processEndpoint(rootNode.getDataBefore(), rootNode.getDataAfter());
                    break;
                case DELETE:
                    endpointManager.processEndpoint(rootNode.getDataBefore(), null);
                    break;
                default:
                    break;
            }
        }
    }

    private void onEndpointL3Changed(Collection<DataTreeModification<EndpointL3>> changes) {
        for (DataTreeModification<EndpointL3> change: changes) {
            DataObjectModification<EndpointL3> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    endpointManager.processL3Endpoint(rootNode.getDataBefore(), rootNode.getDataAfter());
                    break;
                case DELETE:
                    endpointManager.processL3Endpoint(rootNode.getDataBefore(), null);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void close() {
        for (ListenerRegistration<?> reg: listenerRegistrations) {
            reg.close();
        }
    }
}
