/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

public class SecurityRuleUtils {

    public static Optional<SecurityRule> findSecurityRule(Uuid uuid, @Nullable SecurityRules securityRules) {
        if (securityRules == null || securityRules.getSecurityRule() == null) {
            return Optional.absent();
        }
        for (SecurityRule secRule : securityRules.getSecurityRule()) {
            if (secRule.getUuid().equals(uuid)) {
                return Optional.of(secRule);
            }
        }
        return Optional.absent();
    }

    public static Set<SecurityRule> findSecurityRulesBySecGroupAndRemoteSecGroup(Uuid secGroup,
            @Nullable Uuid remoteSecGroup, Neutron neutron) {
        Preconditions.checkNotNull(secGroup);
        return FluentIterable.from(findAllSecurityRules(neutron)).filter(new Predicate<SecurityRule>() {

            @Override
            public boolean apply(SecurityRule secRule) {
                return (secRule.getSecurityGroupId().equals(secGroup)
                        && Objects.equal(secRule.getRemoteGroupId(), remoteSecGroup));
            }
        }).toSet();
    }

    public static Set<Uuid> findSecurityGroupsHavingSecurityRules(Neutron neutron) {
        return FluentIterable.from(findAllSecurityRules(neutron)).transform(new Function<SecurityRule, Uuid>() {

            @Override
            public Uuid apply(SecurityRule secRule) {
                return secRule.getSecurityGroupId();
            }
        }).toSet();
    }

    public static List<SecurityRule> findAllSecurityRules(Neutron neutron) {
        Preconditions.checkNotNull(neutron);
        SecurityRules securityRules = neutron.getSecurityRules();
        if (securityRules == null || securityRules.getSecurityRule() == null) {
            return Collections.emptyList();
        }
        return securityRules.getSecurityRule();
    }

}
