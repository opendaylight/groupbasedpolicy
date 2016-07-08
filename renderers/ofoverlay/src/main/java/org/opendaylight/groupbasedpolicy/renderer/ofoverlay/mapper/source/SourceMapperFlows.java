/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.source;

import com.google.common.base.Preconditions;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

import java.math.BigInteger;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

public class SourceMapperFlows {

    private final NodeId nodeId;
    private final short tableId;

    public SourceMapperFlows(NodeId nodeId, short tableId) {
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.tableId = tableId;
    }

    /**
     * Default flow which drops all traffic
     *
     * @param priority  of flow in the table
     * @param etherType can be set as specific protocol to match
     * @param ofWriter  flow writer
     */
    void dropFlow(int priority, Long etherType, OfWriter ofWriter) {
        FlowId flowId;
        FlowBuilder flowBuilder = FlowUtils.base(tableId)
                .setPriority(priority)
                .setInstructions(FlowUtils.dropInstructions());
        if (etherType != null) {
            MatchBuilder matchBuilder = new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType));
            Match match = matchBuilder.build();
            flowId = FlowIdUtils.newFlowId(tableId, "drop", match);
            flowBuilder.setMatch(match);
        } else {
            flowId = FlowIdUtils.newFlowId("dropAll");
        }
        flowBuilder.setId(flowId);
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Load endpoint ordinals to registers and set tunnel ID value (when exists). Traffic is redirected to destination
     * mapper
     *
     * @param goToTable       table ID for {@link GoToTable} instruction
     * @param priority        of the flow
     * @param ordinals        ordinals of the {@link Endpoint}
     * @param macAddress      of the endpoint
     * @param nodeConnectorId of endpoint openflow port
     * @param ofWriter        flow writer
     */
    public void synchronizeEp(short goToTable, int priority, OrdinalFactory.EndpointFwdCtxOrdinals ordinals,
                              MacAddress macAddress, NodeConnectorId nodeConnectorId, OfWriter ofWriter) {
        int egId = ordinals.getEpgId();
        int bdId = ordinals.getBdId();
        int fdId = ordinals.getFdId();
        int l3Id = ordinals.getL3Id();
        int cgId = ordinals.getCgId();
        int tunnelId = ordinals.getTunnelId();

        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(cgId));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Action tunIdAction = nxLoadTunIdAction(BigInteger.valueOf(tunnelId), false);

        Match match = new MatchBuilder().setEthernetMatch(ethernetMatch(macAddress, null, null))
                .setInPort(nodeConnectorId)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "ep", match);
        FlowBuilder flowBuilder = base(tableId).setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(
                        instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg, tunIdAction),
                                gotoTableIns(goToTable)));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Writes remote tunnel flows. Is evaluated, for which nodes this one acts as a remote node. Then using their
     * ordinals, tunnel flow is written on this node
     *
     * @param goToTable      table ID for {@link GoToTable} instruction
     * @param priority       of the flow
     * @param tunnel         port number
     * @param remoteOrdinals ordinals of remote node
     * @param ofWriter       flow writer
     */
    public void createTunnelFlow(short goToTable, int priority, NodeConnectorId tunnel,
                                 OrdinalFactory.EndpointFwdCtxOrdinals remoteOrdinals, OfWriter ofWriter) {
        int egId = remoteOrdinals.getEpgId();
        int bdId = remoteOrdinals.getBdId();
        int fdId = remoteOrdinals.getFdId();
        int l3Id = remoteOrdinals.getL3Id();
        int tunnelId = remoteOrdinals.getTunnelId();

        MatchBuilder matchBuilder = new MatchBuilder().setInPort(tunnel);
        addNxTunIdMatch(matchBuilder, tunnelId);

        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0xffffff));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "tunnel", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(
                        instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg), gotoTableIns(goToTable)));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Writes remote broadcast flows. Is evaluated, for which nodes this one acts as a remote node. Then using their
     * ordinals, broadcast flow is written on this node
     *
     * @param goToTable      table ID for {@link GoToTable} instruction
     * @param priority       of the flow
     * @param tunnel         port number
     * @param remoteOrdinals ordinals of remote node
     * @param ofWriter       flow writer
     */
    public void createBroadcastFlow(short goToTable, int priority, NodeConnectorId tunnel,
                                    OrdinalFactory.EndpointFwdCtxOrdinals remoteOrdinals, OfWriter ofWriter) {
        int fdId = remoteOrdinals.getFdId();

        MatchBuilder matchBuilder = new MatchBuilder().setInPort(tunnel);
        addNxTunIdMatch(matchBuilder, fdId);

        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));

        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "tunnelFdId", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(instructions(applyActionIns(fdReg), gotoTableIns(goToTable)));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }
}
