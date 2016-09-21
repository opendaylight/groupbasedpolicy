/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapper;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Purpose: provide listener capability to EndPoint templates
 */
public interface EPTemplateListener<T extends DataObject> extends ClusteredDataTreeChangeListener<T>, AutoCloseable {

    InstanceIdentifier<SxpMapper> SXP_MAPPER_TEMPLATE_PARENT_PATH = InstanceIdentifier.create(SxpMapper.class);

    //NOBODY
}
