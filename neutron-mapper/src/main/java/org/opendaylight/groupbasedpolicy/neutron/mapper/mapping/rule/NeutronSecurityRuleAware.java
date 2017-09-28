/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SecurityGroupUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SecurityRuleUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class NeutronSecurityRuleAware implements NeutronAware<SecurityRule> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityRuleAware.class);
    public static final InstanceIdentifier<SecurityRule> SECURITY_RULE_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(SecurityRules.class).child(SecurityRule.class).build();
    private static final String CONTRACT_PROVIDER = "Contract provider: ";
    private final DataBroker dataProvider;
    private final Multiset<InstanceIdentifier<ClassifierInstance>> createdClassifierInstances;
    private final Multiset<InstanceIdentifier<ActionInstance>> createdActionInstances;
    private final Map<SecurityRuleKey, SecurityRule> pendingCreatedRules;
    private final Map<SecurityGroupKey, SecurityGroup> pendingDeletedGroups;
    final static String PROVIDED_BY = "provided_by-";
    final static String POSSIBLE_CONSUMER = "possible_consumer-";
    private final EndpointRegistrator epRegistrator;

    public NeutronSecurityRuleAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this(dataProvider, HashMultiset.create(), HashMultiset.create(), epRegistrator);
    }

    @VisibleForTesting
    NeutronSecurityRuleAware(DataBroker dataProvider,
            Multiset<InstanceIdentifier<ClassifierInstance>> classifierInstanceNames,
            Multiset<InstanceIdentifier<ActionInstance>> createdActionInstances, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.createdClassifierInstances = checkNotNull(classifierInstanceNames);
        this.createdActionInstances = checkNotNull(createdActionInstances);
        this.pendingCreatedRules = new HashMap<>();
        this.pendingDeletedGroups = new HashMap<>();
        this.epRegistrator = Preconditions.checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(SecurityRule secRule, Neutron neutron) {
        LOG.trace("created securityRule - {}", secRule);
        if (neutron.getSecurityGroups() == null || neutron.getSecurityGroups().getSecurityGroup() == null
                || neutron.getSecurityGroups()
                    .getSecurityGroup()
                    .stream()
                    .noneMatch(sg -> sg.getKey().getUuid().equals(secRule.getSecurityGroupId()))) {
            pendingCreatedRules.put(secRule.getKey(), secRule);
            LOG.warn("Security group of security rule {} does not exist yet. The rule will be processed"
                    + "when the missing security group is created.", secRule.getKey());
            return;
        }
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleAdded = true;
        try {
            isNeutronSecurityRuleAdded = addNeutronSecurityRule(secRule, neutron, rwTx);
        } catch (NullPointerException e) {
            LOG.error("Failed to process rule {}", secRule.getUuid());
            isNeutronSecurityRuleAdded = false;
        }
        if (isNeutronSecurityRuleAdded) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
    }

    public void flushPendingSecurityRulesFor(@Nonnull SecurityGroupKey secGroupKey, Neutron neutron) {
        List<SecurityRule> rules = pendingCreatedRules.values()
            .stream()
            .filter(sr -> sr.getSecurityGroupId().equals(secGroupKey.getUuid()))
            .collect(Collectors.toList());
        rules.forEach(sr -> {
            LOG.trace("Flushing pending security rule {}", sr);
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            boolean isNeutronSecurityRuleAdded = addNeutronSecurityRule(sr, neutron, rwTx);
            if (isNeutronSecurityRuleAdded) {
                DataStoreHelper.submitToDs(rwTx);
            } else {
                rwTx.cancel();
            }
            pendingCreatedRules.remove(sr.getKey());
        });
    }

    public boolean addNeutronSecurityRule(SecurityRule secRule, Neutron neutron, ReadWriteTransaction rwTx) {
        return addNeutronSecurityRuleWithAction(secRule, neutron, MappingUtils.ALLOW_ACTION_CHOICE, rwTx);
    }

    public boolean addNeutronSecurityRuleWithAction(SecurityRule secRule, Neutron neutron, ActionChoice action,
            ReadWriteTransaction rwTx) {
        if (secRule.getTenantId() == null) {
            LOG.warn("Skip processing rule {} because TenantId is null.", secRule);
            // TODO This needs to be reworked, SecRules shouldn't use TenantId, Neutron doesn't always configure it
            return true;
        }
        TenantId tenantId = new TenantId(secRule.getTenantId().getValue());
        Uuid providerSecGroupId = secRule.getSecurityGroupId();
        EndpointGroupId providerEpgId = new EndpointGroupId(providerSecGroupId.getValue());

        Description contractDescription = createContractDescription(secRule, neutron);
        SingleRuleContract singleRuleContract = createSingleRuleContract(secRule, contractDescription, action, neutron);
        Contract contract = singleRuleContract.getContract();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, contract.getId()), contract, true);
        SelectorName providerSelector = getSelectorNameWithConsumer(secRule, neutron);
        writeProviderNamedSelectorToEpg(providerSelector, contract.getId(), new EpgKeyDto(providerEpgId, tenantId), rwTx);

        if (secRule.getRemoteGroupId() != null) {
            Uuid consumerSecGroupId = secRule.getRemoteGroupId();
            designContractsBetweenProviderAndConsumer(tenantId, providerSecGroupId, consumerSecGroupId, neutron, rwTx);
            designContractsBetweenProviderAndConsumer(tenantId, consumerSecGroupId, providerSecGroupId, neutron, rwTx);
        } else {
            for (Uuid consumerSecGroupId : SecurityRuleUtils.findSecurityGroupsHavingSecurityRules(neutron)) {
                designContractsBetweenProviderAndConsumer(tenantId, providerSecGroupId, consumerSecGroupId, neutron, rwTx);
                designContractsBetweenProviderAndConsumer(tenantId, consumerSecGroupId, providerSecGroupId, neutron, rwTx);
            }
        }

        ClassifierInstance classifierInstance = singleRuleContract.getSingleClassifierRule().getClassifierInstance();
        createClassifierInstanceIfNotExists(tenantId, classifierInstance, rwTx);
        createAllowActionInstanceIfNotExists(tenantId, rwTx);
        return true;
    }

    @VisibleForTesting
    static Description createContractDescription(SecurityRule secRule, Neutron neutron) {
        Optional<SecurityGroup> providerSecGroup =
            SecurityGroupUtils.findSecurityGroup(secRule.getSecurityGroupId(), neutron.getSecurityGroups());
        if (!providerSecGroup.isPresent()) {
            LOG.error("Neutron Security Group with UUID {} does not exist but it is in {}",
                secRule.getSecurityGroupId().getValue(), secRule);
            throw new IllegalStateException(
                "Neutron Security Group with UUID " + secRule.getSecurityGroupId().getValue() + " does not exist.");
        }
        return new Description(CONTRACT_PROVIDER + SecurityGroupUtils.getNameOrUuid(providerSecGroup.get()));
    }

    @VisibleForTesting
    SingleRuleContract createSingleRuleContract(SecurityRule secRule, Description contractDescription,
            ActionChoice action, Neutron neutron) {
        if (secRule.getRemoteIpPrefix() != null) {
            if (neutron.getNetworks() != null && neutron.getNetworks().getNetwork() != null) {
                java.util.Optional<Network> publicNet = neutron.getNetworks()
                    .getNetwork()
                    .stream()
                    .filter(net -> NetworkUtils.isRouterExternal(net))
                    .findAny();
                if (publicNet.isPresent()) {
                    WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                    wTx.merge(LogicalDatastoreType.CONFIGURATION,
                            IidFactory.externalImplicitGroupIid(new TenantId(secRule.getTenantId().getValue()),
                                    MappingUtils.EPG_EXTERNAL_ID),
                            new ExternalImplicitGroupBuilder().setId(MappingUtils.EPG_EXTERNAL_ID).build(), true);
                    wTx.merge(LogicalDatastoreType.CONFIGURATION,
                            IidFactory.endpointGroupIid(new TenantId(secRule.getTenantId().getValue()),
                                    MappingUtils.EPG_EXTERNAL_ID),
                            new EndpointGroupBuilder().setId(MappingUtils.EPG_EXTERNAL_ID).build(), true);
                    DataStoreHelper.submitToDs(wTx);
                    AddressEndpointRegBuilder addrEpbuilder = new AddressEndpointRegBuilder()
                        .setAddress(String.valueOf(secRule.getRemoteIpPrefix().getValue()))
                        .setAddressType(IpPrefixType.class)
                        .setContextType(MappingUtils.L3_CONTEXT)
                        .setContextId(new ContextId(publicNet.get().getUuid().getValue()))
                        .setTenant(new TenantId(secRule.getTenantId().getValue()))
                        .setTimestamp(System.currentTimeMillis())
                        .setEndpointGroup(ImmutableList.<EndpointGroupId>of(MappingUtils.EPG_EXTERNAL_ID));
                    RegisterEndpointInput regEp = new RegisterEndpointInputBuilder()
                        .setAddressEndpointReg(ImmutableList.of(addrEpbuilder.build())).build();
                    boolean registered = epRegistrator.registerEndpoint(regEp);
                    if (registered) {
                        LOG.info("Registering endpoint representing remote-ip-prefix {}", addrEpbuilder.getKey());
                    } else {
                        LOG.error("Failed to register endpoint {}", addrEpbuilder.getKey());
                    }
                }
            }
            return new SingleRuleContract(secRule, 0, contractDescription, action);
        }
        return new SingleRuleContract(secRule, 400, contractDescription, action);
    }

    @VisibleForTesting
    void designContractsBetweenProviderAndConsumer(TenantId tenantId, Uuid provSecGroupId, Uuid consSecGroupId,
            Neutron neutron, ReadWriteTransaction rwTx) {
        Set<SecurityRule> provSecRules = getProvidedSecRulesBetween(provSecGroupId, consSecGroupId, neutron);
        Set<SecurityRule> consSecRules = getProvidedSecRulesBetween(consSecGroupId, provSecGroupId, neutron);
        EndpointGroupId consEpgId = new EndpointGroupId(consSecGroupId.getValue());
        for (SecurityRule provSecRule : provSecRules) {
            if (isProviderSecRuleSuitableForConsumerSecRules(provSecRule, consSecRules)) {
                SelectorName consumerSelector = getSelectorNameWithProvider(provSecRule, neutron);
                ContractId contractId = SecRuleEntityDecoder.getContractId(provSecRule);
                writeConsumerNamedSelectorToEpg(consumerSelector, contractId, new EpgKeyDto(consEpgId, tenantId), rwTx);
            }
            // TODO add case when port ranges overlap
        }
    }

    @VisibleForTesting
    Set<SecurityRule> getProvidedSecRulesBetween(Uuid provSecGroup, Uuid consSecGroup, Neutron neutron) {
        return Sets.union(SecurityRuleUtils.findSecurityRulesBySecGroupAndRemoteSecGroup(provSecGroup, consSecGroup, neutron),
                SecurityRuleUtils.findSecurityRulesBySecGroupAndRemoteSecGroup(provSecGroup, null, neutron));
    }

    @VisibleForTesting
    static boolean isProviderSecRuleSuitableForConsumerSecRules(SecurityRule provSecRule,
            Set<SecurityRule> consSecRules) {
        Direction directionProvSecRule = SecRuleEntityDecoder.getDirection(provSecRule);
        for (SecurityRule consSecRule : consSecRules) {
            Direction directionConsSecRule = SecRuleEntityDecoder.getDirection(consSecRule);
            if (isDirectionOpposite(directionProvSecRule, directionConsSecRule)
                    && isOneWithinTwo(provSecRule, consSecRule)) {
                return true;
            }
        }
        return false;
    }

    public boolean changeActionOfNeutronSecurityRule(SecurityRule secRule, ActionChoice action, Neutron neutron, ReadWriteTransaction rwTx) {
        addSfcChainActionInstance(action, new TenantId(secRule.getTenantId().getValue()), rwTx);
        LOG.trace("Changing to action {} for secuirity group rule {}", action, secRule);
        return addNeutronSecurityRuleWithAction(secRule, neutron, action, rwTx);
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

    private void writeProviderNamedSelectorToEpg(SelectorName providerSelector, ContractId contractId, EpgKeyDto epgKey,
            WriteTransaction wTx) {
        ProviderNamedSelector providerNamedSelector = new ProviderNamedSelectorBuilder().setName(providerSelector)
            .setContract(ImmutableList.of(contractId))
            .build();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerNamedSelectorIid(epgKey.getTenantId(), epgKey.getEpgId(),
                        providerNamedSelector.getName()), providerNamedSelector, true);
    }

    private void writeConsumerNamedSelectorToEpg(SelectorName consumerSelector, ContractId contractId, EpgKeyDto epgKey,
            WriteTransaction wTx) {
        ConsumerNamedSelector consumerNamedSelector = new ConsumerNamedSelectorBuilder().setName(consumerSelector)
            .setContract(ImmutableList.of(contractId))
            .build();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.consumerNamedSelectorIid(epgKey.getTenantId(), epgKey.getEpgId(),
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
    public void onUpdated(SecurityRule oldSecRule, SecurityRule newSecRule, Neutron oldNeutron, Neutron newNeutron) {
        LOG.warn("updated securityRule - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group rule. \nSecurity group rule: {}",
                newSecRule);
    }

    public void addPendingDeletedSecGroup(SecurityGroup secGroup) {
        LOG.trace("Caching pending deleted security group {}", secGroup.getKey());
        pendingDeletedGroups.put(secGroup.getKey(), secGroup);
    }

    @Override
    public void onDeleted(SecurityRule deletedSecRule, Neutron oldNeutron, Neutron newNeutron) {
        if (deletedSecRule.getTenantId() == null) {
            LOG.warn("Skip deleting SecRule {} because TenantId is null.", deletedSecRule);
            // TODO This needs to be reworked, SecGroups shouldn't use TenantId, Neutron doesn't always configure it
            return;
        }
        LOG.trace("deleted securityRule - {}", deletedSecRule);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isNeutronSecurityRuleDeleted = deleteNeutronSecurityRule(deletedSecRule, oldNeutron, rwTx);
        if (isNeutronSecurityRuleDeleted) {
            DataStoreHelper.submitToDs(rwTx);
            if (newNeutron == null || newNeutron.getSecurityRules() == null
                || newNeutron.getSecurityRules().getSecurityRule() == null
                || newNeutron.getSecurityRules()
                        .getSecurityRule()
                        .stream()
                        .noneMatch(rule -> rule.getSecurityGroupId().equals(deletedSecRule.getSecurityGroupId()))) {
                SecurityGroupKey secGroupKey = new SecurityGroupKey(deletedSecRule.getSecurityGroupId());
                SecurityGroup pendingSg = pendingDeletedGroups.get(secGroupKey);
                if (pendingSg != null) {
                    LOG.trace("Processing pending deleted security group {}", secGroupKey);
                    NeutronSecurityGroupAware.deleteGbpEndpointGroup(dataProvider,
                            new TenantId(deletedSecRule.getTenantId().getValue()),
                            new EndpointGroupId(secGroupKey.getUuid().getValue()));
                    pendingDeletedGroups.remove(secGroupKey);
                }
            }
        } else {
            rwTx.cancel();
        }
    }

    public boolean deleteNeutronSecurityRule(SecurityRule secRule, Neutron neutron, ReadWriteTransaction rwTx) {
        TenantId tenantId = new TenantId(secRule.getTenantId().getValue());
        Uuid providerSecGroupId = secRule.getSecurityGroupId();
        EndpointGroupId providerEpgId = new EndpointGroupId(providerSecGroupId.getValue());

        SelectorName providerSelector = getSelectorNameWithConsumer(secRule, neutron);
        deleteProviderNamedSelectorFromEpg(providerSelector, new EpgKeyDto(providerEpgId, tenantId), rwTx);

        if (secRule.getRemoteGroupId() != null) {
            Uuid consumerSecGroupId = secRule.getRemoteGroupId();
            undesignContractsBetweenProviderAndConsumer(tenantId, providerSecGroupId, consumerSecGroupId, secRule, neutron, rwTx);
            undesignContractsBetweenProviderAndConsumer(tenantId, consumerSecGroupId, providerSecGroupId, secRule, neutron, rwTx);
        } else {
            for (Uuid consumerSecGroupId : SecurityRuleUtils.findSecurityGroupsHavingSecurityRules(neutron)) {
                undesignContractsBetweenProviderAndConsumer(tenantId, providerSecGroupId, consumerSecGroupId, secRule, neutron, rwTx);
                undesignContractsBetweenProviderAndConsumer(tenantId, consumerSecGroupId, providerSecGroupId, secRule, neutron, rwTx);
            }
        }

        ContractId contractId = SecRuleEntityDecoder.getContractId(secRule);
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, contractId));

        ClassifierInstance classifierInstance = SecRuleEntityDecoder.getClassifierInstance(secRule);
        deleteClassifierInstanceIfNotUsed(tenantId, classifierInstance, rwTx);
        return true;
    }

    @VisibleForTesting
    void undesignContractsBetweenProviderAndConsumer(TenantId tenantId, Uuid provSecGroupId,
            Uuid consSecGroupId, SecurityRule removedSecRule, Neutron neutron, ReadWriteTransaction rwTx) {
        Set<SecurityRule> provSecRules = getProvidedSecRulesBetween(provSecGroupId, consSecGroupId, neutron);
        Set<SecurityRule> consSecRules = getProvidedSecRulesBetween(consSecGroupId, provSecGroupId, neutron);
        EndpointGroupId consEpgId = new EndpointGroupId(consSecGroupId.getValue());
        for (SecurityRule provSecRule : provSecRules) {
            if (isProvidersSecRuleSuitableForConsumersSecRulesAndGoodToRemove(provSecRule, consSecRules, removedSecRule)) {
                SelectorName consumerSelector = getSelectorNameWithProvider(provSecRule, neutron);
                deleteConsumerNamedSelector(consumerSelector, new EpgKeyDto(consEpgId, tenantId), rwTx);
            }
            // TODO add case when port ranges overlap
        }
    }

    @VisibleForTesting
    static boolean isProvidersSecRuleSuitableForConsumersSecRulesAndGoodToRemove(SecurityRule provSecRule,
            Set<SecurityRule> consSecRules, SecurityRule removedSecRule) {
        Direction directionProvSecRule = SecRuleEntityDecoder.getDirection(provSecRule);
        for (SecurityRule consSecRule : consSecRules) {
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
    static boolean isRuleIdEqual(SecurityRule one, SecurityRule two) {
        checkNotNull(one);
        checkNotNull(two);
        return one.getSecurityGroupId().equals(two.getSecurityGroupId());
    }

    private void deleteProviderNamedSelectorFromEpg(SelectorName providerSelector, EpgKeyDto providerEpgKey,
            ReadWriteTransaction rwTx) {
        InstanceIdentifier<ProviderNamedSelector> providerSelectorIid = IidFactory.providerNamedSelectorIid(
                providerEpgKey.getTenantId(), providerEpgKey.getEpgId(), providerSelector);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, providerSelectorIid, rwTx);
    }

    private void deleteConsumerNamedSelector(SelectorName consumerSelector, EpgKeyDto consumerEpgKey,
            ReadWriteTransaction rwTx) {
        InstanceIdentifier<ConsumerNamedSelector> consumerSelectorIid = IidFactory.consumerNamedSelectorIid(
                consumerEpgKey.getTenantId(), consumerEpgKey.getEpgId(), consumerSelector);
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

    private SelectorName getSelectorNameWithConsumer(SecurityRule secRule, Neutron neutron) {
        ClauseName clauseName = SecRuleNameDecoder.getClauseName(secRule);
        StringBuilder selectorNameBuilder = new StringBuilder().append(clauseName.getValue());
        Uuid consumerSecGroupId = secRule.getRemoteGroupId();
        if (consumerSecGroupId == null) {
            return new SelectorName(selectorNameBuilder.toString());
        }

        // we cannot use name of security group in selector, because name can be changed
        // therefore name is used only in debug mode
        Optional<SecurityGroup> potentialConsumerSecGroup =
            SecurityGroupUtils.findSecurityGroup(secRule.getRemoteGroupId(), neutron.getSecurityGroups());
        if (!potentialConsumerSecGroup.isPresent()) {
            LOG.error("Neutron Security Group with UUID {} does not exist but it is in {}",
                consumerSecGroupId.getValue(), secRule);
            throw new IllegalStateException(
                "Neutron Security Group with UUID " + consumerSecGroupId.getValue() + " does not exist.");
        }

        selectorNameBuilder.append(MappingUtils.NAME_DOUBLE_DELIMETER)
            .append(POSSIBLE_CONSUMER)
            .append(SecurityGroupUtils.getNameOrUuid(potentialConsumerSecGroup.get()));
        return new SelectorName(selectorNameBuilder.toString());
    }

    private SelectorName getSelectorNameWithProvider(SecurityRule secRule, Neutron neutron) {
        ClauseName clauseName = SecRuleNameDecoder.getClauseName(secRule);
        Uuid providerSecGroupId = secRule.getSecurityGroupId();

        // we cannot use name of security group in selector, because name can be changed
        // therefore name is used only in debug mode
        Optional<SecurityGroup> potentialProviderSecGroup =
            SecurityGroupUtils.findSecurityGroup(secRule.getSecurityGroupId(), neutron.getSecurityGroups());
        if (!potentialProviderSecGroup.isPresent()) {
            LOG.error("Neutron Security Group with UUID {} does not exist but it is in {}",
                providerSecGroupId.getValue(), secRule);
            throw new IllegalStateException(
                "Neutron Security Group with UUID " + providerSecGroupId.getValue() + " does not exist.");
        }
        String selectorName =
            new StringBuilder().append(clauseName.getValue())
                .append(MappingUtils.NAME_DOUBLE_DELIMETER)
                .append(PROVIDED_BY)
                .append(SecurityGroupUtils.getNameOrUuid(potentialProviderSecGroup.get()))
                .toString();
        return new SelectorName(selectorName);
    }

    @VisibleForTesting
    static boolean isDirectionOpposite(Direction one, Direction two) {
        return (one == Direction.In && two == Direction.Out) || (one == Direction.Out && two == Direction.In);
    }

    @VisibleForTesting
    static boolean isOneWithinTwo(SecurityRule one, SecurityRule two) {
        if (!isOneGroupIdWithinTwoRemoteGroupId(one, two) || !isOneGroupIdWithinTwoRemoteGroupId(two, one))
            return false;
        if (!SecRuleEntityDecoder.isEtherTypeOfOneWithinTwo(one, two))
            return false;
        if (!SecRuleEntityDecoder.isProtocolOfOneWithinTwo(one, two))
            return false;
        if (!SecRuleEntityDecoder.isPortsOfOneWithinTwo(one, two))
            return false;
        if (two.getRemoteIpPrefix() != null
                && one.getRemoteIpPrefix() == null)
            return false;
        return true;
    }

    @VisibleForTesting
    static boolean isOneGroupIdWithinTwoRemoteGroupId(SecurityRule one, SecurityRule two) {
        return (two.getRemoteGroupId() == null || two.getRemoteGroupId().equals(
                one.getSecurityGroupId()));
    }

}
