/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.AbstractEndpointRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
        // In order to support both the port-name and the data-path information, allow
        // an EP to register without the augmentations, and resolve later.
        OfOverlayContextBuilder ictx = checkAugmentation(input);
            if(ictx == null) {
                return super.buildEndpoint(input);
            } else {
                return super.buildEndpoint(input).addAugmentation(OfOverlayContext.class, ictx.build());
            }
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

    private class NodesListener implements DataChangeListener {
        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            for (DataObject dao : change.getCreatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    executor.execute(new UpdateEndpoint(node));
                    return;
                }
            }
            for (DataObject dao : change.getUpdatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    executor.execute(new UpdateEndpoint(node));
                    return;
                }
            }
        }
    }

    private class UpdateEndpoint implements Runnable {
        private final Node node;
        private final InstanceIdentifier<Endpoints> endpointsIid;

        public UpdateEndpoint(Node node) {
            this.node = node;
            this.endpointsIid=InstanceIdentifier.builder(Endpoints.class).build();
        }
        @Override
        public void run() {
            Optional<Endpoints> epResult;
            EpKey epKey=null;
            for (NodeConnector nc : node.getNodeConnector()) {
                FlowCapableNodeConnector fcnc = nc
                        .getAugmentation(FlowCapableNodeConnector.class);
                try {
                    epResult = dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, endpointsIid).get();
                    if(epResult.isPresent()) {
                        Endpoints endpoints = epResult.get();
                        if(endpoints.getEndpoint() != null) {
                            WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
                            Boolean isEmpty = true;
                            for (Endpoint ep : endpoints.getEndpoint()){
                                // 2. Search for portname
                                OfOverlayContext currentAugmentation = ep.getAugmentation(OfOverlayContext.class);
                                if(ep.getPortName().getValue().equals(fcnc.getName())) {
                                    NodeId nodeId;
                                    NodeConnectorId nodeConnectorId;
                                    try {
                                        nodeId=currentAugmentation.getNodeId();
                                        nodeConnectorId=currentAugmentation.getNodeConnectorId();
                                    } catch (Exception e) {
                                        nodeId = null;
                                        nodeConnectorId = null;
                                    }
                                    Boolean process=false;
                                    if(nodeId==null && nodeConnectorId ==null) {
                                        LOG.debug("ep NodeID and NC ID Both null");
                                        process=true;
                                    }
                                    if(nodeId!=null && nodeConnectorId !=null) {
                                        if (!(nodeConnectorId.getValue().equals(nc.getId().getValue()))) {
                                            LOG.debug("ep NodeID and NC ID Both NOT null but epNCID !=nodeNCID");
                                            process=true;
                                        }
                                    }
                                    if(process) {
                                        // 3. Update endpoint
                                        EndpointBuilder epBuilder = new EndpointBuilder(ep);
                                        OfOverlayContextBuilder ofOverlayAugmentation = new OfOverlayContextBuilder();
                                        ofOverlayAugmentation.setNodeId(node.getId());
                                        ofOverlayAugmentation.setNodeConnectorId(nc.getId());
                                        epBuilder.addAugmentation(OfOverlayContext.class,ofOverlayAugmentation.build());
                                        //TODO Hack to remove:
                                        List<L3Address> l3Addresses= new ArrayList<>();
                                        for(L3Address l3Address: ep.getL3Address()) {
                                            L3AddressBuilder l3AB = new L3AddressBuilder();
                                            l3AB.setIpAddress(l3Address.getIpAddress()).setL3Context(l3Address.getL3Context());
                                            l3Addresses.add(l3AB.build());
                                        }
                                        epBuilder.setL3Address(l3Addresses);
                                        InstanceIdentifier<Endpoint> iidEp = InstanceIdentifier.builder(Endpoints.class).child(Endpoint.class,ep.getKey()).build();
                                        tx.put(LogicalDatastoreType.OPERATIONAL, iidEp, epBuilder.build());
                                        epKey=new EpKey(ep.getKey().getL2Context(),ep.getKey().getMacAddress());
                                        LOG.debug("Values:");
                                        LOG.debug("node: Node ID:"+node.getId().getValue());
                                        LOG.debug("node: NodeConnectorID: "+nc.getId().getValue());
                                        if(nodeId!=null && nodeConnectorId != null) {
                                            LOG.debug("ep: nodeID:"+nodeId.getValue());
                                            LOG.debug("ep: nodeConnectorID:"+nodeConnectorId.getValue());
                                        }
                                        isEmpty=false;
                                    }
                                }
                            }
                            if(!isEmpty) {
                                CheckedFuture<Void, TransactionCommitFailedException> f = tx.submit();
                                notifyEndpointUpdated(epKey);
                                Futures.addCallback(f, new FutureCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        LOG.error("Could not over-write endpoint with augmentation", t);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        LOG.debug("Success over-writing endpoint augmentation");
                                    }
                                });
                            } else {
                                LOG.debug("UpdateEndpoint: Empty list");
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    LOG.warn("Caught exception in UpdateEndpoint");
                }
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

    private OfOverlayContextBuilder checkAugmentation(RegisterEndpointInput input) {
        OfOverlayContextBuilder ictxBuilder=new OfOverlayContextBuilder();
        OfOverlayContextInput ictx =null;

        ictx = input.getAugmentation(OfOverlayContextInput.class);
        if(ictx!=null) {
            ictxBuilder.setNodeConnectorId(ictx.getNodeConnectorId());
            ictxBuilder.setNodeId(ictx.getNodeId());
        } else {
            NodeInfo augmentation = fetchAugmentation(input.getPortName().getValue());
            if(augmentation != null) {
                ictxBuilder.setNodeId(augmentation.getNode().getId());
                ictxBuilder.setNodeConnectorId(augmentation.getNodeConnector().getId());
            }
        }
        return ictxBuilder;
    }

    // A wrapper class around node, noeConnector info so we can pass a final
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
        NodeInfo nodeInfo=null;

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
                                    nodeInfo=new NodeInfo();
                                    nodeInfo.setNode(node);
                                    nodeInfo.setNodeConnector(nc);
                                    found=true;
                                    break;
                                }
                            }
                            if(found) break;
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
         
        }
        return nodeInfo;
    }
}
