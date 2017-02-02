/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.location.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderContainmentEndpointLocationKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationResolver implements ClusteredDataTreeChangeListener<LocationProvider>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LocationResolver.class);
    private Map<AddressEndpointLocationKey, Map<Long, AbsoluteLocation>> realLocations;
    private DataBroker dataBroker;
    private ListenerRegistration<LocationResolver> listenerRegistation;

    public LocationResolver(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.realLocations = new HashMap<>();
        this.listenerRegistation = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(LocationProviders.class).child(LocationProvider.class).build()),
                this);
    }

    @Override
    public synchronized void onDataTreeChanged(Collection<DataTreeModification<LocationProvider>> changes) {
        for (DataTreeModification<LocationProvider> change : changes) {
            WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    processRemovedLocationProviderData(change.getRootNode().getDataBefore(), wtx);
                    LOG.debug("Data from location provider {} has been removed",
                            change.getRootNode().getDataBefore().getProvider().getValue());
                    break;
                }
                case WRITE: {
                    if (change.getRootNode().getDataBefore() != null) {
                        processRemovedLocationProviderData(change.getRootNode().getDataBefore(), wtx);
                    }
                    processCreatedLocationProviderData(change.getRootNode().getDataAfter(), wtx);
                    LOG.debug("Data from location provider {} has been created",
                            change.getRootNode().getDataAfter().getProvider().getValue());
                    break;
                }
                case SUBTREE_MODIFIED: {
                    processRemovedLocationProviderData(change.getRootNode().getDataBefore(), wtx);
                    processCreatedLocationProviderData(change.getRootNode().getDataAfter(), wtx);
                    LOG.debug("Data from location provider {} has been changed",
                            change.getRootNode().getDataAfter().getProvider().getValue());
                    break;
                }
            }
            LOG.debug("Writing endpoint location changes to DS");
            DataStoreHelper.submitToDs(wtx);
        }
    }

    private void processRemovedLocationProviderData(LocationProvider provider, WriteTransaction wtx) {
        for (ProviderAddressEndpointLocation addressEndpointLocation : nullToEmpty(
                provider.getProviderAddressEndpointLocation())) {
            AddressEndpointLocationKey epKey = createAddressEndpointLocationKey(addressEndpointLocation.getKey());
            long priority;
            if (provider.getPriority() == null) {
                priority = 0;
                LOG.debug("{} provider doesn't provide priority. Using 0 as priority instead.",
                        provider.getProvider().getValue());
            } else {
                priority = provider.getPriority();
            }
            realLocations.get(epKey).remove(priority);
            AbsoluteLocation newAbsoluteLocation = getBestAbsoluteLocation(epKey);
            if (newAbsoluteLocation == null) {
                InstanceIdentifier<AbsoluteLocation> iid = IidFactory.absoluteLocationIid(epKey);
                wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            } else {
                AddressEndpointLocationBuilder newEP =
                        new AddressEndpointLocationBuilder().setKey(epKey).setAbsoluteLocation(newAbsoluteLocation);
                InstanceIdentifier<AddressEndpointLocation> iid = IidFactory.addressEndpointLocationIid(newEP.getKey());
                wtx.merge(LogicalDatastoreType.OPERATIONAL, iid, newEP.build(), true);
            }
            if (addressEndpointLocation.getRelativeLocations() != null) {
                for (InternalLocation location : nullToEmpty(
                        addressEndpointLocation.getRelativeLocations().getInternalLocation())) {
                    InstanceIdentifier<InternalLocation> iid = IidFactory.internalLocationIid(epKey, location.getKey());
                    wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                }
                for (ExternalLocation location : nullToEmpty(
                        addressEndpointLocation.getRelativeLocations().getExternalLocation())) {
                    InstanceIdentifier<ExternalLocation> iid = IidFactory.externalLocationIid(epKey, location.getKey());
                    wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                }
            }
            if (newAbsoluteLocation == null && addressEndpointLocation.getRelativeLocations() == null) {
                InstanceIdentifier<AddressEndpointLocation> iid = IidFactory.addressEndpointLocationIid(epKey);
                wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            }
        }
        for (ProviderContainmentEndpointLocation containmentEndpoint : nullToEmpty(
                provider.getProviderContainmentEndpointLocation())) {
            ContainmentEndpointLocationKey epKey = createContainmentEndpointLocationKey(containmentEndpoint.getKey());
            if (containmentEndpoint.getRelativeLocations() != null) {
                for (InternalLocation location : nullToEmpty(
                        containmentEndpoint.getRelativeLocations().getInternalLocation())) {
                    InstanceIdentifier<InternalLocation> iid = IidFactory.internalLocationIid(epKey, location.getKey());
                    wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                }
                for (ExternalLocation location : nullToEmpty(
                        containmentEndpoint.getRelativeLocations().getExternalLocation())) {
                    InstanceIdentifier<ExternalLocation> iid = IidFactory.externalLocationIid(epKey, location.getKey());
                    wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                }
            }
        }
    }

    private void processCreatedLocationProviderData(LocationProvider provider, WriteTransaction wtx) {
        for (ProviderAddressEndpointLocation addressEndpointLocation : nullToEmpty(
                provider.getProviderAddressEndpointLocation())) {
            AddressEndpointLocationKey epKey = createAddressEndpointLocationKey(addressEndpointLocation.getKey());
            AddressEndpointLocationBuilder newEP = new AddressEndpointLocationBuilder().setKey(epKey);
            if (addressEndpointLocation.getAbsoluteLocation() != null) {
                if (realLocations.get(epKey) == null) {
                    realLocations.put(epKey, new HashMap<>());
                }
                long priority;
                if (provider.getPriority() == null) {
                    priority = 0;
                    LOG.debug("{} provider doesnt provide priority. Using 0 as priority instead.",
                            provider.getProvider().getValue());
                } else {
                    priority = provider.getPriority();
                }
                realLocations.get(epKey).put(priority, addressEndpointLocation.getAbsoluteLocation());
            }
            AbsoluteLocation bestLocation = getBestAbsoluteLocation(epKey);
            if (bestLocation != null) {
                newEP.setAbsoluteLocation(bestLocation);
            }
            if (addressEndpointLocation.getRelativeLocations() != null) {
                newEP.setRelativeLocations(addressEndpointLocation.getRelativeLocations());
            }
            InstanceIdentifier<AddressEndpointLocation> iid = IidFactory.addressEndpointLocationIid(newEP.getKey());
            wtx.merge(LogicalDatastoreType.OPERATIONAL, iid, newEP.build(), true);
        }
        for (ProviderContainmentEndpointLocation containmentEndpointLocation : nullToEmpty(
                provider.getProviderContainmentEndpointLocation())) {
            if (containmentEndpointLocation.getRelativeLocations() != null) {
                ContainmentEndpointLocationKey key =
                        createContainmentEndpointLocationKey(containmentEndpointLocation.getKey());
                ContainmentEndpointLocationBuilder newEP = new ContainmentEndpointLocationBuilder().setKey(key);
                newEP.setRelativeLocations(containmentEndpointLocation.getRelativeLocations());
                InstanceIdentifier<ContainmentEndpointLocation> iid =
                        IidFactory.containmentEndpointLocationIid(newEP.getKey());
                wtx.merge(LogicalDatastoreType.OPERATIONAL, iid, newEP.build(), true);
            }
        }
    }

    private AbsoluteLocation getBestAbsoluteLocation(AddressEndpointLocationKey epKey) {
        if (realLocations.get(epKey) == null) {
            return null;
        }
        long bestPriority = -1;
        for (long priority : realLocations.get(epKey).keySet()) {
            bestPriority = bestPriority > priority ? bestPriority : priority;
        };
        if (bestPriority == -1) {
            return null;
        }
        return (realLocations.get(epKey).get(new Long(bestPriority)));
    }

    private AddressEndpointLocationKey createAddressEndpointLocationKey(ProviderAddressEndpointLocationKey key) {
        return new AddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    private ContainmentEndpointLocationKey createContainmentEndpointLocationKey(
            ProviderContainmentEndpointLocationKey key) {
        return new ContainmentEndpointLocationKey(key.getContextId(), key.getContextType());
    }

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void close() {
        listenerRegistation.close();
    }
}
