/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp.ArpTasker;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3NatInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.napt.translations.NaptTranslation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
 * their locations for renderering. The endpoint manager will maintain
 * appropriate indexes only for switches that are attached to the current
 * controller node.
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular switch and also all the switches containing
 * each particular endpoint group
 */
public class EndpointManager implements AutoCloseable, DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);
    private final static InstanceIdentifier<Nodes> nodesIid = InstanceIdentifier.builder(Nodes.class).build();

    final ListenerRegistration<DataChangeListener> listenerReg;

    private final ConcurrentHashMap<EpKey, Endpoint> endpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EpKey, Endpoint> externalEndpointsWithoutLocation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, ConcurrentMap<EgKey, Set<EpKey>>> endpointsByGroupByNode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, Set<EpKey>> endpointsByNode = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup = new ConcurrentHashMap<>();

    private List<EndpointListener> listeners = new CopyOnWriteArrayList<>();

    final private OfEndpointAug endpointRpcAug = new OfEndpointAug();
    final private OfOverlayL3NatAug ofOverlayL3NatAug = new OfOverlayL3NatAug();

    final private ScheduledExecutorService executor;

    final private DataBroker dataProvider;

    private final ArpTasker arpTasker;
    private final ListenerRegistration<ArpTasker> notificationListenerRegistration;

    public EndpointManager(DataBroker dataProvider, RpcProviderRegistry rpcRegistry, NotificationService notificationService, ScheduledExecutorService executor,
            SwitchManager switchManager) {
        this.executor = executor;
        this.dataProvider = dataProvider;
        if (rpcRegistry != null) {
            EndpointRpcRegistry.register(dataProvider, rpcRegistry, endpointRpcAug);
            EndpointRpcRegistry.register(dataProvider, rpcRegistry, ofOverlayL3NatAug);
            if (notificationService != null && dataProvider != null) {
                this.arpTasker = new ArpTasker(rpcRegistry, dataProvider);
                notificationListenerRegistration = notificationService.registerNotificationListener(arpTasker);
            } else {
                LOG.info("Missinge service {}", NotificationService.class.getSimpleName());
                this.arpTasker = null;
                this.notificationListenerRegistration = null;
            }
        } else {
            LOG.warn("Missinge service {}", RpcProviderRegistry.class.getSimpleName());
            this.arpTasker = null;
            this.notificationListenerRegistration = null;
        }
        if (dataProvider != null) {
            listenerReg = dataProvider.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.endpointsIidWildcard(), this, DataChangeScope.SUBTREE);
        } else {
            listenerReg = null;
        }
        LOG.debug("Initialized OFOverlay endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Add a {@link EndpointListener} to get notifications of switch events
     *
     * @param listener
     *        the {@link EndpointListener} to add
     */
    public void registerListener(EndpointListener listener) {
        listeners.add(listener);
    }

    /**
     * Get a collection of endpoints attached to a particular switch
     *
     * @param nodeId
     *        the nodeId of the switch to get endpoints for
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
     * @param egKey
     *        the egKey of the endpointgroup to get nodes for
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
     * @param nodeId
     *        the node ID to look up
     * @param eg
     *        the group to look up
     * @return the endpoints
     */
    public synchronized Collection<Endpoint> getEndpointsForNode(NodeId nodeId, EgKey eg) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // datastore

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
     * @param nodeId
     *        the node ID to look up
     * @return the endpoints
     */
    public synchronized Collection<Endpoint> getEndpointsForNode(final NodeId nodeId) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // datastore. See commented code below.

        Collection<EpKey> ebn = endpointsByNode.get(nodeId);
        if (ebn == null)
            return Collections.emptyList();
        return ImmutableList.copyOf(Collections2.transform(ebn, indexTransform));
    }

    public synchronized Collection<Endpoint> getExternalEndpointsWithoutLoc() {
        return ImmutableList.copyOf(externalEndpointsWithoutLocation.values());
    }

    public synchronized Endpoint getExternalEndpointWithoutLoc(EpKey epKey) {
        return externalEndpointsWithoutLocation.get(epKey);
    }

    /**
     * Get the endpoint object for the given key
     *
     * @param epKey
     *        the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }

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
     * Get the endpoints container from datastore.
     * Note: There are maps maintained by listener when higher performance is required.
     *
     * @param
     * @return the {@link Endpoints}
     */
    public Endpoints getEndpointsFromDataStore() {
        /*
         * XXX: alagalah I wanted to avoid adding another Map. Due to not being able to
         * get to the granularity of the L3PrefixEndpoint List within the Endpoints container
         * in the datastore, we have to pull all the Endpoints. If this causes performance issues
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
            LOG.warn("No Endpoints present in datastore.");
            return null;
        }
        return endpoints.get();
    }

    /**
     * Return all L3Prefix Endpoints from datastore.
     *
     * @param
     * @return the {@link EndpointL3Prefix}
     */
    public Collection<EndpointL3Prefix> getEndpointsL3Prefix() {
        Endpoints endpoints = getEndpointsFromDataStore();
        if (endpoints == null || endpoints.getEndpointL3Prefix() == null) {
            LOG.warn("No L3 Prefix Endpoints present in datastore.");
            return null;
        }
        return endpoints.getEndpointL3Prefix();
    }

    /**
     * Return all L3Endpoints from datastore.
     *
     * @param
     * @return the {@link EndpointL3}
     */
    public Collection<EndpointL3> getL3Endpoints() {
        Endpoints endpoints = getEndpointsFromDataStore();
        if (endpoints == null || endpoints.getEndpointL3() == null) {
            LOG.warn("No L3  Endpoints present in datastore.");
            return null;
        }
        return endpoints.getEndpointL3();
    }

    public Map<EndpointKey, EndpointL3> getL3EpWithNatByL2Key() {
        Map<EndpointKey, EndpointL3> l3EpByL2EpKey = new HashMap<>();

        Collection<EndpointL3> l3Eps = getL3EndpointsWithNat();
        if (l3Eps == null ) {
            l3EpByL2EpKey = Collections.emptyMap();
            return l3EpByL2EpKey;
        }
        for (EndpointL3 l3Ep : l3Eps) {
            if (l3Ep.getL2Context() != null && l3Ep.getMacAddress() != null) {
                EndpointKey epKey = new EndpointKey(l3Ep.getL2Context(),l3Ep.getMacAddress());
                l3EpByL2EpKey.put(epKey, l3Ep);
            }
        }
        if (l3EpByL2EpKey.isEmpty()) {
            l3EpByL2EpKey = Collections.emptyMap();
        }
        return l3EpByL2EpKey;
    }

    public Collection<EndpointL3> getL3EndpointsWithNat() {
        Collection<EndpointL3> l3Endpoints = getL3Endpoints();
        if (l3Endpoints == null) {
            return null;
        }
        l3Endpoints=Collections2.filter(l3Endpoints, new Predicate<EndpointL3>() {

            @Override
            public boolean apply(EndpointL3 input) {
                if ((input.getAugmentation(OfOverlayL3Nat.class) == null)
                        || (input.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations() == null)
                        || (input.getAugmentation(OfOverlayL3Nat.class).getNaptTranslations().getNaptTranslation() == null)) {
                    return false;
                }
                return true;
            }
        });
        if (l3Endpoints == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.copyOf(l3Endpoints);
    }

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
     * @param learningMode
     *        the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }

    /**
     * Get a collection of endpoints in a particular endpoint group
     *
     * @param eg
     *        endpoint group ID
     * @return a collection of {@link Endpoint} objects.
     */
    public synchronized Collection<Endpoint> getEndpointsForGroup(EgKey eg) {
        Collection<EpKey> ebg = endpointsByGroup.get(eg);
        if (ebg == null)
            return Collections.emptyList();
        return ImmutableList.copyOf(Collections2.transform(ebg, indexTransform));
    }

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
     * Get the effective list of conditions that apply to a particular endpoint.
     * This could include additional conditions over the condition labels
     * directly represented in the endpoint object
     *
     * @param endpoint
     *        the {@link Endpoint} to resolve
     * @return the list of {@link ConditionName}
     */
    public List<ConditionName> getCondsForEndpoint(Endpoint endpoint) {
        // TODO Be alagalah From Helium: consider group conditions as well. Also
        // need to notify
        // endpoint updated if the endpoint group conditions change
        if (endpoint.getCondition() != null)
            return endpoint.getCondition();
        else
            return Collections.emptyList();
    }

    /**
     * Update the endpointL3 indexes. Set newEp to null to remove.
     */
    protected synchronized void updateEndpointL3(EndpointL3 oldL3Ep, EndpointL3 newL3Ep) {
        // TODO Bug 3543 - complete
        // if (oldEp == null && newEp != null ) {
        if (newL3Ep != null) {
            // new L3Endpoint
            LOG.trace("Processing L3Endpoint {}",newL3Ep.getKey());
            if (isValidL3Ep(newL3Ep)) {
                if (newL3Ep.getMacAddress() == null && getLocationType(newL3Ep) != null
                        && getLocationType(newL3Ep).equals(LocationType.External)) {
                    if (newL3Ep.getNetworkContainment() != null) {
                        arpTasker.addMacForL3EpAndCreateEp(newL3Ep);
                        return;
                    } else {
                        LOG.error("Cannot generate MacAddress for L3Endpoint {}. NetworkContainment is null.", newL3Ep);
                        return;
                    }
                }
            } else {
                LOG.error("{} is not a valid L3 Endpoint", newL3Ep);
                return;
            }
            return;
        }

        if (oldL3Ep != null && newL3Ep == null) {
            // deleted L3Endpoint
            return;
        }

        if (oldL3Ep != null && newL3Ep != null) {
            LOG.trace("Updating L3 Endpoint {}");
            // updated Endpoint
            return;
        }
        if (newL3Ep.getAugmentation(OfOverlayL3Context.class) == null) {
            LOG.info("L3Endpoint updatbut no augmentation information");
            return;
        }
    }

    /**
     * Update the endpoint indexes. Set newEp to null to remove.
     */
    protected synchronized void updateEndpoint(Endpoint oldEp, Endpoint newEp) {
        // TODO Be alagalah From Helium only keep track of endpoints that are
        // attached
        // to switches that are actually connected to us

        // TODO Li alagalah: This needs a major clean up and refactor. For now
        // it works.
        NodeId oldLoc = getLocation(oldEp);
        NodeId newLoc = getLocation(newEp);
        // EgKey oldEgKey = getEgKey(oldEp);
        EpKey oldEpKey = getEpKey(oldEp);
        EpKey newEpKey = getEpKey(newEp);

        boolean notifyOldLoc = false;
        boolean notifyNewLoc = false;
        boolean notifyOldEg = false;
        boolean notifyNewEg = false;

        /*
         * When newLoc and oldLoc are null for Internal ports there is nothing to do
         */
        if (newLoc == null && oldLoc == null) {
            if ((oldEp != null && isInternal(oldEp)) || (newEp != null && isInternal(newEp))) {
                return;
            } else {
                // Maintain "external endpoints" map
                if (newEp != null) {
                    externalEndpointsWithoutLocation.put(newEpKey, newEp);
                } else {
                    externalEndpointsWithoutLocation.remove(oldEpKey);
                }
                return; // No more processing for Externals.
            }
        }

        Set<EndpointGroupId> newEpgIds = new HashSet<EndpointGroupId>();
        TenantId tenantId = null;
        if (newEp != null) {
            if (newEp.getEndpointGroups() != null) {
                newEpgIds.addAll(newEp.getEndpointGroups());
            }
            if (newEp.getEndpointGroup() != null) {
                newEpgIds.add(newEp.getEndpointGroup());
            }
            tenantId = newEp.getTenant();
        }

        Set<EndpointGroupId> oldEpgIds = new HashSet<EndpointGroupId>();
        if (oldEp != null) {
            if (oldEp.getEndpointGroups() != null) {
                oldEpgIds.addAll(oldEp.getEndpointGroups());
            }
            if (oldEp.getEndpointGroup() != null) {
                oldEpgIds.add(oldEp.getEndpointGroup());
            }
        }

        /*
         * maintainIndex(endpointsByNode,oldEp,newEp) Maintain following maps
         * endpoints - <EpKey, Endpoint> endpointsByGroupByNode - <NodeId,
         * ConcurrentMap<EgKey, Set<EpKey>>> endpointsByNode -
         * <NodeId,Set<EpKey>> endpointsByGroup ConcurrentHashMap<EgKey,
         * Set<EpKey>>
         */

        // Maintain "endpoints" map
        if (newEp != null) {
            endpoints.put(newEpKey, newEp);
        } else {
            endpoints.remove(oldEpKey);
        }

        /*
         * New endpoint with location information
         */
        if (oldEp == null && newEp != null && newLoc != null) {
            // Update endpointsByNode
            if (endpointsByNode.get(newLoc) == null) {
                // TODO: alagalah cleaner way with checking epsNode
                // then do this.
                Set<EpKey> epsNode = new HashSet<EpKey>();
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

            notifyNewLoc = true;
            notifyNewEg = true;
        }

        /*
         * Removed endpoint
         */
        if (oldEp != null && newEp == null) {
            // Update endpointsByNode
            Set<EpKey> epsNode = endpointsByNode.get(oldLoc);
            if (epsNode != null) {
                epsNode.remove(oldEpKey);
                if (epsNode.isEmpty()) {
                    endpointsByNode.remove(oldLoc);
                    SwitchManager.deactivatingSwitch(oldLoc);
                }
            }
            // Update endpointsByGroupByNode
            // Update endpointsByGroup
            // Get map of EPGs and their Endpoints for Node
            ConcurrentMap<EgKey, Set<EpKey>> map = endpointsByGroupByNode.get(oldLoc);
            // For each EPG in the removed endpoint...
            for (EndpointGroupId oldEpgId : newEpgIds) {
                EgKey oldEgKey = new EgKey(oldEp.getTenant(), oldEpgId);
                // Get list of endpoints for EPG
                Set<EpKey> eps = map.get(oldEgKey);
                // Remove the endpoint from the map
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
            // If map is empty, no more EPGs on this node, remove node from
            // map
            if (map.isEmpty())
                endpointsByGroupByNode.remove(oldLoc, EMPTY_MAP);
            notifyOldLoc = true;
            notifyOldEg = true;
        }

        /*
         * Moved endpoint (from node to node or from NULL to node)
         */
        if ((oldEp != null && newEp != null && oldEpKey != null && newEpKey != null)
                && (oldEpKey.toString().equals(newEpKey.toString()))) {
            // old and new Endpoints have same key. (same endpoint)

            /*
             * Remove old endpoint if moved.
             */
            if (oldLoc != null && !(oldLoc.getValue().equals(newLoc.getValue()))) {
                // This is an endpoint that has moved, remove from old node
                Set<EpKey> epsNode = endpointsByNode.get(oldLoc);
                if (epsNode != null) {
                    epsNode.remove(oldEpKey);
                    if (epsNode.isEmpty()) {
                        endpointsByNode.remove(oldLoc);
                        SwitchManager.deactivatingSwitch(oldLoc);
                    }
                }
                // Update endpointsByGroupByNode
                // Get map of EPGs and their Endpoints for Node
                ConcurrentMap<EgKey, Set<EpKey>> map = endpointsByGroupByNode.get(oldLoc);
                // For each EPG in the removed endpoint...
                for (EndpointGroupId oldEpgId : oldEpgIds) {
                    EgKey oldEgKey = new EgKey(oldEp.getTenant(), oldEpgId);
                    // Get list of endpoints for EPG
                    Set<EpKey> eps = map.get(oldEgKey);
                    // Remove the endpoint from the map
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
                // If map is empty, no more EPGs on this node, remove node
                // from map
                if (map.isEmpty())
                    endpointsByGroupByNode.remove(oldLoc, EMPTY_MAP);
                notifyOldLoc = true;
                notifyOldEg = true;
            }

            /*
             * Add new endpoint
             */
            // Update endpointsByNode
            if (endpointsByNode.get(newLoc) == null) {
                Set<EpKey> newEpsNode = new HashSet<EpKey>();
                newEpsNode.add(newEpKey);
                endpointsByNode.put(newLoc, newEpsNode);
                SwitchManager.activatingSwitch(newLoc);
            } else {
                Set<EpKey> newEpsNode = endpointsByNode.get(newLoc);
                newEpsNode.add(newEpKey);
            }
            notifyNewLoc = true;

            // Update endpointsByGroupByNode
            // Update endpointsByGroup
            for (EndpointGroupId newEpgId : newEpgIds) {
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
                notifyNewEg = true;

                LOG.debug("Endpoint {} added to node {}", newEpKey, newLoc);
            }

        }

        if (newEp != null)
            notifyEndpointUpdated(newEpKey);
        else
            notifyEndpointUpdated(oldEpKey);

        // TODO alagalah NEXt: ensure right notification flags are set.
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

    private Endpoint addEndpointFromL3Endpoint(EndpointL3 l3Ep, ReadWriteTransaction rwTx) {
        // Make an indexed tenant and resolveL2Bridgedomain from L3EP containment if not L3
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

    // ************************
    // Endpoint Augmentation
    // ************************
    private class OfEndpointAug implements EpRendererAugmentation {

        @Override
        public Augmentation<Endpoint> buildEndpointAugmentation(RegisterEndpointInput input) {
            // In order to support both the port-name and the data-path information, allow
            // an EP to register without the augmentations, and resolve later.
            OfOverlayContextBuilder ictx = checkAugmentation(input);
            if (ictx != null) {
                return ictx.build();
            }
            return null;
        }

        @Override
        public Augmentation<EndpointL3> buildEndpointL3Augmentation(RegisterEndpointInput input) {
            OfOverlayContextBuilder ictx = checkAugmentation(input);
            if (ictx != null) {
                return new OfOverlayL3ContextBuilder(ictx.build()).build();
            }
            return null;
        }

        @Override
        public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
            // TODO Auto-generated method stub

        }

        private OfOverlayContextBuilder checkAugmentation(RegisterEndpointInput input) {
            OfOverlayContextInput ictx = input.getAugmentation(OfOverlayContextInput.class);
            if (ictx == null) {
                return null;
            }

            OfOverlayContextBuilder ictxBuilder = new OfOverlayContextBuilder(ictx);
            if (ictx.getPortName() != null && ictx.getNodeId() != null && ictx.getNodeConnectorId() != null) {
                return ictxBuilder;
            }

            /*
             * In the case where they've provided just the port name, go see if
             * we can find the NodeId and NodeConnectorId from inventory.
             */
            if (ictx.getPortName() != null) {
                NodeInfo augmentation = fetchAugmentation(ictx.getPortName().getValue());
                if (augmentation != null) {
                    ictxBuilder.setNodeId(augmentation.getNode().getId());
                    ictxBuilder.setNodeConnectorId(augmentation.getNodeConnector().getId());
                }
            }
            return ictxBuilder;
        }

        private NodeInfo fetchAugmentation(String portName) {
            NodeInfo nodeInfo = null;

            if (dataProvider != null) {

                Optional<Nodes> result;
                try {
                    result = dataProvider.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, nodesIid)
                        .get();
                    if (result.isPresent()) {
                        Nodes nodes = result.get();
                        for (Node node : nodes.getNode()) {
                            if (node.getNodeConnector() != null) {
                                boolean found = false;
                                for (NodeConnector nc : node.getNodeConnector()) {
                                    FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
                                    if (fcnc.getName().equals(portName)) {
                                        nodeInfo = new NodeInfo();
                                        nodeInfo.setNode(node);
                                        nodeInfo.setNodeConnector(nc);
                                        found = true;
                                        break;
                                    }
                                }
                                if (found)
                                    break;
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Caught exception in fetchAugmentation portName", e);
                }

            }
            return nodeInfo;
        }
    }

    private class OfOverlayL3NatAug implements EpRendererAugmentation {

        @Override
        public Augmentation<Endpoint> buildEndpointAugmentation(RegisterEndpointInput input) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Augmentation<EndpointL3> buildEndpointL3Augmentation(RegisterEndpointInput input) {
            if (input.getAugmentation(OfOverlayL3NatInput.class) != null) {
                return new OfOverlayL3NatBuilder(input.getAugmentation(OfOverlayL3NatInput.class)).build();
            }
            return null;
        }

        @Override
        public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
            // TODO Auto-generated method stub

        }

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

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null)
            listenerReg.close();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.close();
        }
        EndpointRpcRegistry.unregister(endpointRpcAug);
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint) {
                updateEndpoint(null, (Endpoint) dao);
            } else if (dao instanceof EndpointL3) {
                updateEndpointL3(null, (EndpointL3) dao);
            } else if (dao instanceof EndpointL3Prefix) {
                continue;
            }
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Endpoint) {
                updateEndpoint((Endpoint) old, null);
            } else if (old instanceof EndpointL3) {
                updateEndpointL3((EndpointL3) old, null);
            } else if (old instanceof EndpointL3Prefix) {
                continue;
            }
        }
        Map<InstanceIdentifier<?>, DataObject> dao = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : dao.entrySet()) {
            if (entry.getValue() instanceof Endpoint) {
                Endpoint oldEp = (Endpoint) change.getOriginalData().get(entry.getKey());
                updateEndpoint(oldEp, (Endpoint) entry.getValue());
            } else if (entry.getValue() instanceof EndpointL3) {
                EndpointL3 oldEp3 = (EndpointL3) change.getOriginalData().get(entry.getKey());
                updateEndpointL3(oldEp3, (EndpointL3) entry.getValue());
            } else if (entry.getValue() instanceof EndpointL3Prefix) {
                continue;
            }
        }
    }

    // **************
    // Helper Functions
    // **************

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

    private Function<EpKey, Endpoint> indexTransform = new Function<EpKey, Endpoint>() {

        @Override
        public Endpoint apply(EpKey input) {
            return endpoints.get(input);
        }
    };

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
        if (!isValidEp(endpoint))
            return null;
        OfOverlayContext context = endpoint.getAugmentation(OfOverlayContext.class);
        if (context != null)
            return context.getNodeId();

        return null;
    }

    private EpKey getEpKey(Endpoint endpoint) {
        if (!isValidEp(endpoint))
            return null;
        return new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
    }

    public EgKey getEgKey(Endpoint endpoint) {
        if (!isValidEp(endpoint))
            return null;
        return new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup());
    }

    public Set<EgKey> getEgKeysForEndpoint(Endpoint ep) {
        Set<EgKey> egKeys = new HashSet<EgKey>();

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

    private static final ConcurrentMap<EgKey, Set<EpKey>> EMPTY_MAP = new ConcurrentHashMap<>();

    private LocationType getLocationType(EndpointL3 epL3) {
        if (epL3 == null || epL3.getAugmentation(OfOverlayL3Context.class) == null
                || epL3.getAugmentation(OfOverlayL3Context.class).getLocationType() == null) {
            return null;
        }
        return epL3.getAugmentation(OfOverlayL3Context.class).getLocationType();
    }

    public static boolean isExternal(Endpoint ep) {
        return !isInternal(ep);
    }

    public static boolean isInternal(Endpoint ep) {
        Preconditions.checkNotNull(ep);
        OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);
        if (ofc == null)
            return true; // Default is internal
        if (ofc.getLocationType() == null || ofc.getLocationType().equals(LocationType.Internal))
            return true; // Default is internal
        return false;
    }

    // A wrapper class around node, nodeConnector info so we can pass a final
    // object inside OnSuccess anonymous inner class
    private static class NodeInfo {

        NodeConnector nodeConnector;
        Node node;

        private NodeInfo() {

        }

        private NodeInfo(NodeConnector nc, Node node) {
            this.nodeConnector = nc;
            this.node = node;
        }

        private Node getNode() {
            return this.node;
        }

        private NodeConnector getNodeConnector() {
            return this.nodeConnector;
        }

        public void setNodeConnector(NodeConnector nodeConnector) {
            this.nodeConnector = nodeConnector;
        }

        public void setNode(Node node) {
            this.node = node;
        }
    }

}
