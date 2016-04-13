/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Purpose: provide listener capability to {@link MasterDatabaseBinding} (Sxp - MasterDB)
 */
public interface MasterDatabaseBindingListener extends DataTreeChangeListener<MasterDatabaseBinding>, AutoCloseable {
    /** path to SXP topology */
    InstanceIdentifier<Topology> SXP_TOPOLOGY_PATH = null;
}
