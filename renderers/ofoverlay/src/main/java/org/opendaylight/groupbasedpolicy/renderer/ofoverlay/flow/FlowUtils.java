/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNshc4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNsiKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg7Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIpv4DstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._1.grouping.NxmNxNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._2.grouping.NxmNxNshc2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._3.grouping.NxmNxNshc3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nshc._4.grouping.NxmNxNshc4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.id.grouping.NxmNxTunIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.dst.grouping.NxmNxTunIpv4DstBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

/**
 * Utilities for constructing OpenFlow flows
 */
public final class FlowUtils {

    /**
     * ARP ethertype
     */
    public static final Long ARP = Long.valueOf(0x0806);
    /**
     * IPv4 ethertype
     */
    public static final Long IPv4 = Long.valueOf(0x0800);
    /**
     * IPv6 ethertype
     */
    public static final Long IPv6 = Long.valueOf(0x86DD);

    /**
     * Creates an Instance Identifier (path) for node with specified id
     *
     * @param nodeId the ID of the node
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Node> createNodePath(final NodeId nodeId) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId)).build();
    }

    /**
     * Creates a table path from a node ID and table ID
     *
     * @param nodeId the ID of the node
     * @param tableId the ID of the table
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Table> createTablePath(final NodeId nodeId, final short tableId) {
        return createNodePath(nodeId).builder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(tableId))
            .build();
    }

    /**
     * Creates a group path from a node ID and group ID
     *
     * @param nodeId the Id of the node
     * @param groupId the ID of the group table
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Group> createGroupPath(final NodeId nodeId, final GroupId groupId) {
        return createNodePath(nodeId).builder()
            .augmentation(FlowCapableNode.class)
            .child(Group.class, new GroupKey(groupId))
            .build();
    }

    public static InstanceIdentifier<Group> createGroupPath(final NodeId nodeId, final Long groupId) {
        return createGroupPath(nodeId, new GroupId(groupId));
    }

    /**
     * Creates a group path from a node ID and group ID
     *
     * @param nodeId the Id of the node
     * @param groupId the ID of the group table
     * @param bucketId the ID of the bucket in the group table
     * @return the {@link InstanceIdentifier}
     */
    public static final InstanceIdentifier<Bucket> createBucketPath(final NodeId nodeId, final GroupId groupId,
            final BucketId bucketId) {
        return createNodePath(nodeId).builder()
            .augmentation(FlowCapableNode.class)
            .child(Group.class, new GroupKey(groupId))
            .child(Buckets.class)
            .child(Bucket.class, new BucketKey(bucketId))
            .build();
    }

    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table the table iid
     * @param flowKey the flow key
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Flow> createFlowPath(final InstanceIdentifier<Table> table, final FlowKey flowKey) {
        return table.child(Flow.class, flowKey);
    }

    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table the table iid
     * @param flowId the flow id
     * @return the {@link InstanceIdentifier}
     */
    public static InstanceIdentifier<Flow> createFlowPath(final InstanceIdentifier<Table> table, final FlowId flowId) {
        return createFlowPath(table, new FlowKey(flowId));
    }

    public static Instructions gotoTableInstructions(short tableId) {
        return new InstructionsBuilder().setInstruction(
                ImmutableList.of(new InstructionBuilder().setOrder(Integer.valueOf(0))
                    .setInstruction(gotoTableIns(tableId))
                    .build())).build();
    }

    public static Instruction gotoTableIns(short tableId) {
        return new GoToTableCaseBuilder().setGoToTable(new GoToTableBuilder().setTableId(tableId).build()).build();
    }

    public static ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList(
            Action... actions) {
        ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> alist = new ArrayList<>();
        int count = 0;
        for (Action action : actions) {
            alist.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(action).build());
        }
        return alist;
    }

    public static ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList(
            List<ActionBuilder> actions) {
        ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> alist = new ArrayList<>();
        int count = 0;
        for (ActionBuilder action : actions) {
            alist.add(action.setOrder(Integer.valueOf(count++)).build());
        }
        return alist;
    }

    public static Instruction applyActionIns(Action... actions) {
        return new ApplyActionsCaseBuilder().setApplyActions(
                new ApplyActionsBuilder().setAction(actionList(actions)).build()).build();
    }

    public static Instruction applyActionIns(List<ActionBuilder> actions) {
        return new ApplyActionsCaseBuilder().setApplyActions(
                new ApplyActionsBuilder().setAction(actionList(actions)).build()).build();
    }

    public static Instruction writeActionIns(List<ActionBuilder> actions) {
        return new WriteActionsCaseBuilder().setWriteActions(
                new WriteActionsBuilder().setAction(actionList(actions)).build()).build();
    }

    public static Instruction writeActionIns(Action... actions) {
        return new WriteActionsCaseBuilder().setWriteActions(
                new WriteActionsBuilder().setAction(actionList(actions)).build()).build();
    }

    public static Instructions instructions(Instruction... instructions) {
        ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction> ins = new ArrayList<>();
        int order = 0;
        for (Instruction i : instructions) {
            ins.add(new InstructionBuilder().setOrder(order++).setInstruction(i).build());
        }
        return new InstructionsBuilder().setInstruction(ins).build();
    }

    public static Instructions dropInstructions() {
        return instructions(applyActionIns(dropAction()));
    }

    public static Action dropAction() {
        return new DropActionCaseBuilder().setDropAction(new DropActionBuilder().build()).build();
    }

    public static Action outputAction(NodeConnectorId id) {
        return new OutputActionCaseBuilder().setOutputAction(
                new OutputActionBuilder().setOutputNodeConnector(new Uri(id.getValue())).build()).build();
    }

    public static Action groupAction(Long id) {
        return new GroupActionCaseBuilder().setGroupAction(new GroupActionBuilder().setGroupId(id).build()).build();
    }

    public static Action setDlSrcAction(MacAddress mac) {
        return new SetDlSrcActionCaseBuilder().setSetDlSrcAction(new SetDlSrcActionBuilder().setAddress(mac).build())
            .build();
    }

    public static Action setDlDstAction(MacAddress mac) {
        return new SetDlDstActionCaseBuilder().setSetDlDstAction(new SetDlDstActionBuilder().setAddress(mac).build())
            .build();
    }

    public static Action setIpv4DstAction(Ipv4Address ipAddress) {
        Ipv4Builder ipDest = new Ipv4Builder();
        Ipv4Prefix prefixdst = new Ipv4Prefix(new Ipv4Prefix(ipAddress.getValue() + "/32"));
        ipDest.setIpv4Address(prefixdst);
        SetNwDstActionBuilder setNwDstActionBuilder = new SetNwDstActionBuilder();
        setNwDstActionBuilder.setAddress(ipDest.build());
        return new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstActionBuilder.build()).build();
    }

    public static Action setIpv6DstAction(Ipv6Address ipAddress) {
        Ipv6Builder ipDest = new Ipv6Builder();
        Ipv6Prefix prefixdst = new Ipv6Prefix(new Ipv6Prefix(ipAddress.getValue() + "/128"));
        ipDest.setIpv6Address(prefixdst);
        SetNwDstActionBuilder setNwDstActionBuilder = new SetNwDstActionBuilder();
        setNwDstActionBuilder.setAddress(ipDest.build());
        return new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstActionBuilder.build()).build();
    }

    public static Action setIpv4SrcAction(Ipv4Address ipAddress) {
        Ipv4Builder ipSrc = new Ipv4Builder();
        Ipv4Prefix prefixdst = new Ipv4Prefix(new Ipv4Prefix(ipAddress.getValue() + "/32"));
        ipSrc.setIpv4Address(prefixdst);
        SetNwSrcActionBuilder setNwSrcActionBuilder = new SetNwSrcActionBuilder();
        setNwSrcActionBuilder.setAddress(ipSrc.build());
        return new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwSrcActionBuilder.build()).build();
    }

    public static Action setIpv6SrcAction(Ipv6Address ipAddress) {
        Ipv6Builder ipSrc = new Ipv6Builder();
        Ipv6Prefix prefixdst = new Ipv6Prefix(new Ipv6Prefix(ipAddress.getValue() + "/128"));
        ipSrc.setIpv6Address(prefixdst);
        SetNwSrcActionBuilder setNwSrcActionBuilder = new SetNwSrcActionBuilder();
        setNwSrcActionBuilder.setAddress(ipSrc.build());
        return new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwSrcActionBuilder.build()).build();
    }

    public static Action decNwTtlAction() {
        return new DecNwTtlCaseBuilder().setDecNwTtl(new DecNwTtlBuilder().build()).build();
    }

    public static Action nxLoadRegAction(DstChoice dstChoice, BigInteger value, int endOffset, boolean groupBucket) {
        NxRegLoad r = new NxRegLoadBuilder().setDst(
                new DstBuilder().setDstChoice(dstChoice)
                    .setStart(Integer.valueOf(0))
                    .setEnd(Integer.valueOf(endOffset))
                    .build())
            .setValue(value)
            .build();
        if (groupBucket) {
            return new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder().setNxRegLoad(r).build();
        } else {
            return new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(r).build();
        }
    }

    public static Action nxSetNsiAction(Short nsi) {
        NxSetNsi newNsi = new NxSetNsiBuilder().setNsi(nsi).build();
        return new NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsi(newNsi).build();
    }

    public static Action nxSetNspAction(Long nsp) {
        NxSetNsp newNsp = new NxSetNspBuilder().setNsp(nsp).build();
        return new NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsp(newNsp).build();
    }

    public static Action nxLoadRegAction(DstChoice dstChoice, BigInteger value) {
        return nxLoadRegAction(dstChoice, value, 31, false);
    }

    public static Action nxLoadRegAction(Class<? extends NxmNxReg> reg, BigInteger value) {
        return nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(reg).build(), value);
    }

    public static Action nxLoadNshc1RegAction(Long value) {
        NxSetNshc1 newNshc1 = new NxSetNshc1Builder().setNshc(value).build();
        return new NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc1(newNshc1).build();
    }

    public static Action nxLoadNshc2RegAction(Long value) {
        NxSetNshc2 newNshc2 = new NxSetNshc2Builder().setNshc(value).build();
        return new NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc2(newNshc2).build();
    }

    public static Action nxLoadNshc3RegAction(Long value) {
        NxSetNshc3 newNshc3 = new NxSetNshc3Builder().setNshc(value).build();
        return new NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc3(newNshc3).build();
    }

    public static Action nxLoadNshc4RegAction(Long value) {
        NxSetNshc4 newNshc4 = new NxSetNshc4Builder().setNshc(value).build();
        return new NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc4(newNshc4).build();
    }

    public static Action nxLoadTunIPv4Action(String ipAddress, boolean groupBucket) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
        long ipl = ip & 0xffffffffL;
        return nxLoadRegAction(new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(ipl), 31, groupBucket);
    }

    public static Action nxLoadArpOpAction(BigInteger value) {
        return nxLoadRegAction(new DstOfArpOpCaseBuilder().setOfArpOp(Boolean.TRUE).build(), value, 15, false);
    }

    public static Action nxLoadArpShaAction(BigInteger value) {
        return nxLoadRegAction(new DstNxArpShaCaseBuilder().setNxArpSha(Boolean.TRUE).build(), value, 47, false);
    }

    public static Action nxLoadArpSpaAction(BigInteger value) {
        return nxLoadRegAction(new DstOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(), value);
    }

    public static Action nxLoadArpSpaAction(String ipAddress) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
        long ipl = ip & 0xffffffffL;
        return nxLoadArpSpaAction(BigInteger.valueOf(ipl));
    }

    public static Action nxMoveRegAction(SrcChoice srcChoice, DstChoice dstChoice, int endOffset, boolean groupBucket) {
        NxRegMove r = new NxRegMoveBuilder().setSrc(
                new SrcBuilder().setSrcChoice(srcChoice)
                    .setStart(Integer.valueOf(0))
                    .setEnd(Integer.valueOf(endOffset))
                    .build())
            .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder().setDstChoice(
                    dstChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
                .build())
            .build();
        if (groupBucket) {
            return new NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder().setNxRegMove(r).build();
        } else {
            return new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegMove(r).build();
        }
    }

    public static Action nxMoveRegAction(SrcChoice srcChoice, DstChoice dstChoice) {
        return nxMoveRegAction(srcChoice, dstChoice, 31, false);
    }

    public static Action nxMoveRegTunDstToNshc1() {
        return nxMoveRegAction(new SrcNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                new DstNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(), 31, false);
    }

    public static Action nxMoveTunIdtoNshc2() {
        return nxMoveRegAction(new SrcNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                new DstNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(), 31, false);
    }

    public static Action nxMoveRegTunIdAction(Class<? extends NxmNxReg> src, boolean groupBucket) {
        return nxMoveRegAction(new SrcNxRegCaseBuilder().setNxReg(src).build(),
                new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(), 31, groupBucket);
    }

    public static Action nxLoadTunIdAction(BigInteger tunnelId, boolean groupBucket) {
        return nxLoadRegAction(new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(), tunnelId, 31, groupBucket);
    }

    public static Action nxMoveArpShaToArpThaAction() {
        return nxMoveRegAction(new SrcNxArpShaCaseBuilder().setNxArpSha(Boolean.TRUE).build(),
                new DstNxArpThaCaseBuilder().setNxArpTha(Boolean.TRUE).build(), 47, false);
    }

    public static Action nxMoveEthSrcToEthDstAction() {
        return nxMoveRegAction(new SrcOfEthSrcCaseBuilder().setOfEthSrc(Boolean.TRUE).build(),
                new DstOfEthDstCaseBuilder().setOfEthDst(Boolean.TRUE).build(), 47, false);
    }

    public static Action nxMoveArpSpaToArpTpaAction() {
        return nxMoveRegAction(new SrcOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(),
                new DstOfArpTpaCaseBuilder().setOfArpTpa(Boolean.TRUE).build());
    }

    public static Action nxOutputRegAction(SrcChoice srcChoice) {
        NxOutputReg r = new NxOutputRegBuilder().setSrc(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.nx.output.reg.SrcBuilder().setSrcChoice(
                        srcChoice)
                    .setOfsNbits(Integer.valueOf(31))
                    .build())
            .setMaxLen(Integer.valueOf(0xffff))
            .build();
        return new NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder().setNxOutputReg(r).build();
    }

    public static Action nxOutputRegAction(Class<? extends NxmNxReg> reg) {
        return nxOutputRegAction(new SrcNxRegCaseBuilder().setNxReg(reg).build());
    }

    public static class RegMatch {

        final Class<? extends NxmNxReg> reg;
        final Long value;

        public RegMatch(Class<? extends NxmNxReg> reg, Long value) {
            super();
            this.reg = reg;
            this.value = value;
        }

        public static RegMatch of(Class<? extends NxmNxReg> reg, Long value) {
            return new RegMatch(reg, value);
        }
    }

    public static void addNxRegMatch(MatchBuilder match, RegMatch... matches) {
        ArrayList<ExtensionList> extensions = new ArrayList<>();
        for (RegMatch rm : matches) {
            Class<? extends ExtensionKey> key;
            if (NxmNxReg0.class.equals(rm.reg)) {
                key = NxmNxReg0Key.class;
            } else if (NxmNxReg1.class.equals(rm.reg)) {
                key = NxmNxReg1Key.class;
            } else if (NxmNxReg2.class.equals(rm.reg)) {
                key = NxmNxReg2Key.class;
            } else if (NxmNxReg3.class.equals(rm.reg)) {
                key = NxmNxReg3Key.class;
            } else if (NxmNxReg4.class.equals(rm.reg)) {
                key = NxmNxReg4Key.class;
            } else if (NxmNxReg5.class.equals(rm.reg)) {
                key = NxmNxReg5Key.class;
            } else if (NxmNxReg6.class.equals(rm.reg)) {
                key = NxmNxReg6Key.class;
            } else {
                key = NxmNxReg7Key.class;
            }
            NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxReg(
                    new NxmNxRegBuilder().setReg(rm.reg).setValue(rm.value).build()).build();
            extensions.add(new ExtensionListBuilder().setExtensionKey(key)
                .setExtension(new ExtensionBuilder().addAugmentation(NxAugMatchNodesNodeTableFlow.class, am).build())
                .build());
        }
        GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder().setExtensionList(
                extensions).build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNshc1RegMatch(MatchBuilder match, Long value) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNshc1(
                new NxmNxNshc1Builder().setValue(value).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNshc1Key.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNshc2RegMatch(MatchBuilder match, Long value) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNshc2(
                new NxmNxNshc2Builder().setValue(value).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNshc2Key.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNshc3RegMatch(MatchBuilder match, Long value) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNshc3(
                new NxmNxNshc3Builder().setValue(value).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNshc3Key.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNshc4RegMatch(MatchBuilder match, Long value) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNshc4(
                new NxmNxNshc4Builder().setValue(value).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNshc4Key.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxTunIdMatch(MatchBuilder match, int tunId) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxTunId(
                new NxmNxTunIdBuilder().setValue(BigInteger.valueOf(tunId)).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxTunIdKey.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxTunIpv4DstMatch(MatchBuilder match, Ipv4Address ipv4Address) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxTunIpv4Dst(
                new NxmNxTunIpv4DstBuilder().setIpv4Address(ipv4Address).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxTunIpv4DstKey.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNsiMatch(MatchBuilder match, short nsi) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNsi(
                new NxmNxNsiBuilder().setNsi(nsi).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNsiKey.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNspMatch(MatchBuilder match, Long nsp) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder().setNxmNxNsp(
                new NxmNxNspBuilder().setValue(nsp).build()).build();
        GeneralAugMatchNodesNodeTableFlow m = addExtensionKeyAugmentationMatcher(NxmNxNspKey.class, am, match);
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static EthernetMatch ethernetMatch(MacAddress srcMac, MacAddress dstMac, Long etherType) {
        EthernetMatchBuilder emb = new EthernetMatchBuilder();
        if (srcMac != null)
            emb.setEthernetSource(new EthernetSourceBuilder().setAddress(srcMac).build());
        if (dstMac != null)
            emb.setEthernetDestination(new EthernetDestinationBuilder().setAddress(dstMac).build());
        if (etherType != null)
            emb.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(etherType)).build());
        return emb.build();
    }

    private static List<ExtensionList> getExistingGeneralAugMatchNodesNodeTableFlow(MatchBuilder match) {
        ArrayList<ExtensionList> extensions = new ArrayList<>();
        if (match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class) != null) {
            List<ExtensionList> existingExtensions = match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)
                .getExtensionList();
            if (existingExtensions != null && !existingExtensions.isEmpty()) {
                extensions.addAll(existingExtensions);
            }
        }
        return extensions;
    }

    private static GeneralAugMatchNodesNodeTableFlow addExtensionKeyAugmentationMatcher(
            Class<? extends ExtensionKey> key, NxAugMatchNodesNodeTableFlow am, MatchBuilder match) {
        List<ExtensionList> extensions = getExistingGeneralAugMatchNodesNodeTableFlow(match);
        extensions.add(new ExtensionListBuilder().setExtensionKey(key)
            .setExtension(new ExtensionBuilder().addAugmentation(NxAugMatchNodesNodeTableFlow.class, am).build())
            .build());
        return new GeneralAugMatchNodesNodeTableFlowBuilder().setExtensionList(extensions).build();
    }

    /**
     * Parse an OF port number from a node connector ID
     *
     * @param id the ID
     * @return the port number
     */
    public static long getOfPortNum(NodeConnectorId id) {
        String cnid = id.getValue();
        int ci = cnid.lastIndexOf(':');
        if (ci < 0 || (ci + 1 >= cnid.length()))
            throw new NumberFormatException("Invalid node connector ID " + cnid);
        return Long.parseLong(cnid.substring(ci + 1));
    }
}
