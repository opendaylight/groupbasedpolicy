/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;

/**
 * Purpose: keeper of {@link IseSourceConfig}, UUID-to-SGT map
 * <br>
 * Unique identifier is tenant-id (expect 1 tenant = 1 ise-source)
 */
public class IseContext {

    private final IseSourceConfig iseSourceConfig;
    private final Map<String, Integer> uuidToSgtMap;

    /**
     * default ctor
     * @param iseSourceConfig ise provider coordinates
     */
    public IseContext(final IseSourceConfig iseSourceConfig) {
        this.iseSourceConfig = iseSourceConfig;
        this.uuidToSgtMap = new HashMap<>();
    }

    /**
     * @return ise coordinates
     */
    public IseSourceConfig getIseSourceConfig() {
        return iseSourceConfig;
    }

    /**
     * @return known UUID-to-SGT relations
     */
    public Map<String, Integer> getUuidToSgtMap() {
        return uuidToSgtMap;
    }
}
