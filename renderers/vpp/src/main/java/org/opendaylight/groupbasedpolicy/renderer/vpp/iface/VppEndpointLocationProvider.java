/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.CloseOnFailTransactionChain;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.SyncedChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.ProviderName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppEndpointLocationProvider
        implements ClusteredDataTreeChangeListener<AddressEndpoint>, VPPLocationProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointLocationProvider.class);
    public static final ProviderName VPP_ENDPOINT_LOCATION_PROVIDER =
            new ProviderName("VPP endpoint location provider");
    public static final long PROVIDER_PRIORITY = 10L;
    private final SyncedChain syncedChain;
    private final Map<VppEndpointKey, VppEndpoint> vppEndpoints = new HashMap<>();
    // private final Map<VppEndpointKey, List<AddressEndpoint>> pendingAddrEndpoints = new
    // HashMap<>();
    private final Table<VppEndpointKey, AddressEndpointKey, AddressEndpoint> pendingAddrEndpoints =
            HashBasedTable.create();

    private ListenerRegistration<VppEndpointLocationProvider> registeredListener;
    private final ReentrantLock THREAD_LOCK = new ReentrantLock();

    public VppEndpointLocationProvider(DataBroker dataProvider) {
        LocationProvider locationProvider = new LocationProviderBuilder().setProvider(VPP_ENDPOINT_LOCATION_PROVIDER)
            .setPriority(PROVIDER_PRIORITY)
            .build();
        syncedChain = new SyncedChain(checkNotNull(dataProvider).createTransactionChain(
                new CloseOnFailTransactionChain(VppEndpointLocationProvider.class.getSimpleName())));
        WriteTransaction wTx = syncedChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER),
                locationProvider, true);
        syncedChain.submitNow(wTx);
        registeredListener =
                dataProvider.registerDataTreeChangeListener(
                        checkNotNull(
                                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                        InstanceIdentifier.builder(Endpoints.class)
                                            .child(AddressEndpoints.class)
                                            .child(AddressEndpoint.class)
                                            .build())),
                        this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<AddressEndpoint>> change) {
        try {
            THREAD_LOCK.lock();
            change.forEach(dtm -> {
                ReadWriteTransaction rwTx = syncedChain.newReadWriteTransaction();
                DataObjectModification<AddressEndpoint> rootNode = dtm.getRootNode();
                if (rootNode.getDataBefore() != null) {
                    InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                            IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                    VppLocationUtils.locationProviderKey(rootNode.getDataBefore().getKey()));
                    LOG.debug("Clearing location {}", iid);
                    DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
                }
                if (rootNode.getDataAfter() == null || !canCreateLocation(rootNode.getDataAfter())) {
                    syncedChain.submitNow(rwTx);
                    return;
                }
                LOG.debug("Resolving location for {}", rootNode.getDataAfter().getKey());
                syncEndpointLocation(rwTx, rootNode.getDataAfter());
                syncedChain.submitNow(rwTx);
            });
        } catch (Exception e) {
            LOG.error("Failed to resolve location. {} ", e);
        } finally {
            THREAD_LOCK.unlock();
        }
    }

    void syncEndpointLocation(ReadWriteTransaction rwTx, AddressEndpoint addrEp) {
        LOG.trace("Processing endpoint {}", addrEp.getAddress());
        if (EndpointUtils.isExternalEndpoint(rwTx, addrEp)) {
            externalLocationWriter.sync(rwTx, addrEp);
        } else if (VppLocationUtils.getL2ChildEndpoints(addrEp).size() > 1) {
            relativeLocationWriter.sync(rwTx, addrEp);
        } else if (VppLocationUtils.getL2ChildEndpoints(addrEp).size() == 1) {
            absoluteLocationWriter.sync(rwTx, addrEp);
        }
    }

    @Override
    public ListenableFuture<Void> createLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        try {
            THREAD_LOCK.lock();
            vppEndpoints.put(vppEndpoint.getKey(), vppEndpoint);
            Collection<AddressEndpoint> cachedAddrEp = pendingAddrEndpoints.row(vppEndpoint.getKey()).values();
            if (cachedAddrEp == null || cachedAddrEp.isEmpty()) {
                return Futures.immediateFuture(null);
            }
            ReadWriteTransaction rwTx = syncedChain.newReadWriteTransaction();
            List<AddressEndpoint> result =
                    cachedAddrEp.stream().filter(ep -> canCreateLocation(ep)).collect(Collectors.toList());
            result.forEach(ep -> syncEndpointLocation(rwTx, ep));
            result.forEach(ep -> pendingAddrEndpoints.remove(vppEndpoint.getKey(), ep.getKey()));
            syncedChain.submitNow(rwTx);
        } catch (Exception e) {
            LOG.error("Failed to resolve location for vpp endpoint {}. {}", vppEndpoint.getKey(), e);
        } finally {
            THREAD_LOCK.unlock();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> deleteLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        try {
            THREAD_LOCK.lock();
            vppEndpoints.remove(vppEndpoint.getKey());
        } catch (Exception e) {
            LOG.warn("Failed to delete vpp endpoint {}", vppEndpoint.getKey());
        } finally {
            THREAD_LOCK.unlock();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> replaceLocationForEndpoint(@Nonnull ExternalLocationCase location,
            @Nonnull AddressEndpointWithLocationKey addrEpWithLocKey) {
        LOG.debug("Replacing location for endpoint {}", addrEpWithLocKey.getAddress());
        THREAD_LOCK.lock();
        try {
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.locationProviderIid(
                    VPP_ENDPOINT_LOCATION_PROVIDER, VppLocationUtils.providerLocationKey(addrEpWithLocKey));
            ReadWriteTransaction rwTx = syncedChain.newReadWriteTransaction();
            Optional<ProviderAddressEndpointLocation> optLoc =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
            if (!optLoc.isPresent() || optLoc.get().getAbsoluteLocation() == null) {
                LOG.warn("No absolute location. Cannot modify bridge domain for endpoint {}.", addrEpWithLocKey);
                // TODO failed future
                syncedChain.submitNow(rwTx);
                return Futures.immediateFuture(null);
            }
            AbsoluteLocation absoluteLocation = optLoc.get().getAbsoluteLocation();
            ProviderAddressEndpointLocation providerLocation = new ProviderAddressEndpointLocationBuilder()
                .setKey(iid.firstKeyOf(ProviderAddressEndpointLocation.class))
                .setAbsoluteLocation(new AbsoluteLocationBuilder(absoluteLocation).setLocationType(location).build())
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER, providerLocation.getKey()),
                    providerLocation);
            syncedChain.submitNow(rwTx);
        } catch (Exception e) {
            LOG.error("Failed to replace location for endpoint {}", addrEpWithLocKey);
        } finally {
            THREAD_LOCK.unlock();
        }
        return Futures.immediateFuture(null);
    }

    @VisibleForTesting
    synchronized boolean canCreateLocation(@Nonnull AddressEndpoint addrEp) {
        if (!VppLocationUtils.validateEndpoint(addrEp)) {
            return false;
        }
        return addrEp.getChildEndpoint()
            .stream()
            .filter(child -> vppEndpoints.get(VppLocationUtils.vppEndpointKey(child.getKey())) == null)
            .noneMatch(child -> {
                VppEndpointKey key = VppLocationUtils.vppEndpointKey(child.getKey());
                LOG.debug("Caching VPP endpoint {}", key.getAddress());
                pendingAddrEndpoints.put(key, addrEp.getKey(), addrEp);
                return true;
            });
    }

    @Override
    public void close() {
        registeredListener.close();
        WriteTransaction wTx = syncedChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER));
        syncedChain.submitNow(wTx);
    }

    private abstract class LocationWriter {

        abstract void sync(ReadWriteTransaction rwTx, AddressEndpoint after);
    }

    LocationWriter absoluteLocationWriter = new LocationWriter() {

        @Override
        void sync(ReadWriteTransaction rwTx, AddressEndpoint after) {
            Preconditions.checkNotNull(after);
            Preconditions.checkArgument(after.getChildEndpoint().size() == 1,
                    "Endpoint should have only one child endpoint " + after.getKey());
            ChildEndpoint child = after.getChildEndpoint().get(0);
            VppEndpoint vpp = vppEndpoints.get(VppLocationUtils.vppEndpointKey(child.getKey()));
            ProviderAddressEndpointLocation location =
                    VppLocationUtils.createLocation(after.getKey(), VppLocationUtils.createAbsLocation(vpp));
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory
                .locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER, location.getKey());
            LOG.debug("Processing endpoint {} : Writing absolute location for endpoint.", after.getAddress());
            rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
        }
    };

    LocationWriter externalLocationWriter = new LocationWriter() {

        @Override
        void sync(ReadWriteTransaction rwTx, AddressEndpoint after) {
            Preconditions.checkNotNull(after);
            Preconditions.checkArgument(EndpointUtils.isExternalEndpoint(rwTx, after),
                    "Endpoint " + after + " is not external");
            ProviderAddressEndpointLocation location = VppLocationUtils.createLocation(after.getKey(), VppLocationUtils
                .createRelativeAddressEndpointLocation(after.getKey(), VppNodeManager.resolvePublicInterfaces(rwTx)));
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory
                .locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER, location.getKey());
            LOG.debug("Processing endpoint {} : Writing relative location for external endpoint.", after.getAddress());
            rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
        }
    };

    LocationWriter relativeLocationWriter = new LocationWriter() {

        @Override
        void sync(ReadWriteTransaction rwTx, AddressEndpoint after) {
            Preconditions.checkNotNull(after);
            List<VppEndpoint> vppEps = VppLocationUtils.getL2ChildEndpoints(after)
                .stream()
                .filter(l2Child -> vppEndpoints.get(VppLocationUtils.vppEndpointKey(l2Child.getKey())) != null)
                .map(l2Child -> vppEndpoints.get(VppLocationUtils.vppEndpointKey(l2Child.getKey())))
                .collect(Collectors.toList());
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.locationProviderIid(
                    VPP_ENDPOINT_LOCATION_PROVIDER, VppLocationUtils.locationProviderKey(after.getKey()));
            LOG.trace("Writing relative location for multi home endpoint {}", after.getAddress());
            rwTx.put(LogicalDatastoreType.CONFIGURATION, iid,
                    VppLocationUtils.createLocation(after.getKey(), VppLocationUtils.createRelLocations(vppEps)), true);
        }
    };
}
