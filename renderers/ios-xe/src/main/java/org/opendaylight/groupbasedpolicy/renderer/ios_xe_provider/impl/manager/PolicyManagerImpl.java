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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.RendererPolicyUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.StatusUtil;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPToSgtMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.IpSgtDistributionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SendIpSgtBindingToPeerInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyManagerImpl implements PolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerImpl.class);
    public static final RendererName IOS_XE_RENDERER = new RendererName("ios-xe-renderer");
    private static final String BASE_POLICY_MAP_NAME = "service-chains-";
    private final DataBroker dataBroker;
    private final NodeManager nodeManager;
    private final EPToSgtMapper epToSgtMapper;
    private final IpSgtDistributionService ipSgtDistributor;

    public PolicyManagerImpl(final DataBroker dataBroker,
                             final NodeManager nodeManager, final EPToSgtMapper epToSgtMapper,
                             final IpSgtDistributionService ipSgtDistributor) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        this.epToSgtMapper = Preconditions.checkNotNull(epToSgtMapper);
        this.ipSgtDistributor = Preconditions.checkNotNull(ipSgtDistributor);
    }

    @Override
    @Nonnull
    public ListenableFuture<Boolean> syncPolicy(@Nullable final Configuration dataAfter, @Nullable final Configuration dataBefore,
                                                final long version) {
        ListenableFuture<Optional<Status>> creationResult;
        if (dataBefore == null && dataAfter != null) {
            creationResult = syncEndpoints(dataAfter, Create);
        } else if (dataBefore != null && dataAfter == null) {
            creationResult = syncEndpoints(dataBefore, Delete);
        } else {
            final ListenableFuture<Optional<Status>> deletionResult = syncEndpoints(dataBefore, Delete);
            creationResult = Futures.transformAsync(deletionResult, new AsyncFunction<Optional<Status>, Optional<Status>>() {
                @Nonnull
                @Override
                public ListenableFuture<Optional<Status>> apply(@Nonnull Optional<Status> deletionResult) throws Exception {
                    if (deletionResult.isPresent()) {
                        // Wait till task is done. Result is not used, delete case has no status
                        deletionResult.get();
                    }
                    return syncEndpoints(dataAfter, Create);
                }
            }, MoreExecutors.directExecutor());
        }
        return Futures.transformAsync(creationResult, new AsyncFunction<Optional<Status>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@Nullable final Optional<Status> statusValue) throws Exception {
                Preconditions.checkArgument(statusValue != null, "provided status must not be null");
                return Futures.transform(reportPolicy(version, statusValue), new Function<Void, Boolean>() {
                    @Override
                    public Boolean apply(@Nullable final Void input) {
                        return Boolean.TRUE;
                    }
                }, MoreExecutors.directExecutor());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Resolve policy for all endpoint pairs
     *
     * @param dataAfter - data used while processing
     * @param action    - specifies whether data are intended for creating or removing of configuration
     * @return status of policy resolution
     */
    @Nonnull
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
            final Optional<String> optionalManagementIpAddress = nodeManager.getNodeManagementIpByMountPointIid(mountpointIid);
            if (! optionalManagementIpAddress.isPresent()) {
                final String info = String.format("Can not create policyWriter, managementIpAddress for mountpoint %s is null",
                        mountpointIid);
                context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
                continue;
            }
            final String managementIpAddress = optionalManagementIpAddress.get();
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

            // TODO: pull timeout for async ops from config
            final long TIMEOUT = 10;
            final TimeUnit UNIT = TimeUnit.SECONDS;

            final SendIpSgtBindingToPeerInputBuilder ipSgtBindings = new SendIpSgtBindingToPeerInputBuilder();
            ipSgtBindings.setBinding(new ArrayList<>());

            final Sgt sourceSgt = PolicyManagerUtil.findSgtTag(epToSgtMapper, rendererEndpoint, dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation(), TIMEOUT, UNIT);
            final AddressEndpointWithLocation sourceEPAddressWithLocation = RendererPolicyUtil.lookupEndpoint(
                    rendererEndpoint, dataAfter.getEndpoints().getAddressEndpointWithLocation());

            PolicyManagerUtil.createIpSgtBindingItem(sourceSgt, sourceEPAddressWithLocation).ifPresent(ipSgtBindings.getBinding()::add);

            // Peer Endpoint
            for (PeerEndpoint peerEndpoint : rendererEndpoint.getPeerEndpoint()) {
                final Sgt destinationSgt = PolicyManagerUtil.findSgtTag(epToSgtMapper, peerEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation(), TIMEOUT, UNIT);
                final AddressEndpointWithLocation destinationEPAddressWithLocation = RendererPolicyUtil.lookupEndpoint(
                        peerEndpoint, dataAfter.getEndpoints().getAddressEndpointWithLocation());
                PolicyManagerUtil.createIpSgtBindingItem(destinationSgt, destinationEPAddressWithLocation)
                        .ifPresent(ipSgtBindings.getBinding()::add);

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

            ipSgtDistributor.sendIpSgtBindingToPeer(ipSgtBindings.build());
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
        }, MoreExecutors.directExecutor());
    }

    private CheckedFuture<Void, TransactionCommitFailedException> reportPolicy(final long version,
                                                                               @Nonnull final Optional<Status> statusValue) {
        if (statusValue.isPresent()) {
            LOG.warn("IOS-XE renderer: operation not successfully completed, check unconfigured policy in operational/renderer:renderers");
        }
        final ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        final InstanceIdentifier<RendererPolicy> iid = InstanceIdentifier.create(Renderers.class)
                .child(Renderer.class, new RendererKey(IOS_XE_RENDERER))
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
