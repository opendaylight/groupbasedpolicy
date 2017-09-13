/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.SecurityRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FaasContractManagerListenerCovrgTest {

    private static final ClauseName CLAUSE_NAME = new ClauseName("clause-1");
    public static final SubjectName SUBJECT_NAME = new SubjectName("subject-name");
    private InstanceIdentifier<Contract> contractIid;
    private final ContractId contractId = new ContractId("contractId");
    private FaasContractManagerListener listener;
    private final TenantId gbpTenantId = new TenantId("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private DataBroker dataProvider;
    private final UlnDatastoreUtil mockUlnDatastoreUtil = mock(UlnDatastoreUtil.class);

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        contractIid = mock(InstanceIdentifier.class);
        dataProvider = mock(DataBroker.class);

        listener = new FaasContractManagerListener(dataProvider, gbpTenantId, faasTenantId,
                MoreExecutors.directExecutor(), mockUlnDatastoreUtil);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteEvent() throws ReadFailedException {
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        WriteTransaction woTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(rwTx.submit()).thenReturn(futureVoid);
        when(woTx.submit()).thenReturn(futureVoid);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(woTx);

        CheckedFuture<Optional<MappedContract>, ReadFailedException> futureMappedContract = mock(CheckedFuture.class);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL, FaasIidFactory.mappedContractIid(gbpTenantId, contractId)))
            .thenReturn(futureMappedContract);
        Optional<MappedContract> optMappedContract = mock(Optional.class);
        when(optMappedContract.isPresent()).thenReturn(true);
        when(futureMappedContract.checkedGet()).thenReturn(optMappedContract);

        Contract contract = new ContractBuilder().setId(contractId).build();

        DataTreeModification<Contract> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Contract> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(contract).when(mockModification).getDataAfter();

        listener.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));
    }

    @Test
    public void testBuildSecurityRuleGroup(){
        List<SecurityRuleGroup> securityRuleGroups;

        Clause clause = new ClauseBuilder()
                .setName(CLAUSE_NAME)
                .setSubjectRefs(ImmutableList.of(SUBJECT_NAME))
                .build();

        Contract contractNoClause = new ContractBuilder()
                .setId(contractId)
                .build();

        securityRuleGroups = listener.buildSecurityRuleGroup(contractNoClause);
        assertNull(securityRuleGroups);

        Clause clauseNoSubjectRefs = new ClauseBuilder()
                .setName(CLAUSE_NAME)
                .build();
        Contract contractClauseNoSubjectRefs = new ContractBuilder()
                .setId(contractId)
                .setClause(ImmutableList.of(clauseNoSubjectRefs))
                .build();

        securityRuleGroups = listener.buildSecurityRuleGroup(contractClauseNoSubjectRefs);
        assertNotNull(securityRuleGroups);
        assertTrue(securityRuleGroups.isEmpty());

        Contract contractNoSubject = new ContractBuilder()
                .setId(contractId)
                .setClause(ImmutableList.of(clause))
                .build();

        securityRuleGroups = listener.buildSecurityRuleGroup(contractNoSubject);
        assertNotNull(securityRuleGroups);
        assertTrue(securityRuleGroups.isEmpty());

        Contract contract = new ContractBuilder()
                .setId(contractId)
                .setClause(ImmutableList.of(clause))
                .setSubject(ImmutableList.of(new SubjectBuilder().setName(SUBJECT_NAME).build()))
                .build();

        securityRuleGroups = listener.buildSecurityRuleGroup(contract);
        assertNotNull(securityRuleGroups);
        assertFalse(securityRuleGroups.isEmpty());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetSecurityRules() throws ReadFailedException, ExecutionException, InterruptedException {
        List<Rule> rules;
        List<SecurityRule> securityRules;

        Clause clause = new ClauseBuilder()
                .setName(CLAUSE_NAME)
                .setSubjectRefs(ImmutableList.of(SUBJECT_NAME))
                .build();

        Contract contract = new ContractBuilder()
                .setId(contractId)
                .setClause(ImmutableList.of(clause))
                .setSubject(ImmutableList.of(new SubjectBuilder().setName(SUBJECT_NAME).build()))
                .build();

        Rule ruleNoRefs = new RuleBuilder()
                .setName(new RuleName("rule-no-refs"))
                .setOrder(1)
                .build();

        Rule ruleEmptyRefs = new RuleBuilder()
                .setClassifierRef(ImmutableList.of())
                .setActionRef(ImmutableList.of())
                .setName(new RuleName("rule-empty-refs"))
                .setOrder(2)
                .build();

        rules = ImmutableList.of(ruleNoRefs, ruleEmptyRefs);

        securityRules = listener.getSecurityRules(contract, SUBJECT_NAME, rules);

        assertEquals(rules.size(), securityRules.size());

        ClassifierName CLASSIFIER_NAME = new ClassifierName("classifier-1");
        ClassifierRef classifierRef = new ClassifierRefBuilder()
                .setName(CLASSIFIER_NAME)
                .build();
        Rule rule = new RuleBuilder()
                .setClassifierRef(ImmutableList.of(classifierRef))
                .setActionRef(ImmutableList.of())
                .setName(new RuleName("rule-1"))
                .setOrder(2)
                .build();

        ReadOnlyTransaction roTx1 = mock(ReadOnlyTransaction.class);

        CheckedFuture<Optional<ClassifierInstance>, ReadFailedException> futureClassifierInstance = mock(CheckedFuture.class);
        Optional<ClassifierInstance> optClassifierInstance = mock(Optional.class);
        when(optClassifierInstance.isPresent()).thenReturn(false);
        when(futureClassifierInstance.checkedGet()).thenReturn(optClassifierInstance);
        when(roTx1.read(eq(LogicalDatastoreType.CONFIGURATION),
                Mockito.<InstanceIdentifier>any())).thenReturn(futureClassifierInstance);

        rules = ImmutableList.of(rule);
        securityRules = listener.getSecurityRules(contract, SUBJECT_NAME, rules);

        assertEquals(rules.size(), securityRules.size());

    }

}
