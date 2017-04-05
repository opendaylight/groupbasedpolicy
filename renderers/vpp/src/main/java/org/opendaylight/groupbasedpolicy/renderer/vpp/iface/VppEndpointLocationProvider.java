/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.CloseOnFailTransactionChain;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointKey;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppEndpointLocationProvider extends DataTreeChangeHandler<AddressEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointLocationProvider.class);
    public static final ProviderName VPP_ENDPOINT_LOCATION_PROVIDER =
            new ProviderName("VPP endpoint location provider");
    public static final long PROVIDER_PRIORITY = 10L;
    private final BindingTransactionChain txChain;
    private final Map<VppEndpointKey, VppEndpoint> vppEndpoints = new HashMap<>();
    private final Map<VppEndpointKey, DataObjectModification<AddressEndpoint>> cachedVppEndpoints = new HashMap<>();

    public VppEndpointLocationProvider(DataBroker dataProvider) {
        super(dataProvider);
        LocationProvider locationProvider = new LocationProviderBuilder().setProvider(VPP_ENDPOINT_LOCATION_PROVIDER)
            .setPriority(PROVIDER_PRIORITY)
            .build();
        txChain = checkNotNull(dataProvider)
            .createTransactionChain(new CloseOnFailTransactionChain(VppEndpointLocationProvider.class.getSimpleName()));
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER),
                locationProvider, true);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("{} was created", VPP_ENDPOINT_LOCATION_PROVIDER.getValue());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("{} was NOT created", VPP_ENDPOINT_LOCATION_PROVIDER.getValue());
            }
        });
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Endpoints.class).child(AddressEndpoints.class).child(AddressEndpoint.class).build()));
    }

    @Override
    protected void onWrite(DataObjectModification<AddressEndpoint> rootNode,
            InstanceIdentifier<AddressEndpoint> rootIdentifier) {
        LOG.debug("onWrite triggered by {}", rootNode.getDataAfter());
        try {
            if (EndpointUtils.isExternalEndpoint(dataProvider, rootNode.getDataAfter())) {
                writeLocation(createRelativeAddressEndpointLocation(rootNode.getDataAfter().getKey(),
                        VppNodeManager.resolvePublicInterfaces(dataProvider))).get();
            } else {
                createAbsoluteAddressEndpointLocation(null, rootNode).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to write location for endpoint {}. {}", rootNode.getDataAfter().getKey(), e.getMessage());
        }
    }

    @Override
    protected void onDelete(DataObjectModification<AddressEndpoint> rootNode,
            InstanceIdentifier<AddressEndpoint> rootIdentifier) {
        LOG.debug("onDelete triggered by {}", rootNode.getDataBefore());
        try {
            if (EndpointUtils.isExternalEndpoint(dataProvider, rootNode.getDataBefore())) {
                deleteLocation(createProviderAddressEndpointLocationKey(rootNode.getDataBefore().getKey())).get();
            } else {
                createAbsoluteAddressEndpointLocation(null, rootNode).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete location for endpoint {}. {}", rootNode.getDataBefore().getKey(),
                    e.getMessage());
        }
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<AddressEndpoint> rootNode,
            InstanceIdentifier<AddressEndpoint> rootIdentifier) {
        LOG.debug("onSubtreeModified triggered by change: before={} after={}", rootNode.getDataBefore(),
                rootNode.getDataAfter());
        if (rootNode.getDataBefore() != null) {
            onDelete(rootNode, rootIdentifier);
        }
        if (rootNode.getDataAfter() != null) {
            onWrite(rootNode, rootIdentifier);
        }
    }

    public ListenableFuture<Void> createLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        return createAbsoluteAddressEndpointLocation(vppEndpoint, null);
    }

    public ListenableFuture<Void> deleteLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        // removing VPP EP from cache out of since block, it's not needed for the other thread.
        vppEndpoints.remove(vppEndpoint.getKey());
        return deleteLocation(createProviderAddressEndpointLocationKey(vppEndpoint));
    }

    /**
     * There are two inputs from which we need to resolve location - {@link AddressEndpoint} and {@link VppEndpoint}
     * These data are delivered by different threads which meet here.
     */
    @VisibleForTesting
    synchronized ListenableFuture<Void> createAbsoluteAddressEndpointLocation(VppEndpoint vppEndpoint,
            DataObjectModification<AddressEndpoint> rootNode) {
        if (vppEndpoint != null) {
            vppEndpoints.put(vppEndpoint.getKey(), vppEndpoint);
            if (cachedVppEndpoints.get(vppEndpoint.getKey()) != null) {
                return processAddrEp(cachedVppEndpoints.get(vppEndpoint.getKey()));
            }
        } else if (rootNode != null) {
            return processAddrEp(rootNode);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processAddrEp(DataObjectModification<AddressEndpoint> rootNode) {
        if (rootNode != null) {
            AddressEndpointChange aec = new AddressEndpointChange(rootNode, dataProvider);
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED: {
                    VppEndpoint vpp = vppEndpoints.get(vppEndpointKeyFrom(rootNode.getDataAfter().getKey()));
                    if (vpp == null) {
                        VppEndpointKey key = vppEndpointKeyFrom(rootNode.getDataAfter().getKey());
                        cachedVppEndpoints.put(key, rootNode);
                        return Futures.immediateFuture(null);
                    }
                    if (aec.hasMoreParents()) {
                        return aec.syncMultiparents();
                    }
                        return aec.write();
                }
                case DELETE: {
                    if (aec.hasMoreParents()) {
                        return aec.syncMultiparents();
                    } else {
                        return aec.delete();
                    }
                }
            }
        }
        return Futures.immediateFuture(null);
    }

    private ProviderAddressEndpointLocation createAbsoluteLocationFromVppEndpoint(VppEndpoint vppEndpoint) {
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(vppEndpoint.getVppNodeId());
        String restIfacePath = VppPathMapper.interfaceToRestPath(vppEndpoint.getVppInterfaceName());
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(new ExternalLocationCaseBuilder()
                    .setExternalNodeMountPoint(vppNodeIid).setExternalNodeConnector(restIfacePath).build()).build();
        return new ProviderAddressEndpointLocationBuilder()
            .setKey(createProviderAddressEndpointLocationKey(vppEndpoint))
            .setAbsoluteLocation(absoluteLocation)
            .build();
    }

    public ProviderAddressEndpointLocation createRelativeAddressEndpointLocation(@Nonnull AddressEndpointKey addrEp,
            @Nonnull Map<NodeId, String> publicIntfNamesByNodes) {
        RelativeLocations relLocations =
                new RelativeLocationsBuilder()
                    .setExternalLocation(publicIntfNamesByNodes.keySet()
                        .stream()
                        .filter(nodeId -> publicIntfNamesByNodes.get(nodeId) != null)
                        .map(nodeId -> new ExternalLocationBuilder()
                            .setExternalNodeMountPoint(VppIidFactory.getNetconfNodeIid(nodeId))
                            .setExternalNodeConnector(
                                    VppPathMapper.interfaceToRestPath(publicIntfNamesByNodes.get(nodeId)))
                            .build())
                        .collect(Collectors.toList()))
                    .build();
        return new ProviderAddressEndpointLocationBuilder().setKey(createProviderAddressEndpointLocationKey(addrEp))
            .setRelativeLocations(relLocations)
            .build();
    }

    public ListenableFuture<Void> writeLocation(ProviderAddressEndpointLocation location) {
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER, location.getKey()),
                location, true);
        return Futures.transform(wTx.submit(), new Function<Void, Void>() {

            @Override
            public Void apply(Void input) {
                LOG.debug("{} provided location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(), location);
                return null;
            }
        });
    }

    public ListenableFuture<Void> deleteLocation(ProviderAddressEndpointLocationKey key) {
        ReadWriteTransaction rwTx = txChain.newReadWriteTransaction();
        LOG.debug("Deleting location for {}", key);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER, key), rwTx);
        return Futures.transform(rwTx.submit(), new Function<Void, Void>() {

            @Override
            public Void apply(Void input) {
                LOG.debug("{} removed location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(), key);
                return null;
            }
        });
    }

    public ListenableFuture<Void> replaceLocationForEndpoint(@Nonnull ExternalLocationCase location, @Nonnull AddressEndpointWithLocationKey addrEpWithLocKey) {
        ProviderAddressEndpointLocationKey provAddrEpLocKey =
                KeyFactory.providerAddressEndpointLocationKey(addrEpWithLocKey);
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(location).build();
        ProviderAddressEndpointLocation providerAddressEndpointLocation = new ProviderAddressEndpointLocationBuilder()
            .setKey(provAddrEpLocKey).setAbsoluteLocation(absoluteLocation).build();
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                        providerAddressEndpointLocation.getKey()),
                providerAddressEndpointLocation);
        LOG.debug("Updating location for {}", provAddrEpLocKey);
        return Futures.transform(wTx.submit(), new Function<Void, Void>() {

            @Override
            public Void apply(Void input) {
                LOG.debug("{} replaced location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        providerAddressEndpointLocation);
                return null;
            }
        });
    }

    static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(VppEndpoint vpp) {
        return new ProviderAddressEndpointLocationKey(vpp.getAddress(), vpp.getAddressType(), vpp.getContextId(),
                vpp.getContextType());
    }

    static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(AddressEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    private static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(ParentEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    private VppEndpointKey vppEndpointKeyFrom(AddressEndpointKey key) {
        return new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    private VppEndpointKey vppEndpointKeyFrom(ParentEndpointKey key) {
        return new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    @Override
    public void close() {
        super.closeRegisteredListener();
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER));
        wTx.submit();
    }

    private class AddressEndpointChange {

        private final AddressEndpoint before;
        private final AddressEndpoint after;
        private final DataBroker dataBroker;

        public AddressEndpointChange(DataObjectModification<AddressEndpoint> addrEp, @Nonnull DataBroker dataBroker) {
            this.before = addrEp.getDataBefore();
            this.after = addrEp.getDataAfter();
            this.dataBroker = dataBroker;
        }

        boolean hasMoreParents() {
            return (before != null && EndpointUtils.getParentEndpoints(before.getParentEndpointChoice()).size() > 1)
                    || (after != null && EndpointUtils.getParentEndpoints(after.getParentEndpointChoice()).size() > 1);
        }

        ListenableFuture<Void> syncMultiparents() {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            if (before != null) {
                for (ParentEndpoint pe : EndpointUtils.getParentEndpoints(before.getParentEndpointChoice())) {
                    InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                            IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                    createProviderAddressEndpointLocationKey(pe.getKey()));
                    DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
                }
            }
            if (after != null) {
                for (ParentEndpoint pe : EndpointUtils.getParentEndpoints(after.getParentEndpointChoice())) {
                    VppEndpoint vppEndpoint = vppEndpoints.get(vppEndpointKeyFrom(after.getKey()));
                    InstanceIdentifier<ProviderAddressEndpointLocation> iid =
                            IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                                    createProviderAddressEndpointLocationKey(pe.getKey()));
                    ProviderAddressEndpointLocation location = createAbsoluteLocationFromVppEndpoint(
                            new VppEndpointBuilder(vppEndpoint).setKey(vppEndpointKeyFrom(pe.getKey())).build());
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
                }
            }
            return rwTx.submit();
        }

        ListenableFuture<Void> write() {
            VppEndpoint vpp = vppEndpoints.get(vppEndpointKeyFrom(after.getKey()));
            WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
            ProviderAddressEndpointLocation location =
                    createAbsoluteLocationFromVppEndpoint(vpp);
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.providerAddressEndpointLocationIid(
                    VPP_ENDPOINT_LOCATION_PROVIDER, createProviderAddressEndpointLocationKey(vpp));
            wTx.put(LogicalDatastoreType.CONFIGURATION, iid, location, true);
            return wTx.submit();
        }

        ListenableFuture<Void> delete() {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            InstanceIdentifier<ProviderAddressEndpointLocation> iid = IidFactory.providerAddressEndpointLocationIid(
                    VPP_ENDPOINT_LOCATION_PROVIDER, createProviderAddressEndpointLocationKey(before.getKey()));
            DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, iid, rwTx);
            return rwTx.submit();
        }
    }
}
