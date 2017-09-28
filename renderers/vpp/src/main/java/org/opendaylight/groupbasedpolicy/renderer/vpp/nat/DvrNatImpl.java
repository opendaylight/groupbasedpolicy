/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand.NatInstanceCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Table;

public class DvrNatImpl extends NatManager {

    public DvrNatImpl(DataBroker dataBroker) {
        super(dataBroker);
    }

    private static final Logger LOG = LoggerFactory.getLogger(DvrNatImpl.class);

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
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        List<InstanceIdentifier<PhysicalInterface>> intfcs = NatUtil.resolvePhysicalInterface(rTx);
        rTx.close();
        return intfcs.stream().map(NatUtil.resolveNodeId::apply).collect(Collectors.toList());
    }

    @Override
    public Map<NodeId, NatInstanceCommandBuilder> staticEntries(
            Table<NodeId, Long, List<MappingEntryBuilder>> staticEntries) {
        Map<NodeId, NatInstanceCommandBuilder> result = new HashMap<>();
        List<NodeId> nodes = resolveNodesForSnat();
        staticEntries.rowKeySet().forEach(nodeId -> {
            NatInstanceCommandBuilder builder = new NatInstanceCommandBuilder()
                .setOperation(General.Operations.PUT)
                .setStaticEntries(staticEntries.row(nodeId));
            if (nodes.contains(nodeId)) {
                result.put(nodeId, builder);
                LOG.debug("Resolved static NAT entries for node {}, NatEntries: {}.", nodeId, builder.toString());
            }
        });
        return result;
    }

    @Override
    public void dynamicEntries(PolicyContext ctx, Map<NodeId, NatInstanceCommandBuilder> nodeBuilders) {
        LOG.info("No dynamic SNAT entries resolved, not supported yet.");
    }

    @Override
    Logger getLogger() {
        return LOG;
    }
}
