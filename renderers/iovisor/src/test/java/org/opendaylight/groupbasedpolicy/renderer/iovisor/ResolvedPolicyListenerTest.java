/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.ClientHandlerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.restclient.RestClient;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ResolvedPolicyListenerTest extends GbpIovisorDataBrokerTest {

    private DataBroker dataBroker;
    private ResolvedPolicyListener resolvedPolicyListener;
    private DataObjectModification<ResolvedPolicy> rootNode;
    private Set<DataTreeModification<ResolvedPolicy>> changes;

    private TenantId tenantId = new TenantId("tenant1");
    private EndpointGroupId consEpg = new EndpointGroupId("client");
    private EndpointGroupId provEpg = new EndpointGroupId("webserver");

    private SubjectName subjectName = new SubjectName("subject1");
    private ContractId contractId = new ContractId("icmp-http-contract");
    private List<Action> action =
            ImmutableList.of(new ActionBuilder().setActionDefinitionId(new AllowAction().getId()).build());
    private List<Classifier> classifier = ImmutableList
        .of(new ClassifierBuilder().setClassifierDefinitionId(new EtherTypeClassifier(null).getId()).build());

    private ResolvedPolicy resolvedPolicy;
    private PolicyRuleGroupWithEndpointConstraints policyRuleGroupWithEndpointConstraints;
    private ResolvedRule resolvedRule;

    private RestClient restClient = mock(RestClient.class);

    private List<PolicyRuleGroupWithEndpointConstraints> listOfPolicyRuleGroupWithEndpointConstraints =
            new ArrayList<>();;
    private List<PolicyRuleGroup> policyRuleGroup;
    private EndpointManager endpointManager;
    private IovisorModuleManager iovisorModuleManager;

    private IovisorModuleInstance iomInstance;

    private final String iom1 = "192.168.50.100:5001";

    @SuppressWarnings("unchecked")
    @Before
    public void iovisorInit() {

        resolvedRule = new ResolvedRuleBuilder().setAction(action).setClassifier(classifier).build();

        policyRuleGroup = ImmutableList.of(new PolicyRuleGroupBuilder().setContractId(contractId)
            .setResolvedRule(ImmutableList.of(resolvedRule))
            .setTenantId(tenantId)
            .setSubjectName(subjectName)
            .build());

        policyRuleGroupWithEndpointConstraints = new PolicyRuleGroupWithEndpointConstraintsBuilder()
            // .setConsumerEndpointConstraints(null)
            // .setProviderEndpointConstraints(null)
            .setPolicyRuleGroup(policyRuleGroup).build();

        listOfPolicyRuleGroupWithEndpointConstraints.add(policyRuleGroupWithEndpointConstraints);

        resolvedPolicy = new ResolvedPolicyBuilder().setConsumerEpgId(consEpg)
            .setConsumerTenantId(tenantId)
            .setProviderEpgId(provEpg)
            .setProviderTenantId(tenantId)
            .setPolicyRuleGroupWithEndpointConstraints(listOfPolicyRuleGroupWithEndpointConstraints)
            .build();

        // when(restClient.post(any(String.class), any(String.class)));

        dataBroker = getDataBroker();
        endpointManager = new EndpointManager(dataBroker, mock(EpRendererAugmentationRegistry.class));
        iovisorModuleManager = mock(IovisorModuleManager.class);
        // iovisorModuleManager = endpointManager.getIovisorModuleManager();
        resolvedPolicyListener = spy(new ResolvedPolicyListener(dataBroker, iovisorModuleManager));

        ResolvedPolicyKey key = mock(ResolvedPolicyKey.class);
        rootNode = mock(DataObjectModification.class);
        InstanceIdentifier<ResolvedPolicy> rootIdentifier =
                InstanceIdentifier.builder(ResolvedPolicies.class)
                        .child(ResolvedPolicy.class, key)
                        .build();
        DataTreeIdentifier<ResolvedPolicy> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);
        DataTreeModification<ResolvedPolicy> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        when(rootNode.getDataAfter()).thenReturn(resolvedPolicy);
    }

    @Test(expected = NullPointerException.class)
    public void onWriteTestNull() {
        resolvedPolicyListener.processResolvedPolicyNotification(null);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        resolvedPolicyListener.onDataTreeChanged(changes);

        verify(resolvedPolicyListener).processResolvedPolicyNotification(eq(resolvedPolicy));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        resolvedPolicyListener.onDataTreeChanged(changes);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        resolvedPolicyListener.onDataTreeChanged(changes);
    }

    @Test
    public void policyUriBuilderTest() {
        String target = "/restconf/operational/resolved-policy:resolved-policies/resolved-policy/" + tenantId.getValue()
                + "/" + consEpg.getValue() + "/" + tenantId.getValue() + "/" + provEpg.getValue() + "/";
        Assert.assertEquals(target, resolvedPolicyListener.buildPolicyUris(resolvedPolicy));
    }

    // TODO FIXME Need to resolve correct REST MOCK in order to remove expected exception.
    @Test(expected = ClientHandlerException.class)
    public void processResolvedPolicyNotificationTest() {
        iomInstance = new IovisorModuleInstanceBuilder().setId(new IovisorModuleId(iom1)).setUri(new Uri(iom1)).build();
        List<IovisorModuleInstanceId> listIomId = new ArrayList<>();
        listIomId.add(new IovisorModuleInstanceIdBuilder().setId(new IovisorModuleId(iom1)).build());
        when(iovisorModuleManager.getActiveIovisorModule(any(IovisorModuleId.class))).thenReturn(iomInstance);
        when(iovisorModuleManager.getIovisorModulesByTenantByEpg(any(TenantId.class), any(EndpointGroupId.class)))
            .thenReturn(listIomId);
        resolvedPolicyListener.processResolvedPolicyNotification(resolvedPolicy);
    }
}
