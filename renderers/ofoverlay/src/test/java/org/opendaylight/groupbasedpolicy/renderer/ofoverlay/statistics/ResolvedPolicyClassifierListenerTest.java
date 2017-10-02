/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.ParameterValueList;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.TestUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ResolvedPolicyClassifierListenerTest {

    private ResolvedPolicyClassifierListener classifierListener;
    private DataObjectModification<ResolvedPolicy> rootNode;
    private Set<DataTreeModification<ResolvedPolicy>> changes;

    private InstanceIdentifier<ResolvedPolicy> rootIdentifier;

    private final EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpg1");
    private final EndpointGroupId providerEpgId = new EndpointGroupId("providerEpg1");
    private final ContractId contractId = new ContractId("contract1");
    private final TenantId tenantId = new TenantId("tenant1");
    private final ClassifierName classifierName = new ClassifierName("classifier1");
    private final SubjectName subjectName = new SubjectName("subject1");
    private final RuleName ruleName = new RuleName("rule1");

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        DataBroker dataProvider = mock(DataBroker.class);
        OFStatisticsManager ofStatisticsManager = mock(OFStatisticsManager.class);

        classifierListener = spy(new ResolvedPolicyClassifierListener(dataProvider, ofStatisticsManager));

        ResolvedPolicyKey key = mock(ResolvedPolicyKey.class);
        rootNode = mock(DataObjectModification.class);
        rootIdentifier = InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class, key).build();
        DataTreeIdentifier<ResolvedPolicy> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);

        DataTreeModification<ResolvedPolicy> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        ParameterValueList parameterValues = new ParameterValueList();
        parameterValues.addEthertype(EtherTypeClassifierDefinition.IPv4_VALUE)
            .addProto(IpProtoClassifierDefinition.TCP_VALUE);

        Classifier classifier = new ClassifierBuilder().setName(classifierName)
            .setClassifierDefinitionId(IpProtoClassifierDefinition.ID)
            .setParameterValue(parameterValues)
            .build();
        ResolvedPolicy rp = TestUtils.newResolvedPolicy(tenantId, contractId, subjectName, ruleName, consumerEpgId,
                providerEpgId, classifier);
        when(rootNode.getDataBefore()).thenReturn(rp);
        when(rootNode.getDataAfter()).thenReturn(rp);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        classifierListener.onDataTreeChanged(changes);

        verify(classifierListener).onWrite(rootNode, rootIdentifier);
    }

    @Test
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        classifierListener.onDataTreeChanged(changes);

        verify(classifierListener).onDelete(rootNode, rootIdentifier);
    }

    @Test
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        classifierListener.onDataTreeChanged(changes);

        verify(classifierListener).onSubtreeModified(rootNode, rootIdentifier);
    }

}
