/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.EncapsulationFormat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * Manage connected switches and ensure their configuration is set up
 * correctly
 */
public class SwitchManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchManager.class);

    protected Map<NodeId, SwitchState> switches = new HashMap<>();
    protected List<SwitchListener> listeners = new CopyOnWriteArrayList<>();

    private final FlowCapableNodeListener nodeListener;
    private final OfOverlayNodeListener ofOverlayNodeListener;
    private final FlowCapableNodeConnectorListener nodeConnectorListener;

    public SwitchManager(DataBroker dataProvider) {
        if (dataProvider == null) {
            LOG.warn("No data provider for {}. Listeners {}, {}, {} are not registered.",
                    SwitchManager.class.getSimpleName(), FlowCapableNodeListener.class.getSimpleName(),
                    OfOverlayNodeListener.class.getSimpleName(), FlowCapableNodeConnectorListener.class.getSimpleName());
            nodeListener = null;
            ofOverlayNodeListener = null;
            nodeConnectorListener = null;
        } else {
            nodeListener = new FlowCapableNodeListener(dataProvider, this);
            ofOverlayNodeListener = new OfOverlayNodeListener(dataProvider, this);
            nodeConnectorListener = new FlowCapableNodeConnectorListener(dataProvider, this);
        }
        LOG.debug("Initialized OFOverlay switch manager");
    }

    /**
     * Get the collection of switches that are in the "ready" state. Note
     * that the collection is immutable.
     *
     * @return A {@link Collection} containing the switches that are ready.
     */
    public synchronized Collection<NodeId> getReadySwitches() {
        ImmutableList<NodeId> readySwitches = FluentIterable.from(switches.values())
            .filter(new Predicate<SwitchState>() {

                @Override
                public boolean apply(SwitchState input) {
                    return input.status == SwitchStatus.READY;
                }
            })
            .transform(new Function<SwitchState, NodeId>() {

                @Override
                public NodeId apply(SwitchState input) {
                    return input.nodeId;
                }
            })
            .toList();
        LOG.trace("Get ready switches: {}", readySwitches);
        return readySwitches;
    }

    public synchronized Set<NodeConnectorId> getExternalPorts(NodeId nodeId) {
        SwitchState state = switches.get(nodeId);
        if (state == null)
            return Collections.emptySet();
        return ImmutableSet.copyOf(state.externalPorts);
    }

    public synchronized NodeConnectorId getTunnelPort(NodeId nodeId, Class<? extends TunnelTypeBase> tunnelType) {
        SwitchState state = switches.get(nodeId);
        if (state == null) {
            return null;
        }
        TunnelBuilder tunnel = state.tunnelBuilderByType.get(tunnelType);
        if (tunnel == null) {
            return null;
        }
        return tunnel.getNodeConnectorId();
    }

    public synchronized IpAddress getTunnelIP(NodeId nodeId, Class<? extends TunnelTypeBase> tunnelType) {
        SwitchState state = switches.get(nodeId);
        if (state == null) {
            return null;
        }
        TunnelBuilder tunnel = state.tunnelBuilderByType.get(tunnelType);
        if (tunnel == null) {
            return null;
        }
        return tunnel.getIp();
    }

    /**
     * Add a {@link SwitchListener} to get notifications of switch events
     *
     * @param listener the {@link SwitchListener} to add
     */
    public void registerListener(SwitchListener listener) {
        listeners.add(listener);
    }

    /**
     * Set the encapsulation format the specified value
     *
     * @param format The new format
     */
    public void setEncapsulationFormat(EncapsulationFormat format) {
        // No-op for now
    }

    synchronized void updateSwitch(NodeId nodeId, @Nullable FlowCapableNode fcNode) {
        SwitchState state = getSwitchState(checkNotNull(nodeId));
        SwitchStatus oldStatus = state.status;
        state.setFlowCapableNode(fcNode);
        handleSwitchState(state, oldStatus);
    }

    synchronized void updateSwitchNodeConnectorConfig(InstanceIdentifier<NodeConnector> ncIid,
            @Nullable FlowCapableNodeConnector fcnc) {
        NodeId nodeId = ncIid.firstKeyOf(Node.class, NodeKey.class).getId();
        SwitchState state = getSwitchState(nodeId);
        SwitchStatus oldStatus = state.status;
        state.setNodeConnectorConfig(ncIid, fcnc);
        handleSwitchState(state, oldStatus);
    }

    synchronized void updateSwitchConfig(NodeId nodeId, @Nullable OfOverlayNodeConfig config) {
        SwitchState state = getSwitchState(checkNotNull(nodeId));
        SwitchStatus oldStatus = state.status;
        state.setConfig(config);
        handleSwitchState(state, oldStatus);
    }

    private SwitchState getSwitchState(NodeId id) {
        SwitchState state = switches.get(id);
        if (state == null) {
            state = new SwitchState(id);
            switches.put(id, state);
            LOG.trace("Switch {} added to switches {}", state.nodeId.getValue(), switches.keySet());
        }
        return state;
    }

    private void handleSwitchState(SwitchState state, SwitchStatus oldStatus) {
        if (oldStatus == SwitchStatus.READY && state.status != SwitchStatus.READY) {
            LOG.info("Switch {} removed", state.nodeId.getValue());
            notifySwitchRemoved(state.nodeId);
        } else if (oldStatus != SwitchStatus.READY && state.status == SwitchStatus.READY) {
            LOG.info("Switch {} ready", state.nodeId.getValue());
            notifySwitchReady(state.nodeId);
        } else if (oldStatus == SwitchStatus.READY && state.status == SwitchStatus.READY) {
            // TODO Be msunal we could improve this by ignoring of updates where uninteresting fields are changed
            LOG.debug("Switch {} updated", state.nodeId.getValue());
            notifySwitchUpdated(state.nodeId);
        }
        if (state.status == SwitchStatus.DISCONNECTED && state.isConfigurationEmpty()) {
            switches.remove(state.nodeId);
            LOG.trace("Switch {} removed from switches {}", state.nodeId, switches.keySet());
        }
    }

    private void notifySwitchRemoved(NodeId nodeId) {
        for (SwitchListener listener : listeners) {
            listener.switchRemoved(nodeId);
        }
    }

    private void notifySwitchReady(NodeId nodeId) {
        for (SwitchListener listener : listeners) {
            listener.switchReady(nodeId);
        }
    }

    private void notifySwitchUpdated(NodeId nodeId) {
        for (SwitchListener listener : listeners) {
            listener.switchUpdated(nodeId);
        }
    }

    @Override
    public void close() throws Exception {
        nodeListener.close();
        ofOverlayNodeListener.close();
        nodeConnectorListener.close();
    }

    /**
     * Internal representation of the state of a connected switch
     */
    protected static final class SwitchState {

        private NodeId nodeId;
        private FlowCapableNode fcNode;
        private OfOverlayNodeConfig nodeConfig;
        private Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = Maps.newHashMap();

        Map<Class<? extends TunnelTypeBase>, TunnelBuilder> tunnelBuilderByType = new HashMap<>();
        Set<NodeConnectorId> externalPorts = new HashSet<>();

        SwitchStatus status;

        public SwitchState(NodeId switchNode) {
            super();
            nodeId = switchNode;
        }

        /**
         * Constructor used for tests
         */
        public SwitchState(NodeId node, NodeConnectorId tunnelPort, Set<NodeConnectorId> externalPorts,
                OfOverlayNodeConfig nodeConfig) {
            this.nodeId = node;
            this.nodeConfig = nodeConfig;
            update();
            this.externalPorts = externalPorts;
        }

        private void update() {
            tunnelBuilderByType = new HashMap<>();
            externalPorts = new HashSet<>();
            for (Entry<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIidEntry : fcncByNcIid.entrySet()) {
                FlowCapableNodeConnector fcnc = fcncByNcIidEntry.getValue();
                if (fcnc.getName() == null) {
                    continue;
                }
                InstanceIdentifier<NodeConnector> ncIid = fcncByNcIidEntry.getKey();
                NodeConnectorId ncId = ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
                if (fcnc.getName().matches(".*(vxlan).*")) {
                    TunnelBuilder tunnelBuilder = tunnelBuilderByType.get(TunnelTypeVxlan.class);
                    if (tunnelBuilder == null) {
                        tunnelBuilder = new TunnelBuilder().setTunnelType(TunnelTypeVxlan.class);
                        tunnelBuilderByType.put(TunnelTypeVxlan.class, tunnelBuilder);
                    }
                    tunnelBuilder.setNodeConnectorId(ncId);
                }
                if (nodeConfig != null && nodeConfig.getExternalInterfaces() != null) {
                    for (String pattern : nodeConfig.getExternalInterfaces()) {
                        if (fcnc.getName().matches(pattern)) {
                            externalPorts.add(ncId);
                            break;
                        }
                    }
                }
            }
            if (nodeConfig != null && nodeConfig.getTunnel() != null) {
                for (Tunnel tunnel : nodeConfig.getTunnel()) {
                    TunnelBuilder tunnelBuilder = tunnelBuilderByType.get(tunnel.getTunnelType());
                    if (tunnelBuilder == null) {
                        tunnelBuilder = new TunnelBuilder();
                        tunnelBuilderByType.put(tunnel.getTunnelType(), tunnelBuilder);
                    }
                    if (tunnel.getIp() != null) {
                        tunnelBuilder.setIp(tunnel.getIp());
                    }
                    if (tunnel.getNodeConnectorId() != null) {
                        tunnelBuilder.setNodeConnectorId(tunnel.getNodeConnectorId());
                    }
                    if (tunnel.getPort() != null) {
                        tunnelBuilder.setPort(tunnel.getPort());
                    }
                }
            }
        }

        private void updateStatus() {
            boolean tunnelWithIpAndNcExists = tunnelWithIpAndNcExists();
            if (fcNode != null) {
                if (tunnelWithIpAndNcExists) {
                    setStatus(SwitchStatus.READY);
                } else {
                    setStatus(SwitchStatus.PREPARING);
                }
            } else {
                setStatus(SwitchStatus.DISCONNECTED);
            }
        }

        private void setStatus(SwitchStatus newStatus) {
            if (Objects.equal(status, newStatus)) {
                return;
            }
            LOG.debug("Switch {} is changing status from {} to {}", nodeId.getValue(), this.status, newStatus);
            this.status = newStatus;
        }

        private boolean tunnelWithIpAndNcExists() {
            if (tunnelBuilderByType.isEmpty()) {
                LOG.trace("No tunnel on switch {}", nodeId.getValue());
                return false;
            }
            LOG.trace("Iterating over tunnel till tunnel with IP and node-connector is not found.");
            for (TunnelBuilder tb : tunnelBuilderByType.values()) {
                if (tb.getIp() != null && tb.getNodeConnectorId() != null) {
                    LOG.trace("Tunnel found. Type: {} IP: {} Port: {} Node-connector: {}", tb.getTunnelType()
                        .getSimpleName(), tb.getIp(), tb.getPort(), tb.getNodeConnectorId());
                    return true;
                } else {
                    LOG.trace("Tunnel which is not completed: Type: {} IP: {} Port: {} Node-connector: {}",
                            tb.getTunnelType().getSimpleName(), tb.getIp(), tb.getPort(), tb.getNodeConnectorId());
                }
            }
            return false;
        }

        public boolean isConfigurationEmpty() {
            if (fcNode != null)
                return false;
            if (nodeConfig != null)
                return false;
            if (!fcncByNcIid.isEmpty())
                return false;
            return true;
        }

        public void setFlowCapableNode(FlowCapableNode fcNode) {
            this.fcNode = fcNode;
            LOG.trace("Switch {} set {}", nodeId.getValue(), fcNode);
            updateStatus();
        }

        public void setConfig(OfOverlayNodeConfig config) {
            this.nodeConfig = config;
            LOG.trace("Switch {} set {}", nodeId.getValue(), config);
            update();
            updateStatus();
        }

        public void setNodeConnectorConfig(InstanceIdentifier<NodeConnector> ncIid, FlowCapableNodeConnector fcnc) {
            if (fcnc == null) {
                fcncByNcIid.remove(ncIid);
            } else {
                fcncByNcIid.put(ncIid, fcnc);
            }
            LOG.trace("Switch {} node connector {} set {}", nodeId.getValue(),
                    ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue(), fcnc);
            update();
            updateStatus();
        }

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

}
