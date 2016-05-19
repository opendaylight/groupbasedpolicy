/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.vpp_provider.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppRenderer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(VppRenderer.class);

    private DataBroker dataBroker;

    public VppRenderer(DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker);

        this.dataBroker = dataBroker;

        LOG.info("VPP Renderer has Started");
    }

    @Override
    public void close() throws Exception {
        this.dataBroker = null;
    }

}
