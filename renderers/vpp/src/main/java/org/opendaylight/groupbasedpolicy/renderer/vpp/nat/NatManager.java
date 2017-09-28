/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.NatInstanceCommand.NatInstanceCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.NatAddressRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

public abstract class NatManager {

    protected final DataBroker dataBroker;

    public NatManager(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    abstract Logger getLogger();

    abstract void submit(NatInstanceCommandBuilder nat, NodeId nodeId);

    public abstract List<NodeId> resolveNodesForSnat();

    abstract public Map<NodeId, NatInstanceCommandBuilder> staticEntries(
            Table<NodeId, Long, List<MappingEntryBuilder>> staticEntries);

    public abstract void dynamicEntries(PolicyContext ctx, Map<NodeId, NatInstanceCommandBuilder> nodeBuilders);

    public void clearNodes(@Nonnull Endpoints before, @Nullable Endpoints after) {
        Function<Endpoints, Set<NodeId>> f = x -> {
            return nullToEmpty(x.getAddressEndpointWithLocation()).stream()
                .filter(ep -> ep.getAugmentation(NatAddressRenderer.class) != null)
                .filter(ep -> ep.getAbsoluteLocation() != null)
                .map(ep -> (ExternalLocationCase) ep.getAbsoluteLocation().getLocationType())
                .filter(loc -> loc.getExternalNodeMountPoint() != null)
                .map(loc -> loc.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId())
                .collect(Collectors.toSet());
        };
        Set<NodeId> nodesToClear = (after != null) ? Sets.difference(f.apply(before), f.apply(after)) : f.apply(before);
        NatInstanceCommand natCmd = new NatInstanceCommandBuilder().setOperation(General.Operations.DELETE).build();
        getLogger().info("Clearing NAT from nodes {}", nodesToClear);
        nodesToClear.forEach(nodeId -> GbpNetconfTransaction
            .netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(nodeId), natCmd, GbpNetconfTransaction.RETRY_COUNT));
    }

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public void submitNatChanges(Map<NodeId, NatInstanceCommandBuilder> nat) {
        nat.keySet().forEach(nodeId -> {
            submit(nat.get(nodeId), nodeId);
        });
    }
}
