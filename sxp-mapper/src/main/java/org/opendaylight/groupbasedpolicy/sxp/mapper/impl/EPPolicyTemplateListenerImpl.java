/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listens to EP policy template and propagates change events for further processing
 */
public class EPPolicyTemplateListenerImpl implements EPTemplateListener {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateListenerImpl.class);
    private final ListenerRegistration<DataChangeListener> listenerRegistration;
    private final InstanceIdentifier<?> templatePath;

    public EPPolicyTemplateListenerImpl(final DataBroker dataBroker, final SxpMapperReactor sxpMapperReactor) {
        // TODO: get template path
        templatePath = null;
        listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, templatePath,
                this, AsyncDataBroker.DataChangeScope.BASE);
        LOG.debug("started listening to {}", templatePath);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent) {
        // sxpMapperReactor.process(template)
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
