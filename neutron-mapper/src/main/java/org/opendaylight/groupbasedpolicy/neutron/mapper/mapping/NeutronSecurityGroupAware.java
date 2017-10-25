/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSecurityGroupAware implements NeutronAware<SecurityGroup> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityGroupAware.class);
    public static final InstanceIdentifier<SecurityGroup> SECURITY_GROUP_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(SecurityGroups.class).child(SecurityGroup.class).build();
    private final DataBroker dataProvider;
    private final NeutronSecurityRuleAware ruleAware;

    public NeutronSecurityGroupAware(DataBroker dataProvider, NeutronSecurityRuleAware ruleAware) {
        this.dataProvider = checkNotNull(dataProvider);
        this.ruleAware = checkNotNull(ruleAware);
    }

    @Override
    public void onCreated(SecurityGroup createdSecGroup, Neutron neutron) {
        LOG.trace("created securityGroup - {}", createdSecGroup);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        boolean isSecGroupCreated = addNeutronSecurityGroup(createdSecGroup, rwTx);
        if (isSecGroupCreated) {
            DataStoreHelper.submitToDs(rwTx);
        } else {
            rwTx.cancel();
        }
        ruleAware.flushPendingSecurityRulesFor(createdSecGroup.getKey(), neutron);
    }

    boolean addNeutronSecurityGroup(SecurityGroup secGroup, ReadWriteTransaction rwTx) {
        if (secGroup.getTenantId() == null) {
            LOG.warn("Skip processing group {} because TenantId is null.", secGroup);
            // TODO This needs to be reworked, SecGroups shouldn't use TenantId, Neutron doesn't always configure it
            return true;
        }
        TenantId tenantId = new TenantId(secGroup.getTenantId().getValue());
        EndpointGroupId epgId = new EndpointGroupId(secGroup.getUuid().getValue());
        if (epgId.getValue().equals(MappingUtils.EIG_UUID.getValue())) {
            ExternalImplicitGroup eig = new ExternalImplicitGroupBuilder().setId(epgId).build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.externalImplicitGroupIid(tenantId, epgId), eig,
                true);
        }
        EndpointGroupBuilder epgBuilder = new EndpointGroupBuilder().setId(epgId);
        if (!Strings.isNullOrEmpty(secGroup.getName())) {
            try {
                epgBuilder.setName(new Name(secGroup.getName()));
            } catch (NullPointerException | IllegalArgumentException e) {
                LOG.info("Name '{}' of Neutron Security-group '{}' is ignored.", secGroup.getName(),
                        secGroup.getUuid().getValue());
                LOG.debug("Name exception", e);
            }
        }
        epgBuilder.setIntraGroupPolicy(IntraGroupPolicy.RequireContract);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, epgId),
                epgBuilder.build(), true);
        return true;
    }

    @Override
    public void onUpdated(SecurityGroup oldItem, SecurityGroup newItem, Neutron oldNeutron, Neutron newNeutron) {
        LOG.warn("updated securityGroup - Never should be called "
                + "- neutron API does not allow UPDATE on neutron security group. \nSecurity group: {}", newItem);
    }

    @Override
    public void onDeleted(SecurityGroup deletedSecGroup, Neutron oldNeutron, Neutron newNeutron) {
        if (deletedSecGroup.getTenantId() == null) {
            LOG.warn("Skip deleting SecGroup {} because TenantId is null.", deletedSecGroup);
            // TODO This needs to be reworked, SecGroups shouldn't use TenantId, Neutron doesn't always configure it
            return;
        }
        LOG.trace("deleted securityGroup - {}", deletedSecGroup);
        if (newNeutron != null && newNeutron.getSecurityRules() != null
                && newNeutron.getSecurityRules().getSecurityRule() != null
                && newNeutron.getSecurityRules()
                    .getSecurityRule()
                    .stream()
                    .anyMatch(sr -> sr.getSecurityGroupId().equals(deletedSecGroup.getUuid()))) {
            LOG.warn("Cannot remove security group {} before removing last security rule.", deletedSecGroup.getKey());
            ruleAware.addPendingDeletedSecGroup(deletedSecGroup);
            return;
        }
        deleteGbpEndpointGroup(dataProvider, new TenantId(deletedSecGroup.getTenantId().getValue()),
                new EndpointGroupId(deletedSecGroup.getUuid().getValue()));
    }

    public static void deleteGbpEndpointGroup(DataBroker dataBroker, TenantId tenantId, EndpointGroupId epgId) {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        Optional<EndpointGroup> potentialEpg = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, epgId), rwTx);
        if (!potentialEpg.isPresent()) {
            LOG.warn("Illegal state - Endpoint group {} does not exist.", epgId.getValue());
            rwTx.cancel();
            return;
        }
        DataStoreHelper.submitToDs(rwTx);
    }
}
