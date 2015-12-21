/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.module;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class IovisorModuleManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorModuleManager.class);

    private IovisorModuleListener iovisorModuleListener;

    public IovisorModuleManager(DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "DataBroker instance must not be null");

        this.iovisorModuleListener = new IovisorModuleListener(dataBroker);
        LOG.info("Initialized IOVisor IovisorModuleManager");
    }

    @Override
    public void close() throws Exception {
        if (iovisorModuleListener != null) {
            iovisorModuleListener.close();
        }
    }
}
