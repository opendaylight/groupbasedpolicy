/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.statistics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.stat.records.ep.to.ep.statistic.EpEpgToEpEpgStatistic;
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

public class StatisticManagerImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String READING_EXCEPTION_MSG = "Reading throws exception";

    private DataBroker dataBroker;
    private StatisticsManagerImpl manager;
    private WriteTransaction wtx;
    private ReadOnlyTransaction rtx;
    private MacAddress srcMac;
    private MacAddress dstMac;
    private L2BridgeDomainId srcL2C;
    private L2BridgeDomainId dstL2C;
    private EndpointGroupId srcEPG;
    private EndpointGroupId dstEPG;
    private TenantId srcTenant;
    private TenantId dstTenant;
    private ContractId contract;
    private SubjectName subject;
    private RuleName rule;
    private ActionName action;
    private ClassifierName classifier;

    @Before
    public void init() {
        srcMac = new MacAddress("00:00:00:00:00:01");
        dstMac = new MacAddress("00:00:00:00:00:02");
        srcL2C = new L2BridgeDomainId("srcL2C");
        dstL2C = new L2BridgeDomainId("dstL2C");
        srcEPG = new EndpointGroupId("srcEPG");
        dstEPG = new EndpointGroupId("dstEPG");
        srcTenant = new TenantId("srcTenant");
        dstTenant = new TenantId("dstTenant");
        contract = new ContractId("contract");
        subject = new SubjectName("subject");
        rule = new RuleName("rule");
        action = new ActionName("action");
        classifier = new ClassifierName("classifier");

        dataBroker = mock(DataBroker.class);
        wtx = mock(WriteTransaction.class);
        rtx = mock(ReadOnlyTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rtx);

        manager = new StatisticsManagerImpl(dataBroker);
    }

    @Test
    public void testWriteStat() {
        StatRecords input = inputForWriting();
        StatisticRecordKey key = new StatisticRecordKey(new RecordId(0l));
        StatisticRecord output = outputForWriting(key);

        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
        when(wtx.submit()).thenReturn(future);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);

        manager.writeStat(input);
        verify(wtx).put(LogicalDatastoreType.OPERATIONAL, IidFactory.statisticRecordIid(key), output, true);
    }

    @Test
    public void testReadStats() throws Exception {
        List<StatisticRecord> stats = inputForReading();
        StatRecords statRecords = outputForReading();

        CheckedFuture<Optional<StatisticsStore>, ReadFailedException> future = mock(CheckedFuture.class);
        when(future.get()).thenReturn(Optional.of(new StatisticsStoreBuilder().setStatisticRecord(stats).build()));
        when(rtx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(StatisticsStore.class).build()))
            .thenReturn(future);

        StatRecords read = manager.readStats();

        Assert.assertEquals(statRecords, read);
    }

    @Test
    public void testReadStats_throwsException() throws ExecutionException, InterruptedException {
        CheckedFuture<Optional<StatisticsStore>, ReadFailedException> future = mock(CheckedFuture.class);
        when(future.get()).thenThrow(new RuntimeException(READING_EXCEPTION_MSG));
        when(rtx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(StatisticsStore.class).build()))
            .thenReturn(future);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(READING_EXCEPTION_MSG);
        manager.readStats();
    }

    private List<StatisticRecord> inputForReading() {
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
        List<StatisticRecord> stats = new ArrayList<>();
        stats.add(statRecord.build());
        statRecord.setKey(new StatisticRecordKey(new RecordId(1l)));
        stats.add(statRecord.build());

        return stats;
    }

    private StatRecords outputForReading() {
        StatRecordsBuilder recordsBuilder = new StatRecordsBuilder();
        EpEpgToEpEpgStatisticBuilder epgBuilder = newEpEpgToEpEpgStatisticBuilder(50, 10);
        EpToEpStatisticBuilder epBuilder = newEpToEpStatisticBuilder(Collections.singletonList(epgBuilder.build()));
        recordsBuilder.setEpToEpStatistic(Collections.singletonList(epBuilder.build()));

        return recordsBuilder.build();
    }

    private StatRecords inputForWriting() {
        StatRecordsBuilder recordsBuilder = new StatRecordsBuilder();
        EpEpgToEpEpgStatisticBuilder epgBuilder = newEpEpgToEpEpgStatisticBuilder(25, 5);
        EpToEpStatisticBuilder epBuilder = newEpToEpStatisticBuilder(Collections.singletonList(epgBuilder.build()));
        recordsBuilder.setEpToEpStatistic(Collections.singletonList(epBuilder.build()));

        return recordsBuilder.build();
    }

    private StatisticRecord outputForWriting(StatisticRecordKey key) {
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
        StatisticRecordBuilder statRecord = new StatisticRecordBuilder().setKey(key)
            .setStatistic(Collections.singletonList(statBuilder.build()))
            .setSrcEndpoint(srcBuilder.build())
            .setDstEndpoint(dstBuilder.build());

        return statRecord.build();
    }

    private EpEpgToEpEpgStatisticBuilder newEpEpgToEpEpgStatisticBuilder(long byteCount, long packetCount) {
        return new EpEpgToEpEpgStatisticBuilder().setSrcEpg(srcEPG)
            .setDstEpg(dstEPG)
            .setKey(new EpEpgToEpEpgStatisticKey(dstEPG, srcEPG))
            .setMatchedRuleStatistic(Collections.singletonList(
                    new MatchedRuleStatisticBuilder().setKey(new MatchedRuleStatisticKey(contract, rule, subject))
                        .setContract(contract)
                        .setSubject(subject)
                        .setMatchedRule(rule)
                        .setAction(Collections.singletonList(action))
                        .setClassifier(Collections.singletonList(classifier))
                        .setByteCount(byteCount)
                        .setPacketCount(packetCount)
                        .build()));
    }

    private EpToEpStatisticBuilder newEpToEpStatisticBuilder(List<EpEpgToEpEpgStatistic> list) {
        return new EpToEpStatisticBuilder().setSrcMacAddress(srcMac)
            .setDstMacAddress(dstMac)
            .setSrcL2c(srcL2C)
            .setDstL2c(dstL2C)
            .setSrcTenant(srcTenant)
            .setDstTenant(dstTenant)
            .setKey(new EpToEpStatisticKey(dstL2C, dstMac, srcL2C, srcMac))
            .setEpEpgToEpEpgStatistic(list);
    }

}
