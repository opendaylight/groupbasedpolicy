/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Create;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Delete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.StatusUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NetconfTransactionCreator;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.StatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.UnconfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
    @Nonnull
    public ListenableFuture<Boolean> syncPolicy(@Nullable final Configuration dataAfter, @Nullable final Configuration dataBefore,
                                                final long version) {
        final ListenableFuture<Optional<Status>> result;
        if (dataBefore == null && dataAfter != null) {
            result = syncPolicy(dataAfter, Create);
        } else if (dataBefore != null && dataAfter == null) {
            result = syncPolicy(dataBefore, Delete);
        } else {
            syncPolicy(dataBefore, Delete);
            syncPolicy(dataAfter, Create);
            result = Futures.immediateFuture(Optional.empty());
        }

        return Futures.transform(result, new AsyncFunction<Optional<Status>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@Nullable final Optional<Status> statusValue) throws Exception {
                Preconditions.checkArgument(statusValue != null, "provided status must not be null");
                return Futures.transform(reportPolicy(version, statusValue), new Function<Void, Boolean>() {
                    @Override
                    public Boolean apply(@Nullable final Void input) {
                        return Boolean.TRUE;
                    }
                });
            }
        });
    }

    private ListenableFuture<Optional<Status>> syncPolicy(final Configuration dataAfter, DsAction action) {
        if (dataAfter.getRendererEndpoints() == null
                || dataAfter.getRendererEndpoints().getRendererEndpoint() == null) {
            LOG.debug("no configuration obtained - skipping");
            return Futures.immediateFuture(Optional.empty());
        }

        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        final Map<DataBroker, PolicyWriter> policyWriterPerDeviceCache = new HashMap<>();
        for (RendererEndpoint rendererEndpoint : dataAfter.getRendererEndpoints().getRendererEndpoint()) {
            // store the endpoint currently being configured
            context.setCurrentRendererEP(rendererEndpoint);

            if (dataAfter.getEndpoints() == null || dataAfter.getEndpoints().getAddressEndpointWithLocation() == null) {
                final String info = "renderer-endpoint: missing address-endpoint-with-location";
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                continue;
            }
            final List<AddressEndpointWithLocation> endpointsWithLocation = dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation();
            final InstanceIdentifier mountpointIid = PolicyManagerUtil.getAbsoluteLocationMountpoint(rendererEndpoint, endpointsWithLocation);
            final DataBroker mountpoint = nodeManager.getNodeMountPoint(mountpointIid);
            if (mountpoint == null) {
                final String info = String.format("no data-broker for mount-point [%s] available", mountpointIid);
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
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
                    final String info = String.format("can not create policyWriter: interface=%s, managementIpAddress=%s",
                            interfaceName, managementIpAddress);
                    context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                    continue;
                }
                policyWriter = new PolicyWriter(mountpoint, interfaceName, managementIpAddress, policyMapName, nodeId);
                policyWriterPerDeviceCache.put(mountpoint, policyWriter);
            }

            // assign policyWriter for current mount-point
            context.setPolicyWriter(policyWriter);

            final Sgt sourceSgt = PolicyManagerUtil.findSgtTag(rendererEndpoint, dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation());
            // Peer Endpoint
            for (PeerEndpoint peerEndpoint : rendererEndpoint.getPeerEndpoint()) {
                final Sgt destinationSgt = PolicyManagerUtil.findSgtTag(peerEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation());
                if (sourceSgt == null || destinationSgt == null) {
                    final String info = String.format("endpoint-policy: missing sgt value(sourceSgt=%s, destinationSgt=%s)",
                            sourceSgt, destinationSgt);
                    context.appendUnconfiguredRendererEP(
                            StatusUtil.assembleNotConfigurableRendererEPForPeer(context, peerEndpoint, info));
                    continue;
                }
                PolicyManagerUtil.syncPolicyEntities(sourceSgt, destinationSgt, context, dataAfter, peerEndpoint,
                        dataBroker, action);
            }
        }

        final List<CheckedFuture<Boolean, TransactionCommitFailedException>> allFutureResults = new ArrayList<>();
        if (action.equals(Create)) {
            policyWriterPerDeviceCache.values().forEach((pw) -> allFutureResults.add(pw.commitToDatastore()));
        } else if (action.equals(Delete)) {
            policyWriterPerDeviceCache.values().forEach((pw) -> allFutureResults.add(pw.removeFromDatastore()));
        } else {
            LOG.info("unsupported policy manage action: {}", action);
        }
        final ListenableFuture<List<Boolean>> cumulativeResult = Futures.allAsList(allFutureResults);

        return Futures.transform(cumulativeResult, new Function<List<Boolean>, Optional<Status>>() {
            @Nullable
            @Override
            public Optional<Status> apply(@Nullable final List<Boolean> input) {
                //TODO: inspect if all booleans are true

                LOG.trace("considering all submits as successful - otherwise there will be exception");
                final Status status = new StatusBuilder()
                        .setUnconfiguredEndpoints(new UnconfiguredEndpointsBuilder()
                                .setUnconfiguredRendererEndpoint(context.getUnconfiguredRendererEPBag())
                                .build())
                        .build();

                return Optional.of(status);
            }
        });
    }

    private CheckedFuture<Void, TransactionCommitFailedException> reportPolicy(long version, @Nonnull final Optional<Status> statusValue) {
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
        final RendererPolicy rendererPolicy = new RendererPolicyBuilder()
                .setVersion(version)
                .setStatus(statusValue.orElse(null))
                .build();
        readWriteTransaction.merge(LogicalDatastoreType.OPERATIONAL, iid, rendererPolicy);
        return readWriteTransaction.submit();
    }

    @Override
    public void close() {
        //NOOP
    }

    public enum DsAction {Create, Delete}

    public enum ActionCase {ALLOW, CHAIN}
}
