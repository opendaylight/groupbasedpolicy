/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OfTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination.DestinationMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.egressnat.EgressNatMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.external.ExternalMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.ingressnat.IngressNatMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer.PolicyEnforcer;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity.PortSecurity;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.source.SourceMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.SingletonTask;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Manage policies on switches by subscribing to updates from the
 * policy resolver and information about endpoints from the endpoint
 * registry
 */
public class PolicyManager
        implements SwitchListener, EndpointListener, ClusteredDataTreeChangeListener<ResolvedPolicy>, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManager.class);

    private Map<InstanceIdentifier<Table>, TableBuilder> previousGbpFlows  = new HashMap<>();

    private short tableOffset;
    private static final short TABLEID_PORTSECURITY = 0;
    private static final short TABLEID_INGRESS_NAT = 1;
    private static final short TABLEID_SOURCE_MAPPER = 2;
    private static final short TABLEID_DESTINATION_MAPPER = 3;
    private static final short TABLEID_POLICY_ENFORCER = 4;
    private static final short TABLEID_EGRESS_NAT = 5;
    private static final short TABLEID_EXTERNAL_MAPPER = 6;
    private static final short TABLEID_SFC_INGRESS = 7;
    private static final short TABLEID_SFC_EGRESS = 0;

    private final SwitchManager switchManager;
    private final EndpointManager endpointManager;

    private final ListenerRegistration<PolicyManager> registerDataTreeChangeListener;

    private final ScheduledExecutorService executor;
    private final SingletonTask flowUpdateTask;
    private final DataBroker dataBroker;

    /**
     * The delay before triggering the flow update task in response to an
     * event in milliseconds.
     */
    private final static int FLOW_UPDATE_DELAY = 250;

    public PolicyManager(DataBroker dataBroker, SwitchManager switchManager, EndpointManager endpointManager,
            ScheduledExecutorService executor, short tableOffset) {
        super();
        this.switchManager = switchManager;
        this.executor = executor;
        this.dataBroker = dataBroker;
        this.tableOffset = tableOffset;
        try {
            // to validate against model
            verifyMaxTableId(tableOffset);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to start OF-Overlay renderer\n."
                    + "Max. table ID would be out of range. Check config-subsystem.\n{}", e);
        }

        if (dataBroker != null) {
            registerDataTreeChangeListener = dataBroker.registerDataTreeChangeListener(
                    new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                            InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build()),
                    this);
        } else {
            registerDataTreeChangeListener = null;
            LOG.error("DataBroker is null. Listener for {} was not registered.",
                    ResolvedPolicy.class.getCanonicalName());
        }
        if (switchManager != null)
            switchManager.registerListener(this);
        this.endpointManager = endpointManager;
        endpointManager.registerListener(this);

        if (!setSfcTableOffset(TABLEID_SFC_INGRESS, TABLEID_SFC_EGRESS)) {
            LOG.error("Could not set SFC Ingress Table offset.");
        }
        flowUpdateTask = new SingletonTask(executor, new FlowUpdateTask());
        scheduleUpdate();

        LOG.debug("Initialized OFOverlay policy manager");
    }

    private boolean setSfcTableOffset(short tableidSfcIngress, short tableidSfcEgress) {
        SfcOfRendererConfig sfcOfRendererConfig = new SfcOfRendererConfigBuilder()
            .setSfcOfTableOffset(tableidSfcIngress).setSfcOfAppEgressTableOffset(tableidSfcEgress).build();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, SfcIidFactory.sfcOfRendererConfigIid(), sfcOfRendererConfig);
        return DataStoreHelper.submitToDs(wTx);
    }

    private List<? extends OfTable> createFlowPipeline(OfContext ofCtx) {
        // TODO - PORTSECURITY is kept in table 0.
        // According to openflow spec,processing on vSwitch always starts from table 0.
        // Packets will be droped if table 0 is empty.
        // Alternative workaround - table-miss flow entries in table 0.
        return ImmutableList.of(new PortSecurity(ofCtx, (short) 0), new GroupTable(ofCtx),
                new IngressNatMapper(ofCtx, getTABLEID_INGRESS_NAT()),
                new SourceMapper(ofCtx, getTABLEID_SOURCE_MAPPER()),
                new DestinationMapper(ofCtx, getTABLEID_DESTINATION_MAPPER()),
                new PolicyEnforcer(ofCtx, getTABLEID_POLICY_ENFORCER()),
                new EgressNatMapper(ofCtx, getTABLEID_EGRESS_NAT()),
                new ExternalMapper(ofCtx, getTABLEID_EXTERNAL_MAPPER()));
    }

    /**
     * @param tableOffset the new offset value
     * @return {@link ListenableFuture} to indicate that tables have been synced
     */
    public ListenableFuture<Void> changeOpenFlowTableOffset(final short tableOffset) {
        try {
            verifyMaxTableId(tableOffset);
        } catch (IllegalArgumentException e) {
            LOG.error("Cannot update table offset. Max. table ID would be out of range.\n{}", e);
            // TODO - invalid offset value remains in conf DS
            // It's not possible to validate offset value by using constrains in model,
            // because number of tables in pipeline varies.
            return Futures.immediateFuture(null);
        }
        List<Short> tableIDs = getTableIDs();
        this.tableOffset = tableOffset;
        return Futures.transform(removeUnusedTables(tableIDs), new Function<Void, Void>() {

            @Override
            public Void apply(Void tablesRemoved) {
                scheduleUpdate();
                return null;
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * @param tableIDs - IDs of tables to delete
     * @return ListenableFuture<Void> - which will be filled when clearing is done
     */
    private ListenableFuture<Void> removeUnusedTables(final List<Short> tableIDs) {
        List<ListenableFuture<Void>> checkList = new ArrayList<>();
        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        for (Short tableId : tableIDs) {
            for (NodeId nodeId : switchManager.getReadySwitches()) {
                final InstanceIdentifier<Table> tablePath = FlowUtils.createTablePath(nodeId, tableId);
                checkList.add(deleteTableIfExists(rwTx, tablePath));
            }
        }
        ListenableFuture<List<Void>> allAsListFuture = Futures.allAsList(checkList);
        return Futures.transformAsync(allAsListFuture, new AsyncFunction<List<Void>, Void>() {

            @Override
            public ListenableFuture<Void> apply(List<Void> readyToSubmit) {
                return rwTx.submit();
            }
        }, MoreExecutors.directExecutor());
    }

    private List<Short> getTableIDs() {
        List<Short> tableIds = new ArrayList<>();
        tableIds.add(getTABLEID_PORTSECURITY());
        tableIds.add(getTABLEID_INGRESS_NAT());
        tableIds.add(getTABLEID_SOURCE_MAPPER());
        tableIds.add(getTABLEID_DESTINATION_MAPPER());
        tableIds.add(getTABLEID_POLICY_ENFORCER());
        tableIds.add(getTABLEID_EGRESS_NAT());
        tableIds.add(getTABLEID_EXTERNAL_MAPPER());
        return tableIds;
    }

    private ListenableFuture<Void> deleteTableIfExists(final ReadWriteTransaction rwTx,
            final InstanceIdentifier<Table> tablePath) {
        return Futures.transform(rwTx.read(LogicalDatastoreType.CONFIGURATION, tablePath),
                new Function<Optional<Table>, Void>() {

                    @Override
                    public Void apply(Optional<Table> optTable) {
                        if (optTable.isPresent()) {
                            rwTx.delete(LogicalDatastoreType.CONFIGURATION, tablePath);
                        }
                        return null;
                    }
                }, MoreExecutors.directExecutor());
    }

    // **************
    // SwitchListener
    // **************

    public short getTABLEID_PORTSECURITY() {
        return (short) (tableOffset + TABLEID_PORTSECURITY);
    }

    public short getTABLEID_INGRESS_NAT() {
        return (short) (tableOffset + TABLEID_INGRESS_NAT);
    }

    public short getTABLEID_SOURCE_MAPPER() {
        return (short) (tableOffset + TABLEID_SOURCE_MAPPER);
    }

    public short getTABLEID_DESTINATION_MAPPER() {
        return (short) (tableOffset + TABLEID_DESTINATION_MAPPER);
    }

    public short getTABLEID_POLICY_ENFORCER() {
        return (short) (tableOffset + TABLEID_POLICY_ENFORCER);
    }

    public short getTABLEID_EGRESS_NAT() {
        return (short) (tableOffset + TABLEID_EGRESS_NAT);
    }

    public short getTABLEID_EXTERNAL_MAPPER() {
        return (short) (tableOffset + TABLEID_EXTERNAL_MAPPER);
    }

    public short getTABLEID_SFC_EGRESS() {
        return TABLEID_SFC_EGRESS;
    }

    public short getTABLEID_SFC_INGRESS() {
        return TABLEID_SFC_INGRESS;
    }

    public TableId verifyMaxTableId(short tableOffset) {
        return new TableId((short) (tableOffset + TABLEID_EXTERNAL_MAPPER));
    }

    @Override
    public void switchReady(final NodeId nodeId) {
        scheduleUpdate();
    }

    @Override
    public void switchRemoved(NodeId sw) {
        // XXX TODO purge switch flows
        scheduleUpdate();
    }

    @Override
    public void switchUpdated(NodeId sw) {
        scheduleUpdate();
    }

    // ****************
    // EndpointListener
    // ****************

    @Override
    public void endpointUpdated(EpKey epKey) {
        scheduleUpdate();
    }

    @Override
    public void nodeEndpointUpdated(NodeId nodeId, EpKey epKey) {
        scheduleUpdate();
    }

    @Override
    public void groupEndpointUpdated(EgKey egKey, EpKey epKey) {
        // TODO a renderer should remove followed-EPG and followed-tenant at some point
        if (dataBroker == null) {
            LOG.error("DataBroker is null. Cannot write followed-epg {}", epKey);
            return;
        }
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        FollowedEndpointGroup followedEpg = new FollowedEndpointGroupBuilder().setId(egKey.getEgId()).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.followedEndpointgroupIid(OFOverlayRenderer.RENDERER_NAME,
                egKey.getTenantId(), egKey.getEgId()), followedEpg, true);
        DataStoreHelper.submitToDs(wTx);
        scheduleUpdate();
    }

    // **************
    // ClusteredDataTreeChangeListener<ResolvedPolicy>
    // **************

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ResolvedPolicy>> changes) {
        scheduleUpdate();
    }

    // *************
    // PolicyManager
    // *************

    /**
     * Set the learning mode to the specified value
     *
     * @param learningMode the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }

    // **************
    // Implementation
    // **************

    private void scheduleUpdate() {
        if (switchManager != null) {
            LOG.trace("Scheduling flow update task");
            flowUpdateTask.reschedule(FLOW_UPDATE_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Update the flows on a particular switch
     */
    private class SwitchFlowUpdateTask implements Callable<Void> {

        private final OfWriter ofWriter;

        public SwitchFlowUpdateTask(OfWriter ofWriter) {
            this.ofWriter = ofWriter;
        }

        @Override
        public Void call() throws Exception {
            OfContext ofCtx = new OfContext(dataBroker, PolicyManager.this, switchManager, endpointManager, executor);
            if (ofCtx.getCurrentPolicy() == null)
                return null;
            List<? extends OfTable> flowPipeline = createFlowPipeline(ofCtx);
            for (OfTable table : flowPipeline) {
                try {
                    for (Endpoint endpoint : endpointManager.getEndpoints()) {
                        if (switchManager.getReadySwitches().contains(endpointManager.getEndpointNodeId(endpoint))) {
                            table.sync(endpoint, ofWriter);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to write Openflow table {}", table.getClass().getSimpleName(), e);
                }
            }

            return null;
        }
    }

    /**
     * Update all flows on all switches as needed. Note that this will block
     * one of the threads on the executor.
     */
    private class FlowUpdateTask implements Runnable {

        @Override
        public void run() {
            LOG.debug("Beginning flow update task");

            CompletionService<Void> ecs = new ExecutorCompletionService<>(executor);

            OfWriter ofWriter = new OfWriter();

            SwitchFlowUpdateTask swut = new SwitchFlowUpdateTask(ofWriter);
            ecs.submit(swut);

            try {
                ecs.take().get();
                // Current gbp flow must be independent, find out where this run() ends,
                // set flows to one field and reset another
                Map<InstanceIdentifier<Table>, TableBuilder> actualGbpFlows = new HashMap<>();
                actualGbpFlows.putAll(ofWriter.commitToDataStore(dataBroker, previousGbpFlows));
                previousGbpFlows = actualGbpFlows;
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to update flow tables", e);
            }
            LOG.debug("Flow update completed");
        }
    }

    @Override
    public void close() throws IOException {
        if (registerDataTreeChangeListener != null)
            registerDataTreeChangeListener.close();
        // TODO unregister classifier and action instance validators
    }

}
