/*
 * Copyright (c) 2017 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 2/17/17.
 */
public class NeutronTenantToVniMapper {
    private HashMap<String, Long> neutronTenantToVniMapper;
    private long id = 1;

    private static final NeutronTenantToVniMapper INSTANCE = new NeutronTenantToVniMapper();

    public static NeutronTenantToVniMapper getInstance() {
        return INSTANCE;
    }

    private NeutronTenantToVniMapper() {
        neutronTenantToVniMapper = new HashMap<>();
    }

    public synchronized long getVni(String tenantUuid) {
        if (neutronTenantToVniMapper.containsKey(tenantUuid)) {
            return neutronTenantToVniMapper.get(tenantUuid);
        }
        neutronTenantToVniMapper.put(tenantUuid, id);
        return id++;
    }
}
