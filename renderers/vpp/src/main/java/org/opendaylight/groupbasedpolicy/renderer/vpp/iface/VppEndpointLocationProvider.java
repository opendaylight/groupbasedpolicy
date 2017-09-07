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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.CloseOnFailTransactionChain;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.ProviderName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppEndpointLocationProvider
        implements ClusteredDataTreeChangeListener<AddressEndpoint>, VPPLocationProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointLocationProvider.class);
    public static final ProviderName VPP_ENDPOINT_LOCATION_PROVIDER =
            new ProviderName("VPP endpoint location provider");
    public static final long PROVIDER_PRIORITY = 10L;
    private final SyncedWriter syncedWriter;
    private final Map<VppEndpointKey, VppEndpoint> vppEndpoints = new HashMap<>();
    private final Map<VppEndpointKey,AddressEndpoint> pendingAddrEndpoints = new HashMap<>();
    private ListenerRegistration<VppEndpointLocationProvider> registeredListener;

    public VppEndpointLocationProvider(DataBroker dataProvider) {
        LocationProvider locationProvider = new LocationProviderBuilder().setProvider(VPP_ENDPOINT_LOCATION_PROVIDER)
            .setPriority(PROVIDER_PRIORITY)
            .build();
        syncedWriter = new SyncedWriter(checkNotNull(dataProvider)
            .createTransactionChain(new CloseOnFailTransactionChain(VppEndpointLocationProvider.class.getSimpleName())));
        WriteTransaction wTx = syncedWriter.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER),
                locationProvider, true);
        syncedWriter.submitNow(wTx);
        registeredListener = dataProvider.registerDataTreeChangeListener(
                               checkNotNull(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                        InstanceIdentifier.builder(Endpoints.class)
                                            .child(AddressEndpoints.class)
                                            .child(AddressEndpoint.class)
                                            .build())), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<AddressEndpoint>> change) {
        change.forEach(dtm -> {
            ReadWriteTransaction rwTx = syncedWriter.newReadWriteTransaction();
            DataObjectModification<AddressEndpoint> rootNode = dtm.getRootNode();
            if (rootNode.getDataBefore() != null) {
                locationFromParentWriter.clear(rwTx, rootNode.getDataBefore());
                regularLocationWriter.clear(rwTx, rootNode.getDataBefore());
            }
            if (rootNode.getDataAfter() == null || !canCreateLocation(dtm.getRootNode())) {
                syncedWriter.submitNow(rwTx);
                return;
            }
            if (VppLocationUtils.hasMultihomeParent(rwTx, rootNode.getDataAfter().getParentEndpointChoice())
                    || VppLocationUtils.hasMultipleParents(rootNode.getDataAfter())) {
                locationFromParentWriter.sync(rwTx, rootNode.getDataAfter());
            } else {
                regularLocationWriter.sync(rwTx, rootNode.getDataAfter());
            }
            syncedWriter.submitNow(rwTx);
        });
    }

    @Override
    public ListenableFuture<Void> createLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        vppEndpoints.put(vppEndpoint.getKey(), vppEndpoint);
        ReadWriteTransaction rwTx = syncedWriter.newReadWriteTransaction();
        Optional<AddressEndpoint> cachedAddrEp =
                Optional.fromNullable(pendingAddrEndpoints.get(vppEndpoint.getKey()));
        if (cachedAddrEp.isPresent()) {
            AddressEndpoint after = cachedAddrEp.get();
            if (after != null && (VppLocationUtils.hasMultihomeParent(rwTx, after.getParentEndpointChoice())
                    || VppLocationUtils.hasMultipleParents(after))) {
                locationFromParentWriter.sync(rwTx, after);
            } else {
                regularLocationWriter.sync(rwTx, after);
            }
        }
        syncedWriter.submitNow(rwTx);
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> deleteLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        // onDelete location from DS
        vppEndpoints.remove(vppEndpoint.getKey());
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> replaceLocationForEndpoint(@Nonnull ExternalLocationCase location,
            @Nonnull AddressEndpointWithLocationKey addrEpWithLocKey) {
        InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                        VppLocationUtils.createProviderAddressEndpointLocationKey(addrEpWithLocKey));
        ReadOnlyTransaction rTx = syncedWriter.newReadOnlyTransaction();
        Optional<ProviderAddressEndpointLocation> optLoc =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        syncedWriter.close(rTx);
        ProviderAddressEndpointLocationKey provAddrEpLocKey =
                KeyFactory.providerAddressEndpointLocationKey(addrEpWithLocKey);
        ProviderAddressEndpointLocationBuilder builder =
                new ProviderAddressEndpointLocationBuilder().setKey(provAddrEpLocKey);
        if (optLoc.isPresent() && optLoc.get().getAbsoluteLocation() != null) {
            AbsoluteLocation absoluteLocation = optLoc.get().getAbsoluteLocation();
            builder
                .setAbsoluteLocation(new AbsoluteLocationBuilder(absoluteLocation).setLocationType(location).build());
        } else if (optLoc.isPresent() && optLoc.get().getRelativeLocations() != null) {
            ExternalLocation extLoc = new ExternalLocationBuilder().setExternalNode(location.getExternalNode())
                .setExternalNodeConnector(location.getExternalNodeConnector())
                .setExternalNodeMountPoint(location.getExternalNodeMountPoint())
                .build();
            List<ExternalLocation> externalLocation = optLoc.get().getRelativeLocations().getExternalLocation();
            externalLocation.add(extLoc);
            RelativeLocations relativeLocation = new RelativeLocationsBuilder(optLoc.get().getRelativeLocations())
                .setExternalLocation(externalLocation).build();
            builder.setRelativeLocations(relativeLocation);
        } else {
            LOG.warn("Cannot replace location for endpoint {}", addrEpWithLocKey);
            return Futures.immediateFuture(null);
        }
        ProviderAddressEndpointLocation providerLocation = builder.build();
        WriteTransaction wTx = syncedWriter.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.providerAddressEndpointLocationIid(
                VPP_ENDPOINT_LOCATION_PROVIDER, providerLocation.getKey()), providerLocation);
        LOG.debug("Updating location for {}", builder.build().getKey());
        syncedWriter.submitNow(wTx);
        return Futures.immediateFuture(null);
    }

    @VisibleForTesting
    synchronized boolean canCreateLocation(@Nonnull DataObjectModification<AddressEndpoint> rootNode) {
        VppEndpoint vpp = vppEndpoints.get(VppLocationUtils.vppEndpointKeyFrom(rootNode.getDataAfter().getKey()));
        if (vpp == null) {
            VppEndpointKey key = VppLocationUtils.vppEndpointKeyFrom(rootNode.getDataAfter().getKey());
            LOG.debug("Caching VPP endpoint {}" + key);
            pendingAddrEndpoints.put(key, rootNode.getDataAfter());
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        registeredListener.close();
        WriteTransaction wTx = syncedWriter.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER));
        syncedWriter.submitNow(wTx);
    }

    private class SyncedWriter {

        private final BindingTransactionChain txChain;
        private final ReentrantLock SYNC_LOCK = new ReentrantLock();

        SyncedWriter(BindingTransactionChain txChain) {
            this.txChain = txChain;
        }

        ReadOnlyTransaction newReadOnlyTransaction() {
            SYNC_LOCK.lock();
            return txChain.newReadOnlyTransaction();
        }

        WriteTransaction newWriteOnlyTransaction() {
            SYNC_LOCK.lock();
            return txChain.newWriteOnlyTransaction();
        }

        ReadWriteTransaction newReadWriteTransaction() {
            SYNC_LOCK.lock();
            return txChain.newReadWriteTransaction();
        }

        void close(ReadOnlyTransaction rTx) {
            rTx.close();
            SYNC_LOCK.unlock();
        }

        void submitNow(WriteTransaction wTx) {
            CheckedFuture<Void, TransactionCommitFailedException> submit = wTx.submit();
            SYNC_LOCK.unlock();
            try {
                submit.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to submit transaction {}", e);
            }
            LOG.info("Submit done.");
        }
    }

    private abstract class LocationWriter {

        abstract void clear(ReadWriteTransaction rwTx, AddressEndpoint before);

        abstract void sync(ReadWriteTransaction rwTx, AddressEndpoint after);
    }

    LocationWriter regularLocationWriter = new LocationWriter() {

        @Override
        void sync(ReadWriteTransaction rwTx, AddressEndpoint after) {
            if (after == null) {
                return;
            }
            if (EndpointUtils.isExternalEndpoint(rwTx, after)) {
                ProviderAddressEndpointLocation location = VppLocationUtils.createRelativeAddressEndpointLocation(
                        after.getKey(), VppNodeManager.resolvePublicInterfaces(rwTx));
                InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.providerAddressEndpointLocationIid(
                        VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER, location.getKey());
                rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
            } else {
                VppEndpoint vpp = vppEndpoints.get(VppLocationUtils.vppEndpointKeyFrom(after.getKey()));
                ProviderAddressEndpointLocation location = VppLocationUtils.createAbsoluteLocationFromVppEndpoint(vpp);
                InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.providerAddressEndpointLocationIid(
                        VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER, location.getKey());
                rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
            }
        }

        @Override
        void clear(ReadWriteTransaction rwTx, AddressEndpoint before) {
            if (before != null) {
                InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                        IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                VppLocationUtils.createProviderAddressEndpointLocationKey(before.getKey()));
                DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
            }
        }
    };

    LocationWriter locationFromParentWriter = new LocationWriter() {

        @Override
        void clear(ReadWriteTransaction rwTx, AddressEndpoint before) {
            if (before != null) {
                InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                        IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                VppLocationUtils.createProviderAddressEndpointLocationKey(before.getKey()));
                DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
                for (ParentEndpoint l3Endpoint : EndpointUtils.getParentEndpoints(before.getParentEndpointChoice())) {
                    iid = IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                            VppLocationUtils.createProviderAddressEndpointLocationKey(l3Endpoint.getKey()));
                    DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
                }
            }
        }

        @Override
        void sync(ReadWriteTransaction rwTx, AddressEndpoint after) {
            if (after == null) {
                return;
            }
            for (ParentEndpoint l3Endpoint : EndpointUtils.getParentEndpoints(after.getParentEndpointChoice())) {
                InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                        IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                VppLocationUtils.createProviderAddressEndpointLocationKey(l3Endpoint.getKey()));
                List<VppEndpoint> l2childs = getL2Childs(rwTx, l3Endpoint);
                if (!l2childs.isEmpty() && l2childs.size() > 1) {
                    // multihome interface
                    ProviderAddressEndpointLocation location = VppLocationUtils.createRelativeAddressEndpointLocation(
                            VppLocationUtils.createProviderAddressEndpointLocationKey(l3Endpoint.getKey()), l2childs);
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
                } else {
                    VppEndpoint vppEndpoint = vppEndpoints.get(VppLocationUtils.vppEndpointKeyFrom(after.getKey()));
                    ProviderAddressEndpointLocation location =
                            VppLocationUtils.createAbsoluteLocationFromVppEndpoint(new VppEndpointBuilder(vppEndpoint)
                                .setKey(VppLocationUtils.vppEndpointKeyFrom(l3Endpoint.getKey())).build());
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
                }
            }
        }

        private List<VppEndpoint> getL2Childs(ReadTransaction rTx, ParentEndpoint parentEndpoint) {
            AddressEndpointKey addrEpKey =
                    new AddressEndpointKey(AddressEndpointUtils.fromParentEndpointKey(parentEndpoint.getKey()));
            Optional<AddressEndpoint> optParent = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.addressEndpointIid(addrEpKey), rTx);
            if (!optParent.isPresent() || optParent.get().getChildEndpoint() == null) {
                return Collections.emptyList();
            }
            return optParent.get()
                .getChildEndpoint()
                .stream()
                .filter(l2Ep -> vppEndpoints.get(VppLocationUtils.vppEndpointKeyFrom(l2Ep.getKey())) != null)
                .map(l2Ep -> vppEndpoints.get(VppLocationUtils.vppEndpointKeyFrom(l2Ep.getKey())))
                .collect(Collectors.toList());
        }
    };
}
