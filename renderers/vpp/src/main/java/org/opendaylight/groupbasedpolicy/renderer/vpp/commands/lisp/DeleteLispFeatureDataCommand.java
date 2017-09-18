/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteLispFeatureDataCommand extends AbstractLispCommand<LispFeatureData> {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteLispFeatureDataCommand.class);

    @Override
    public InstanceIdentifier<LispFeatureData> getIid() {
        return VppIidFactory.getLispFeatureDataIid();
    }

    @Override
    public LispFeatureData getData() {
        LOG.debug("Delete commands should not invoke getData()");
        return null;
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + "DeleteLispFeatureDataCommand{}";
    }
}
