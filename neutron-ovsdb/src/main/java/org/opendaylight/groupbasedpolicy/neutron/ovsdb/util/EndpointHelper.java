/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;
import static org.opendaylight.groupbasedpolicy.util.IidFactory.endpointIid;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;


public class EndpointHelper {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointHelper.class);

    /**
     * Look up the {@link Endpoint} from the Endpoint Registry.
     *
     * @param epKey The {@link EndpointKey} to look up
     * @param transaction The {@link ReadOnlyTransaction}
     * @return The corresponding {@link Endpoint}, null if not found
     */
    public static Endpoint lookupEndpoint(EndpointKey epKey, ReadOnlyTransaction transaction) {

        Optional<Endpoint> optionalEp = readFromDs(LogicalDatastoreType.OPERATIONAL, endpointIid(epKey.getL2Context(),epKey.getMacAddress()), transaction );
        if (optionalEp.isPresent()) {
            return optionalEp.get();
        }
        return null;
    }

    /**
     * Updates an {@link Endpoint} location based on OVSDB Termination point notification.
     *
     * Note this updates the datastore directly. It does not use the Endpoint RPC, as this has
     * unfortunate side-effects on EndpointL3 augmentations.
     *
     * @param endpoint               the network endpoint
     * @param nodeIdString           the string representation of the inventory NodeId
     * @param nodeConnectorIdString  the string representation of the inventory NodeConnectorId
     * @param rwTx                   a reference to ReadWriteTransaction object
     */
    public static void updateEndpointWithLocation(Endpoint endpoint, String nodeIdString,
        String nodeConnectorIdString, ReadWriteTransaction rwTx) {

        NodeId invNodeId = new NodeId(nodeIdString);
        NodeConnectorId ncId = new NodeConnectorId(nodeConnectorIdString);

        OfOverlayContext ofc = endpoint.getAugmentation(OfOverlayContext.class);
        OfOverlayContextBuilder ofcBuilder = new OfOverlayContextBuilder(ofc).setNodeConnectorId(ncId).setNodeId(invNodeId);
        EndpointBuilder epBuilder = new EndpointBuilder(endpoint);
        epBuilder.addAugmentation(OfOverlayContext.class, ofcBuilder.build());
        Endpoint newEp = epBuilder.build();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.endpointIid(newEp.getL2Context(), newEp.getMacAddress()), newEp);
        DataStoreHelper.submitToDs(rwTx);
    }

    public static void updateEndpointRemoveLocation(Endpoint endpoint, ReadWriteTransaction rwTx) {
        EndpointBuilder epBuilder = new EndpointBuilder(endpoint);
        Endpoint newEp = epBuilder.build();
        epBuilder.removeAugmentation(OfOverlayContext.class);
        rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.endpointIid(newEp.getL2Context(), newEp.getMacAddress()), newEp);
        DataStoreHelper.submitToDs(rwTx);
    }

}
