/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;

import com.google.common.base.Optional;

public class SubnetUtils {

    public static Optional<Subnet> findSubnet(Uuid uuid, @Nullable Subnets subnets) {
        if (subnets == null || subnets.getSubnet() == null) {
            return Optional.absent();
        }
        for (Subnet subnet : subnets.getSubnet()) {
            if (subnet.getUuid().equals(uuid)) {
                return Optional.of(subnet);
            }
        }
        return Optional.absent();
    }
}
