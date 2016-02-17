/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;

import com.google.common.base.Strings;

public class SecGroupDao {

    private final Map<EndpointGroupId, NeutronSecurityGroup> secGroupById = new HashMap<>();

    public void addSecGroup(NeutronSecurityGroup secGrp) {
        checkNotNull(secGrp);
        secGroupById.put(new EndpointGroupId(Utils.normalizeUuid(secGrp.getSecurityGroupUUID())), secGrp);
    }

    public NeutronSecurityGroup getSecGroupById(EndpointGroupId id) {
        return secGroupById.get(id);
    }

    /**
     * @param id {@link EndpointGroupId} EndpointGroupId
     * @return {@code empty string} if security group with given ID does not exist; returns
     *         {@code name of security group} if has some; otherwise security group id
     */
    public String getNameOrIdOfSecGroup(EndpointGroupId id) {
        NeutronSecurityGroup secGrp = secGroupById.get(checkNotNull(id));
        if (secGrp == null) {
            return "";
        }
        if (!Strings.isNullOrEmpty(secGrp.getSecurityGroupName())) {
            return secGrp.getSecurityGroupName();
        }
        return id.getValue();
    }

    public void removeSecGroup(EndpointGroupId id) {
        secGroupById.remove(checkNotNull(id));
    }

}
