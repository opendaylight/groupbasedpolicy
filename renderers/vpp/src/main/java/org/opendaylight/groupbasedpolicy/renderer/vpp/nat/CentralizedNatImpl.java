/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand.NatInstanceCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.LoopbackCase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

public class CentralizedNatImpl extends NatManager {

    private NodeId routingNode = null;
    private static final Logger LOG = LoggerFactory.getLogger(CentralizedNatImpl.class);
    private static final long DEFAULT_FIB = 0L;

    public CentralizedNatImpl(DataBroker dataBroker) {
        super(dataBroker);
    }

    @Override
    void submit(NatInstanceCommandBuilder builder, NodeId nodeId) {
        NatInstanceCommand nat = builder.build();
        switch (nat.getOperation()) {
            case MERGE:
            case PUT:
                GbpNetconfTransaction.netconfSyncedWrite(VppIidFactory.getNetconfNodeIid(nodeId), nat,
                        GbpNetconfTransaction.RETRY_COUNT);
                break;
            case DELETE:
                GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(nodeId), nat,
                        GbpNetconfTransaction.RETRY_COUNT);
                break;
        }
    }

    @Override
    public List<NodeId> resolveNodesForSnat() {
        if (routingNode == null) {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<Config> cfg = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    VppIidFactory.getVppRendererConfig(), rTx);
            rTx.close();
            if (!cfg.isPresent() || cfg.get().getVppEndpoint() == null) {
                return Collections.emptyList();
            } ;
            java.util.Optional<NodeId> nodeId = cfg.get()
                .getVppEndpoint()
                .stream()
                .filter(vppEp -> vppEp.getInterfaceTypeChoice() instanceof LoopbackCase)
                .filter(vppEp -> ((LoopbackCase) vppEp.getInterfaceTypeChoice()).isBvi() != null)
                .filter(vppEp -> ((LoopbackCase) vppEp.getInterfaceTypeChoice()).isBvi())
                .map(vppEp -> vppEp.getVppNodeId())
                .findFirst();
            if (!nodeId.isPresent()) {
                return Collections.emptyList();
            }
            routingNode = nodeId.get();
        }
        LOG.debug("Resolved node for SNAT: {}", routingNode);
        return ImmutableList.of(routingNode);
    }

    @Override public Map<NodeId, NatInstanceCommandBuilder> staticEntries(
        Table<NodeId, Long, List<MappingEntryBuilder>> staticEntries) {
        NatInstanceCommandBuilder builder = new NatInstanceCommandBuilder();
        builder.setOperation(General.Operations.PUT);
        staticEntries.rowKeySet().forEach(nodeId -> {
            builder.setStaticEntries(staticEntries.row(nodeId)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(x -> DEFAULT_FIB, y -> y.getValue())));
        });
        Map<NodeId, NatInstanceCommandBuilder> result = new HashMap<>();
        resolveNodesForSnat().forEach(nodeId -> {
            result.put(nodeId, builder);
            LOG.debug("Resolved static NAT entries for node {}: {}", nodeId, builder.toString());
        });
        return result;
    }

    @Override
    public void dynamicEntries(PolicyContext ctx, Map<NodeId, NatInstanceCommandBuilder> nodeBuilders) {
        List<MappingEntryBuilder> collect = nodeBuilders.values()
            .stream()
            .filter(natInstCmdBldr -> natInstCmdBldr.getStaticEntries() != null)
            .map(natInstCmdBldr -> natInstCmdBldr.getStaticEntries().values())
            .flatMap(mappingEntryBldr -> mappingEntryBldr.stream())
            .flatMap(mappingEntryBldr -> mappingEntryBldr.stream())
            .collect(Collectors.toList());
        List<ExternalIpAddressPool> dynamicEntries = NatUtil.resolveDynamicNat(ctx, collect);
        LOG.info("Resolved dynamic NAT entries in cfg version {} for node {}: {}", ctx.getPolicy().getVersion(),
                routingNode, dynamicEntries);
        if (routingNode != null && nodeBuilders.get(routingNode) != null) {
            nodeBuilders.get(routingNode).setDynamicEntries(dynamicEntries);
            return;
        } else if (routingNode != null) {
            nodeBuilders.put(routingNode,
                    new NatInstanceCommandBuilder().setOperation(Operations.PUT).setDynamicEntries(dynamicEntries));
        }
    }

    @Override
    Logger getLogger() {
        return LOG;
    }
}
