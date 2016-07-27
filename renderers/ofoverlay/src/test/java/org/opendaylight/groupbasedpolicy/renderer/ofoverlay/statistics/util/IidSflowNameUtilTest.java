/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.ResolvedPolicyClassifierListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.TestUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IidSflowNameUtilTest {

    private final EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpg1");
    private final EndpointGroupId providerEpgId = new EndpointGroupId("providerEpg1");
    private final ContractId contractId = new ContractId("contract1");
    private final TenantId tenantId = new TenantId("tenant1");
    private final ClassifierName classifierName = ClassifierName.getDefaultInstance("classifier1");
    private final SubjectName subjectName = SubjectName.getDefaultInstance("subject1");
    private final RuleName ruleName = new RuleName("rule1");
    private InstanceIdentifier<ResolvedPolicy> rpIid;
    private String testName;
    private ResolvedPolicy resolvedPolicy;

    @Before
    public void init() {
        Classifier classifier = mock(Classifier.class);
        when(classifier.getKey()).thenReturn(new ClassifierKey(classifierName));

        resolvedPolicy = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName,
                consumerEpgId, providerEpgId, classifier);

        rpIid = InstanceIdentifier.create(ResolvedPolicy.class);
        testName =
                tenantId.getValue() + IidSflowNameUtil.KEY_DELIMETER + contractId.getValue() + IidSflowNameUtil.KEY_DELIMETER + subjectName
                        .getValue() + IidSflowNameUtil.DELIMETER + ruleName.getValue() + IidSflowNameUtil.DELIMETER + classifierName
                        .getValue() + IidSflowNameUtil.DELIMETER + FlowCacheCons.Value.BYTES.get();
    }

    @Test
    public void testCreateFlowCacheName() {
        InstanceIdentifier<Classifier> classifierIid = TestUtils.getClassifierIid(
                ResolvedPolicyClassifierListener.resolveClassifiers(resolvedPolicy, rpIid));

        assertEquals(testName,
                IidSflowNameUtil.createFlowCacheName(classifierIid, FlowCacheCons.Value.BYTES));
    }

    @Test
    public void testResolveContractIdFromFlowCacheName() {
        assertEquals(contractId.getValue(),
                IidSflowNameUtil.resolveContractIdFromFlowCacheName(testName).getValue());
    }

    @Test
    public void testResolveSubjectNameFromFlowCacheName() {
        assertEquals(subjectName.getValue(),
                IidSflowNameUtil.resolveSubjectNameFromFlowCacheName(testName).getValue());
    }

    @Test
    public void testResolveRuleNameFromFlowCacheName() {
        assertEquals(ruleName.getValue(),
                IidSflowNameUtil.resolveRuleNameFromFlowCacheName(testName).getValue());
    }

    @Test
    public void testResolveClassifierNameFromFlowCacheName() {
        assertEquals(classifierName.getValue(),
                IidSflowNameUtil.resolveClassifierNameFromFlowCacheName(testName).getValue());
    }

    @Test
    public void testResolveFlowCacheValue() {
        assertEquals(FlowCacheCons.Value.BYTES.get(),
                IidSflowNameUtil.resolveFlowCacheValue(testName));
    }

}
