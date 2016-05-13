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
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;

import com.google.common.base.Optional;

public class RouterUtils {

    public static Optional<Router> findRouter(Uuid uuid, @Nullable Routers routers) {
        if (routers == null || routers.getRouter() == null) {
            return Optional.absent();
        }
        for (Router router : routers.getRouter()) {
            if (router.getUuid().equals(uuid)) {
                return Optional.of(router);
            }
        }
        return Optional.absent();
    }
}
