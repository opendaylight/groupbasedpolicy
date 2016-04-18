/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.MappingProcessor;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.StatusCode;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.SecGroupDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolUdp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class NeutronSecurityRuleAware implements MappingProcessor<SecurityRule, NeutronSecurityRule> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityRuleAware.class);
    private static final String CONTRACT_PROVIDER = "Contract provider: ";
    private final DataBroker dataProvider;
    private final SecRuleDao secRuleDao;
    private final SecGroupDao secGroupDao;
    private final Multiset<InstanceIdentifier<ClassifierInstance>> createdClassifierInstances;
    private final Multiset<InstanceIdentifier<ActionInstance>> createdActionInstances;
    final static String PROVIDED_BY = "provided_by-";
    final static String POSSIBLE_CONSUMER = "possible_consumer-";

    public NeutronSecurityRuleAware(DataBroker dataProvider, SecRuleDao secRuleDao, SecGroupDao secGroupDao) {
        this(dataProvider, secRuleDao, secGroupDao, HashMultiset.<InstanceIdentifier<ClassifierInstance>>create(),
                HashMultiset.<InstanceIdentifier<ActionInstance>>create());
    }

    @VisibleForTesting
    NeutronSecurityRuleAware(DataBroker dataProvider, SecRuleDao secRuleDao, SecGroupDao secGroupDao,
            Multiset<InstanceIdentifier<ClassifierInstance>> classifierInstanceNames,
            Multiset<InstanceIdentifier<ActionInstance>> createdActionInstances) {
        this.dataProvider = checkNotNull(dataProvider);
        this.secRuleDao = checkNotNull(secRuleDao);
        this.secGroupDao = checkNotNull(secGroupDao);
        this.createdClassifierInstances = checkNotNull(classifierInstanceNames);
        this.createdActionInstances = checkNotNull(createdActionInstances);
    }

    // copied from Neutron's NeutronSecurityRuleInterface
    private static final ImmutableBiMap<Class<? extends DirectionBase>,String> DIRECTION_MAP
    = new ImmutableBiMap.Builder<Class<? extends DirectionBase>,String>()
    .put(DirectionEgress.class,"egress")
    .put(DirectionIngress.class,"ingress")
    .build();

    // copied from Neutron's NeutronSecurityRuleInterface
    private static final ImmutableBiMap<Class<? extends EthertypeBase>,String> ETHERTYPE_MAP
    = new ImmutableBiMap.Builder<Class<? extends EthertypeBase>,String>()
    .put(EthertypeV4.class,"IPv4")
    .put(EthertypeV6.class,"IPv6")
    .build();

    // copied from Neutron's NeutronSecurityRuleInterface
    private static final ImmutableBiMap<Class<? extends ProtocolBase>,String> PROTOCOL_MAP
    = new ImmutableBiMap.Builder<Class<? extends ProtocolBase>,String>()
    .put(ProtocolIcmp.class,"icmp")
    .put(ProtocolTcp.class,"tcp")
    .put(ProtocolUdp.class,"udp")
    .put(ProtocolIcmpV6.class,"icmpv6")
    .build();

    @Override
    public NeutronSecurityRule convertToNeutron(SecurityRule rule) {
        // copied from Neutron's NeutronSecurityRuleInterface
        NeutronSecurityRule answer = new NeutronSecurityRule();
        if (rule.getTenantId() != null) {
            answer.setTenantID(rule.getTenantId());
        }
        if (rule.getDirection() != null) {
            answer.setSecurityRuleDirection(DIRECTION_MAP.get(rule.getDirection()));
        }
        if (rule.getSecurityGroupId() != null) {
            answer.setSecurityRuleGroupID(rule.getSecurityGroupId().getValue());
        }
        if (rule.getRemoteGroupId() != null) {
            answer.setSecurityRemoteGroupID(rule.getRemoteGroupId().getValue());
        }
        if (rule.getRemoteIpPrefix() != null) {
            answer.setSecurityRuleRemoteIpPrefix(new String(rule.getRemoteIpPrefix().getValue()));
        }
        if (rule.getProtocol() != null) {
            answer.setSecurityRuleProtocol(PROTOCOL_MAP.get(rule.getProtocol()));
        }
        if (rule.getEthertype() != null) {
            answer.setSecurityRuleEthertype(ETHERTYPE_MAP.get(rule.getEthertype()));
        }
        if (rule.getPortRangeMin() != null) {
            answer.setSecurityRulePortMin(rule.getPortRangeMin());
        }
        if (rule.getPortRangeMax() != null) {
            answer.setSecurityRulePortMax(rule.getPortRangeMax());
        }
        if (rule.getId() != null) {
            answer.setID(rule.getId().getValue());
        }
        return answer;
    }

    @Override
    public int canCreate(NeutronSecurityRule securityRule) {
        LOG.trace("canCreate securityRule - {}", securityRule);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void created(NeutronSecurityRule securityRule) {
        LOG.trace("created securityRule - {}", securityRule);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleAdded = addNeutronSecurityRule(securityRule, rwTx);
        if (isNeutronSecurityRuleAdded) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
    }

    public boolean changeActionOfNeutronSecurityRule(Uuid secRuleId, ActionChoice action, ReadWriteTransaction rwTx) {
        NeutronSecurityRule secRule = secRuleDao.getSecRuleByUuid(secRuleId);
        addSfcChainActionInstance(action, SecRuleEntityDecoder.getTenantId(secRule), rwTx);
        LOG.trace("Changing to action {} for secuirity group rule {}", action, secRule);
        return addNeutronSecurityRuleWithAction(secRule, action, rwTx);
    }

    private void addSfcChainActionInstance(ActionChoice action, TenantId tenantId, ReadWriteTransaction rwTx) {
        if (action instanceof SfcActionCase) {
            String sfcChainName = ((SfcActionCase) action).getSfcChainName();
            ActionName actionName = new ActionName(sfcChainName);
            ActionInstance sfcActionInstance = new ActionInstanceBuilder().setName(actionName)
                .setActionDefinitionId(ChainActionDefinition.ID)
                .setParameterValue(
                        ImmutableList.of(new ParameterValueBuilder().setName(
                                new ParameterName(ChainActionDefinition.SFC_CHAIN_NAME))
                            .setStringValue(sfcChainName)
                            .build()))
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.actionInstanceIid(tenantId, actionName),
                    sfcActionInstance, true);
        }
    }

    public boolean addNeutronSecurityRule(NeutronSecurityRule secRule, ReadWriteTransaction rwTx) {
        return addNeutronSecurityRuleWithAction(secRule, MappingUtils.ALLOW_ACTION_CHOICE, rwTx);
    }

    public boolean addNeutronSecurityRuleWithAction(NeutronSecurityRule secRule, ActionChoice action,
            ReadWriteTransaction rwTx) {
        TenantId tenantId = SecRuleEntityDecoder.getTenantId(secRule);
        EndpointGroupId providerEpgId = SecRuleEntityDecoder.getProviderEpgId(secRule);
        secRuleDao.addSecRule(secRule);

        Description contractDescription = new Description(CONTRACT_PROVIDER
                + secGroupDao.getNameOrIdOfSecGroup(providerEpgId));
        SingleRuleContract singleRuleContract = createSingleRuleContract(secRule, contractDescription, action);
        Contract contract = singleRuleContract.getContract();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, contract.getId()), contract, true);
        SelectorName providerSelector = getSelectorNameWithConsumer(secRule);
        writeProviderNamedSelectorToEpg(providerSelector, contract.getId(), new EgKey(tenantId, providerEpgId), rwTx);

        if (SecRuleEntityDecoder.getConsumerEpgId(secRule) != null) {
            EndpointGroupId consumerEpgId = SecRuleEntityDecoder.getConsumerEpgId(secRule);
            designContractsBetweenProviderAndConsumer(tenantId, providerEpgId, consumerEpgId, rwTx);
            designContractsBetweenProviderAndConsumer(tenantId, consumerEpgId, providerEpgId, rwTx);
        } else {
            for (EndpointGroupId consumerEpgId : secRuleDao.getAllOwnerSecGrps()) {
                designContractsBetweenProviderAndConsumer(tenantId, providerEpgId, consumerEpgId, rwTx);
                designContractsBetweenProviderAndConsumer(tenantId, consumerEpgId, providerEpgId, rwTx);
            }
        }

        ClassifierInstance classifierInstance = singleRuleContract.getSingleClassifierRule().getClassifierInstance();
        createClassifierInstanceIfNotExists(tenantId, classifierInstance, rwTx);
        createAllowActionInstanceIfNotExists(tenantId, rwTx);
        return true;
    }

    @VisibleForTesting
    static SingleRuleContract createSingleRuleContract(NeutronSecurityRule secRule, Description contractDescription,
            ActionChoice action) {
        if (!Strings.isNullOrEmpty(secRule.getSecurityRuleRemoteIpPrefix())) {
            return new SingleRuleContract(secRule, 0, contractDescription, action);
        }
        return new SingleRuleContract(secRule, 400, contractDescription, action);
    }

    @VisibleForTesting
    void designContractsBetweenProviderAndConsumer(TenantId tenantId, EndpointGroupId provEpgId,
            EndpointGroupId consEpgId, ReadWriteTransaction rwTx) {
        Set<NeutronSecurityRule> provSecRules = getProvidedSecRulesBetween(provEpgId, consEpgId);
        Set<NeutronSecurityRule> consSecRules = getProvidedSecRulesBetween(consEpgId, provEpgId);
        for (NeutronSecurityRule provSecRule : provSecRules) {
            if (isProviderSecRuleSuitableForConsumerSecRules(provSecRule, consSecRules)) {
                SelectorName consumerSelector = getSelectorNameWithProvider(provSecRule);
                ContractId contractId = SecRuleEntityDecoder.getContractId(provSecRule);
                writeConsumerNamedSelectorToEpg(consumerSelector, contractId, new EgKey(tenantId, consEpgId), rwTx);
            }
            // TODO add case when port ranges overlap
        }
    }

    @VisibleForTesting
    Set<NeutronSecurityRule> getProvidedSecRulesBetween(EndpointGroupId provEpgId, EndpointGroupId consEpgId) {
        return Sets.union(secRuleDao.getSecRulesBySecGrpIdAndRemoteSecGrpId(provEpgId, consEpgId),
                secRuleDao.getSecRulesWithoutRemoteSecGrpBySecGrpId(provEpgId));
    }

    @VisibleForTesting
    static boolean isProviderSecRuleSuitableForConsumerSecRules(NeutronSecurityRule provSecRule,
            Set<NeutronSecurityRule> consSecRules) {
        Direction directionProvSecRule = SecRuleEntityDecoder.getDirection(provSecRule);
        for (NeutronSecurityRule consSecRule : consSecRules) {
            Direction directionConsSecRule = SecRuleEntityDecoder.getDirection(consSecRule);
            if (isDirectionOpposite(directionProvSecRule, directionConsSecRule)
                    && isOneWithinTwo(provSecRule, consSecRule)) {
                return true;
            }
        }
        return false;
    }

    private void writeProviderNamedSelectorToEpg(SelectorName providerSelector, ContractId contractId, EgKey epgKey,
            WriteTransaction wTx) {
        ProviderNamedSelector providerNamedSelector = new ProviderNamedSelectorBuilder().setName(providerSelector)
            .setContract(ImmutableList.of(contractId))
            .build();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerNamedSelectorIid(epgKey.getTenantId(), epgKey.getEgId(),
                        providerNamedSelector.getName()), providerNamedSelector, true);
    }

    private void writeConsumerNamedSelectorToEpg(SelectorName consumerSelector, ContractId contractId, EgKey epgKey,
            WriteTransaction wTx) {
        ConsumerNamedSelector consumerNamedSelector = new ConsumerNamedSelectorBuilder().setName(consumerSelector)
            .setContract(ImmutableList.of(contractId))
            .build();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.consumerNamedSelectorIid(epgKey.getTenantId(), epgKey.getEgId(),
                        consumerNamedSelector.getName()), consumerNamedSelector, true);
    }

    @VisibleForTesting
    void createClassifierInstanceIfNotExists(TenantId tenantId, ClassifierInstance classifierInstance,
            WriteTransaction wTx) {
        InstanceIdentifier<ClassifierInstance> classifierInstanceIid = IidFactory.classifierInstanceIid(tenantId,
                classifierInstance.getName());
        if (!createdClassifierInstances.contains(classifierInstanceIid)) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, classifierInstanceIid, classifierInstance, true);
        }
        createdClassifierInstances.add(classifierInstanceIid);
    }

    @VisibleForTesting
    void createAllowActionInstanceIfNotExists(TenantId tenantId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<ActionInstance> actionInstanceIid = IidFactory.actionInstanceIid(tenantId,
                MappingUtils.ACTION_ALLOW.getName());
        if (!createdActionInstances.contains(actionInstanceIid)) {
            rwTx.put(LogicalDatastoreType.CONFIGURATION, actionInstanceIid, MappingUtils.ACTION_ALLOW, true);
        }
        createdActionInstances.add(actionInstanceIid);
    }

    @Override
    public int canUpdate(NeutronSecurityRule delta, NeutronSecurityRule original) {
        LOG.warn("canUpdate securityRule - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group rule. \nDelta: {} \nOriginal: {}",
                delta, original);
        return StatusCode.BAD_REQUEST;
    }

    @Override
    public void updated(NeutronSecurityRule securityRule) {
        LOG.warn("updated securityRule - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group rule. \nSecurity group rule: {}",
                securityRule);
    }

    @Override
    public int canDelete(NeutronSecurityRule securityRule) {
        LOG.trace("canDelete - securityRule - {}", securityRule);
        // nothing to consider
        return StatusCode.OK;
    }

    @Override
    public void deleted(NeutronSecurityRule secRule) {
        LOG.trace("deleted securityRule - {}", secRule);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleDeleted = deleteNeutronSecurityRule(secRule, rwTx);
        if (isNeutronSecurityRuleDeleted) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
    }

    public boolean deleteNeutronSecurityRule(NeutronSecurityRule secRule, ReadWriteTransaction rwTx) {
        TenantId tenantId = SecRuleEntityDecoder.getTenantId(secRule);
        EndpointGroupId providerEpgId = SecRuleEntityDecoder.getProviderEpgId(secRule);

        SelectorName providerSelector = getSelectorNameWithConsumer(secRule);
        deleteProviderNamedSelectorFromEpg(providerSelector, new EgKey(tenantId, providerEpgId), rwTx);

        if (SecRuleEntityDecoder.getConsumerEpgId(secRule) != null) {
            EndpointGroupId consumerEpgId = SecRuleEntityDecoder.getConsumerEpgId(secRule);
            undesignContractsBetweenProviderAndConsumer(tenantId, providerEpgId, consumerEpgId, secRule, rwTx);
            undesignContractsBetweenProviderAndConsumer(tenantId, consumerEpgId, providerEpgId, secRule, rwTx);
        } else {
            for (EndpointGroupId consumerEpgId : secRuleDao.getAllOwnerSecGrps()) {
                undesignContractsBetweenProviderAndConsumer(tenantId, providerEpgId, consumerEpgId, secRule, rwTx);
                undesignContractsBetweenProviderAndConsumer(tenantId, consumerEpgId, providerEpgId, secRule, rwTx);
            }
        }

        secRuleDao.removeSecRule(secRule);
        ContractId contractId = SecRuleEntityDecoder.getContractId(secRule);
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, contractId));

        ClassifierInstance classifierInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        deleteClassifierInstanceIfNotUsed(tenantId, classifierInstance, rwTx);
        return true;
    }

    @VisibleForTesting
    void undesignContractsBetweenProviderAndConsumer(TenantId tenantId, EndpointGroupId provEpgId,
            EndpointGroupId consEpgId, NeutronSecurityRule removedSecRule, ReadWriteTransaction rwTx) {
        Set<NeutronSecurityRule> provSecRules = getProvidedSecRulesBetween(provEpgId, consEpgId);
        Set<NeutronSecurityRule> consSecRules = getProvidedSecRulesBetween(consEpgId, provEpgId);
        for (NeutronSecurityRule provSecRule : provSecRules) {
            if (isProvidersSecRuleSuitableForConsumersSecRulesAndGoodToRemove(provSecRule, consSecRules, removedSecRule)) {
                SelectorName consumerSelector = getSelectorNameWithProvider(provSecRule);
                deleteConsumerNamedSelector(consumerSelector, new EgKey(tenantId, consEpgId), rwTx);
            }
            // TODO add case when port ranges overlap
        }
    }

    @VisibleForTesting
    static boolean isProvidersSecRuleSuitableForConsumersSecRulesAndGoodToRemove(NeutronSecurityRule provSecRule,
            Set<NeutronSecurityRule> consSecRules, NeutronSecurityRule removedSecRule) {
        Direction directionProvSecRule = SecRuleEntityDecoder.getDirection(provSecRule);
        for (NeutronSecurityRule consSecRule : consSecRules) {
            if (isRuleIdEqual(removedSecRule, consSecRule) || isRuleIdEqual(removedSecRule, provSecRule)) {
                Direction directionConsSecRule = SecRuleEntityDecoder.getDirection(consSecRule);
                if (isDirectionOpposite(directionProvSecRule, directionConsSecRule)
                        && isOneWithinTwo(provSecRule, consSecRule)) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    static boolean isRuleIdEqual(NeutronSecurityRule one, NeutronSecurityRule two) {
        checkNotNull(one);
        checkNotNull(two);
        return one.getSecurityRuleUUID().equals(two.getSecurityRuleUUID());
    }

    private void deleteProviderNamedSelectorFromEpg(SelectorName providerSelector, EgKey providerEpgKey,
            ReadWriteTransaction rwTx) {
        InstanceIdentifier<ProviderNamedSelector> providerSelectorIid = IidFactory.providerNamedSelectorIid(
                providerEpgKey.getTenantId(), providerEpgKey.getEgId(), providerSelector);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, providerSelectorIid, rwTx);
    }

    private void deleteConsumerNamedSelector(SelectorName consumerSelector, EgKey consumerEpgKey,
            ReadWriteTransaction rwTx) {
        InstanceIdentifier<ConsumerNamedSelector> consumerSelectorIid = IidFactory.consumerNamedSelectorIid(
                consumerEpgKey.getTenantId(), consumerEpgKey.getEgId(), consumerSelector);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, consumerSelectorIid, rwTx);
    }

    private void deleteClassifierInstanceIfNotUsed(TenantId tenantId, ClassifierInstance classifierInstance,
            ReadWriteTransaction rwTx) {
        InstanceIdentifier<ClassifierInstance> classifierInstanceIid = IidFactory.classifierInstanceIid(tenantId,
                classifierInstance.getName());
        createdClassifierInstances.remove(classifierInstanceIid);
        if (!createdClassifierInstances.contains(classifierInstanceIid)) {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, classifierInstanceIid, rwTx);
        }
    }

    @VisibleForTesting
    void deleteAllowActionInstanceIfNotUsed(TenantId tenantId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<ActionInstance> actionInstanceIid = IidFactory.actionInstanceIid(tenantId,
                MappingUtils.ACTION_ALLOW.getName());
        createdActionInstances.remove(actionInstanceIid);
        if (!createdActionInstances.contains(actionInstanceIid)) {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, actionInstanceIid, rwTx);
        }
    }

    private SelectorName getSelectorNameWithConsumer(NeutronSecurityRule secRule) {
        ClauseName clauseName = SecRuleNameDecoder.getClauseName(secRule);
        StringBuilder selectorNameBuilder = new StringBuilder().append(clauseName.getValue());
        EndpointGroupId consumerEpgId = SecRuleEntityDecoder.getConsumerEpgId(secRule);
        if (consumerEpgId != null) {
            selectorNameBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER)
                .append(POSSIBLE_CONSUMER)
                .append(secGroupDao.getNameOrIdOfSecGroup(consumerEpgId));
        }
        return new SelectorName(selectorNameBuilder.toString());
    }

    private SelectorName getSelectorNameWithProvider(NeutronSecurityRule secRule) {
        ClauseName clauseName = SecRuleNameDecoder.getClauseName(secRule);
        EndpointGroupId providerEpgId = SecRuleEntityDecoder.getProviderEpgId(secRule);
        String selectorName = new StringBuilder().append(clauseName.getValue())
            .append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(PROVIDED_BY)
            .append(secGroupDao.getNameOrIdOfSecGroup(providerEpgId))
            .toString();
        return new SelectorName(selectorName);
    }

    @VisibleForTesting
    static boolean isDirectionOpposite(Direction one, Direction two) {
        return (one == Direction.In && two == Direction.Out) || (one == Direction.Out && two == Direction.In);
    }

    @VisibleForTesting
    static boolean isOneWithinTwo(NeutronSecurityRule one, NeutronSecurityRule two) {
        if (!isOneGroupIdWithinTwoRemoteGroupId(one, two) || !isOneGroupIdWithinTwoRemoteGroupId(two, one))
            return false;
        if (!SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two))
            return false;
        if (!SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two))
            return false;
        if (!SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two))
            return false;
        if (!Strings.isNullOrEmpty(two.getSecurityRuleRemoteIpPrefix())
                && Strings.isNullOrEmpty(one.getSecurityRuleRemoteIpPrefix()))
            return false;
        return true;
    }

    @VisibleForTesting
    static boolean isOneGroupIdWithinTwoRemoteGroupId(NeutronSecurityRule one, NeutronSecurityRule two) {
        return (Strings.isNullOrEmpty(two.getSecurityRemoteGroupID()) || two.getSecurityRemoteGroupID().equals(
                one.getSecurityRuleGroupID()));
    }
}
