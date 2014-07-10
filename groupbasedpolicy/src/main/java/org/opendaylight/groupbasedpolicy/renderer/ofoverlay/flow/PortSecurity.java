package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.HashMap;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage the table that enforces port security
 * @author readams
 */
public class PortSecurity extends FlowTable {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(PortSecurity.class);
    
    public static final short TABLE_ID = 0;
    private static final Long ARP = Long.valueOf(0x0806);
    private static final Long IPv4 = Long.valueOf(0x0800);
    private static final Long IPv6 = Long.valueOf(0x86DD);
    
    public PortSecurity(FlowTableCtx ctx) {
        super(ctx);
    }
    
    @Override
    public Table getEmptyTable() {
        return new TableBuilder()
            .setId(Short.valueOf((short)TABLE_ID))
            .build();
    }
    
    @Override
    public void update(NodeId nodeId, Dirty dirty) throws Exception {
        ReadWriteTransaction t = ctx.dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Table> tiid = 
                FlowUtils.createTablePath(nodeId, TABLE_ID);
        Optional<DataObject> r = 
                t.read(LogicalDatastoreType.CONFIGURATION, tiid).get();

        HashMap<String, FlowCtx> flowMap = new HashMap<>();

        if (r.isPresent()) {
            Table curTable = (Table)r.get();

            if (curTable.getFlow() != null) {
                for (Flow f : curTable.getFlow()) {
                    flowMap.put(f.getId().getValue(), new FlowCtx(f));
                }
            }
        }
        
        dropFlow(t, tiid, flowMap, 1, null);
        dropFlow(t, tiid, flowMap, 110, ARP);
        dropFlow(t, tiid, flowMap, 111, IPv4);
        dropFlow(t, tiid, flowMap, 113, IPv6);

        for (Endpoint e : ctx.endpointManager.getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc != null && ofc.getNodeConnectorId() != null) {
                l3flow(t, tiid, flowMap, e, ofc, 120, false);
                l3flow(t, tiid, flowMap, e, ofc, 121, true);
                l2flow(t, tiid, flowMap, e, ofc, 100);
            }
        }
        
        for (FlowCtx fx : flowMap.values()) {
            if (!fx.visited) {
                t.delete(LogicalDatastoreType.CONFIGURATION,
                         FlowUtils.createFlowPath(tiid, fx.f.getKey()));
            }
        }
        
        ListenableFuture<RpcResult<TransactionStatus>> result = t.commit();
        Futures.addCallback(result, updateCallback);
    }
    
    private static FlowBuilder base() {
        return new FlowBuilder()
            .setTableId(TABLE_ID)
            .setBarrier(false)
            .setHardTimeout(0)
            .setIdleTimeout(0)
            .setInstructions(FlowUtils.gotoTable((short)(TABLE_ID + 1)));
    }
    
    private void dropFlow(ReadWriteTransaction t,
                          InstanceIdentifier<Table> tiid,
                          HashMap<String, FlowCtx> flowMap,
                          Integer priority, Long etherType) {
        FlowId flowid = new FlowId(new StringBuilder()
            .append("drop|")
            .append(etherType)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            Flow flow = base()
                .setId(flowid)
                .setPriority(priority)
                .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(null, null, 
                                                                  etherType))
                        .build())
                 .setInstructions(FlowUtils.dropInstructions())
                .build();
            LOG.trace("{} {}", flow.getId(), flow);
            t.put(LogicalDatastoreType.CONFIGURATION, 
                  FlowUtils.createFlowPath(tiid, flowid), 
                  flow);
        }
    }
    
    private void l2flow(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        HashMap<String, FlowCtx> flowMap,
                        Endpoint e, OfOverlayContext ofc,
                        Integer priority) {
        FlowId flowid = new FlowId(new StringBuilder()
            .append(e.getMacAddress().getValue())
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            FlowBuilder flowb = base()
                .setPriority(priority)
                    .setId(flowid)
                    .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(e.getMacAddress(), 
                                                                  null, null))
                        .setInPort(ofc.getNodeConnectorId())
                        .build());

            Flow flow = flowb.build();
            LOG.trace("{} {}", flow.getId(), flow);
            t.put(LogicalDatastoreType.CONFIGURATION, 
                  FlowUtils.createFlowPath(tiid, flowid), 
                  flow);
        }
    }
    

    private void l3flow(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        HashMap<String, FlowCtx> flowMap,
                        Endpoint e, OfOverlayContext ofc,
                        Integer priority,
                        boolean arp) {
        if (e.getL3Address() == null) return;
        for (L3Address l3 : e.getL3Address()) {
            if (l3.getIpAddress() == null) continue;
            Layer3Match m = null;
            Long etherType = null;
            String ikey = null;
            if (l3.getIpAddress().getIpv4Address() != null) {
                ikey = l3.getIpAddress().getIpv4Address().getValue();
                if (arp) {
                    m = new ArpMatchBuilder()
                        .setArpSourceTransportAddress(new Ipv4Prefix(ikey))
                        .build();
                    etherType = ARP;
                } else {
                    m = new Ipv4MatchBuilder()
                        .setIpv4Source(new Ipv4Prefix(ikey))
                        .build();
                    etherType = IPv4;
                }
            } else if (l3.getIpAddress().getIpv6Address() != null) {
                if (arp) continue;
                ikey = l3.getIpAddress().getIpv6Address().getValue();
                m = new Ipv6MatchBuilder()
                    .setIpv6Source(new Ipv6Prefix(ikey))
                    .build();
                etherType = IPv6;
            } else {
                continue;
            }
            FlowId flowid = new FlowId(new StringBuilder()
                .append(e.getMacAddress().getValue())
                .append("|")
                .append(ikey)
                .append("|")
                .append(etherType)
                .toString());
            if (visit(flowMap, flowid.getValue())) {
                Flow flow = base()
                    .setPriority(priority)
                    .setId(flowid)
                    .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(e.getMacAddress(), 
                                                                  null, 
                                                                  etherType))
                        .setLayer3Match(m)
                        .setInPort(ofc.getNodeConnectorId())
                        .build())
                    .build();
                LOG.trace("{} {}", flow.getId(), flow);

                t.put(LogicalDatastoreType.CONFIGURATION, 
                      FlowUtils.createFlowPath(tiid, flowid), 
                      flow);
            }
        }
    }
    
    private static boolean visit(HashMap<String, FlowCtx> flowMap, 
                                 String flowId) {
        FlowCtx c = flowMap.get(flowId);
        if (c != null) {
            c.visited = true;
            return false;
        }
        return true;
    }
    
    private static class FlowCtx {
        Flow f;
        boolean visited = false;

        public FlowCtx(Flow f) {
            super();
            this.f = f;
        }
    }
}
