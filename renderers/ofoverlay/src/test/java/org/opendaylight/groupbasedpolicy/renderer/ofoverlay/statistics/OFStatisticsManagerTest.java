/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.TestUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OFStatisticsManagerTest {

    private final EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpg1");
    private final EndpointGroupId providerEpgId = new EndpointGroupId("providerEpg1");
    private final ContractId contractId = new ContractId("contract1");
    private final TenantId tenantId = new TenantId("tenant1");
    private final ClassifierName classifierName = ClassifierName.getDefaultInstance("classifier1");
    private final SubjectName subjectName = SubjectName.getDefaultInstance("subject1");
    private final RuleName ruleName = new RuleName("rule1");

    private InstanceIdentifier<ResolvedPolicy> rpIid;
    private ResolvedPolicy resolvedPolicy;

    private ScheduledExecutorService executor;
    private StatisticsManager statisticsManager;

    private OFStatisticsManager ofStatisticsManager;
    private Classifier classifier;
    private Map<InstanceIdentifier<Classifier>, Classifier> classifierByIid;

    @Before
    public void init() {
        executor = mock(ScheduledExecutorService.class);
        statisticsManager = mock(StatisticsManager.class);

        classifier = new ClassifierBuilder()
                .setName(classifierName)
                .build();
        resolvedPolicy = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName,
                consumerEpgId, providerEpgId, classifier);

        rpIid = InstanceIdentifier.create(ResolvedPolicy.class);

        ofStatisticsManager = new OFStatisticsManager(executor, statisticsManager);
        ofStatisticsManager.setDelay(20L);
        ofStatisticsManager.setSflowCollectorUri("http://localhost:1234");

        classifierByIid =
                ResolvedPolicyClassifierListener.resolveClassifiers(resolvedPolicy, rpIid);
    }

    @Test
    public void testConstructor() throws Exception {
        OFStatisticsManager other = new OFStatisticsManager(executor, statisticsManager);
        other.close();
    }

    @Test
    public void testTTT(){
        for (Map.Entry<InstanceIdentifier<Classifier>, Classifier> classifierEntry : classifierByIid.entrySet()) {
            ofStatisticsManager.pullStatsForClassifier(classifierEntry.getKey(),
                    classifierEntry.getValue());
        }
    }
}
