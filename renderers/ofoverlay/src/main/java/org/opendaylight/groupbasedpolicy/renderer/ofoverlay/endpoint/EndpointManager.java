/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp.ArpTasker;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.napt.translations.NaptTranslation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Keep track of endpoints on the system. Maintain an index of endpoints and
 * their locations for rendering. The endpoint manager will maintain
 * appropriate indexes only for switches that are attached to the current
 * controller node.
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular switch and also all the switches containing
 * each particular endpoint group
 */
public class EndpointManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);
    private final EndpointManagerListener endpointListener;
    private final ConcurrentHashMap<EpKey, Endpoint> endpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EpKey, Endpoint> externalEndpointsWithoutLocation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, ConcurrentMap<EgKey, Set<EpKey>>> endpointsByGroupByNode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, Set<EpKey>> endpointsByNode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private final DataBroker dataProvider;
    private final ArpTasker arpTasker;
    private final ListenerRegistration<ArpTasker> notificationListenerRegistration;
    private List<EndpointListener> listeners = new CopyOnWriteArrayList<>();
    private Function<EpKey, Endpoint> indexTransform = new Function<EpKey, Endpoint>() {

        @Override
        public Endpoint apply(EpKey input) {
            return endpoints.get(input);
        }
    };

    public EndpointManager(DataBroker dataProvider, RpcProviderRegistry rpcRegistry, NotificationService notificationService, ScheduledExecutorService executor,
                           SwitchManager switchManager) {
        this.executor = executor;
        this.dataProvider = dataProvider;
        if (rpcRegistry != null) {
            if (notificationService != null && dataProvider != null) {
                this.arpTasker = new ArpTasker(rpcRegistry, dataProvider);
                notificationListenerRegistration = notificationService.registerNotificationListener(arpTasker);
            } else {
                LOG.info("Missing service {}", NotificationService.class.getSimpleName());
                this.arpTasker = null;
                this.notificationListenerRegistration = null;
            }
        } else {
            LOG.warn("Missing service {}", RpcProviderRegistry.class.getSimpleName());
            this.arpTasker = null;
            this.notificationListenerRegistration = null;
        }
        if (dataProvider != null) {
            endpointListener = new EndpointManagerListener(this.dataProvider, this);
        } else {
            endpointListener = null;
        }
        LOG.debug("Initialized OFOverlay endpoint manager");
    }

    /**
     * Add a {@link EndpointListener} to get notifications of switch events
     *
     * @param listener - the {@link EndpointListener} to add
     */
    public void registerListener(EndpointListener listener) {
        listeners.add(listener);
    }

    /**
     * Get a collection of endpoints attached to a particular switch
     *
     * @param nodeId - the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public synchronized Set<EgKey> getGroupsForNode(NodeId nodeId) {
        Map<EgKey, Set<EpKey>> nodeEps = endpointsByGroupByNode.get(nodeId);
        if (nodeEps == null)
            return Collections.emptySet();
        return ImmutableSet.copyOf(nodeEps.keySet());
    }

    /**
     * Get the set of nodes
     *
     * @param egKey - the egKey of the endpoint group to get nodes for
     * @return a collection of {@link NodeId} objects.
     */
    public synchronized Set<NodeId> getNodesForGroup(final EgKey egKey) {
        return ImmutableSet.copyOf(Sets.filter(endpointsByGroupByNode.keySet(), new Predicate<NodeId>() {

            @Override
            public boolean apply(NodeId input) {
                Map<EgKey, Set<EpKey>> nodeEps = endpointsByGroupByNode.get(input);
                return (nodeEps != null && nodeEps.containsKey(egKey));
            }

        }));
    }

    /**
     * Get the endpoints in a particular group on a particular node
     *
     * @param nodeId - the node ID to look up
     * @param eg     - the group to look up
     * @return the endpoints
     */
    public synchronized Collection<Endpoint> getEndpointsForNode(NodeId nodeId, EgKey eg) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // data store

        Map<EgKey, Set<EpKey>> nodeEps = endpointsByGroupByNode.get(nodeId);
        if (nodeEps == null)
            return Collections.emptyList();
        Collection<EpKey> ebn = nodeEps.get(eg);
        if (ebn == null)
            return Collections.emptyList();
        return ImmutableList.copyOf(Collections2.transform(ebn, indexTransform));
    }

    /**
     * Get the endpoints on a particular node
     *
     * @param nodeId - the node ID to look up
     * @return the endpoints
     */
    public synchronized Collection<Endpoint> getEndpointsForNode(final NodeId nodeId) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // data store. See commented code below.

        Collection<EpKey> ebn = endpointsByNode.get(nodeId);
        if (ebn == null)
            return Collections.emptyList();
        return ImmutableList.copyOf(Collections2.transform(ebn, indexTransform));
    }

    /**
     * Get the endpoint object for the given key
     *
     * @param epKey - the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }

    /**
     * Get a collection of endpoints in a particular endpoint group
     *
     * @param eg - Endpoint group key (contains endpoint group and tenant ID)
     * @return a collection of {@link Endpoint} objects.
     */
    public synchronized Collection<Endpoint> getEndpointsForGroup(EgKey eg) {
        Collection<EpKey> ebg = endpointsByGroup.get(eg);
        if (ebg == null)
            return Collections.emptyList();
        return ImmutableList.copyOf(Collections2.transform(ebg, indexTransform));
    }

    /**
     * Return set of external endpoints without location belonging to a particular endpoint group
     *
     * @param eg - Endpoint group key (contains endpoint group and tenant ID)
     * @return a collection of {@link Endpoint} objects.
     */
    public synchronized Collection<Endpoint> getExtEpsNoLocForGroup(final EgKey eg) {

        return ImmutableSet.copyOf(Collections2.filter(externalEndpointsWithoutLocation.values(),
                new Predicate<Endpoint>() {

                    @Override
                    public boolean apply(Endpoint input) {
                        Set<EndpointGroupId> epgIds = new HashSet<>();
                        if (input.getEndpointGroup() != null) {
                            epgIds.add(input.getEndpointGroup());
                        }
                        if (input.getEndpointGroups() != null) {
                            epgIds.addAll(input.getEndpointGroups());
                        }
                        if (epgIds.isEmpty()) {
                            LOG.error("No EPGs for {}. This is not a valid Endpoint.", input.getKey());
                            return false;
                        }
                        return (epgIds.contains(eg.getEgId()));
                    }

                }));
    }

    /**
     * Character of input parameters will determine action - create, update or delete L3Endpoint
     *
     * @param oldL3Ep the old L3 endpoint
     * @param newL3Ep the new L3 endpoint
     */
    protected synchronized void processL3Endpoint(EndpointL3 oldL3Ep, EndpointL3 newL3Ep) {
        // TODO Bug 3543
        //create L3 endpoint
        if (oldL3Ep == null && newL3Ep != null) {
            createL3Endpoint(newL3Ep);
        }

        //update L3 endpoint
        if (oldL3Ep != null && newL3Ep != null) {
            updateL3Endpoint(newL3Ep);
        }

        //remove L3 endpoint
        if (oldL3Ep != null && newL3Ep == null) {
            removeL3Endpoint(oldL3Ep);
        }
    }

    /**
     * Character of input parameters will determine action - create, update or delete Endpoint
     *
     * @param oldEp - oldEp the new endpoint
     * @param newEp - newEp the new endpoint
     */
    protected synchronized void processEndpoint(Endpoint oldEp, Endpoint newEp) {
        NodeId oldLoc = getLocation(oldEp);
        NodeId newLoc = getLocation(newEp);
        EpKey oldEpKey = getEpKey(oldEp);
        EpKey newEpKey = getEpKey(newEp);
        TenantId tenantId = (newEp == null) ? null : newEp.getTenant();

        Set<EndpointGroupId> oldEpgIds = getEndpointGroupsFromEndpoint(oldEp);
        Set<EndpointGroupId> newEpgIds = getEndpointGroupsFromEndpoint(newEp);

        boolean notifyOldLoc = false;
        boolean notifyNewLoc = false;
        boolean notifyOldEg = false;
        boolean notifyNewEg = false;

        //maintain external endpoints map
        if (newLoc == null && oldLoc == null) {
            if (newEp != null && newEpKey != null) {
                externalEndpointsWithoutLocation.put(newEpKey, newEp);
            }
            if (oldEp != null && oldEpKey != null) {
                externalEndpointsWithoutLocation.remove(oldEpKey);
            }
            return;
        }

        //maintain endpoint maps
        //new endpoint
        if (newEp != null && newEpKey != null) {
            endpoints.put(newEpKey, newEp);
            notifyEndpointUpdated(newEpKey);
        }
        //odl endpoint
        else if (oldEpKey != null) {
            endpoints.remove(oldEpKey);
            notifyEndpointUpdated(oldEpKey);
        }

        //create endpoint
        if (oldEp == null && newEp != null && newLoc != null) {
            createEndpoint(newLoc, newEpKey, newEpgIds, tenantId);
            notifyNewLoc = true;
            notifyNewEg = true;
        }

        //update endpoint
        if (oldEp != null && newEp != null && oldEpKey != null && newEpKey != null &&
                (oldEpKey.toString().equals(newEpKey.toString()))) {

            //endpoint was moved, new location exists but is different from old one
            if (oldLoc != null && newLoc != null && !(oldLoc.getValue().equals(newLoc.getValue()))) {
                //remove old endpoint
                removeEndpoint(oldEp, oldLoc, oldEpKey, oldEpgIds);
                notifyOldLoc = true;
                notifyOldEg = true;
            }
            //add moved endpoint
            createEndpoint(newLoc, newEpKey, newEpgIds, tenantId);
            notifyNewLoc = true;
            notifyNewEg = true;
        }

        //remove endpoint
        if (oldEp != null && newEp == null) {
            removeEndpoint(oldEp, oldLoc, oldEpKey, oldEpgIds);
            notifyOldLoc = true;
            notifyOldEg = true;
        }

        //notifications
        if (notifyOldLoc)
            notifyNodeEndpointUpdated(oldLoc, oldEpKey);
        if (notifyNewLoc)
            notifyNodeEndpointUpdated(newLoc, newEpKey);
        if (notifyOldEg)
            for (EndpointGroupId oldEpgId : oldEpgIds) {
                EgKey oldEgKey = new EgKey(oldEp.getTenant(), oldEpgId);
                notifyGroupEndpointUpdated(oldEgKey, oldEpKey);
            }
        if (notifyNewEg)
            for (EndpointGroupId newEpgId : newEpgIds) {
                EgKey newEgKey = new EgKey(newEp.getTenant(), newEpgId);
                notifyGroupEndpointUpdated(newEgKey, newEpKey);
            }
    }

    private void createEndpoint(NodeId newLoc, EpKey newEpKey, Set<EndpointGroupId> newEpgIds, TenantId tenantId) {
        // Update endpointsByNode
        if (endpointsByNode.get(newLoc) == null) {
            Set<EpKey> epsNode = new HashSet<>();
            epsNode.add(newEpKey);
            endpointsByNode.put(newLoc, epsNode);
            SwitchManager.activatingSwitch(newLoc);
        } else {
            Set<EpKey> epsNode = endpointsByNode.get(newLoc);
            epsNode.add(newEpKey);
        }

        // Update endpointsByGroupByNode and endpointsByGroup
        for (EndpointGroupId newEpgId : newEpgIds) {
            // endpointsByGroupByNode
            EgKey newEgKey = new EgKey(tenantId, newEpgId);
            Set<EpKey> eps = getEpNGSet(newLoc, newEgKey);
            eps.add(newEpKey);
            // endpointsByGroup
            Set<EpKey> geps = endpointsByGroup.get(newEgKey);
            if (geps == null) {
                geps = new HashSet<>();
            }
            geps.add(newEpKey);
            endpointsByGroup.put(newEgKey, geps);
            LOG.debug("Endpoint {} added to node {}", newEpKey, newLoc);

        }
    }

    private void removeEndpoint(Endpoint oldEp, NodeId oldLoc, EpKey oldEpKey, Set<EndpointGroupId> oldEpgIds) {
        // Update endpointsByNode
        Set<EpKey> epsNode = endpointsByNode.get(oldLoc);
        if (epsNode != null) {
            epsNode.remove(oldEpKey);
            if (epsNode.isEmpty()) {
                endpointsByNode.remove(oldLoc);
                SwitchManager.deactivatingSwitch(oldLoc);
            }
        }

        // Update endpointsByGroupByNode and endpointsByGroup, get map of EPGs and their Endpoints for Node
        ConcurrentMap<EgKey, Set<EpKey>> map = endpointsByGroupByNode.get(oldLoc);
        for (EndpointGroupId oldEpgId : oldEpgIds) {
            // endpointsByGroupByNode
            EgKey oldEgKey = new EgKey(oldEp.getTenant(), oldEpgId);
            Set<EpKey> eps = map.get(oldEgKey);
            if (eps != null) {
                eps.remove(oldEpKey);
                if (eps.isEmpty())
                    map.remove(oldEgKey, Collections.emptySet());
            }
            // endpointsByGroup
            Set<EpKey> geps = endpointsByGroup.get(oldEgKey);
            if (geps != null) {
                geps.remove(oldEpKey);
                if (geps.isEmpty())
                    endpointsByGroup.remove(oldEgKey);
            }
        }

        // If map is empty, no more EPGs on this node, remove node from map
        if (map.isEmpty())
            endpointsByGroupByNode.remove(oldLoc);

    }

    private void createL3Endpoint(EndpointL3 newL3Ep) {
        LOG.trace("Processing L3Endpoint {}", newL3Ep.getKey());
        if (isValidL3Ep(newL3Ep)) {
            if (newL3Ep.getMacAddress() == null && getLocationType(newL3Ep) != null
                    && getLocationType(newL3Ep).equals(LocationType.External)) {
                if (newL3Ep.getNetworkContainment() != null) {
                    arpTasker.addMacForL3EpAndCreateEp(newL3Ep);
                } else {
                    LOG.error("Cannot generate MacAddress for L3Endpoint {}. NetworkContainment is null.", newL3Ep);
                    return;
                }
            }
            if (newL3Ep.getL2Context() != null && newL3Ep.getMacAddress() != null) {
                notifyEndpointUpdated(new EpKey(newL3Ep.getL2Context(), newL3Ep.getMacAddress()));
                return;
            }
        } else {
            LOG.error("{} is not a valid L3 Endpoint", newL3Ep);
            return;
        }
        if (newL3Ep.getAugmentation(OfOverlayL3Context.class) == null) {
            LOG.info("L3Endpoint created but no augmentation information");
        }
    }

    private void updateL3Endpoint(EndpointL3 newL3Ep) {
        LOG.trace("Updating L3 Endpoint {}");
        notifyEndpointUpdated(new EpKey(newL3Ep.getL2Context(), newL3Ep.getMacAddress()));
        if (newL3Ep.getAugmentation(OfOverlayL3Context.class) == null) {
            LOG.info("L3Endpoint updated but no augmentation information");
        }
    }

    private void removeL3Endpoint(EndpointL3 oldL3Ep) {
        LOG.trace("Removing L3 Endpoint {}");
        notifyEndpointUpdated(new EpKey(oldL3Ep.getL2Context(), oldL3Ep.getMacAddress()));

    }

    //auto closeable
    @Override
    public void close() throws Exception {
        if (endpointListener != null)
            endpointListener.close();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.close();
        }
    }

    private Set<EpKey> getEpNGSet(NodeId location, EgKey eg) {
        ConcurrentMap<EgKey, Set<EpKey>> map = endpointsByGroupByNode.get(location);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ConcurrentMap<EgKey, Set<EpKey>> old = endpointsByGroupByNode.putIfAbsent(location, map);
            if (old != null)
                map = old;
        }
        return SetUtils.getNestedSet(eg, map);
    }

    protected boolean isInternal(Endpoint ep) {
        Preconditions.checkNotNull(ep);
        OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);
        return ofc == null || ofc.getLocationType() == null || ofc.getLocationType().equals(EndpointLocation.LocationType.Internal);
    }

    public boolean isExternal(Endpoint ep) {
        return !isInternal(ep);
    }

    /**
     * Get the endpoints container from data store.
     * Note: There are maps maintained by listener when higher performance is required.
     *
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints}
     */
    protected Endpoints getEndpointsFromDataStore() {
        /*
         * XXX: alagalah I wanted to avoid adding another Map. Due to not being able to
         * get to the granularity of the L3PrefixEndpoint List within the Endpoints container
         * in the data store, we have to pull all the Endpoints. If this causes performance issues
         * we may have to revisit a Map in updateEndpoint but note, this Endpoint doesn't have a
         * location
         * and hence we would have to process it outside the null location check.
         */
        if (dataProvider == null) {
            LOG.error("Null DataProvider in EndpointManager getEndpointsL3Prefix");
            return null;
        }
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<Endpoints> endpoints = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointsIidWildcard(), rTx);
        if (!endpoints.isPresent()) {
            LOG.warn("No Endpoints present in data store.");
            return null;
        }
        return endpoints.get();
    }

    /**
     * Return all L3Endpoints from data store.
     *
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3}
     */
    protected Collection<EndpointL3> getL3Endpoints() {
        Endpoints endpoints = getEndpointsFromDataStore();
        if (endpoints == null || endpoints.getEndpointL3() == null) {
            LOG.warn("No L3  Endpoints present in data store.");
            return null;
        }
        return endpoints.getEndpointL3();
    }

    /**
     * Return all L3Prefix Endpoints from data store.
     *
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix}
     */
    private Collection<EndpointL3Prefix> getEndpointsL3Prefix() {
        Endpoints endpoints = getEndpointsFromDataStore();
        if (endpoints == null || endpoints.getEndpointL3Prefix() == null) {
            LOG.warn("No L3 Prefix Endpoints present in data store.");
            return null;
        }
        return endpoints.getEndpointL3Prefix();
    }

    /**
     * Return all L3Prefix Endpoints which come under particular tenant
     *
     * @param tenantId - the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId} to resolve
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix}
     */
    public Collection<EndpointL3Prefix> getEndpointsL3PrefixForTenant(final TenantId tenantId) {
        Collection<EndpointL3Prefix> l3PrefixEndpoints = getEndpointsL3Prefix();
        if (l3PrefixEndpoints == null) {
            // Log message already generated in getEndpointsL3Prefix()
            return null;
        }
        return ImmutableSet.copyOf(Collections2.filter(l3PrefixEndpoints, new Predicate<EndpointL3Prefix>() {

            @Override
            public boolean apply(EndpointL3Prefix input) {
                return (input.getTenant().equals(tenantId));
            }

        }));
    }

    /**
     * Return all L3Endpoints containing network and port address translation in augmentation
     *
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3}
     */
    public Collection<EndpointL3> getL3EndpointsWithNat() {
        Collection<EndpointL3> l3Endpoints = getL3Endpoints();
        if (l3Endpoints == null) {
            return null;
        }
        l3Endpoints = Collections2.filter(l3Endpoints, new Predicate<EndpointL3>() {

            @Override
            public boolean apply(EndpointL3 input) {
                return !((input.getAugmentation(OfOverlayL3Nat.class) == null)
                        || (input.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations() == null)
                        || (input.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations().getNaptTranslation() == null));
            }
        });
        if (l3Endpoints == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.copyOf(l3Endpoints);
    }

    /**
     * Return NAPT of concrete endpoint
     *
     * @param endpointL3 - the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3} to resolve
     * @return the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint}
     */
    public List<NaptTranslation> getNaptAugL3Endpoint(EndpointL3 endpointL3) {
        if ((endpointL3.getAugmentation(OfOverlayL3Nat.class) == null)
                || (endpointL3.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations() == null)
                || (endpointL3.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations().getNaptTranslation() == null)) {
            return null;
        }
        return endpointL3.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations().getNaptTranslation();
    }

    /**
     * Set the learning mode to the specified value
     *
     * @param learningMode - the learning mode to set
     */
    @SuppressWarnings({"UnusedParameters", "EmptyMethod"})
    public void setLearningMode(OfOverlayConfig.LearningMode learningMode) {
        // No-op for now
    }

    /**
     * Get the effective list of conditions that apply to a particular endpoint.
     * This could include additional conditions over the condition labels
     * directly represented in the endpoint object
     * set
     *
     * @param endpoint - the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint} to resolve
     * @return the list of {@link org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName}
     */
    public List<ConditionName> getConditionsForEndpoint(Endpoint endpoint) {
        // TODO Be alagalah From Helium: consider group conditions as well. Also
        // need to notify
        // endpoint updated if the endpoint group conditions change
        if (endpoint.getCondition() != null)
            return endpoint.getCondition();
        else
            return Collections.emptyList();
    }

    private void notifyEndpointUpdated(EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.endpointUpdated(epKey);
        }
    }

    private void notifyNodeEndpointUpdated(NodeId nodeId, EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.nodeEndpointUpdated(nodeId, epKey);
        }
    }

    private void notifyGroupEndpointUpdated(EgKey egKey, EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.groupEndpointUpdated(egKey, epKey);
        }
    }

    private boolean isValidEp(Endpoint endpoint) {
        return (endpoint != null && endpoint.getTenant() != null
                && (endpoint.getEndpointGroup() != null || endpoint.getEndpointGroups() != null)
                && endpoint.getL2Context() != null && endpoint.getMacAddress() != null);
    }

    private boolean isValidL3Ep(EndpointL3 endpoint) {
        return (endpoint != null && endpoint.getTenant() != null
                && (endpoint.getEndpointGroup() != null || endpoint.getEndpointGroups() != null)
                && endpoint.getL3Context() != null && endpoint.getIpAddress() != null);
    }

    private NodeId getLocation(Endpoint endpoint) {
        if (isValidEp(endpoint)) {
            OfOverlayContext context = endpoint.getAugmentation(OfOverlayContext.class);
            if (context != null)
                return context.getNodeId();
        }
        return null;
    }

    private EpKey getEpKey(Endpoint endpoint) {
        if (isValidEp(endpoint))
            return new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
        return null;
    }

    public Set<EgKey> getEgKeysForEndpoint(Endpoint ep) {
        Set<EgKey> egKeys = new HashSet<>();

        if (ep.getEndpointGroup() != null) {
            egKeys.add(new EgKey(ep.getTenant(), ep.getEndpointGroup()));
        }
        if (ep.getEndpointGroups() != null) {
            for (EndpointGroupId epgId : ep.getEndpointGroups()) {
                egKeys.add(new EgKey(ep.getTenant(), epgId));
            }
        }
        return egKeys;
    }

    private EndpointLocation.LocationType getLocationType(EndpointL3 epL3) {
        if (epL3 == null || epL3.getAugmentation(OfOverlayL3Context.class) == null
                || epL3.getAugmentation(OfOverlayL3Context.class).getLocationType() == null) {
            return null;
        }
        return epL3.getAugmentation(OfOverlayL3Context.class).getLocationType();
    }

    @SuppressWarnings("unused")
    private Endpoint addEndpointFromL3Endpoint(EndpointL3 l3Ep, ReadWriteTransaction rwTx) {
        // Make an indexed tenant and resolveL2BridgeDomain from L3EP containment if not L3
        // (instanceof)
        OfOverlayL3Context ofL3Ctx = l3Ep.getAugmentation(OfOverlayL3Context.class);
        OfOverlayContext ofCtx = getOfOverlayContextFromL3Endpoint(ofL3Ctx);
        if (l3Ep.getNetworkContainment() instanceof L3Context) {
            LOG.error("Cannot generate Endpoint from EndpointL3, network containment is L3Context.");
            rwTx.cancel();
            return null;
        }

        IndexedTenant indexedTenant;
        Optional<Tenant> tenant = readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.tenantIid(l3Ep.getTenant()), rwTx);
        if (tenant.isPresent()) {
            indexedTenant = new IndexedTenant(tenant.get());
        } else {
            LOG.error("Could not find tenant {} for EndpointL3 {}", l3Ep.getTenant(), l3Ep);
            rwTx.cancel();
            return null;
        }
        List<L3Address> l3Address = new ArrayList<>();
        l3Address.add(new L3AddressBuilder().setIpAddress(l3Ep.getIpAddress())
                .setL3Context(l3Ep.getL3Context())
                .setKey(new L3AddressKey(l3Ep.getIpAddress(), l3Ep.getL3Context()))
                .build());
        L2BridgeDomain l2Bd = indexedTenant.resolveL2BridgeDomain(l3Ep.getNetworkContainment());
        Endpoint ep = new EndpointBuilder().setKey(new EndpointKey(l2Bd.getId(), l3Ep.getMacAddress()))
                .setMacAddress(l3Ep.getMacAddress())
                .setL2Context(l2Bd.getId())
                .setEndpointGroups(l3Ep.getEndpointGroups())
                .setTenant(l3Ep.getTenant())
                .setL3Address(l3Address)
                .setCondition(l3Ep.getCondition())
                .setNetworkContainment(l3Ep.getNetworkContainment())
                .addAugmentation(OfOverlayContext.class, ofCtx)
                .build();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.endpointIid(ep.getL2Context(), ep.getMacAddress()), ep);
        return ep;
    }

    private OfOverlayContext getOfOverlayContextFromL3Endpoint(OfOverlayL3Context ofL3Ctx) {
        OfOverlayContextBuilder ofBuilder = new OfOverlayContextBuilder();
        if (ofL3Ctx.getInterfaceId() != null) {
            ofBuilder.setInterfaceId(ofL3Ctx.getInterfaceId());
        }
        if (ofL3Ctx.getLocationType() != null) {
            ofBuilder.setLocationType(ofL3Ctx.getLocationType());
        }
        if (ofL3Ctx.getNodeConnectorId() != null) {
            ofBuilder.setNodeConnectorId(ofL3Ctx.getNodeConnectorId());
        }
        if (ofL3Ctx.getNodeId() != null) {
            ofBuilder.setNodeId(ofL3Ctx.getNodeId());
        }
        if (ofL3Ctx.getPortName() != null) {
            ofBuilder.setPortName(ofL3Ctx.getPortName());
        }

        return ofBuilder.build();
    }

    private Set<EndpointGroupId> getEndpointGroupsFromEndpoint(Endpoint ep) {
        if (ep == null)
            return new HashSet<>();
        Set<EndpointGroupId> epgIds = new HashSet<>();
        if (ep.getEndpointGroups() != null) {
            epgIds.addAll(ep.getEndpointGroups());
        }
        if (ep.getEndpointGroup() != null) {
            epgIds.add(ep.getEndpointGroup());
        }
        return epgIds;
    }

    protected Map<EndpointKey, EndpointL3> getL3EpWithNatByL2Key() {
        Map<EndpointKey, EndpointL3> l3EpByL2EpKey = new HashMap<>();

        Collection<EndpointL3> l3Eps = getL3EndpointsWithNat();
        if (l3Eps == null) {
            l3EpByL2EpKey = Collections.emptyMap();
            return l3EpByL2EpKey;
        }
        for (EndpointL3 l3Ep : l3Eps) {
            if (l3Ep.getL2Context() != null && l3Ep.getMacAddress() != null) {
                EndpointKey epKey = new EndpointKey(l3Ep.getL2Context(), l3Ep.getMacAddress());
                l3EpByL2EpKey.put(epKey, l3Ep);
            }
        }
        if (l3EpByL2EpKey.isEmpty()) {
            l3EpByL2EpKey = Collections.emptyMap();
        }
        return l3EpByL2EpKey;
    }

    public EgKey getEgKey(Endpoint endpoint) {
        if (!isValidEp(endpoint))
            return null;
        return new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup());
    }
}