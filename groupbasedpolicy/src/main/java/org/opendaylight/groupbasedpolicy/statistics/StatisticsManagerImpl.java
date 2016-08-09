/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecordsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatisticKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatisticKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.ep.epg.to.ep.epg.statistic.MatchedRuleStatistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.ep.epg.to.ep.epg.statistic.MatchedRuleStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.ep.epg.to.ep.epg.statistic.MatchedRuleStatisticKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.RecordId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.StatisticsStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.StatisticsStoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.dst.ep.fields.DstEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.source.ep.fields.SrcEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.StatisticRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.StatisticRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.StatisticRecordKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.statistic.record.Statistic;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.statistic.record.StatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.statistic.record.StatisticKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class StatisticsManagerImpl implements StatisticsManager, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsManagerImpl.class);
    private static long recordKey = 0;
    private DataBroker dataBroker;

    public StatisticsManagerImpl(DataBroker broker) {
        this.dataBroker = broker;
        inicializeStatistics();
    }

    private void inicializeStatistics() {
        InstanceIdentifier<StatisticsStore> statsIID = InstanceIdentifier.builder(StatisticsStore.class).build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        LOG.debug("Initalizing statistics");
        wtx.put(LogicalDatastoreType.OPERATIONAL, statsIID, new StatisticsStoreBuilder().build());
        wtx.submit();
    }

    @Override
    public boolean writeStat(StatRecords record) {
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        for (EpToEpStatistic epStats : record.getEpToEpStatistic()) {
            SrcEndpointBuilder srcBuilder = new SrcEndpointBuilder();
            DstEndpointBuilder dstBuilder = new DstEndpointBuilder();
            srcBuilder.setMacAddress(epStats.getSrcMacAddress())
                    .setL2Context(epStats.getSrcL2c())
                    .setTenant(epStats.getSrcTenant());
            dstBuilder.setMacAddress(epStats.getDstMacAddress())
                    .setL2Context(epStats.getDstL2c())
                    .setTenant(epStats.getDstTenant());
            for (EpEpgToEpEpgStatistic epgStats : epStats.getEpEpgToEpEpgStatistic()) {
                StatisticRecordKey key = new StatisticRecordKey(new RecordId(recordKey++));
                StatisticRecord statRecord;
                srcBuilder.setEndpointGroup(epgStats.getSrcEpg());
                dstBuilder.setEndpointGroup(epgStats.getDstEpg());
                List<Statistic> statisticList = new ArrayList<>();
                for (MatchedRuleStatistic ruleStats : epgStats.getMatchedRuleStatistic()) {
                    Statistic statistic = new StatisticBuilder()
                            .setKey(new StatisticKey(ruleStats.getContract(),
                                    ruleStats.getMatchedRule(), ruleStats.getSubject()))
                            .setContract(ruleStats.getContract())
                            .setSubject(ruleStats.getSubject())
                            .setRule(ruleStats.getMatchedRule())
                            .setAction(ruleStats.getAction())
                            .setClassifier(ruleStats.getClassifier())
                            .setByteCount(ruleStats.getByteCount())
                            .setPacketCount(ruleStats.getPacketCount())
                            .build();
                    statisticList.add(statistic);

                }
                statRecord = new StatisticRecordBuilder().setKey(key)
                        .setRecordId(new RecordId(recordKey))
                        .setTimestamp(epStats.getTimestamp())
                        .setSrcEndpoint(srcBuilder.build())
                        .setDstEndpoint(dstBuilder.build())
                        .setStatistic(statisticList)
                        .build();

                InstanceIdentifier<StatisticRecord> statIID = IidFactory.statisticRecordIid(key);
                LOG.debug("Writing statistics to datastore: {}", statRecord);
                wtx.put(LogicalDatastoreType.OPERATIONAL, statIID, statRecord, true);
            }
        }
        return DataStoreHelper.submitToDs(wtx);
    }

    @Override
    public StatRecords readStats() {
        InstanceIdentifier<StatisticsStore> statsIID = InstanceIdentifier.builder(StatisticsStore.class).build();
        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        LOG.debug("Reading statistics");
        Optional<StatisticsStore> storeOpt = Optional.absent();
        try {
            storeOpt = rtx.read(LogicalDatastoreType.OPERATIONAL, statsIID).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception thrown on reading from Datastore: {}", e.getMessage());
            return null;
        }
        if (storeOpt.isPresent()) {
            StatisticsStore store = storeOpt.get();
            Map<EpToEpStatisticKey, EpToEpStatisticBuilder> map = new HashMap<>();
            for (StatisticRecord storeStat : store.getStatisticRecord()) {
                MacAddress srcMac = storeStat.getSrcEndpoint().getMacAddress();
                MacAddress dstMac = storeStat.getDstEndpoint().getMacAddress();
                L2BridgeDomainId srcL2C = storeStat.getSrcEndpoint().getL2Context();
                L2BridgeDomainId dstL2C = storeStat.getDstEndpoint().getL2Context();
                TenantId srcTenant = storeStat.getSrcEndpoint().getTenant();
                TenantId dstTenant = storeStat.getDstEndpoint().getTenant();
                EpToEpStatisticKey epKey = new EpToEpStatisticKey(dstL2C, dstMac, srcL2C, srcMac);
                EpEpgToEpEpgStatisticKey epgKey = new EpEpgToEpEpgStatisticKey(
                        storeStat.getDstEndpoint().getEndpointGroup(), storeStat.getSrcEndpoint().getEndpointGroup());
                EpToEpStatisticBuilder epStat = map.get(epKey);
                if (epStat == null) {
                    // eptoep combination doesnt exist
                    epStat = new EpToEpStatisticBuilder();
                    epStat.setKey(epKey)
                        .setSrcMacAddress(srcMac)
                        .setSrcL2c(srcL2C)
                        .setSrcTenant(storeStat.getSrcEndpoint().getTenant())
                        .setDstMacAddress(dstMac)
                        .setDstL2c(dstL2C)
                        .setDstTenant(storeStat.getDstEndpoint().getTenant());
                }
                List<MatchedRuleStatistic> ruleStatList = new ArrayList<>();
                for(Statistic statistic : storeStat.getStatistic())
                {
                    MatchedRuleStatisticBuilder statBuilder = new MatchedRuleStatisticBuilder()
                            .setKey(new MatchedRuleStatisticKey(statistic.getContract()
                                    ,statistic.getRule()
                                    ,statistic.getSubject()))
                            .setContract(statistic.getContract())
                            .setSubject(statistic.getSubject())
                            .setMatchedRule(statistic.getRule())
                            .setAction(statistic.getAction())
                            .setClassifier(statistic.getClassifier())
                            .setByteCount(statistic.getByteCount())
                            .setPacketCount(statistic.getPacketCount());
                    ruleStatList.add(statBuilder.build());
                }
                EpEpgToEpEpgStatisticBuilder epgtoepgBuilder = new EpEpgToEpEpgStatisticBuilder();
                epgtoepgBuilder.setKey(epgKey)
                    .setSrcEpg(storeStat.getSrcEndpoint().getEndpointGroup())
                    .setDstEpg(storeStat.getDstEndpoint().getEndpointGroup())
                    .setMatchedRuleStatistic(ruleStatList);
                epStat.setEpEpgToEpEpgStatistic(
                        addIfNotExists(epgtoepgBuilder, epStat.getEpEpgToEpEpgStatistic()));
                map.put(epKey, epStat);
            }
            List<EpToEpStatistic> epList = new ArrayList<>();
            for (EpToEpStatisticBuilder statBuilder : map.values()) {
                epList.add(statBuilder.build());
            }
            StatRecordsBuilder statStore = new StatRecordsBuilder();
            statStore.setEpToEpStatistic(epList);
            return statStore.build();
        }
        LOG.debug("Statistics store empty");
        return null;
    }

    private List<EpEpgToEpEpgStatistic> addIfNotExists(EpEpgToEpEpgStatisticBuilder stat,
            List<EpEpgToEpEpgStatistic> list) {
        if ( list == null ) {
            list = new ArrayList<>();
        } else {
            Iterator<EpEpgToEpEpgStatistic> iterator = list.iterator();
            while (iterator.hasNext()) {
                EpEpgToEpEpgStatistic epgStat = iterator.next();
                if (stat.getKey().equals(epgStat.getKey())) {
                    List<MatchedRuleStatistic> newMatches = new ArrayList<>();
                    Iterator<MatchedRuleStatistic> iteratorNew = stat.getMatchedRuleStatistic().iterator();
                    while(iteratorNew.hasNext()) {
                        MatchedRuleStatistic newStat = iteratorNew.next();
                        Iterator<MatchedRuleStatistic> iteratorOld = epgStat.getMatchedRuleStatistic().iterator();
                        boolean matched = false;
                        while(iteratorOld.hasNext()) {
                            MatchedRuleStatistic oldStat = iteratorOld.next();
                            if(oldStat.getKey().equals(newStat.getKey())) {
                                MatchedRuleStatistic newRuleStat = new MatchedRuleStatisticBuilder(oldStat)
                                        .setByteCount(sumNullableValues(oldStat.getByteCount(), newStat.getByteCount()))
                                        .setPacketCount(sumNullableValues(oldStat.getPacketCount(), newStat.getPacketCount()))
                                        .build();
                                newMatches.add(newRuleStat);
                                matched = true;
                            } else {
                                newMatches.add(oldStat);
                            }
                        }
                        if (!matched) {
                            newMatches.add(newStat);
                        }
                    }
                    stat.setMatchedRuleStatistic(newMatches);
                    iterator.remove();
                    break;
                }
            }
        }
        list.add(stat.build());
        return list;
    }

    public Long sumNullableValues (Long... x ) {
        long result = 0;
        for (Long num : x) {
            if (num != null) {
                result += num;
            }
        }
        return result;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
