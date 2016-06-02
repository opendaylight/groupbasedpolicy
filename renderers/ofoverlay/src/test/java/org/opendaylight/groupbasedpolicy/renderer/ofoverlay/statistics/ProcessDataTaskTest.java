/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheData;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.FlowCacheCons;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.IidSflowNameUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OFStatisticsManager.class, IidSflowNameUtil.class, OFStatisticsManager.class})
public class ProcessDataTaskTest {

    private final String IP_PROTO = "6";
    private final String SRC_IP = "192.168.35.2";
    private final String DST_IP = "192.168.36.2";
    private final TenantId tenantId = new TenantId("tenantId");
    private final EndpointGroupId srcEpgId = new EndpointGroupId("srcEpgId");
    private final EndpointGroupId dstEpgId = new EndpointGroupId("dstEpgId");
    private final ContractId contractId = new ContractId("contractId");
    private final ClassifierName classifierName = new ClassifierName("classifierName");

    private FlowCache flowCache;
    private FlowCacheData data;
    private ProcessDataTask task;

    private EndpointL3 srcEpL3;
    private EndpointL3 dstEpL3;
    private EndpointGroup srcEpg;
    private EndpointGroup dstEpg;

    @Before
    public void init() {
        PowerMockito.mockStatic(OFStatisticsManager.class);
        PowerMockito.mockStatic(IidSflowNameUtil.class);
        PowerMockito.mockStatic(OFStatisticsManager.class);

        String[] keyNames = {FlowCacheCons.Key.IP_PROTOCOL.get(), FlowCacheCons.Key.IP_SOURCE.get(),
                FlowCacheCons.Key.IP_DESTINATION.get()};
        flowCache = mock(FlowCache.class);
        when(flowCache.getKeyNum()).thenReturn(3);
        when(flowCache.getKeyNames()).thenReturn(keyNames);
        when(flowCache.getName()).thenReturn("flowcache1");
        when(flowCache.getDirection()).thenReturn(HasDirection.Direction.Out);
        data = mock(FlowCacheData.class);
        when(data.getKey()).thenReturn(IP_PROTO + "," + SRC_IP + "," + DST_IP);
        List<FlowCacheData> dataList = new ArrayList<>();
        dataList.add(data);
        BigInteger timestamp = BigInteger.ZERO;
        StatisticsManager statisticsManager = mock(StatisticsManager.class);
        when(statisticsManager.writeStat(any(StatRecords.class))).thenReturn(true);

        task = new ProcessDataTask(flowCache, dataList, timestamp, statisticsManager);

        srcEpg = new EndpointGroupBuilder().setId(srcEpgId).build();
        dstEpg = new EndpointGroupBuilder().setId(dstEpgId).build();
        srcEpL3 = new EndpointL3Builder().setTenant(tenantId).setEndpointGroup(srcEpg.getId()).build();
        dstEpL3 = new EndpointL3Builder().setTenant(tenantId).setEndpointGroup(dstEpg.getId()).build();
        ConsEpgKey consEpgKey = new EpgKeyDto(srcEpg.getId(), tenantId);
        ProvEpgKey provEpgKey = new EpgKeyDto(dstEpg.getId(), tenantId);
        Pair<ConsEpgKey, ProvEpgKey> pair = Pair.of(consEpgKey, provEpgKey);
        Set<Pair<ConsEpgKey, ProvEpgKey>> epgsForContract = new HashSet<>();
        epgsForContract.add(pair);

        when(OFStatisticsManager.getEpgsForContract(contractId)).thenReturn(epgsForContract);
    }

    @Test
    public void testRun() {
        when(OFStatisticsManager.getEndpointL3ForIp(SRC_IP)).thenReturn(srcEpL3);
        when(OFStatisticsManager.getEndpointL3ForIp(DST_IP)).thenReturn(dstEpL3);
        when(IidSflowNameUtil.resolveContractIdFromFlowCacheName(flowCache.getName())).thenReturn(contractId);
        when(IidSflowNameUtil.resolveClassifierNameFromFlowCacheName(flowCache.getName())).thenReturn(classifierName);
        when(IidSflowNameUtil.resolveFlowCacheValue(flowCache.getName())).thenReturn(FlowCacheCons.Value.BYTES.get());

        task.run();
    }

    @Test
    public void testRun_reversedConsProv() {
        when(OFStatisticsManager.getEndpointL3ForIp(SRC_IP)).thenReturn(srcEpL3);
        when(OFStatisticsManager.getEndpointL3ForIp(DST_IP)).thenReturn(dstEpL3);
        when(IidSflowNameUtil.resolveContractIdFromFlowCacheName(flowCache.getName())).thenReturn(contractId);
        when(IidSflowNameUtil.resolveClassifierNameFromFlowCacheName(flowCache.getName())).thenReturn(classifierName);
        when(IidSflowNameUtil.resolveFlowCacheValue(flowCache.getName())).thenReturn(FlowCacheCons.Value.FRAMES.get());

        ConsEpgKey consEpgKey = new EpgKeyDto(dstEpg.getId(), tenantId);
        ProvEpgKey provEpgKey = new EpgKeyDto(srcEpg.getId(), tenantId);
        Pair<ConsEpgKey, ProvEpgKey> pair = Pair.of(consEpgKey, provEpgKey);
        Set<Pair<ConsEpgKey, ProvEpgKey>> epgsForContract = new HashSet<>();
        epgsForContract.add(pair);
        when(OFStatisticsManager.getEpgsForContract(contractId)).thenReturn(epgsForContract);

        task.run();
    }

    @Test
    public void testRun_noConsProv() {
        when(OFStatisticsManager.getEndpointL3ForIp(SRC_IP)).thenReturn(srcEpL3);
        when(OFStatisticsManager.getEndpointL3ForIp(DST_IP)).thenReturn(dstEpL3);
        when(IidSflowNameUtil.resolveContractIdFromFlowCacheName(flowCache.getName())).thenReturn(contractId);
        when(IidSflowNameUtil.resolveClassifierNameFromFlowCacheName(flowCache.getName())).thenReturn(classifierName);
        when(IidSflowNameUtil.resolveFlowCacheValue(flowCache.getName())).thenReturn(FlowCacheCons.Value.FRAMES.get());

        Set<Pair<ConsEpgKey, ProvEpgKey>> epgsForContract = new HashSet<>();
        when(OFStatisticsManager.getEpgsForContract(contractId)).thenReturn(epgsForContract);

        task.run();
    }

    @Test
    public void testRun_wrongDataResponse() {
        when(data.getKey()).thenReturn("1,2");
        task.run();
    }

}
