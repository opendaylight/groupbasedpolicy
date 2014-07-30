/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.AbstractEndpointRegistry;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * Keep track of endpoints on the system.  Maintain an index of endpoints
 * and their locations for renderering.  The endpoint manager will maintain
 * appropriate indexes only for switches that are attached to the current
 * controller node.
 * 
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular switch and also all the switches containing 
 * each particular endpoint group
 * @author readams
 */
public class EndpointManager 
        extends AbstractEndpointRegistry 
        implements AutoCloseable, DataChangeListener
    {
    private static final Logger LOG = 
            LoggerFactory.getLogger(EndpointManager.class);
    
    private static final InstanceIdentifier<Endpoint> endpointsIid = 
            InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class).build();
    final ListenerRegistration<DataChangeListener> listenerReg;

    private final ConcurrentHashMap<EpKey, Endpoint> endpoints =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, 
                                    ConcurrentMap<EgKey, Set<EpKey>>> endpointsByNode =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup = 
            new ConcurrentHashMap<>();
            
    private List<EndpointListener> listeners = new CopyOnWriteArrayList<>();

    public EndpointManager(DataBroker dataProvider,
                           RpcProviderRegistry rpcRegistry,
                           ScheduledExecutorService executor,
                           SwitchManager switchManager) {
        super(dataProvider, rpcRegistry, executor);
        
        if (dataProvider != null) {
            listenerReg = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, 
                                                endpointsIid, 
                                                this, 
                                                DataChangeScope.ONE);
        } else
            listenerReg = null;

        LOG.debug("Initialized OFOverlay endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Add a {@link EndpointListener} to get notifications of switch events
     * @param listener the {@link EndpointListener} to add
     */
    public void registerListener(EndpointListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Get a collection of endpoints attached to a particular switch
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Set<EgKey> getGroupsForNode(NodeId nodeId) {
        Map<EgKey, Set<EpKey>> nodeEps = endpointsByNode.get(nodeId);
        if (nodeEps == null) return Collections.emptySet();
        return Collections.unmodifiableSet(nodeEps.keySet());
    }
    
    /**
     * Get the set of nodes
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Set<NodeId> getNodesForGroup(final EgKey egKey) {
        return Collections.unmodifiableSet(Sets.filter(endpointsByNode.keySet(),
                                                       new Predicate<NodeId>() {
            @Override
            public boolean apply(NodeId input) {
                Map<EgKey, Set<EpKey>> nodeEps = 
                        endpointsByNode.get(input);
                return (nodeEps != null && 
                        nodeEps.containsKey(egKey));
            }

        }));
    }
    
    /**
     * Get the endpoints in a particular group on a particular node
     * @param nodeId the node ID to look up
     * @param eg the group to look up
     * @return the endpoints
     */
    public Collection<Endpoint> getEPsForNode(NodeId nodeId, EgKey eg) {
        Map<EgKey, Set<EpKey>> nodeEps = endpointsByNode.get(nodeId);
        if (nodeEps == null) return Collections.emptyList();
        Collection<EpKey> ebn = nodeEps.get(eg); 
        if (ebn == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(Collections2
                                                  .transform(ebn, 
                                                             indexTransform));
    }

    /**
     * Get the endpoint object for the given key
     * @param epKey the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }
    
    /**
     * Set the learning mode to the specified value
     * @param learningMode the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }
    
    /**
     * Get a collection of endpoints in a particular endpoint group
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Collection<Endpoint> getEndpointsForGroup(EgKey eg) {
        Collection<EpKey> ebg = endpointsByGroup.get(eg);
        if (ebg == null) return Collections.emptyList();
        return Collections2.transform(ebg, indexTransform);
    }

    /**
     * Get the effective list of conditions that apply to a particular 
     * endpoint.  This could include additional conditions over the condition
     * labels directly represented in the endpoint object
     * @param endpoint the {@link Endpoint} to resolve
     * @return the list of {@link ConditionName}
     */
    public List<ConditionName> getCondsForEndpoint(Endpoint endpoint) {
        // XXX TODO consider group conditions as well.  Also need to notify
        // endpoint updated if the endpoint group conditions change
        if (endpoint.getCondition() != null)
            return endpoint.getCondition();
        else return Collections.emptyList();
    }
    
    // ************************
    // AbstractEndpointRegistry
    // ************************
    
    @Override
    protected EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        OfOverlayContextInput ictx = 
                input.getAugmentation(OfOverlayContextInput.class);
        return super.buildEndpoint(input)
                .addAugmentation(OfOverlayContext.class, 
                                 new OfOverlayContextBuilder(ictx).build());
    }

    @Override
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        return super.buildEndpointL3(input);
    }
    
    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null) listenerReg.close();
        super.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint)
                updateEndpoint(null, (Endpoint)dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint((Endpoint)old, null);
        }
        Map<InstanceIdentifier<?>,DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if (!(entry.getValue() instanceof Endpoint)) continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            Endpoint oldEp = null;
            if (old != null && old instanceof Endpoint)
                oldEp = (Endpoint)old;
            updateEndpoint(oldEp, (Endpoint)entry.getValue());
        }
    }
    // **************
    // Implementation
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

    private Function<EpKey, Endpoint> indexTransform = 
            new Function<EpKey, Endpoint>() {
        @Override
        public Endpoint apply(EpKey input) {
            return endpoints.get(input);
        }
    };
    
    private boolean validEp(Endpoint endpoint) {
        return (endpoint != null && endpoint.getTenant() != null && 
                endpoint.getEndpointGroup() != null &&
                endpoint.getL2Context() != null &&
                endpoint.getMacAddress() != null);
    }
    
    private NodeId getLocation(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;
        OfOverlayContext context = 
                endpoint.getAugmentation(OfOverlayContext.class);
        if (context != null)
            return context.getNodeId();

        return null;
    }
    
    private EpKey getEpKey(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;
        return new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
    }
    
    private EgKey getEgKey(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;
        return new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup());
    }
    
    private Set<EpKey> getEpNGSet(NodeId location, EgKey eg) {
        ConcurrentMap<EgKey, Set<EpKey>> map = endpointsByNode.get(location);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ConcurrentMap<EgKey, Set<EpKey>> old = 
                    endpointsByNode.putIfAbsent(location, map);
            if (old != null)
                map = old;
        }
        return SetUtils.getNestedSet(eg, map);
    }
    
    private static final ConcurrentMap<EgKey, Set<EpKey>> EMPTY_MAP =
            new ConcurrentHashMap<>();

    private Set<EpKey> getEpGSet(EgKey eg) {
        return SetUtils.getNestedSet(eg, endpointsByGroup);
    }
    
    /**
     * Update the endpoint indexes.  Set newEp to null to remove.
     */
    protected void updateEndpoint(Endpoint oldEp, Endpoint newEp) {
        // XXX TODO only keep track of endpoints that are attached 
        // to switches that are actually connected to us
        NodeId oldLoc = getLocation(oldEp);
        NodeId newLoc = getLocation(newEp);

        EgKey oldKey = getEgKey(oldEp);
        EgKey newKey = getEgKey(newEp);

        EpKey epKey = getEpKey(oldEp);
        if (epKey == null) epKey = getEpKey(newEp);
        if (epKey == null) return;

        boolean notifyOldLoc = false;
        boolean notifyNewLoc = false;
        boolean notifyOldEg = false;
        boolean notifyNewEg = false;
        
        if (newEp != null)
            endpoints.put(epKey, newEp);

        if (oldLoc != null && oldKey != null &&
            (newLoc == null || !oldLoc.equals(newLoc) ||
            newKey == null || !oldKey.equals(newKey))) {
            ConcurrentMap<EgKey, Set<EpKey>> map = 
                    endpointsByNode.get(oldLoc);
            Set<EpKey> eps = map.get(oldKey);
            eps.remove(epKey);
            map.remove(oldKey, Collections.emptySet());
            endpointsByNode.remove(oldLoc, EMPTY_MAP);
            notifyOldLoc = true;
        }
        if (oldKey != null &&
            (newKey == null || !oldKey.equals(newKey))) {
            Set<EpKey> gns = getEpGSet(oldKey);
            gns.remove(epKey);
            notifyOldEg = true;
        }

        if (newLoc != null && newKey != null) {
            Set<EpKey> eps = getEpNGSet(newLoc, newKey);
            eps.add(epKey);
            LOG.debug("Endpoint {} added to node {}", epKey, newLoc);
            notifyNewLoc = true;
        }
        if (newKey != null) {
            Set<EpKey> gns = getEpGSet(newKey);
            gns.add(epKey);
            LOG.debug("Endpoint {} added to group {}", epKey, newKey);
            notifyNewEg = true;
        }

        if (newEp == null)
            endpoints.remove(epKey);
        
        notifyEndpointUpdated(epKey);

        if (notifyOldLoc)
            notifyNodeEndpointUpdated(oldLoc,epKey);
        if (notifyNewLoc)
            notifyNodeEndpointUpdated(newLoc,epKey);
        if (notifyOldEg)
            notifyGroupEndpointUpdated(oldKey, epKey);
        if (notifyNewEg)
            notifyGroupEndpointUpdated(newKey, epKey);
    }
}
