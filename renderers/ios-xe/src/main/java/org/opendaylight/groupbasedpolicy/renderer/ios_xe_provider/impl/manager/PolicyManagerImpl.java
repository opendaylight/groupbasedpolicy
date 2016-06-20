/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Create;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Delete;

public class PolicyManagerImpl implements PolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerImpl.class);
    private static final String policyMapName = "service-chains";
    private final DataBroker dataBroker;
    private final NodeManager nodeManager;

    public PolicyManagerImpl(final DataBroker dataBroker,
                             final NodeManager nodeManager) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
    }

    @Override
    public ListenableFuture<Boolean> syncPolicy(final Configuration dataAfter, final Configuration dataBefore) {
        if (dataBefore == null && dataAfter != null) {
            return createPolicy(dataAfter);
        }
        if (dataBefore != null && dataAfter != null) {
            return updatePolicy(dataAfter, dataBefore);
        }
        if (dataBefore != null) {
            return deletePolicy(dataBefore);
        }
        return Futures.immediateFuture(false);
    }

    private ListenableFuture<Boolean> syncPolicy(final Configuration dataAfter, DsAction action) {
        if (dataAfter.getRendererEndpoints() == null
                || dataAfter.getRendererEndpoints().getRendererEndpoint() == null) {
            LOG.debug("no configuration obtained - skipping");
            return Futures.immediateFuture(true);
        }
        final Map<DataBroker, PolicyWriter> policyWriterPerDeviceCache = new HashMap<>();
        for (RendererEndpoint rendererEndpoint : dataAfter.getRendererEndpoints().getRendererEndpoint()) {
            if (dataAfter.getEndpoints() == null || dataAfter.getEndpoints().getAddressEndpointWithLocation() == null) {
                LOG.debug("renderer-endpoint: missing address-endpoint-with-location");
                continue;
            }
            final List<AddressEndpointWithLocation> endpointsWithLocation = dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation();
            final InstanceIdentifier mountpointIid = PolicyManagerUtil.getAbsoluteLocationMountpoint(rendererEndpoint, endpointsWithLocation);
            final DataBroker mountpoint = nodeManager.getNodeMountPoint(mountpointIid);
            if (mountpoint == null) {
                LOG.debug("no data-broker for mount-point [{}] available", mountpointIid);
                continue;
            }
            // Find policy writer
            PolicyWriter policyWriter = policyWriterPerDeviceCache.get(mountpoint);
            if (policyWriter == null) {
                // Initialize new policy writer
                final String interfaceName = PolicyManagerUtil.getInterfaceNameForPolicyMap(rendererEndpoint, endpointsWithLocation);
                final NodeId nodeId = nodeManager.getNodeIdByMountpointIid(mountpointIid);
                final String managementIpAddress = nodeManager.getNodeManagementIpByMountPointIid(mountpointIid);
                if (interfaceName == null || managementIpAddress == null) {
                    LOG.debug("can not create policyWriter: interface={}, managementIpAddress={}",
                            interfaceName, managementIpAddress);
                    continue;
                }
                policyWriter = new PolicyWriter(mountpoint, interfaceName, managementIpAddress, policyMapName, nodeId);
                policyWriterPerDeviceCache.put(mountpoint, policyWriter);
            }

            final Sgt sourceSgt = PolicyManagerUtil.findSgtTag(rendererEndpoint, dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation());
            // Peer Endpoint
            for (PeerEndpointWithPolicy peerEndpoint : rendererEndpoint.getPeerEndpointWithPolicy()) {
                final Sgt destinationSgt = PolicyManagerUtil.findSgtTag(peerEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation());
                if (sourceSgt == null || destinationSgt == null) {
                    LOG.debug("endpoint-policy: missing sgt value(sourceSgt={}, destinationSgt={})",
                            sourceSgt, destinationSgt);
                    continue;
                }
                PolicyManagerUtil.syncPolicyEntities(sourceSgt, destinationSgt, policyWriter, dataAfter, peerEndpoint);
            }
        }
        if (action.equals(Create)) {
            policyWriterPerDeviceCache.values().forEach(PolicyWriter::commitToDatastore);
            return Futures.immediateFuture(true);
        } else if (action.equals(Delete)) {
            policyWriterPerDeviceCache.values().forEach(PolicyWriter::removeFromDatastore);
            return Futures.immediateFuture(true);
        }
        return Futures.immediateFuture(false);
    }

    private ListenableFuture<Boolean> createPolicy(Configuration data) {
        return syncPolicy(data, Create);
    }

    private ListenableFuture<Boolean> deletePolicy(Configuration data) {
        return syncPolicy(data, Delete);
    }

    private ListenableFuture<Boolean> updatePolicy(Configuration dataAfter, Configuration dataBefore) {
        // TODO implement
        return null;
    }

    @Override
    public void close() {
        //NOOP
    }

    enum DsAction {Create, Delete}

    public enum ActionCase {ALLOW, CHAIN}
}
