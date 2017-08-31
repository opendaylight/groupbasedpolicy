/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.faas;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Text;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.parameter.values.grouping.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.parameter.values.grouping.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.parameter.values.grouping.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.SecurityRuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.SecurityRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.SecurityRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.SecurityRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.security.rule.RuleAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.security.rule.RuleActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.security.rule.RuleClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.security.rule.RuleClassifier.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.security.rule.groups.security.rule.group.security.rule.RuleClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaasContractManagerListener implements DataTreeChangeListener<Contract> {

    private static final Logger LOG = LoggerFactory.getLogger(FaasContractManagerListener.class);
    private final ConcurrentHashMap<ContractId, Uuid> mappedContracts = new ConcurrentHashMap<>();
    private final Executor executor;
    private final DataBroker dataProvider;
    private final TenantId gbpTenantId;
    private final Uuid faasTenantId;

    public FaasContractManagerListener(DataBroker dataProvider, TenantId gbpTenantId, Uuid faasTenantId,
            Executor executor) {
        this.executor = executor;
        this.gbpTenantId = gbpTenantId;
        this.faasTenantId = faasTenantId;
        this.dataProvider = dataProvider;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Contract>> changes) {
        executor.execute(() -> executeEvent(changes));
    }

    private void executeEvent(final Collection<DataTreeModification<Contract>> changes) {
        for (DataTreeModification<Contract> change: changes) {
            DataObjectModification<Contract> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    Contract updatedContract = rootNode.getDataAfter();
                    LOG.debug("Contract {} is Updated.", updatedContract.getId().getValue());
                    UlnDatastoreApi.submitSecurityGroupsToDs(initSecurityGroupBuilder(updatedContract).build());
                    break;
                case DELETE:
                    Contract deletedContract = rootNode.getDataBefore();
                    LOG.debug("Contract {} is removed.", deletedContract.getId().getValue());
                    ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
                    Optional<MappedContract> op = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                            FaasIidFactory.mappedContractIid(gbpTenantId, deletedContract.getId()), rwTx);
                    if (op.isPresent()) {
                        DataStoreHelper.submitToDs(rwTx);
                    }
                    Uuid val = mappedContracts.remove(deletedContract.getId());
                    if (val != null) {
                        UlnDatastoreApi.removeSecurityGroupsFromDsIfExists(faasTenantId, val);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void loadAll(List<Contract> contracts, List<MappedContract> mpContracts) {
        if (mpContracts != null) {
            for (MappedContract mpContract : mpContracts) {
                mappedContracts.putIfAbsent(mpContract.getGbpContractId(), mpContract.getFaasSecurityRulesId());
            }
        }
        if (contracts != null) {
            for (Contract contract : contracts) {
                LOG.debug("Loading Contract {}", contract.getId().getValue());
                UlnDatastoreApi.submitSecurityGroupsToDs(initSecurityGroupBuilder(contract).build());
            }
        }
    }

    protected SecurityRuleGroupsBuilder initSecurityGroupBuilder(Contract contract) {
        LOG.trace("Start initSecurityGroupBuilder");
        SecurityRuleGroupsBuilder builder = new SecurityRuleGroupsBuilder();
        builder.setUuid(getFaasSecurityRulesId(contract.getId()));
        builder.setName(new Text(contract.getId().getValue()));
        if (contract.getDescription() != null) {
            builder.setDescription(new Text("gbp-contract: " + contract.getDescription().getValue()));
        } else {
            builder.setDescription(new Text("gbp-contract"));
        }
        builder.setTenantId(faasTenantId);
        builder.setSecurityRuleGroup(buildSecurityRuleGroup(contract));
        LOG.trace("Contract {} is mapped to Faas Security Rules {} ", contract.getId().getValue(), builder.getUuid()
            .getValue());
        return builder;
    }

    private Uuid getFaasSecurityRulesId(ContractId contractId) {
        Uuid val = mappedContracts.get(contractId);
        if (val != null) {
            return val;
        }
        Uuid faasContractId = null;
        if (FaasPolicyManager.isUUid(contractId.getValue())) {
            faasContractId = new Uuid(contractId.getValue());
        } else {
            faasContractId = new Uuid(UUID.randomUUID().toString());
        }
        mappedContracts.putIfAbsent(contractId, faasContractId);
        val = mappedContracts.get(contractId);
        MappedContractBuilder builder = new MappedContractBuilder();
        builder.setFaasSecurityRulesId(val);
        builder.setGbpContractId(contractId);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        MappedContract result = builder.build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedContractIid(gbpTenantId, contractId), result);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("Cached in Datastore Mapped Contract {}", result);
        } else {
            LOG.error("Couldn't Cache in Datastore Mapped Contract {}", result);
        }
        return val;
    }

    @VisibleForTesting
    List<SecurityRuleGroup> buildSecurityRuleGroup(Contract contract) {
        LOG.trace("Start buildSecurityRuleGroup for contract {}", contract.getId().getValue());
        List<SecurityRuleGroup> securityRuleGroups = new ArrayList<>();
        if (contract.getClause() == null) {
            LOG.debug("contract {} has no Clause", contract.getId().getValue());
            return null;
        }
        for (Clause clause : contract.getClause()) {
            if (clause.getSubjectRefs() == null) {
                LOG.debug("Clause {} in contract {} has no Subject Ref", clause.getName().getValue(), contract.getId()
                    .getValue());
                continue;
            }
            if (contract.getSubject() == null) {
                LOG.warn("Couldn't find in Contract {} the expected subject references", contract.getId().getValue());
                continue;
            }
            for (SubjectName subjectRef : clause.getSubjectRefs()) {
                LOG.trace("Start Parsing Subject Ref {} in Contract {}", subjectRef, contract.getId().getValue());
                for (Subject sub : contract.getSubject()) {
                    if (subjectRef.equals(sub.getName())) {
                        SecurityRuleGroupBuilder securityRuleGroupBuilder = new SecurityRuleGroupBuilder();
                        securityRuleGroupBuilder.setName(new Name(subjectRef.getValue()));
                        List<Rule> subRules = sub.getRule();
                        if (subRules == null) {
                            LOG.warn("Subject {} in Contract {} doesn't have rules", subjectRef.getValue(),
                                    contract.getId().getValue());
                        } else {
                            List<SecurityRule> securityRules = getSecurityRules(contract, subjectRef, subRules);
                            LOG.debug("Subject {} in Contract {} has {} rules", subjectRef.getValue(), contract.getId()
                                .getValue(), securityRules.size());
                            securityRuleGroupBuilder.setSecurityRule(securityRules);
                        }
                        LOG.debug("Added Rule {} to Subject {} in Contract {}", securityRuleGroupBuilder.getName()
                            .getValue(), subjectRef.getValue(), contract.getId().getValue());
                        securityRuleGroups.add(securityRuleGroupBuilder.build());
                    }
                }
            }
        }
        LOG.trace("Done with buildSecurityRuleGroup for contract {}", contract.getId().getValue());
        return securityRuleGroups;
    }

    @VisibleForTesting
    List<SecurityRule> getSecurityRules(Contract contract, SubjectName subjectRef, List<Rule> subRules) {
        List<SecurityRule> securityRules = new ArrayList<>();
        for (Rule rule : subRules) {
            List<ClassifierRef> classifierRefs = rule.getClassifierRef();
            List<RuleClassifier> pClassifiers = null;
            if (classifierRefs == null || classifierRefs.isEmpty()) {
                LOG.warn("Rule {} in Subject {} in Contract {} doesn't have classifiers", rule.getName(), subjectRef,
                        contract.getId());
            } else {
                pClassifiers = getClassifiers(gbpTenantId, contract, classifierRefs, dataProvider);
                if (pClassifiers == null || pClassifiers.isEmpty()) {
                    LOG.warn("Rule {} in Subject {} in Contract {} doesn't have classifiers -- Will ignore this rule",
                            rule.getName(), subjectRef, contract.getId());
                }
            }
            List<ActionRef> actionRefs = rule.getActionRef();
            List<RuleAction> pActions = null;
            if (actionRefs == null || actionRefs.isEmpty()) {
                LOG.warn("Rule {} in Subject {} in Contract {} doesn't have actions", rule.getName(), subjectRef,
                        contract.getId());
            } else {
                pActions = getActions(contract, actionRefs);
                if (pActions == null || pActions.isEmpty()) {
                    LOG.warn("Rule {} in Subject {} in Contract {} doesn't have actions", rule.getName(), subjectRef,
                            contract.getId());
                }
            }

            securityRules.add(new SecurityRuleBuilder().setName(new Name(rule.getName().getValue()))
                .setOrder(rule.getOrder())
                .setRuleClassifier(pClassifiers)
                .setRuleAction(pActions)
                .build());
        } // for rules
        return securityRules;
    }

    private List<RuleAction> getActions(Contract contract, List<ActionRef> actionRefs) {
        LOG.trace("Start Parsing Actions for actionRefs count {} in Contract {}", actionRefs.size(), contract.getId()
            .getValue());
        List<RuleAction> pActions = new ArrayList<>();
        for (ActionRef actionRef : actionRefs) {
            if (actionRef.getName() == null) {
                LOG.warn("Couldn't find an Action in Contract {} -- ignored Action", contract.getId().getValue());
                continue;
            }
            RuleActionBuilder ruleActionBuilder = new RuleActionBuilder();
            ruleActionBuilder.setName(new Name(actionRef.getName().getValue()));
            ruleActionBuilder.setOrder(actionRef.getOrder());
            ActionInstance actionInstance = getActionInstance(actionRef.getName());
            if (actionInstance == null) {
                LOG.warn("Action instance {} is not found -- will only use the Action ref info", actionRef.getName());
            } else {
                if (actionInstance.getActionDefinitionId() != null) {
                    ruleActionBuilder.setAdditionalInfo(new Text(actionInstance.getActionDefinitionId().getValue()));
                }
                List<ParameterValue> parms = null;
                if (actionInstance.getParameterValue() != null) {
                    LOG.trace("Action Instance {} has {} parameters", actionInstance.getName().getValue(),
                            actionInstance.getParameterValue().size());
                    parms = new ArrayList<>();
                    for (org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue instance : actionInstance.getParameterValue()) {
                        ParameterValueBuilder pBuilder = new ParameterValueBuilder();
                        pBuilder.setIntValue(instance.getIntValue());
                        if (instance.getName() != null) {
                            pBuilder.setName(new Name(instance.getName().getValue()));
                        }
                        pBuilder.setStringValue(instance.getStringValue());
                        if (instance.getRangeValue() != null) {
                            RangeValueBuilder rBuilder = new RangeValueBuilder().setMax(
                                    instance.getRangeValue().getMax()).setMin(instance.getRangeValue().getMin());
                            pBuilder.setRangeValue(rBuilder.build());
                        }
                        ParameterValue parm = pBuilder.build();
                        LOG.trace("Added Parm {} from Action Instance {}", parm, actionInstance.getName().getValue());
                        parms.add(parm);
                    }
                } else {
                    LOG.trace("Action Instance {} has no parameters", actionInstance.getName().getValue());
                }
                ruleActionBuilder.setParameterValue(parms);
            }
            pActions.add(ruleActionBuilder.build());
        }

        return pActions;
    }

    private ActionInstance getActionInstance(ActionName name) {
        ReadOnlyTransaction trans = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<ActionInstance> ciId = InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(gbpTenantId))
            .child(Policy.class)
            .child(SubjectFeatureInstances.class)
            .child(ActionInstance.class, new ActionInstanceKey(name))
            .build();
        try {
            Optional<ActionInstance> data = trans.read(LogicalDatastoreType.CONFIGURATION, ciId).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (Exception e) {
            LOG.error("Couldn't read Action instance from datastore. Exception: ", e);
        }
        return null;
    }

    private List<RuleClassifier> getClassifiers(TenantId tenantId, Contract contract,
            List<ClassifierRef> classifierRefs, DataBroker dataProvider) {
        List<RuleClassifier> fClassifiers = new ArrayList<>();
        for (ClassifierRef classifierRef : classifierRefs) {
            if (classifierRef.getName() == null) {
                LOG.warn("Found a Classifer without name in Contract {} ", contract.getId().getValue());
                continue;
            }
            RuleClassifierBuilder ruleClassifierBuilder = new RuleClassifierBuilder();
            ruleClassifierBuilder.setName(new Name(classifierRef.getName().getValue()));
            ClassifierInstance classifierInstance = getClassifierInstance(tenantId, classifierRef.getName(),
                    dataProvider);
            if (classifierInstance == null) {
                LOG.warn("Classifer instance {} is not found -- will only use the classifier Ref info",
                        classifierRef.getName());
            } else {
                if (classifierInstance.getClassifierDefinitionId() != null) {
                    ruleClassifierBuilder.setAdditionalInfo(new Text(classifierInstance.getClassifierDefinitionId()
                        .getValue()));
                }
                List<ParameterValue> parms = null;
                if (classifierInstance.getParameterValue() != null) {
                    LOG.trace("Calssifier Instance {} has {} parameters", classifierInstance.getName().getValue(),
                            classifierInstance.getParameterValue().size());
                    parms = new ArrayList<>();
                    for (org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue instance : classifierInstance.getParameterValue()) {
                        ParameterValueBuilder pBuilder = new ParameterValueBuilder();
                        pBuilder.setIntValue(instance.getIntValue());
                        pBuilder.setName(new Name(instance.getName().getValue()));
                        pBuilder.setStringValue(instance.getStringValue());
                        if (instance.getRangeValue() != null) {
                            RangeValueBuilder rBuilder = new RangeValueBuilder().setMax(
                                    instance.getRangeValue().getMax()).setMin(instance.getRangeValue().getMin());
                            pBuilder.setRangeValue(rBuilder.build());
                        }
                        ParameterValue parm = pBuilder.build();
                        LOG.trace("Added parm {} from Classifier Instance {}", parm, classifierInstance.getName()
                            .getValue());
                        parms.add(parm);
                    }
                } else {
                    LOG.trace("Classifier Instance {} has no parameters", classifierInstance.getName().getValue());
                }
                ruleClassifierBuilder.setParameterValue(parms);
            }
            if (classifierRef.getDirection() != null) {
                ruleClassifierBuilder.setDirection(Direction.forValue(classifierRef.getDirection().getIntValue()));
            } else {
                ruleClassifierBuilder.setDirection(Direction.Bidirectional);
            }
            fClassifiers.add(ruleClassifierBuilder.build());
        }
        return fClassifiers;
    }

    private ClassifierInstance getClassifierInstance(TenantId tenantId, ClassifierName name, DataBroker dataProvider) {
        ReadOnlyTransaction trans = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<ClassifierInstance> ciId = InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(SubjectFeatureInstances.class)
            .child(ClassifierInstance.class, new ClassifierInstanceKey(name))
            .build();
        try {
            Optional<ClassifierInstance> data = trans.read(LogicalDatastoreType.CONFIGURATION, ciId).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (Exception e) {
            LOG.error("Couldn't read Classifier instance from datastore. Exception: ", e);
        }
        return null;
    }
}
