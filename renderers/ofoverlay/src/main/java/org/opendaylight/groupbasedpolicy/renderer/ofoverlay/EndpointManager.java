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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * Keep track of endpoints on the system. Maintain an index of endpoints and
 * their locations for renderering. The endpoint manager will maintain
 * appropriate indexes only for switches that are attached to the current
 * controller node.
 *
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular switch and also all the switches containing
 * each particular endpoint group
 *
 * @author readams
 */
public class EndpointManager implements AutoCloseable, DataChangeListener
{
    private static final Logger LOG =
            LoggerFactory.getLogger(EndpointManager.class);
    private final static InstanceIdentifier<Nodes> nodesIid = InstanceIdentifier
            .builder(Nodes.class).build();
    private final static InstanceIdentifier<Node> nodeIid = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).build();
    private ListenerRegistration<DataChangeListener> nodesReg;

    private static final InstanceIdentifier<Endpoint> endpointsIid =
            InstanceIdentifier.builder(Endpoints.class)
                    .child(Endpoint.class).build();
    final ListenerRegistration<DataChangeListener> listenerReg;

    private final ConcurrentHashMap<EpKey, Endpoint> endpoints =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, ConcurrentMap<EgKey, Set<EpKey>>> endpointsByGroupByNode =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, Set<EpKey>> endpointsByNode =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup =
            new ConcurrentHashMap<>();

    private List<EndpointListener> listeners = new CopyOnWriteArrayList<>();

    final private OfEndpointAug endpointRpcAug = new OfEndpointAug();

    final private ScheduledExecutorService executor;

    final private DataBroker dataProvider;

    public EndpointManager(DataBroker dataProvider,
            RpcProviderRegistry rpcRegistry,
            ScheduledExecutorService executor,
            SwitchManager switchManager) {
        this.executor = executor;
        this.dataProvider = dataProvider;
        EndpointRpcRegistry.register(dataProvider, rpcRegistry, executor, endpointRpcAug);
        if (dataProvider != null) {
            listenerReg = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                            endpointsIid,
                            this,
                            DataChangeScope.ONE);
            nodesReg = dataProvider.registerDataChangeListener(
                    LogicalDatastoreType.OPERATIONAL, nodeIid,
                    new NodesListener(), DataChangeScope.SUBTREE);

        } else
            listenerReg = null;

        LOG.debug("Initialized OFOverlay endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Add a {@link EndpointListener} to get notifications of switch events
     *
     * @param listener
     *            the {@link EndpointListener} to add
     */
    public void registerListener(EndpointListener listener) {
        listeners.add(listener);
    }

    /**
     * Get a collection of endpoints attached to a particular switch
     *
     * @param nodeId
     *            the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Set<EgKey> getGroupsForNode(NodeId nodeId) {
        Map<EgKey, Set<EpKey>> nodeEps = endpointsByGroupByNode.get(nodeId);
        if (nodeEps == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(nodeEps.keySet());
    }

    /**
     * Get the set of nodes
     *
     * @param egKey
     *            the egKey of the endpointgroup to get nodes for
     * @return a collection of {@link NodeId} objects.
     */
    public Set<NodeId> getNodesForGroup(final EgKey egKey) {
        return Collections.unmodifiableSet(Sets.filter(endpointsByGroupByNode.keySet(),
                new Predicate<NodeId>() {
                    @Override
                    public boolean apply(NodeId input) {
                        Map<EgKey, Set<EpKey>> nodeEps =
                                endpointsByGroupByNode.get(input);
                        return (nodeEps != null &&
                        nodeEps.containsKey(egKey));
                    }

                }));
    }

    /**
     * Get the endpoints in a particular group on a particular node
     *
     * @param nodeId
     *            the node ID to look up
     * @param eg
     *            the group to look up
     * @return the endpoints
     */
    public Collection<Endpoint> getEndpointsForNode(NodeId nodeId, EgKey eg) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // datastore

        Map<EgKey, Set<EpKey>> nodeEps = endpointsByGroupByNode.get(nodeId);
        if (nodeEps == null)
            return Collections.emptyList();
        Collection<EpKey> ebn = nodeEps.get(eg);
        if (ebn == null)
            return Collections.emptyList();
        return Collections.unmodifiableCollection(Collections2
                .transform(ebn,
                        indexTransform));
    }

    /**
     * Get the endpoints on a particular node
     *
     * @param nodeId
     *            the node ID to look up
     * @return the endpoints
     */
    public Collection<Endpoint> getEndpointsForNode(final NodeId nodeId) {
        // TODO: alagalah Create method findEndpointsByNode() that uses
        // datastore. See commented code below.

        Collection<Endpoint> epsByNode = Collections.emptyList();
        // Blocking for test.
        // // Predicate for filtering only the endpoints we need for this nodeID
        // //TODO: This pulls from datastore. Will be more performant to update
        // // endpointByNode in updateEndpoint.
        // Predicate<Endpoint> predicate = new Predicate<Endpoint>() {
        // @Override
        // public boolean apply(Endpoint ep) {
        // return
        // ep.getAugmentation(OfOverlayContext.class).getNodeId().getValue().equals(nodeId.getValue());
        // }
        // };
        //
        // Optional<Endpoints> epResult;
        // final InstanceIdentifier<Endpoints> endpointsIid =
        // InstanceIdentifier.builder(Endpoints.class).build();
        // try {
        // epResult =
        // dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
        // endpointsIid).get();
        // if(epResult.isPresent()) {
        // Endpoints endpoints = epResult.get();
        // epsByNode =
        // Collections2.filter((Collection<Endpoint>)endpoints.getEndpoint(),predicate);
        // }
        // } catch (InterruptedException | ExecutionException e) {
        // LOG.error("Caught exception in getEPsForNode");
        // }
        Collection<EpKey> ebn = endpointsByNode.get(nodeId);
        if (ebn == null)
            return Collections.emptyList();
        return Collections.unmodifiableCollection(Collections2
                .transform(ebn,
                        indexTransform));

    }

    /**
     * Get the endpoint object for the given key
     *
     * @param epKey
     *            the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }

    /**
     * Set the learning mode to the specified value
     *
     * @param learningMode
     *            the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }

    /**
     * Get a collection of endpoints in a particular endpoint group
     *
     * @param nodeId
     *            the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Collection<Endpoint> getEndpointsForGroup(EgKey eg) {
        Collection<EpKey> ebg = endpointsByGroup.get(eg);
        if (ebg == null)
            return Collections.emptyList();
        return Collections2.transform(ebg, indexTransform);
    }

    /**
     * Get the effective list of conditions that apply to a particular endpoint.
     * This could include additional conditions over the condition labels
     * directly represented in the endpoint object
     *
     * @param endpoint
     *            the {@link Endpoint} to resolve
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

    // ************************
    // Endpoint Augmentation
    // ************************
    private class OfEndpointAug implements EpRendererAugmentation {

        @Override
        public void buildEndpointAugmentation(EndpointBuilder eb,
                RegisterEndpointInput input) {
            // In order to support both the port-name and the data-path
            // information, allow
            // an EP to register without the augmentations, and resolve later.
            OfOverlayContextBuilder ictx = checkAugmentation(input);
            if (ictx != null) {
                eb.addAugmentation(OfOverlayContext.class, ictx.build());
            }
        }

        @Override
        public void buildEndpointL3Augmentation(EndpointL3Builder eb,
                RegisterEndpointInput input) {
        }

        @Override
        public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
            // TODO Auto-generated method stub

        }
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null)
            listenerReg.close();
        EndpointRpcRegistry.unregister(endpointRpcAug);
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint)
                updateEndpoint(null, (Endpoint) dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint((Endpoint) old, null);
        }
        Map<InstanceIdentifier<?>, DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if (!(entry.getValue() instanceof Endpoint))
                continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            Endpoint oldEp = null;
            if (old != null && old instanceof Endpoint)
                oldEp = (Endpoint) old;
            updateEndpoint(oldEp, (Endpoint) entry.getValue());
        }
    }

    // TODO: alagalah Investigate using the internal project listener structure
    // for this. ie Endpoint should listen to
    // SwitchManager updates and update the EP maps accordingly (update
    // Endpoint). Removal should include the overloaded
    // method updateEndpoint(Node node)
    private class NodesListener implements DataChangeListener {
        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            for (DataObject dao : change.getCreatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    updateEndpoint(node);
                }
            }
            for (DataObject dao : change.getUpdatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    updateEndpoint(node);
                }
            }
        }
    }

    // TODO Li alagalah move this near to other updateEndpoint()
    private void updateEndpoint(Node node) {
        final InstanceIdentifier<Endpoints> endpointsIid = InstanceIdentifier.builder(Endpoints.class).build();

        Optional<Endpoints> epResult;
        EpKey epKey = null;
        for (NodeConnector nc : node.getNodeConnector()) {
            FlowCapableNodeConnector fcnc = nc
                    .getAugmentation(FlowCapableNodeConnector.class);
            try {
                epResult = dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, endpointsIid)
                        .get();
                if (epResult.isPresent()) {
                    Endpoints endpoints = epResult.get();
                    if (endpoints.getEndpoint() != null) {
                        Boolean isEmpty = true;
                        for (Endpoint ep : endpoints.getEndpoint()) {
                            // 2. Search for portname
                            OfOverlayContext currentAugmentation = ep.getAugmentation(OfOverlayContext.class);
                            if (currentAugmentation.getPortName() != null && fcnc.getName() != null
                                    && currentAugmentation.getPortName().getValue().equals(fcnc.getName())) {
                                NodeId nodeId;
                                NodeConnectorId nodeConnectorId;
                                Name name;
                                try {
                                    nodeId = currentAugmentation.getNodeId();
                                    nodeConnectorId = currentAugmentation.getNodeConnectorId();
                                    name = currentAugmentation.getPortName();
                                } catch (Exception e) {
                                    nodeId = null;
                                    nodeConnectorId = null;
                                    name = null;
                                }
                                Boolean process = false;
                                if (nodeId == null && nodeConnectorId == null) {
                                    LOG.debug("ep NodeID and NC ID Both null");
                                    process = true;
                                }
                                if (nodeId != null && nodeConnectorId != null) {
                                    if (!(nodeConnectorId.getValue().equals(nc.getId().getValue()))) {
                                        LOG.debug("ep NodeID and NC ID Both NOT null but epNCID !=nodeNCID");
                                        process = true;
                                    }
                                }
                                if (process) {
                                    WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
                                    // 3. Update endpoint
                                    EndpointBuilder epBuilder = new EndpointBuilder(ep);
                                    OfOverlayContextBuilder ofOverlayAugmentation = new OfOverlayContextBuilder();
                                    ofOverlayAugmentation.setNodeId(node.getId());
                                    ofOverlayAugmentation.setNodeConnectorId(nc.getId());
                                    ofOverlayAugmentation.setPortName(name);
                                    epBuilder.addAugmentation(OfOverlayContext.class, ofOverlayAugmentation.build());
                                    epBuilder.setL3Address(ep.getL3Address());
                                    InstanceIdentifier<Endpoint> iidEp = InstanceIdentifier.builder(Endpoints.class)
                                            .child(Endpoint.class, ep.getKey()).build();
                                    tx.put(LogicalDatastoreType.OPERATIONAL, iidEp, epBuilder.build());
                                    tx.submit().get();
                                    epKey = new EpKey(ep.getKey().getL2Context(), ep.getKey().getMacAddress());
                                    notifyEndpointUpdated(epKey);
                                    LOG.debug("Values:");
                                    LOG.debug("node: Node ID:" + node.getId().getValue());
                                    LOG.debug("node: NodeConnectorID: " + nc.getId().getValue());
                                    if (nodeId != null && nodeConnectorId != null) {
                                        LOG.debug("ep: nodeID:" + nodeId.getValue());
                                        LOG.debug("ep: nodeConnectorID:" + nodeConnectorId.getValue());
                                    }
                                    isEmpty = false;
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Exception in UpdateEndpoint", e);
            }
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
                (endpoint.getEndpointGroup() != null || endpoint.getEndpointGroups() != null) &&
                endpoint.getL2Context() != null && endpoint.getMacAddress() != null);
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

    public EgKey getEgKey(Endpoint endpoint) {
        if (!validEp(endpoint))
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
            ConcurrentMap<EgKey, Set<EpKey>> old =
                    endpointsByGroupByNode.putIfAbsent(location, map);
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
     * Update the endpoint indexes. Set newEp to null to remove.
     */
    protected void updateEndpoint(Endpoint oldEp, Endpoint newEp) {
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

        // When newLoc and oldLoc are null there is nothing to do
        if (!(newLoc == null && oldLoc == null)) {

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
             * maintainIndex(endpointsByNode,oldEp,newEp) Maintain following
             * maps endpoints - <EpKey, Endpoint> endpointsByGroupByNode -
             * <NodeId, ConcurrentMap<EgKey, Set<EpKey>>> endpointsByNode -
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
                    if (epsNode.isEmpty())
                        endpointsByNode.remove(oldLoc);
                }
                // Update endpointsByGroupByNode
                // Update endpointsByGroup
                // Get map of EPGs and their Endpoints for Node
                ConcurrentMap<EgKey, Set<EpKey>> map =
                        endpointsByGroupByNode.get(oldLoc);
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
            if ((oldEp != null && newEp != null && oldEpKey != null && newEpKey != null) &&
                    (oldEpKey.toString().equals(newEpKey.toString()))) {
                // old and new Endpoints have same key. (same endpoint)

                /*
                 * Remove old endpoint if moved.
                 */
                if (oldLoc != null && !(oldLoc.getValue().equals(newLoc.getValue()))) {
                    // This is an endpoint that has moved, remove from old node
                    Set<EpKey> epsNode = endpointsByNode.get(oldLoc);
                    if (epsNode != null) {
                        epsNode.remove(oldEpKey);
                        if (epsNode.isEmpty())
                            endpointsByNode.remove(oldLoc);
                    }
                    // Update endpointsByGroupByNode
                    // Get map of EPGs and their Endpoints for Node
                    ConcurrentMap<EgKey, Set<EpKey>> map =
                            endpointsByGroupByNode.get(oldLoc);
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
                        if (geps != null)
                        {
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
    }

    private OfOverlayContextBuilder checkAugmentation(RegisterEndpointInput input) {
        OfOverlayContextBuilder ictxBuilder = new OfOverlayContextBuilder();
        OfOverlayContextInput ictx = null;

        ictx = input.getAugmentation(OfOverlayContextInput.class);
        if (ictx != null) {
            /*
             * In the case where they've provided just the port name,
             * go see if we can find the NodeId and NodeConnectorId
             * from inventory.
             */
            if (ictx.getPortName() != null &&
               (ictx.getNodeId() == null &&
                ictx.getNodeConnectorId() == null)) {
                NodeInfo augmentation = fetchAugmentation(ictx.getPortName().getValue());
                if (augmentation != null) {
                    ictxBuilder.setNodeId(augmentation.getNode().getId());
                    ictxBuilder.setNodeConnectorId(augmentation.getNodeConnector().getId());
                    ictxBuilder.setPortName(ictx.getPortName());
                }
            } else {
                ictxBuilder.setNodeConnectorId(ictx.getNodeConnectorId());
                ictxBuilder.setNodeId(ictx.getNodeId());
                ictxBuilder.setPortName(ictx.getPortName());
            }
        } else {
            ictxBuilder = null;
        }
        return ictxBuilder;
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

    private NodeInfo fetchAugmentation(String portName) {
        NodeInfo nodeInfo = null;

        if (dataProvider != null) {

            Optional<Nodes> result;
            try {
                result = dataProvider
                        .newReadOnlyTransaction().read(
                                LogicalDatastoreType.OPERATIONAL, nodesIid).get();
                if (result.isPresent()) {
                    Nodes nodes = result.get();
                    for (Node node : nodes.getNode()) {
                        if (node.getNodeConnector() != null) {
                            boolean found = false;
                            for (NodeConnector nc : node.getNodeConnector()) {
                                FlowCapableNodeConnector fcnc = nc
                                        .getAugmentation(FlowCapableNodeConnector.class);
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
