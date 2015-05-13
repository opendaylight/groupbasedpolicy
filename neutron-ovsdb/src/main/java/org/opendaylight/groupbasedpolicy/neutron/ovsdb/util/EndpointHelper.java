/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.DataStore.readFromDs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class EndpointHelper {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointHelper.class);

    /**
     * Look up the {@link Endpoint} from the Endpoint Registry.
     *
     * @param epKey The {@link EndpointKey} to look up
     * @param dataBroker The {@link DataBroker} to use for the transaction
     * @return The corresponding {@link Endpoint}, null if not found
     */
    public static Endpoint lookupEndpoint(EndpointKey epKey, DataBroker dataBroker) {
        InstanceIdentifier<Endpoint> iid = InstanceIdentifier
                .builder(Endpoints.class)
                .child(Endpoint.class, epKey).build();

        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        Optional<Endpoint> optionalEp = readFromDs(LogicalDatastoreType.OPERATIONAL, iid, transaction );
        if (optionalEp.isPresent()) {
            return optionalEp.get();
        }

        return null;
    }

    /**
     * Update a {@link Endpoint} in the Endpoint Repository with the
     * location information and port name in the OfOverlay augmentation.
     * It looks up the {@link EndpointKey} in a data store maintained by the
     * neutron-mapper, and uses that to look up the {@link Endpoint}, and
     * then uses RPCs to delete then add the updated {@link Endpoint}
     *
     * @param epKey The {@link EndpointKey}
     * @param nodeIdString
     * @param ovsdbBridge
     */
    public static void updateEndpointWithLocation(Endpoint endpoint, String nodeIdString,
            String nodeConnectorIdString, String portName, EndpointService endpointService) {
        EndpointBuilder epBuilder = new EndpointBuilder(endpoint);
        UnregisterEndpointInput unregisterEpRpcInput = createUnregisterEndpointInput(endpoint);
        RegisterEndpointInput registerEpRpcInput =
            createRegisterEndpointInput(epBuilder.build(),
                                        portName,
                                        nodeIdString,
                                        nodeConnectorIdString);
        try {
            RpcResult<Void> rpcResult = endpointService.unregisterEndpoint(unregisterEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC unregisterEndpoint failed. Input of RPC: {}", unregisterEpRpcInput);
                return;
            }
            rpcResult = endpointService.registerEndpoint(registerEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - RPC registerEndpoint failed. Input of RPC: {}", registerEpRpcInput);
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("addPort - RPC invocation failed.", e);
            return;
        }
    }

    /**
     * Create the {@link UnregisterEndpointInput} state needed to remove
     * the existing {@link Endpoint} from the Endpoint Registry.
     *
     * @param ep The {@link Endpoint} to remove from the registry
     * @return The {@link UnregisterEndpointInput}, null if failure
     */
    public static UnregisterEndpointInput createUnregisterEndpointInput(Endpoint ep) {
        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        L2 l2Ep = new L2Builder().setL2Context(ep.getL2Context()).setMacAddress(ep.getMacAddress()).build();
        inputBuilder.setL2(ImmutableList.of(l2Ep));
        // TODO Li msunal this has to be rewrite when OFOverlay renderer will support l3-endpoints.
        // Endpoint probably will not have l3-addresses anymore, because L2 and L3 endpoints should
        // be registered separately.
        if (ep.getL3Address() != null && !ep.getL3Address().isEmpty()) {
            List<L3> l3Eps = new ArrayList<>();
            for (L3Address ip : ep.getL3Address()) {
                l3Eps.add(new L3Builder().setL3Context(ip.getL3Context()).setIpAddress(ip.getIpAddress()).build());
            }
            inputBuilder.setL3(l3Eps);
        }
        return inputBuilder.build();
    }

    /**
     * Create the updated {@link RegisterEndpointInput}, using the existing
     * {@link Endpoint} and the new OfOverlay augmentation information (i.e.
     * port name, NodeId, and NodeConnectorId).
     *
     * @param ep The existing {@link Endpoint} state
     * @param portName The new port name in the OfOverlay augmentation
     * @param nodeIdString The new NodeId in the OfOverlay augmentation
     * @param nodeConnectorIdString The new NodeConnectorId in the OfOverlay augmentation
     * @return The new {@link RegisterEndpointInput}, null if failure
     */
    public static RegisterEndpointInput createRegisterEndpointInput(Endpoint ep,
            String portName, String nodeIdString, String nodeConnectorIdString) {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId invNodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeIdString);
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId ncId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId(nodeConnectorIdString);

        RegisterEndpointInputBuilder inputBuilder = new RegisterEndpointInputBuilder().setL2Context(
                ep.getL2Context())
            .setMacAddress(new MacAddress(ep.getMacAddress()))
            .setTenant(new TenantId(ep.getTenant()))
            .setEndpointGroups(ep.getEndpointGroups())
            .addAugmentation(OfOverlayContextInput.class,
                    new OfOverlayContextInputBuilder()
                            .setPortName(new Name(portName))
                            .setNodeId(invNodeId)
                            .setNodeConnectorId(ncId)
                            .build())
            .setTimestamp(System.currentTimeMillis());

        // TODO Li msunal this getting of just first IP has to be rewrite when OFOverlay renderer
        // will support l3-endpoints. Then we will register L2 and L3 endpoints separately.
        if (ep.getNetworkContainment() != null) {
            inputBuilder.setNetworkContainment(new SubnetId(ep.getNetworkContainment()));
            L3Address l3Address = new L3AddressBuilder()
                    .setIpAddress(ep.getL3Address().get(0).getIpAddress())
                    .setL3Context(ep.getL3Address().get(0).getL3Context())
                    .build();
            inputBuilder.setL3Address(ImmutableList.of(l3Address));
        }
        return inputBuilder.build();
    }

}
