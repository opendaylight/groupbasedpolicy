/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointListener extends DataTreeChangeHandler<EndpointL3> {

    private final EndpointManager endpointManager;

    public EndpointListener(DataBroker dataProvider, EndpointManager endpointManager) {
        super(dataProvider,
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointsIidWildcard()));
        this.endpointManager = endpointManager;
    }

    @Override
    protected void onWrite(DataObjectModification<EndpointL3> rootNode, InstanceIdentifier<EndpointL3> rootIdentifier) {
        onSubtreeModified(rootNode, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<EndpointL3> rootNode,
            InstanceIdentifier<EndpointL3> rootIdentifier) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<EndpointL3> rootNode,
            InstanceIdentifier<EndpointL3> rootIdentifier) {
        endpointManager.processEndpoint(rootNode.getDataAfter());
    }
}
