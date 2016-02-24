/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheData;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.FlowCacheCons;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.IidSflowNameUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecordsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.ep.epg.to.ep.epg.statistic.MatchedRuleStatisticBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class ProcessDataTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessDataTask.class);

    private FlowCache flowCache;
    private BigInteger timestamp;
    private StatisticsManager statisticsManager;
    List<FlowCacheData> dataList;

    public ProcessDataTask(FlowCache flowCache, List<FlowCacheData> dataList, BigInteger timestamp,
            StatisticsManager statisticsManager) {
        this.flowCache = flowCache;
        this.dataList = dataList;
        this.timestamp = timestamp;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void run() {
        for (FlowCacheData flowCacheData : dataList) {
            Map<String, String> flowCacheDataMap = createFlowCacheDataMap(flowCacheData);
            if (flowCacheDataMap == null) {
                LOG.info("Stats are skipped for {}", flowCacheData);
                continue;
            }
            String srcIp = flowCacheDataMap.get(FlowCacheCons.Key.IP_SOURCE.get());
            String dstIp = flowCacheDataMap.get(FlowCacheCons.Key.IP_DESTINATION.get());
            EndpointL3 srcEpL3 = OFStatisticsManager.getEndpointL3ForIp(srcIp);
            EndpointL3 dstEpL3 = OFStatisticsManager.getEndpointL3ForIp(dstIp);
            if (srcEpL3 != null && dstEpL3 != null) {
                ContractId contractId = IidSflowNameUtil.resolveContractIdFromFlowCacheName(flowCache.getName());
                MatchedRuleStatisticBuilder matchedRuleStatisticBuilder = new MatchedRuleStatisticBuilder()
                .setContract(contractId)
                .setSubject(IidSflowNameUtil.resolveSubjectNameFromFlowCacheName(flowCache.getName()))
                .setMatchedRule(IidSflowNameUtil.resolveRuleNameFromFlowCacheName(flowCache.getName()))
                .setClassifier(ImmutableList
                    .of(IidSflowNameUtil.resolveClassifierNameFromFlowCacheName(flowCache.getName())));
                if (FlowCacheCons.Value.BYTES.get().equals(IidSflowNameUtil.resolveFlowCacheValue(flowCache.getName()))) {
                    matchedRuleStatisticBuilder.setByteCount(Math.round(flowCacheData.getValue()));
                } else if (FlowCacheCons.Value.FRAMES.get().equals(IidSflowNameUtil.resolveFlowCacheValue(flowCache.getName()))) {
                    matchedRuleStatisticBuilder.setPacketCount(Math.round(flowCacheData.getValue()));
                }

                Set<Pair<ConsEpgKey, ProvEpgKey>> epgsForContract = OFStatisticsManager.getEpgsForContract(contractId);
                Set<EpgKey> epgsFromSrcEp = getEpgsFromEndpoint(srcEpL3);
                Set<EpgKey> epgsFromDstEp = getEpgsFromEndpoint(dstEpL3);
                Pair<? extends EpgKey, ? extends EpgKey> leftSrcEpgRightDstEpg = getMatchingEpgs(epgsForContract, epgsFromSrcEp, epgsFromDstEp, flowCache.getDirection());
                if (leftSrcEpgRightDstEpg == null) {
                    LOG.info("Stats are skipped for {}", flowCacheData);
                    continue;
                }

                EpEpgToEpEpgStatistic epEpgToEpEpgStats = new EpEpgToEpEpgStatisticBuilder()
                    .setSrcEpg(leftSrcEpgRightDstEpg.getLeft().getEpgId())
                    .setDstEpg(leftSrcEpgRightDstEpg.getRight().getEpgId())
                    .setMatchedRuleStatistic(ImmutableList.of(matchedRuleStatisticBuilder.build()))
                    .build();

                EpToEpStatistic e2e = new EpToEpStatisticBuilder().setSrcL2c(srcEpL3.getL2Context())
                    .setSrcMacAddress(srcEpL3.getMacAddress())
                    .setSrcTenant(srcEpL3.getTenant())
                    .setDstL2c(dstEpL3.getL2Context())
                    .setDstMacAddress(dstEpL3.getMacAddress())
                    .setDstTenant(dstEpL3.getTenant())
                    .setEpEpgToEpEpgStatistic(ImmutableList.of(epEpgToEpEpgStats))
                    .setTimestamp(timestamp)
                    .build();

                StatRecords statRecords = new StatRecordsBuilder().setEpToEpStatistic(ImmutableList.of(e2e)).build();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("[sflow] writing StatRecords: {}", statRecords);
                }
                statisticsManager.writeStat(statRecords);
            }
        }
    }

    private Set<EpgKey> getEpgsFromEndpoint(EndpointL3 epL3) {
        Set<EpgKey> result = new HashSet<>();
        TenantId tenantId = epL3.getTenant();
        if (epL3.getEndpointGroup() != null) {
            result.add(new EpgKeyDto(epL3.getEndpointGroup(), tenantId));
        }
        List<EndpointGroupId> epgs = epL3.getEndpointGroups();
        if (epgs != null) {
            for (EndpointGroupId epg : epgs) {
                result.add(new EpgKeyDto(epg, tenantId));
            }
        }
        return result;
    }

    private Pair<? extends EpgKey, ? extends EpgKey> getMatchingEpgs(Set<Pair<ConsEpgKey, ProvEpgKey>> epgsForContract,
            Set<EpgKey> epgsFromSrcEp, Set<EpgKey> epgsFromDstEp, Direction direction) {
        if (direction == null || Direction.Bidirectional == direction) {
            LOG.info("The bidirectional direction is not supported.");
            return null;
        }
        for (Pair<ConsEpgKey, ProvEpgKey> epgForContract : epgsForContract) {
            ConsEpgKey consEpg = epgForContract.getLeft();
            ProvEpgKey provEpg = epgForContract.getRight();
            if (epgsFromSrcEp.contains(consEpg) && epgsFromDstEp.contains(provEpg)) {
                if (Direction.In.equals(direction)) {
                    return Pair.of(consEpg, provEpg);
                } else if (Direction.Out.equals(direction)) {
                    return Pair.of(provEpg, consEpg);
                }
            }
            if (epgsFromSrcEp.contains(provEpg) && epgsFromDstEp.contains(consEpg)) {
                if (Direction.In.equals(direction)) {
                    return Pair.of(consEpg, provEpg);
                } else if (Direction.Out.equals(direction)) {
                    return Pair.of(provEpg, consEpg);
                }
            }
        }
        LOG.info(
                "EPGs of srcEP and dstEp does not match against EPGs for contract:"
                        + "\nsrcEP EPGs: {}\ndstEP EPGs: {}\nEPGs for contract: {}",
                epgsFromSrcEp, epgsFromDstEp, epgsForContract);
        return null;
    }

    private Map<String, String> createFlowCacheDataMap(FlowCacheData flowCacheData) {
        String[] splitValues = flowCacheData.getKey().split(",");
        if (splitValues.length != flowCache.getKeyNum()) {
            LOG.error(
                    "Key names and key values lists length do not match: {} != {}. Not processing.",
                    flowCache.getKeyNum(), splitValues.length);
            return null;
        }
        Map<String, String> flowCacheDataMap = new HashMap<>();
        for (int i = 0; i < flowCache.getKeyNum(); i++) {
            flowCacheDataMap.put(flowCache.getKeyNames()[i], splitValues[i]);
        }
        return flowCacheDataMap;
    }

}
