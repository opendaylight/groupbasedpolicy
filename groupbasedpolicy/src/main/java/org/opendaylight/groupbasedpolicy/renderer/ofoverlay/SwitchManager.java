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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.EncapsulationFormat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage connected switches and ensure their configuration is set up
 * correctly
 * @author readams
 */
public class SwitchManager implements AutoCloseable {
    private static final Logger LOG = 
            LoggerFactory.getLogger(SwitchManager.class);

    private final DataBroker dataProvider;

    private final static InstanceIdentifier<Nodes> nodesIid =
            InstanceIdentifier.builder(Nodes.class).build();
    private final static InstanceIdentifier<Node> nodeIid =
            InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
    private ListenerRegistration<DataChangeListener> nodesReg;
    private ListenerRegistration<DataChangeListener> nodesConfigReg;

    protected ConcurrentHashMap<NodeId, SwitchState> switches = 
            new ConcurrentHashMap<>();
    protected List<SwitchListener> listeners = new CopyOnWriteArrayList<>();

    public SwitchManager(DataBroker dataProvider,
                         ScheduledExecutorService executor) {
        super();
        this.dataProvider = dataProvider;
        if (dataProvider != null) {
            nodesReg = dataProvider
                .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, 
                                            nodeIid, new NodesListener(), 
                                            DataChangeScope.SUBTREE);
            nodesConfigReg = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, 
                                                nodeIid, new NodesConfigListener(), 
                                                DataChangeScope.SUBTREE);
        }
        readSwitches();
        LOG.debug("Initialized OFOverlay switch manager");
    }

    // *************
    // SwitchManager
    // *************

    /**
     * Get the collection of switches that are in the "ready" state.  Note
     * that the collection may be concurrently modified
     * @return A {@link Collection} containing the switches that are ready.
     */
    public Collection<NodeId> getReadySwitches() {
        Collection<SwitchState> ready =
                Collections2.filter(switches.values(),
                            new Predicate<SwitchState>() {
                    @Override
                    public boolean apply(SwitchState input) {
                        return SwitchStatus.READY.equals(input.status);
                    }
                });
        return Collections2.transform(ready,
                                      new Function<SwitchState, NodeId>() {
            @Override
            public NodeId apply(SwitchState input) {
                return input.nodeId;
            }
        });
    }

    /**
     * Check whether the specified switch is in the ready state
     * @param nodeId the node
     * @return <code>true</code> if the switch is in the ready state
     */
    public boolean isSwitchReady(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state == null) return false;
        return SwitchStatus.READY.equals(state.status);
    }
    
    public Set<NodeConnectorId> getExternalPorts(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state == null) return Collections.emptySet();
        return state.externalPorts;
    }
    
    public NodeConnectorId getTunnelPort(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state == null) return null;
        return state.tunnelPort;
    }
    
    public IpAddress getTunnelIP(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state == null || state.nodeConfig == null) return null;
        return state.nodeConfig.getTunnelIp();
    }

    /**
     * Add a {@link SwitchListener} to get notifications of switch events
     * @param listener the {@link SwitchListener} to add
     */
    public void registerListener(SwitchListener listener) {
        listeners.add(listener);
    }

    /**
     * Set the encapsulation format the specified value
     * @param format The new format
     */
    public void setEncapsulationFormat(EncapsulationFormat format) {
        // No-op for now
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        nodesReg.close();
        nodesConfigReg.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    private class NodesListener implements DataChangeListener {

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, 
                                                       DataObject> change) {
            for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
                DataObject old = change.getOriginalData().get(iid);
                if (old != null && old instanceof Node) {
                    removeSwitch(((Node)old).getId());
                }
            }

            for (DataObject dao : change.getCreatedData().values()) {
                updateSwitch(dao);
            }
            for (DataObject dao : change.getUpdatedData().values()) {
                updateSwitch(dao);
            }
        }
    }
    
    private class NodesConfigListener implements DataChangeListener {

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, 
                                                       DataObject> change) {
            readSwitches();
        }
    }

    // **************
    // Implementation
    // **************

    private SwitchState getSwitchState(NodeId id) {
        SwitchState state = switches.get(id); 
        if (state == null) {
            state = new SwitchState(id);
            SwitchState old = 
                    switches.putIfAbsent(id, state);
            if (old != null)
                state = null;
        }
        return state;
    }
    
    private void updateSwitch(DataObject dao) {
        if (!(dao instanceof Node)) return;
        // Switches are registered as Nodes in the inventory; OpenFlow switches
        // are of type FlowCapableNode
        Node node = (Node)dao;
        FlowCapableNode fcn = node.getAugmentation(FlowCapableNode.class);
        if (fcn == null) return;

        LOG.debug("{} update", node.getId());

        SwitchState state = getSwitchState(node.getId());

        state.setNode(node);
        
        if (SwitchStatus.DISCONNECTED.equals(state.status))
            switchConnected(node.getId());
        else if (SwitchStatus.READY.equals(state.status))
            notifySwitchUpdated(node.getId());
    }
    
    private void updateSwitchConfig(NodeId nodeId, OfOverlayNodeConfig config) {
        SwitchState state = getSwitchState(nodeId);
        state.setConfig(config);
        notifySwitchUpdated(nodeId);
    }

    private void notifySwitchUpdated(NodeId nodeId) {
        for (SwitchListener listener : listeners) {
            listener.switchUpdated(nodeId);
        }
    }

    // XXX there's a race condition here if a switch exists at startup and is
    // removed very quickly.
    private final FutureCallback<Optional<Nodes>> readSwitchesCallback =
            new FutureCallback<Optional<Nodes>>() {
        @Override
        public void onSuccess(Optional<Nodes> result) {
            if (result.isPresent() && result.get() instanceof Nodes) {
                Nodes nodes = (Nodes)result.get();
                for (Node node : nodes.getNode()) {
                    updateSwitch(node);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.error("Count not read switch information", t);
        }
    };

    private final FutureCallback<Optional<Nodes>> readSwitchConfCallback =
            new FutureCallback<Optional<Nodes>>() {
        @Override
        public void onSuccess(Optional<Nodes> result) {
            if (result.isPresent()) {
                Nodes nodes = (Nodes)result.get();
                for (Node node : nodes.getNode()) {
                    OfOverlayNodeConfig config = 
                            node.getAugmentation(OfOverlayNodeConfig.class);
                    if (config != null)
                        updateSwitchConfig(node.getId(), config);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.error("Count not read switch information", t);
        }
    };

    /**
     * Read the set of switches from the ODL inventory and update our internal
     * map.
     *
     * <p>This is safe only if there can only be one notification at a time,
     * as there are race conditions in the face of concurrent data change
     * notifications
     */
    private void readSwitches() {
        if (dataProvider != null) {
            ListenableFuture<Optional<Nodes>> future = 
                    dataProvider.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.CONFIGURATION, nodesIid);
            Futures.addCallback(future, readSwitchConfCallback);
            
            future = dataProvider.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.OPERATIONAL, nodesIid);
            Futures.addCallback(future, readSwitchesCallback);
        }
    }

    /**
     * Set the ready state of the node to PREPARING and begin the initialization
     * process
     */
    private void switchConnected(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state != null) {
            // XXX - TODO - For now we just go straight to ready state.
            // need to configure tunnels and tables as needed
            switchReady(nodeId);
            LOG.info("New switch {} connected", nodeId);
        }
    }

    /**
     * Set the ready state of the node to READY and notify listeners
     */
    private void switchReady(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state != null) {
            state.status = SwitchStatus.READY;
            for (SwitchListener listener : listeners) {
                listener.switchReady(nodeId);
            }
        }
    }

    /**
     * Remove the switch from the switches we're keeping track of and
     * notify listeners
     */
    private void removeSwitch(NodeId nodeId) {
        switches.remove(nodeId);
        for (SwitchListener listener : listeners) {
            listener.switchRemoved(nodeId);
        }
        LOG.info("Switch {} removed", nodeId);
    }

    protected enum SwitchStatus {
        /**
         * The switch is not currently connected
         */
        DISCONNECTED,
        /**
         * The switch is connected but not yet configured
         */
        PREPARING,
        /**
         * The switch is ready to for policy rules to be installed
         */
        READY
    }

    /**
     * Internal representation of the state of a connected switch
     */
    protected static class SwitchState {
        NodeId nodeId;

        Node switchNode;
        OfOverlayNodeConfig nodeConfig;
        
        NodeConnectorId tunnelPort;
        Set<NodeConnectorId> externalPorts = Collections.emptySet();

        SwitchStatus status = SwitchStatus.DISCONNECTED;
        
        public SwitchState(NodeId switchNode) {
            super();
            nodeId = switchNode;
        }

        /**
         * Constructor used for tests
         */
        public SwitchState(NodeId node,
                           NodeConnectorId tunnelPort,
                           Set<NodeConnectorId> externalPorts,
                           OfOverlayNodeConfig nodeConfig) {
            this.nodeId = node;
            this.tunnelPort = tunnelPort;
            this.externalPorts = externalPorts;
            this.nodeConfig = nodeConfig;
        }
        
        private void update() {
            if (switchNode == null) return;
            FlowCapableNode fcn = 
                    switchNode.getAugmentation(FlowCapableNode.class);
            if (fcn == null) return;
            
            List<NodeConnector> ports = switchNode.getNodeConnector();
            HashSet<NodeConnectorId> externalPorts = new HashSet<>();
            if (ports != null) {
                for (NodeConnector nc : ports) {
                    FlowCapableNodeConnector fcnc = 
                            nc.getAugmentation(FlowCapableNodeConnector.class);
                    if (fcnc == null || fcnc.getName() == null) continue;

                    if (fcnc.getName().matches(".*_(vxlan|tun)\\d+")) {
                        tunnelPort = nc.getId();
                    }
                    if (nodeConfig != null && nodeConfig.getExternalInterfaces() != null ) {
                        for (String pattern : nodeConfig.getExternalInterfaces()) {
                            if (fcnc.getName().matches(pattern))
                                externalPorts.add(nc.getId());
                        }
                    }
                }
            }
            this.externalPorts = Collections.unmodifiableSet(externalPorts);
        }
        
        public void setNode(Node switchNode) {
            this.switchNode = switchNode;
            update();
        }
    
        public void setConfig(OfOverlayNodeConfig config) {
            nodeConfig = config;
            update();
        }
    }

}
