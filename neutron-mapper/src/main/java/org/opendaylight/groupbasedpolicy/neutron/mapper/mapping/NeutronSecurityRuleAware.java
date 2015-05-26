package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronMapperIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class NeutronSecurityRuleAware implements INeutronSecurityRuleAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityRuleAware.class);
    private final DataBroker dataProvider;

    public NeutronSecurityRuleAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    @Override
    public int canCreateNeutronSecurityRule(NeutronSecurityRule securityRule) {
        LOG.trace("canCreateNeutronSecurityRule - {}", securityRule);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void neutronSecurityRuleCreated(NeutronSecurityRule securityRule) {
        LOG.trace("neutronSecurityRuleCreated - {}", securityRule);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleAdded = addNeutronSecurityRule(securityRule, rwTx);
        if (isNeutronSecurityRuleAdded) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
    }

    /**
     * <b>ASSUMPTION</b>: Endpoint group with id
     * {@link NeutronSecurityRule#getSecurityRuleGroupID()} and
     * endpoint group with id {@link NeutronSecurityRule#getSecurityRemoteGroupID()} already exist
     * in transaction.
     *
     * @param secRule neutron security rule from which GBP entities are created
     * @param rwTx GBP entities are stored to this transaction. This method NEVER submits or cancel
     *        the transaction.
     * @return {@code true} if operation was successful; {@code false} if an illegal state occurs -
     *         the transaction may contain just partial result
     */
    public static boolean addNeutronSecurityRule(NeutronSecurityRule secRule, ReadWriteTransaction rwTx) {
        TransformSecRule transform = new TransformSecRule(secRule);
        TenantId tenantId = transform.getTenantId();
        EndpointGroupId providerEpgId = transform.getProviderEpgId();
        EndpointGroupId consumerEpgId = transform.getConsumerEpgId();
        SubjectName subjectName = transform.getSubjectName();

        Optional<ContractId> potentialContractId = readContractIdFromEpgPairToContractMapping(providerEpgId,
                consumerEpgId, rwTx);
        ContractId contractId = null;
        if (potentialContractId.isPresent()) {
            contractId = potentialContractId.get();
            Optional<Subject> potentialSubject = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.subjectIid(tenantId, contractId, subjectName), rwTx);
            if (!potentialSubject.isPresent()) {
                // it also means that clause for this subject does not exist
                rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subjectIid(tenantId, contractId, subjectName),
                        transform.createSubject());
                rwTx.put(LogicalDatastoreType.CONFIGURATION,
                        IidFactory.clauseIid(tenantId, contractId, transform.getClauseName()), transform.createClause());
            }
        } else {
            // check assumption that provider EPG exists
            Optional<EndpointGroup> potentialProviderEpg = DataStoreHelper.readFromDs(
                    LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, providerEpgId), rwTx);
            if (!potentialProviderEpg.isPresent()) {
                LOG.warn("Illegal state - Endpoint group {} does not exist.", providerEpgId.getValue());
                return false;
            }

            if (providerEpgId.equals(consumerEpgId)) {
                EndpointGroup providerConsumerEpg = potentialProviderEpg.get();
                if (providerConsumerEpg.getIntraGroupPolicy() == null
                        || !providerConsumerEpg.getIntraGroupPolicy().equals(IntraGroupPolicy.RequireContract)) {
                    EndpointGroup newProviderConsumerEpg = new EndpointGroupBuilder(providerConsumerEpg).setIntraGroupPolicy(
                            IntraGroupPolicy.RequireContract)
                        .build();
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, providerEpgId),
                            newProviderConsumerEpg);
                }
            } else {
                Optional<EndpointGroup> potentialConsumerEpg = DataStoreHelper.readFromDs(
                        LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, consumerEpgId), rwTx);
                if (!potentialConsumerEpg.isPresent()) {
                    if (MappingUtils.EPG_ANY_ID.equals(consumerEpgId)) {
                        EndpointGroup epgAny = createEpgAny();
                        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_ANY_ID),
                                epgAny);
                    } else {
                        LOG.warn("Illegal state - Endpoint group {} does not exist.", consumerEpgId.getValue());
                        return false;
                    }
                }
            }
            // creates and stores contract with clause and subject
            Subject subject = transform.createSubject();
            Clause clause = transform.createClause();
            Contract contract = createContract(clause, subject);
            contractId = contract.getId();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, contractId), contract);
            putEpgPairToContractMapping(providerEpgId, consumerEpgId, contractId, rwTx);

            // adds provider and consumer named selectors
            ProviderNamedSelector providerSelector = createProviderNamedSelector(contractId);
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.providerNamedSelectorIid(tenantId, providerEpgId, providerSelector.getName()),
                    providerSelector);
            ConsumerNamedSelector consumerSelector = createConsumerNamedSelector(contractId);
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.consumerNamedSelectorIid(tenantId, consumerEpgId, consumerSelector.getName()),
                    consumerSelector);
        }

        // create classifier-instance
        ClassifierName classifierName = transform.getClassifierName();
        ClassifierInstance classifier = transform.createClassifier();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierInstanceIid(tenantId, classifierName),
                classifier, true);
        // create action-instance if it does not exist yet
        Optional<ActionInstance> potentialAction = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.actionInstanceIid(tenantId, MappingUtils.ACTION_ALLOW.getName()), rwTx);
        if (!potentialAction.isPresent()) {
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.actionInstanceIid(tenantId, MappingUtils.ACTION_ALLOW.getName()),
                    MappingUtils.ACTION_ALLOW, true);
        }

        // create rule
        Rule rule = transform.createRule(0);
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.ruleIid(tenantId, contractId, subjectName, rule.getName()), rule);
        return true;
    }

    private static EndpointGroup createEpgAny() {
        return new EndpointGroupBuilder().setId(MappingUtils.EPG_ANY_ID)
                .setDescription(new Description(MappingUtils.NEUTRON_RULE__ + "epg_any"))
                .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
                .build();
    }

    @Override
    public int canUpdateNeutronSecurityRule(NeutronSecurityRule delta, NeutronSecurityRule original) {
        LOG.warn("canUpdateNeutronSecurityRule - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group rule. \nDelta: {} \nOriginal: {}",
                delta, original);
        return StatusCode.BAD_REQUEST;
    }

    @Override
    public void neutronSecurityRuleUpdated(NeutronSecurityRule securityRule) {
        LOG.warn("neutronSecurityRuleUpdated - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group rule. \nSecurity group rule: {}",
                securityRule);
    }

    @Override
    public int canDeleteNeutronSecurityRule(NeutronSecurityRule securityRule) {
        LOG.trace("canDeleteNeutronSecurityRule - {}", securityRule);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void neutronSecurityRuleDeleted(NeutronSecurityRule secRule) {
        LOG.trace("neutronSecurityRuleCreated - {}", secRule);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleDelete = deleteNeutronSecurityRule(secRule, rwTx);
        if (isNeutronSecurityRuleDelete) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    /**
     * @param secRule neutron security rule from which GBP entities are deleted
     * @param rwTx GBP entities are stored to this transaction. This method NEVER submits or cancel
     *        the transaction.
     * @return {@code true} if operation was successful; {@code false} if an illegal state occurs -
     *         the transaction may contain just partial result
     */
    public static boolean deleteNeutronSecurityRule(NeutronSecurityRule secRule, ReadWriteTransaction rwTx) {
        TransformSecRule transform = new TransformSecRule(secRule);
        TenantId tenantId = transform.getTenantId();
        EndpointGroupId providerEpgId = transform.getProviderEpgId();
        EndpointGroupId consumerEpgId = transform.getConsumerEpgId();

        Optional<ContractId> potentialContractId = readContractIdFromEpgPairToContractMapping(providerEpgId,
                consumerEpgId, rwTx);
        if (!potentialContractId.isPresent()) {
            LOG.warn("Illegal state - mapping EPG pair (provider EPG {} consumer EPG {}) does not exist.",
                    providerEpgId.getValue(), consumerEpgId.getValue());
            return false;
        }

        ContractId contractId = potentialContractId.get();
        ClassifierName classifierName = transform.getClassifierName();
        InstanceIdentifier<ClassifierInstance> classifierIid = IidFactory.classifierInstanceIid(tenantId,
                classifierName);
        Optional<ClassifierInstance> potentialClassifier = DataStoreHelper.removeIfExists(
                LogicalDatastoreType.CONFIGURATION, classifierIid, rwTx);
        if (!potentialClassifier.isPresent()) {
            LOG.warn("Illegal state - classifier-instance {} does not exist. {}", classifierName.getValue(),
                    classifierIid);
            return false;
        }

        RuleName ruleName = transform.getRuleName();
        SubjectName subjectName = transform.getSubjectName();
        InstanceIdentifier<Rule> ruleIid = IidFactory.ruleIid(tenantId, contractId, subjectName, ruleName);
        Optional<Rule> potentionalRule = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, ruleIid,
                rwTx);
        if (!potentionalRule.isPresent()) {
            LOG.warn("Illegal state - rule {} does not exist. {}", ruleName.getValue(), ruleIid);
            return false;
        }

        InstanceIdentifier<Subject> subjectIid = IidFactory.subjectIid(tenantId, contractId, subjectName);
        Optional<Subject> potentionalSubject = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                subjectIid, rwTx);
        if (!potentionalSubject.isPresent()) {
            LOG.warn("Illegal state - subject {} does not exist. {}", subjectName.getValue(), subjectName);
            return false;
        }

        ClauseName clauseName = transform.getClauseName();
        InstanceIdentifier<Clause> clauseIid = IidFactory.clauseIid(tenantId, contractId, clauseName);
        Optional<Clause> potentialClause = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, clauseIid,
                rwTx);
        if (!potentialClause.isPresent()) {
            LOG.warn("Illegal state - clause {} does not exist. {}", clauseName.getValue(), clauseIid);
            return false;
        }

        Subject subject = potentionalSubject.get();
        if (subject.getRule() == null || subject.getRule().isEmpty()) {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, clauseIid);
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, subjectIid);
        }

        InstanceIdentifier<Contract> contractIid = IidFactory.contractIid(tenantId, contractId);
        Optional<Contract> potentialContract = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                contractIid, rwTx);
        if (!potentialContract.isPresent()) {
            LOG.warn("Illegal state - contract {} does not exist. {}", contractId.getValue(), contractIid);
            return false;
        }

        Contract contract = potentialContract.get();
        if (contract.getSubject() == null || contract.getSubject().isEmpty()) {
            // remove contract and named selectors from EPGs
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, contractIid);
            SelectorName providerSelectorName = createNameOfNamedSelector(contractId);
            InstanceIdentifier<ProviderNamedSelector> providerSelectorIid = IidFactory.providerNamedSelectorIid(
                    tenantId, providerEpgId, providerSelectorName);
            Optional<ProviderNamedSelector> potentialProviderSelector = DataStoreHelper.removeIfExists(
                    LogicalDatastoreType.CONFIGURATION, providerSelectorIid, rwTx);
            if (!potentialProviderSelector.isPresent()) {
                LOG.warn("Illegal state - provider-name-selector {} does not exist. {}",
                        providerSelectorName.getValue(), providerSelectorIid);
                return false;
            }
            SelectorName consumerSelectorName = createNameOfNamedSelector(contractId);
            InstanceIdentifier<ConsumerNamedSelector> consumerSelectorIid = IidFactory.consumerNamedSelectorIid(
                    tenantId, consumerEpgId, consumerSelectorName);
            Optional<ConsumerNamedSelector> potentialConsuemrSelector = DataStoreHelper.removeIfExists(
                    LogicalDatastoreType.CONFIGURATION, consumerSelectorIid, rwTx);
            if (!potentialConsuemrSelector.isPresent()) {
                LOG.warn("Illegal state - consumer-name-selector {} does not exist. {}",
                        consumerSelectorName.getValue(), consumerSelectorIid);
                return false;
            }
        }
        return true;
    }

    public static Optional<ContractId> readContractIdFromEpgPairToContractMapping(EndpointGroupId providerEpgId,
            EndpointGroupId consumerEpgId, ReadTransaction rTx) {
        Optional<EndpointGroupPairToContractMapping> potentialMapping = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL,
                NeutronMapperIidFactory.endpointGroupPairToContractMappingIid(providerEpgId, consumerEpgId), rTx);
        if (potentialMapping.isPresent()) {
            return Optional.of(potentialMapping.get().getContractId());
        }
        return Optional.absent();
    }

    private static void putEpgPairToContractMapping(EndpointGroupId providerEpgId, EndpointGroupId consumerEpgId,
            ContractId contractId, WriteTransaction wTx) {
        EndpointGroupPairToContractMapping epgPairToContractMapping = new EndpointGroupPairToContractMappingBuilder().setProviderEpgId(
                providerEpgId)
            .setConsumerEpgId(consumerEpgId)
            .setContractId(contractId)
            .build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, NeutronMapperIidFactory.endpointGroupPairToContractMappingIid(
                epgPairToContractMapping.getProviderEpgId(), epgPairToContractMapping.getConsumerEpgId()),
                epgPairToContractMapping, true);
    }

    private static Contract createContract(Clause clause, Subject subject) {
        ContractId contractId = new ContractId(UUID.randomUUID().toString());
        return new ContractBuilder().setId(contractId)
            .setClause(ImmutableList.of(clause))
            .setSubject(ImmutableList.of(subject))
            .build();
    }

    private static ProviderNamedSelector createProviderNamedSelector(ContractId contractId) {
        return new ProviderNamedSelectorBuilder().setName(createNameOfNamedSelector(contractId))
            .setContract(ImmutableList.of(contractId))
            .build();
    }

    private static ConsumerNamedSelector createConsumerNamedSelector(ContractId contractId) {
        return new ConsumerNamedSelectorBuilder().setName(createNameOfNamedSelector(contractId))
            .setContract(ImmutableList.of(contractId))
            .build();
    }

    private static SelectorName createNameOfNamedSelector(ContractId contractId) {
        return new SelectorName(MappingUtils.NEUTRON_RULE__ + contractId.getValue());
    }

}
