/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockEndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockPolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowCtx;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowTableCtx;
import org.opendaylight.groupbasedpolicy.resolver.MockPolicyResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

import static org.mockito.Mockito.*;

public class FlowTableTest {
    FlowTableCtx ctx;
    FlowTable table;
    MockEndpointManager endpointManager;
    MockPolicyResolver policyResolver;
    MockPolicyManager policyManager;
    
    NodeId nodeId = new NodeId("openflow:1");
    InstanceIdentifier<Table> tiid;
    
    L3ContextId l3c = new L3ContextId("2cf51ee4-e996-467e-a277-2d380334a91d");
    L2BridgeDomainId bd = new L2BridgeDomainId("c95182ba-7807-43f8-98f7-6c7c720b7639");
    L2FloodDomainId fd = new L2FloodDomainId("98e1439e-52d2-46f8-bd69-5136e6088771");
    SubnetId sub = new SubnetId("4fcf8dfc-53b5-4aef-84d3-6b5586992fcb");
    TenantId tid = new TenantId("1118c691-8520-47ad-80b8-4cf5e3fe3302");
    EndpointGroupId eg = new EndpointGroupId("36dec84a-08c7-497b-80b6-a0035af72a12");
    
    public void initCtx() {
        endpointManager = new MockEndpointManager();
        policyResolver = new MockPolicyResolver();
        policyManager = new MockPolicyManager(policyResolver, endpointManager);
        ctx = new FlowTableCtx(null, 
                               null, 
                               policyManager, 
                               policyResolver, 
                               null, 
                               endpointManager, 
                               null);
    }
    
    public void setup() throws Exception {
        tiid = FlowUtils.createTablePath(nodeId, 
                                         table.getTableId());
    }

    public TenantBuilder baseTenant() {
        return new TenantBuilder()
            .setId(tid)
            .setEndpointGroup(ImmutableList.of(new EndpointGroupBuilder()
                .setId(eg)
                .setNetworkDomain(sub)
                .build()))
            .setL3Context(ImmutableList.of(new L3ContextBuilder()
                .setId(l3c)
                .build()))
            .setL2BridgeDomain(ImmutableList.of(new L2BridgeDomainBuilder()
                .setId(bd)
                .setParent(l3c)
                .build()))
            .setL2FloodDomain(ImmutableList.of(new L2FloodDomainBuilder()
                .setId(fd)
                .setParent(bd)
                .build()))
            .setSubnet(ImmutableList.of(new SubnetBuilder()
                .setId(sub)
                .setParent(fd)
                .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.1/24")))
                .build()));
    }

    public EndpointBuilder baseEP() {
        OfOverlayContext ofc = new OfOverlayContextBuilder()
            .setNodeId(nodeId)
            .setNodeConnectorId(new NodeConnectorId(nodeId.getValue() + ":1"))
            .build();
        return new EndpointBuilder()
            .addAugmentation(OfOverlayContext.class, ofc)
            .setL2Context(bd)
            .setTenant(tid)
            .setEndpointGroup(eg)
            .setMacAddress(new MacAddress("00:00:00:00:00:01"));
    }
    
    public ReadWriteTransaction dosync(Map<String, FlowCtx> flowMap) 
              throws Exception {
        ReadWriteTransaction t = mock(ReadWriteTransaction.class);
        if (flowMap == null)
            flowMap = Collections.emptyMap();
        table.sync(t, tiid, flowMap, nodeId, null);
        return t;
    }

}
