/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NetconfTransactionCreator;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public ListenableFuture<Boolean> syncPolicy(final Configuration dataAfter, final Configuration dataBefore,
                                                final long version) {
        final ListenableFuture<Boolean> result;
        if (dataBefore == null && dataAfter != null) {
            result = syncPolicy(dataAfter, Create);
        } else if (dataBefore != null && dataAfter == null) {
            result = syncPolicy(dataBefore, Delete);
        } else {
            syncPolicy(dataBefore, Delete);
            syncPolicy(dataAfter, Create);
            result = Futures.immediateFuture(false);
        }

        reportVersion(version);

        // chain version update (TODO: status)
        return Futures.transform(result, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(final Boolean input) throws Exception {
                if (input != null && input) {
                    return Futures.transform(reportVersion(version), new Function<Void, Boolean>() {
                        @Nullable
                        @Override
                        public Boolean apply(@Nullable final Void input) {
                            return Boolean.TRUE;
                        }
                    });
                } else {
                    return Futures.immediateFuture(input);
                }
            }
        });
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
                //TODO: dump all resolvedRule-rule-peerEP-EP combinantions to status
                continue;
            }
            final List<AddressEndpointWithLocation> endpointsWithLocation = dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation();
            final InstanceIdentifier mountpointIid = PolicyManagerUtil.getAbsoluteLocationMountpoint(rendererEndpoint, endpointsWithLocation);
            final DataBroker mountpoint = nodeManager.getNodeMountPoint(mountpointIid);
            if (mountpoint == null) {
                LOG.debug("no data-broker for mount-point [{}] available", mountpointIid);
                //TODO: dump all resolvedRule-rule-peerEP-EP combinantions to status
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
                    //TODO: dump all resolvedRule-rule-peerEP-EP combinantions to status
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
                    //TODO: dump particular resolvedRule-rule-peerEP-EP combinantions to status
                    continue;
                }
                PolicyManagerUtil.syncPolicyEntities(sourceSgt, destinationSgt, policyWriter, dataAfter, peerEndpoint,
                        dataBroker, action);
            }
        }

        //TODO: return real (cumulated) future
        final List<CheckedFuture<Boolean, TransactionCommitFailedException>> allFutureResults = new ArrayList<>();
        if (action.equals(Create)) {
            policyWriterPerDeviceCache.values().forEach(pw -> allFutureResults.add(pw.commitToDatastore()));
        } else if (action.equals(Delete)) {
            policyWriterPerDeviceCache.values().forEach(pw -> allFutureResults.add(pw.removeFromDatastore()));
        } else {
            LOG.info("unsupported policy manage action: {}", action);
        }
        final ListenableFuture<List<Boolean>> cumulativeResult = Futures.allAsList(allFutureResults);

        return Futures.transform(cumulativeResult, new Function<List<Boolean>, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable final List<Boolean> input) {
                LOG.trace("considering all submits as successful - otherwise there will be exception");
                return Boolean.TRUE;
            }
        });
    }

    private CheckedFuture<Void, TransactionCommitFailedException> reportVersion(long version) {
        final Optional<ReadWriteTransaction> optionalReadWriteTransaction =
                NetconfTransactionCreator.netconfReadWriteTransaction(dataBroker);
        if (!optionalReadWriteTransaction.isPresent()) {
            LOG.warn("Failed to create transaction, mountpoint: {}", dataBroker);
            return Futures.immediateCheckedFuture(null);
        }
        final ReadWriteTransaction readWriteTransaction = optionalReadWriteTransaction.get();
        final InstanceIdentifier<RendererPolicy> iid = InstanceIdentifier.create(Renderers.class)
                .child(Renderer.class, new RendererKey(NodeManager.iosXeRenderer))
                .child(RendererPolicy.class);
        readWriteTransaction.merge(LogicalDatastoreType.OPERATIONAL, iid,
                new RendererPolicyBuilder().setVersion(version).build());
        return readWriteTransaction.submit();
    }

    @Override
    public void close() {
        //NOOP
    }

    public enum DsAction {Create, Delete}

    public enum ActionCase {ALLOW, CHAIN}
}
