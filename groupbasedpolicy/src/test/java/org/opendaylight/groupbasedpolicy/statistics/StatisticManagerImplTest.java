/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.statistics;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecordsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.EpToEpStatisticKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatisticKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.statistic.record.StatisticBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.store.rev151215.statistics.store.statistic.record.StatisticKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class StatisticManagerImplTest {

    private DataBroker dataBroker;
    private StatisticsManagerImpl manager;
    private WriteTransaction wtx;
    private ReadOnlyTransaction rtx;

    @Before
    public void init() {
        dataBroker = Mockito.mock(DataBroker.class);
        wtx = Mockito.mock(WriteTransaction.class);
        rtx = Mockito.mock(ReadOnlyTransaction.class);
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rtx);
        manager = new StatisticsManagerImpl(dataBroker);
    }

    @Test
    public void writeStatTest() {
        MacAddress srcMac = new MacAddress("00:00:00:00:00:01");
        MacAddress dstMac = new MacAddress("00:00:00:00:00:02");
        L2BridgeDomainId srcL2C = new L2BridgeDomainId("srcL2C");
        L2BridgeDomainId dstL2C = new L2BridgeDomainId("dstL2C");
        EndpointGroupId srcEPG = new EndpointGroupId("srcEPG");
        EndpointGroupId dstEPG = new EndpointGroupId("dstEPG");
        TenantId srcTenant = new TenantId("srcTenant");
        TenantId dstTenant = new TenantId("dstTenant");
        ContractId contract = new ContractId("contract");
        SubjectName subject = new SubjectName("subject");
        RuleName rule = new RuleName("rule");
        ActionName action = new ActionName("action");
        ClassifierName classifier = new ClassifierName("classifier");
        // input
        StatRecordsBuilder recordsBuilder = new StatRecordsBuilder();
        EpToEpStatisticBuilder epBuilder = new EpToEpStatisticBuilder();
        EpEpgToEpEpgStatisticBuilder epgBuilder = new EpEpgToEpEpgStatisticBuilder();
        epgBuilder.setSrcEpg(srcEPG)
            .setDstEpg(dstEPG)
            .setKey(new EpEpgToEpEpgStatisticKey(dstEPG, srcEPG))
            .setMatchedRuleStatistic(Collections.singletonList(new MatchedRuleStatisticBuilder()
                .setKey(new MatchedRuleStatisticKey(contract, rule, subject))
                .setContract(contract)
                .setSubject(subject)
                .setMatchedRule(rule)
                .setAction(Collections.singletonList(action))
                .setClassifier(Collections.singletonList(classifier))
                .setByteCount(25l)
                .setPacketCount(5l)
                .build()));
        epBuilder.setSrcMacAddress(srcMac)
            .setDstMacAddress(dstMac)
            .setSrcL2c(srcL2C)
            .setDstL2c(dstL2C)
            .setSrcTenant(srcTenant)
            .setDstTenant(dstTenant)
            .setKey(new EpToEpStatisticKey(dstL2C, dstMac, srcL2C, srcMac))
            .setEpEpgToEpEpgStatistic(Collections.singletonList(epgBuilder.build()));
        recordsBuilder.setEpToEpStatistic(Collections.singletonList(epBuilder.build()));
        // output
        SrcEndpointBuilder srcBuilder = new SrcEndpointBuilder();
        DstEndpointBuilder dstBuilder = new DstEndpointBuilder();
        srcBuilder.setMacAddress(srcMac).setL2Context(srcL2C).setTenant(srcTenant);
        dstBuilder.setMacAddress(dstMac).setL2Context(dstL2C).setTenant(dstTenant);
        srcBuilder.setEndpointGroup(srcEPG);
        dstBuilder.setEndpointGroup(dstEPG);
        StatisticBuilder statBuilder = new StatisticBuilder().setKey(new StatisticKey(contract, rule, subject))
            .setContract(contract)
            .setRule(rule)
            .setSubject(subject)
            .setClassifier(Collections.singletonList(classifier))
            .setAction(Collections.singletonList(action))
            .setByteCount(25l)
            .setPacketCount(5l);
        StatisticRecordKey key = new StatisticRecordKey(new RecordId(0l));
        StatisticRecordBuilder statRecord = new StatisticRecordBuilder().setKey(key)
            .setStatistic(Collections.singletonList(statBuilder.build()))
            .setSrcEndpoint(srcBuilder.build())
            .setDstEndpoint(dstBuilder.build());

//        manager.writeStat(recordsBuilder.build());
//        Mockito.verify(wtx).put(LogicalDatastoreType.OPERATIONAL,
//                IidFactory.statisticRecordIid(key),
//                statRecord.build());
        CheckedFuture<Void,TransactionCommitFailedException> future = Mockito.mock(CheckedFuture.class);
        Mockito.when(wtx.submit()).thenReturn(future);
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);

        manager.writeStat(recordsBuilder.build());
        Mockito.verify(wtx).put(LogicalDatastoreType.OPERATIONAL,
                IidFactory.statisticRecordIid(key),
                statRecord.build(), true);
    }

    @Test
    public void readStatsTest() {
        MacAddress srcMac = new MacAddress("00:00:00:00:00:01");
        MacAddress dstMac = new MacAddress("00:00:00:00:00:02");
        L2BridgeDomainId srcL2C = new L2BridgeDomainId("srcL2C");
        L2BridgeDomainId dstL2C = new L2BridgeDomainId("dstL2C");
        EndpointGroupId srcEPG = new EndpointGroupId("srcEPG");
        EndpointGroupId dstEPG = new EndpointGroupId("dstEPG");
        TenantId srcTenant = new TenantId("srcTenant");
        TenantId dstTenant = new TenantId("dstTenant");
        ContractId contract = new ContractId("contract");
        SubjectName subject = new SubjectName("subject");
        RuleName rule = new RuleName("rule");
        ActionName action = new ActionName("action");
        ClassifierName classifier = new ClassifierName("classifier");
        // input
        SrcEndpointBuilder srcBuilder = new SrcEndpointBuilder();
        DstEndpointBuilder dstBuilder = new DstEndpointBuilder();
        srcBuilder.setMacAddress(srcMac).setL2Context(srcL2C).setTenant(srcTenant);
        dstBuilder.setMacAddress(dstMac).setL2Context(dstL2C).setTenant(dstTenant);
        srcBuilder.setEndpointGroup(srcEPG);
        dstBuilder.setEndpointGroup(dstEPG);
        StatisticBuilder statBuilder = new StatisticBuilder().setKey(new StatisticKey(contract, rule, subject))
            .setContract(contract)
            .setRule(rule)
            .setSubject(subject)
            .setClassifier(Collections.singletonList(classifier))
            .setAction(Collections.singletonList(action))
            .setByteCount(25l)
            .setPacketCount(5l);
        StatisticRecordKey key = new StatisticRecordKey(new RecordId(0l));
        StatisticRecordBuilder statRecord = new StatisticRecordBuilder().setKey(key)
            .setStatistic(Collections.singletonList(statBuilder.build()))
            .setSrcEndpoint(srcBuilder.build())
            .setDstEndpoint(dstBuilder.build());
        ArrayList<StatisticRecord> stats = new ArrayList<>();
        stats.add(statRecord.build());
        statRecord.setKey(new StatisticRecordKey(new RecordId(1l)));
        stats.add(statRecord.build());
        // output
        StatRecordsBuilder recordsBuilder = new StatRecordsBuilder();
        EpToEpStatisticBuilder epBuilder = new EpToEpStatisticBuilder();
        EpEpgToEpEpgStatisticBuilder epgBuilder = new EpEpgToEpEpgStatisticBuilder();
        epgBuilder.setSrcEpg(srcEPG)
            .setDstEpg(dstEPG)
            .setKey(new EpEpgToEpEpgStatisticKey(dstEPG, srcEPG))
            .setMatchedRuleStatistic(Collections.singletonList(new MatchedRuleStatisticBuilder()
                .setKey(new MatchedRuleStatisticKey(contract, rule, subject))
                .setContract(contract)
                .setSubject(subject)
                .setMatchedRule(rule)
                .setAction(Collections.singletonList(action))
                .setClassifier(Collections.singletonList(classifier))
                .setByteCount(50l)
                .setPacketCount(10l)
                .build()));
        epBuilder.setSrcMacAddress(srcMac)
            .setDstMacAddress(dstMac)
            .setSrcL2c(srcL2C)
            .setDstL2c(dstL2C)
            .setSrcTenant(srcTenant)
            .setDstTenant(dstTenant)
            .setKey(new EpToEpStatisticKey(dstL2C, dstMac, srcL2C, srcMac))
            .setEpEpgToEpEpgStatistic(Collections.singletonList(epgBuilder.build()));
        recordsBuilder.setEpToEpStatistic(Collections.singletonList(epBuilder.build()));

        CheckedFuture<Optional<StatisticsStore>, ReadFailedException> future = Mockito.mock(CheckedFuture.class);
        try {
            Mockito.when(future.get())
                .thenReturn(Optional.of(new StatisticsStoreBuilder().setStatisticRecord(stats).build()));
            Mockito.when(rtx.read(LogicalDatastoreType.OPERATIONAL,
                    InstanceIdentifier.builder(StatisticsStore.class).build()))
                .thenReturn(future);
            StatRecords read = manager.readStats();
            Assert.assertEquals(recordsBuilder.build(), read);
        } catch (Exception e) {
        }

    }
}
