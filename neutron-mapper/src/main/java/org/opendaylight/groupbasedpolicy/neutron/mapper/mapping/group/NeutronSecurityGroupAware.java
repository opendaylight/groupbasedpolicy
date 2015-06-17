/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.StatusCode;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronSecurityGroupAware;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

public class NeutronSecurityGroupAware implements INeutronSecurityGroupAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityGroupAware.class);
    private final DataBroker dataProvider;
    private final NeutronSecurityRuleAware secRuleAware;
    private final SecGroupDao secGroupDao;

    public NeutronSecurityGroupAware(DataBroker dataProvider, NeutronSecurityRuleAware secRuleAware, SecGroupDao secGroupDao) {
        this.dataProvider = checkNotNull(dataProvider);
        this.secRuleAware = checkNotNull(secRuleAware);
        this.secGroupDao = checkNotNull(secGroupDao);
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#canCreateNeutronSecurityGroup(org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public int canCreateNeutronSecurityGroup(NeutronSecurityGroup securityGroup) {
        LOG.trace("canCreateNeutronSecurityGroup - {}", securityGroup);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#neutronSecurityGroupCreated(org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public void neutronSecurityGroupCreated(NeutronSecurityGroup secGroup) {
        LOG.trace("neutronSecurityGroupCreated - {}", secGroup);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isSecGroupCreated = addNeutronSecurityGroup(secGroup, rwTx);
        if (isSecGroupCreated) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
    }

    public boolean addNeutronSecurityGroup(NeutronSecurityGroup secGroup, ReadWriteTransaction rwTx) {
        secGroupDao.addSecGroup(secGroup);
        TenantId tenantId = new TenantId(Utils.normalizeUuid(secGroup.getSecurityGroupTenantID()));
        EndpointGroupId providerEpgId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        EndpointGroupBuilder providerEpgBuilder = new EndpointGroupBuilder().setId(providerEpgId);
        if (!Strings.isNullOrEmpty(secGroup.getSecurityGroupName())) {
            try {
                providerEpgBuilder.setName(new Name(secGroup.getSecurityGroupName()));
            } catch (Exception e) {
                LOG.info("Name '{}' of Neutron Security-group '{}' is ignored.",
                        secGroup.getSecurityGroupName(), secGroup.getSecurityGroupUUID());
                LOG.debug("Name exception", e);
            }
        }
        if (!Strings.isNullOrEmpty(secGroup.getSecurityGroupDescription())) {
            try {
                providerEpgBuilder.setDescription(new Description(secGroup.getSecurityGroupDescription()));
            } catch (Exception e) {
                LOG.info("Description '{}' of Neutron Security-group '{}' is ignored.",
                        secGroup.getSecurityGroupDescription(), secGroup.getSecurityGroupUUID());
                LOG.debug("Description exception", e);
            }
        }
        providerEpgBuilder.setIntraGroupPolicy(IntraGroupPolicy.RequireContract);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, providerEpgId),
                providerEpgBuilder.build(), true);
        List<NeutronSecurityRule> secRules = secGroup.getSecurityRules();
        SortedSecurityGroupRules sortedSecGrpRules = new SortedSecurityGroupRules(secRules);
        ListMultimap<EndpointGroupId, NeutronSecurityRule> secRuleByRemoteSecGrpId = sortedSecGrpRules.secRuleByRemoteSecGrpId;
        for (EndpointGroupId consumerEpgId : secRuleByRemoteSecGrpId.keySet()) {
            addEpgIfMissing(tenantId, consumerEpgId, rwTx);
            boolean areSecRulesAdded = addNeutronSecurityRule(secRuleByRemoteSecGrpId.get(consumerEpgId), rwTx);
            if (!areSecRulesAdded) {
                return false;
            }
        }
        ListMultimap<IpPrefix, NeutronSecurityRule> secRuleByRemoteIpPrefix = sortedSecGrpRules.secRuleByRemoteIpPrefix;
        for (IpPrefix remoteIpPrefex : secRuleByRemoteIpPrefix.keySet()) {
            boolean areSecRulesAdded = addNeutronSecurityRule(secRuleByRemoteIpPrefix.get(remoteIpPrefex), rwTx);
            if (!areSecRulesAdded) {
                return false;
            }
        }
        boolean areSecRulesAdded = addNeutronSecurityRule(sortedSecGrpRules.secRulesWithoutRemote, rwTx);
        if (!areSecRulesAdded) {
            return false;
        }
        return true;
    }

    public static void addEpgIfMissing(TenantId tenantId, EndpointGroupId epgId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<EndpointGroup> epgIid = IidFactory.endpointGroupIid(tenantId, epgId);
        Optional<EndpointGroup> potentialConsumerEpg =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, epgIid, rwTx);
        if (!potentialConsumerEpg.isPresent()) {
            EndpointGroup epg = new EndpointGroupBuilder().setId(epgId)
                .setDescription(new Description(MappingUtils.NEUTRON_GROUP
                        + "EPG was created just based on remote group ID from a security rule."))
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, epgIid, epg);
        }
    }

    private boolean addNeutronSecurityRule(List<NeutronSecurityRule> secRules, ReadWriteTransaction rwTx) {
        for (NeutronSecurityRule secRule : secRules) {
            boolean isSecRuleAdded = secRuleAware.addNeutronSecurityRule(secRule, rwTx);
            if (!isSecRuleAdded) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#canUpdateNeutronSecurityGroup(org.opendaylight.neutron.spi.NeutronSecurityGroup,
     *      org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public int canUpdateNeutronSecurityGroup(NeutronSecurityGroup delta, NeutronSecurityGroup original) {
        LOG.warn("canUpdateNeutronSecurityGroup - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group. \nDelta: {} \nOriginal: {}", delta,
                original);
        return StatusCode.BAD_REQUEST;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#neutronSecurityGroupUpdated(org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public void neutronSecurityGroupUpdated(NeutronSecurityGroup securityGroup) {
        LOG.warn("neutronSecurityGroupUpdated - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group. \nSecurity group: {}", securityGroup);
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#canDeleteNeutronSecurityGroup(org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public int canDeleteNeutronSecurityGroup(NeutronSecurityGroup securityGroup) {
        LOG.trace("canDeleteNeutronSecurityGroup - {}", securityGroup);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronSecurityGroupAware#neutronSecurityGroupDeleted(org.opendaylight.neutron.spi.NeutronSecurityGroup)
     */
    @Override
    public void neutronSecurityGroupDeleted(NeutronSecurityGroup secGroup) {
        LOG.trace("neutronSecurityGroupDeleted - {}", secGroup);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        List<NeutronSecurityRule> secRules = secGroup.getSecurityRules();
        if (secRules != null) {
            boolean areSecRulesAdded = deleteNeutronSecurityRules(secRules, rwTx);
            if (!areSecRulesAdded) {
                rwTx.cancel();
                return;
            }
        }
        TenantId tenantId = new TenantId(Utils.normalizeUuid(secGroup.getSecurityGroupTenantID()));
        EndpointGroupId epgId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        secGroupDao.removeSecGroup(epgId);
        Optional<EndpointGroup> potentialEpg = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, epgId), rwTx);
        if (!potentialEpg.isPresent()) {
            LOG.warn("Illegal state - Endpoint group {} does not exist.", epgId.getValue());
            rwTx.cancel();
            return;
        }

        DataStoreHelper.submitToDs(rwTx);
    }

    private boolean deleteNeutronSecurityRules(List<NeutronSecurityRule> secRules, ReadWriteTransaction rwTx) {
        for (NeutronSecurityRule secRule : secRules) {
            boolean isSecRuleDeleted = secRuleAware.deleteNeutronSecurityRule(secRule, rwTx);
            if (!isSecRuleDeleted) {
                return false;
            }
        }
        return true;
    }

    private static final class SortedSecurityGroupRules {

        private final ListMultimap<EndpointGroupId, NeutronSecurityRule> secRuleByRemoteSecGrpId;
        private final ListMultimap<IpPrefix, NeutronSecurityRule> secRuleByRemoteIpPrefix;
        private final List<NeutronSecurityRule> secRulesWithoutRemote;

        private SortedSecurityGroupRules(List<NeutronSecurityRule> securityRules) {
            Preconditions.checkNotNull(securityRules);
            ListMultimap<EndpointGroupId, NeutronSecurityRule> tmpSecRuleByRemoteSecGrpId = ArrayListMultimap.create();
            ListMultimap<IpPrefix, NeutronSecurityRule> tmpSecRuleByRemoteIpPrefix = ArrayListMultimap.create();
            List<NeutronSecurityRule> tmpSecRulesWithoutRemote = new ArrayList<>();
            for (NeutronSecurityRule securityRule : securityRules) {
                String remoteSecGroupId = securityRule.getSecurityRemoteGroupID();
                String remoteIpPrefix = securityRule.getSecurityRuleRemoteIpPrefix();
                boolean isRemoteSecGroupId = remoteSecGroupId != null && !"null".equals(remoteSecGroupId);
                boolean isRemoteIpPrefix = remoteIpPrefix != null && !"null".equals(remoteIpPrefix);
                if (isRemoteSecGroupId && isRemoteIpPrefix) {
                    throw new IllegalArgumentException("Either remote group id or ip prefix "
                            + "must be speciefied in neutron security group rule." + securityRule.toString());
                }
                if (isRemoteSecGroupId) {
                    tmpSecRuleByRemoteSecGrpId.put(new EndpointGroupId(remoteSecGroupId), securityRule);
                } else if (isRemoteIpPrefix) {
                    tmpSecRuleByRemoteIpPrefix.put(Utils.createIpPrefix(remoteIpPrefix), securityRule);
                } else {
                    tmpSecRulesWithoutRemote.add(securityRule);
                }
            }
            secRuleByRemoteSecGrpId = ImmutableListMultimap.copyOf(tmpSecRuleByRemoteSecGrpId);
            secRuleByRemoteIpPrefix = ImmutableListMultimap.copyOf(tmpSecRuleByRemoteIpPrefix);
            secRulesWithoutRemote = ImmutableList.copyOf(tmpSecRulesWithoutRemote);
        }
    }

}
