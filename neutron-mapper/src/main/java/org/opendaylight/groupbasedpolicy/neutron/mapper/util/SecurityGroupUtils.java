/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;

public class SecurityGroupUtils {

    public static Optional<SecurityGroup> findSecurityGroup(Uuid secGrpUuid, @Nullable SecurityGroups securityGroups) {
        Preconditions.checkNotNull(secGrpUuid);
        if (securityGroups == null || securityGroups.getSecurityGroup() == null) {
            return Optional.absent();
        }
        for (SecurityGroup secGroup : securityGroups.getSecurityGroup()) {
            if (secGrpUuid.equals(secGroup.getUuid())) {
                return Optional.of(secGroup);
            }
        }
        return Optional.absent();
    }

    public static String getNameOrUuid(SecurityGroup secGroup) {
        if (!Strings.isNullOrEmpty(secGroup.getName())) {
            return secGroup.getName();
        }
        return secGroup.getUuid().getValue();
    }

}
