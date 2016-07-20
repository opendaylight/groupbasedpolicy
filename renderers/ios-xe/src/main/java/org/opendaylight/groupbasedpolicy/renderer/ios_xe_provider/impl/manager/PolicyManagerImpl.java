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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.StatusUtil;
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

public class PolicyManagerImpl implements PolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerImpl.class);
    private static final String BASE_POLICY_MAP_NAME = "service-chains-";
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
            result = syncEndpoints(dataAfter, Create);
        } else if (dataBefore != null && dataAfter == null) {
            result = syncEndpoints(dataBefore, Delete);
        } else {
            syncEndpoints(dataBefore, Delete);
            syncEndpoints(dataAfter, Create);
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

    /**
     * Resolve policy for all endpoint pairs
     *
     * @param dataAfter - data used while processing
     * @param action    - specifies whether data are intended for creating or removing of configuration
     * @return status of policy resolution
     */
    private ListenableFuture<Optional<Status>> syncEndpoints(final Configuration dataAfter, final DsAction action) {
        if (dataAfter.getRendererEndpoints() == null
                || dataAfter.getRendererEndpoints().getRendererEndpoint() == null) {
            LOG.debug("No configuration obtained - skipping");
            return Futures.immediateFuture(Optional.empty());
        }
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        // Renderer endpoint
        for (RendererEndpoint rendererEndpoint : dataAfter.getRendererEndpoints().getRendererEndpoint()) {
            context.setCurrentRendererEP(rendererEndpoint);

            if (dataAfter.getEndpoints() == null || dataAfter.getEndpoints().getAddressEndpointWithLocation() == null) {
                final String info = "Renderer-endpoint: missing address-endpoint-with-location";
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                continue;
            }
            final List<AddressEndpointWithLocation> endpointsWithLocation = dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation();
            final InstanceIdentifier mountpointIid = PolicyManagerUtil.getMountpointIidFromAbsoluteLocation(rendererEndpoint, endpointsWithLocation);
            final DataBroker mountpoint = nodeManager.getNodeMountPoint(mountpointIid);
            if (mountpoint == null) {
                final String info = String.format("No data-broker for mount-point [%s] available", mountpointIid);
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                continue;
            }
            final String managementIpAddress = nodeManager.getNodeManagementIpByMountPointIid(mountpointIid);
            if (managementIpAddress == null) {
                final String info = String.format("Can not create policyWriter, managementIpAddress for mountpoint %s is null",
                        mountpointIid);
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                continue;
            }
            final String interfaceName = PolicyManagerUtil.getInterfaceNameFromAbsoluteLocation(rendererEndpoint, endpointsWithLocation);
            final NodeId nodeId = nodeManager.getNodeIdByMountpointIid(mountpointIid);
            if (interfaceName == null || nodeId == null) {
                final String info = String.format("Cannot compose policy-map, missing value. Interface: %s, NodeId: %s", interfaceName, nodeId);
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                LOG.warn(info);
                continue;
            }
            final String policyMapName = BASE_POLICY_MAP_NAME.concat(interfaceName);
            final PolicyMapLocation policyMapLocation = new PolicyMapLocation(policyMapName, interfaceName, nodeId,
                    managementIpAddress, mountpoint);
            context.setPolicyMapLocation(policyMapLocation);

            final Sgt sourceSgt = PolicyManagerUtil.findSgtTag(rendererEndpoint, dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation());
            // Peer Endpoint
            for (PeerEndpoint peerEndpoint : rendererEndpoint.getPeerEndpoint()) {
                final Sgt destinationSgt = PolicyManagerUtil.findSgtTag(peerEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation());
                if (sourceSgt == null || destinationSgt == null) {
                    final String info = String.format("Endpoint-policy: missing sgt value(sourceSgt=%s, destinationSgt=%s)",
                            sourceSgt, destinationSgt);
                    context.appendUnconfiguredRendererEP(
                            StatusUtil.assembleNotConfigurableRendererEPForPeer(context, peerEndpoint, info));
                    continue;
                }
                // Resolve policy between endpoints
                if (action.equals(Create)) {
                    LOG.debug("Setting up policy between endpoint {}, sgt: {} and peer {}, sgt: {}", rendererEndpoint,
                            sourceSgt, peerEndpoint, destinationSgt);
                    PolicyManagerUtil.syncEndpointPairCreatePolicy(sourceSgt, destinationSgt, context, dataAfter,
                            peerEndpoint, dataBroker);
                } else {
                    LOG.debug("Removing policy between endpoint {}, sgt: {} and peer {}, sgt: {}", rendererEndpoint,
                            sourceSgt, peerEndpoint, destinationSgt);
                    PolicyManagerUtil.syncEndpointPairRemovePolicy(sourceSgt, destinationSgt, context, dataAfter,
                            peerEndpoint);
                }
            }
        }
        final ListenableFuture<List<Boolean>> cumulativeResult = context.getCumulativeResult();
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

    private CheckedFuture<Void, TransactionCommitFailedException> reportPolicy(final long version,
                                                                               @Nonnull final Optional<Status> statusValue) {
        final ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
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

    enum DsAction {Create, Delete}

    public enum ActionCase {ALLOW, CHAIN}

    /**
     * Wrapper class - contains all necessary information to clearly localize policy-map/interface/node in network
     */
    public static class PolicyMapLocation {

        private final String policyMapName;
        private final String interfaceName;
        private final NodeId nodeId;
        private final String managementIpAddress;
        private final DataBroker mountpoint;

        public PolicyMapLocation(final String policyMapName, final String interfaceName, final NodeId nodeId,
                                 final String managementIpAddress, final DataBroker mountpoint) {
            this.policyMapName = Preconditions.checkNotNull(policyMapName);
            this.interfaceName = Preconditions.checkNotNull(interfaceName);
            this.nodeId = Preconditions.checkNotNull(nodeId);
            this.managementIpAddress = Preconditions.checkNotNull(managementIpAddress);
            this.mountpoint = Preconditions.checkNotNull(mountpoint);
        }

        public String getPolicyMapName() {
            return policyMapName;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public NodeId getNodeId() {
            return nodeId;
        }

        public String getManagementIpAddress() {
            return managementIpAddress;
        }

        public DataBroker getMountpoint() {
            return mountpoint;
        }
    }
}