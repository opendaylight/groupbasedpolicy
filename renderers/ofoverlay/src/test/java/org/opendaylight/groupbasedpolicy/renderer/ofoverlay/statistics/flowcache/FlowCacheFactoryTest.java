/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.ResolvedPolicyClassifierListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.FlowCacheCons;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.IidSflowNameUtil;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.ParameterValueList;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.TestUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
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

public class FlowCacheFactoryTest {

    private final EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpg1");
    private final EndpointGroupId providerEpgId = new EndpointGroupId("providerEpg1");
    private final ContractId contractId = new ContractId("contract1");
    private final TenantId tenantId = new TenantId("tenant1");
    private final ClassifierName classifierName = ClassifierName.getDefaultInstance("classifier1");
    private final SubjectName subjectName = SubjectName.getDefaultInstance("subject1");
    private final RuleName ruleName = new RuleName("rule1");

    private InstanceIdentifier<ResolvedPolicy> rpIid;
    private String expectedName;

    @Before
    public void init() {
        rpIid = InstanceIdentifier.create(ResolvedPolicy.class);
        expectedName = tenantId.getValue() + IidSflowNameUtil.KEY_DELIMETER + contractId.getValue()
                + IidSflowNameUtil.KEY_DELIMETER + subjectName.getValue() + IidSflowNameUtil.DELIMETER
                + ruleName.getValue() + IidSflowNameUtil.DELIMETER + classifierName.getValue()
                + IidSflowNameUtil.DELIMETER + FlowCacheCons.Value.BYTES.get();
    }

    @Test
    public void testCreateFlowCache_EtherTypeClassifier_IPv4() {
        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE);

        Classifier classifier = newEtherTypeClassifier(parameterValues);
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);

        FlowCache flowCache = callCreateFlowCache(rp, classifier);

        assertNotNull(flowCache);

        List<String> keys = Arrays.asList(flowCache.getKeyNames());
        List<String> expectedKeys = ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(),
                FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCache.getName());
        assertEquals(expectedKeys.size(), keys.size());
        assertTrue(keys.containsAll(expectedKeys));
    }

    @Test
    public void testCreateFlowCache_IpProtoClassifier_TCP_IPv4() {

        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.TCP_VALUE);

        Classifier classifier = newIpProtoClassifier(parameterValues);
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);

        FlowCache flowCache = callCreateFlowCache(rp, classifier);

        assertNotNull(flowCache);

        List<String> keys = Arrays.asList(flowCache.getKeyNames());
        List<String> expectedKeys =
                ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(), FlowCacheCons.Key.IP_PROTOCOL.get(),
                        FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCache.getName());
        assertEquals(expectedKeys.size(), keys.size());
        assertTrue(keys.containsAll(expectedKeys));
    }

    @Test
    public void testCreateFlowCache_IpProtoClassifier_UDP_noEthertype() {
        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addProto(IpProtoClassifierDefinition.UDP_VALUE);

        Classifier classifier = newIpProtoClassifier(parameterValues);
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);

        FlowCache flowCache = callCreateFlowCache(rp, classifier);

        assertNotNull(flowCache);

        List<String> keys = Arrays.asList(flowCache.getKeyNames());
        List<String> expectedKeys =
                ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(), FlowCacheCons.Key.IP_PROTOCOL.get(),
                        FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCache.getName());
        assertEquals(expectedKeys.size(), keys.size());
        assertTrue(keys.containsAll(expectedKeys));
    }

    @Test
    public void testCreateFlowCache_L4Classifier_dstPort() {
        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.TCP_VALUE)
            .addDstPort((long) 80);
        Classifier classifier = newL4Classifier(parameterValues);
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);

        FlowCache flowCache = callCreateFlowCache(rp, classifier);

        assertNotNull(flowCache);

        List<String> keys = Arrays.asList(flowCache.getKeyNames());
        List<String> expectedKeys = ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(),
                FlowCacheCons.Key.IP_PROTOCOL.get(), FlowCacheCons.Key.TCP_DST_PORT.get(),
                FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCache.getName());
        assertEquals(expectedKeys.size(), keys.size());
        assertTrue(keys.containsAll(expectedKeys));

        ParameterValueList parameterValuesUDP = new ParameterValueList();
        parameterValuesUDP.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.UDP_VALUE)
            .addDstPort((long) 80);
        Classifier classifierUDP = newL4Classifier(parameterValuesUDP);
        ResolvedPolicy rpUDP = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifierUDP);

        FlowCache flowCacheUDP = callCreateFlowCache(rpUDP, classifierUDP);

        assertNotNull(flowCacheUDP);

        List<String> keysUDP = Arrays.asList(flowCacheUDP.getKeyNames());
        List<String> expectedKeysUDP = ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(),
                FlowCacheCons.Key.IP_PROTOCOL.get(), FlowCacheCons.Key.UDP_DST_PORT.get(),
                FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCacheUDP.getName());
        assertEquals(expectedKeysUDP.size(), keysUDP.size());
        assertTrue(keysUDP.containsAll(expectedKeysUDP));
    }

    @Test
    public void testCreateFlowCache_L4Classifier_srcPort() {
        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.TCP_VALUE)
            .addSrcPort((long) 80);
        Classifier classifier = newL4Classifier(parameterValues);
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);

        FlowCache flowCache = callCreateFlowCache(rp, classifier);

        assertNotNull(flowCache);

        List<String> keys = Arrays.asList(flowCache.getKeyNames());
        List<String> expectedKeys = ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(),
                FlowCacheCons.Key.IP_PROTOCOL.get(), FlowCacheCons.Key.TCP_SRC_PORT.get(),
                FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCache.getName());
        assertEquals(expectedKeys.size(), keys.size());
        assertTrue(keys.containsAll(expectedKeys));

        ParameterValueList parameterValuesUDP = new ParameterValueList();
        parameterValuesUDP.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.UDP_VALUE)
            .addSrcPort((long) 80);
        Classifier classifierUDP = newL4Classifier(parameterValuesUDP);
        ResolvedPolicy rpUDP = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifierUDP);

        FlowCache flowCacheUDP = callCreateFlowCache(rpUDP, classifierUDP);

        assertNotNull(flowCacheUDP);

        List<String> keysUDP = Arrays.asList(flowCacheUDP.getKeyNames());
        List<String> expectedKeysUDP = ImmutableList.of(FlowCacheCons.Key.ETH_PROTOCOL.get(),
                FlowCacheCons.Key.IP_PROTOCOL.get(), FlowCacheCons.Key.UDP_SRC_PORT.get(),
                FlowCacheCons.Key.IP_SOURCE.get(), FlowCacheCons.Key.IP_DESTINATION.get());

        assertEquals(expectedName, flowCacheUDP.getName());
        assertEquals(expectedKeysUDP.size(), keysUDP.size());
        assertTrue(keysUDP.containsAll(expectedKeysUDP));
    }

    private Classifier newClassifier(ClassifierDefinitionId classifierDefinitionId,
            ParameterValueList parameterValues) {
        return new ClassifierBuilder().setName(classifierName)
            .setClassifierDefinitionId(classifierDefinitionId)
            .setParameterValue(parameterValues)
            .build();
    }

    private Classifier newEtherTypeClassifier(ParameterValueList parameterValues) {
        return newClassifier(EtherTypeClassifierDefinition.ID, parameterValues);
    }

    private Classifier newIpProtoClassifier(ParameterValueList parameterValues) {
        return newClassifier(IpProtoClassifierDefinition.ID, parameterValues);
    }

    private Classifier newL4Classifier(ParameterValueList parameterValues) {
        return newClassifier(L4ClassifierDefinition.ID, parameterValues);
    }

    private FlowCache callCreateFlowCache(ResolvedPolicy rp, Classifier classifier) {
        return FlowCacheFactory.createFlowCache(
                TestUtils.getClassifierIid(ResolvedPolicyClassifierListener.resolveClassifiers(rp, rpIid)), classifier,
                FlowCacheCons.Value.BYTES);
    }

}
