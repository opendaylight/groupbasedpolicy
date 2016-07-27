/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.SecurityRuleAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;

import com.google.common.base.Strings;

public final class NeutronEntityFactory {

    private NeutronEntityFactory() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    public static SecurityGroup securityGroup(String id, String tenantId) {
        return new SecurityGroupBuilder().setUuid(new Uuid(id)).setTenantId(new Uuid(tenantId)).build();
    }

    public static SecurityGroup securityGroupWithName(String id, String tenantId, String name) {
        return new SecurityGroupBuilder().setUuid(new Uuid(id)).setTenantId(new Uuid(tenantId)).setName(name).build();
    }

    public static SecurityRule securityRuleWithoutGroupIds(String id, String tenant, Class<? extends EthertypeBase> etherType,
            Class<? extends DirectionBase> direction, Class<? extends ProtocolBase> protocol, int portMin, int portMax) {
        SecurityRuleAttributes.Protocol proto = new SecurityRuleAttributes.Protocol(protocol);
        SecurityRuleBuilder secRule = new SecurityRuleBuilder();
        secRule.setUuid(new Uuid(id));
        secRule.setTenantId(new Uuid(tenant));
        secRule.setEthertype(etherType);
        secRule.setDirection(direction);
        secRule.setProtocol(proto);
        secRule.setPortRangeMin(portMin);
        secRule.setPortRangeMax(portMax);
        return secRule.build();
    }

    public static SecurityRule securityRuleWithEtherType(String id, String tenant,
            Class<? extends EthertypeBase> etherType, Class<? extends DirectionBase> direction, String ownerGroupId,
            String remoteGroupId) {
        SecurityRuleBuilder securityRuleBuilder = new SecurityRuleBuilder().setUuid(new Uuid(id))
            .setTenantId(new Uuid(tenant))
            .setEthertype(etherType)
            .setDirection(direction)
            .setSecurityGroupId(new Uuid(ownerGroupId));
        if (!Strings.isNullOrEmpty(remoteGroupId)) {
            securityRuleBuilder.setRemoteGroupId(new Uuid(remoteGroupId));
        }
        return securityRuleBuilder.build();
    }
}
