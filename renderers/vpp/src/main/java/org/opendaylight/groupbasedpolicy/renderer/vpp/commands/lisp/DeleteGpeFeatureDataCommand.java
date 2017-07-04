/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 6/21/17.
 */
public class DeleteGpeFeatureDataCommand  extends AbstractLispCommand<GpeFeatureData> {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteGpeFeatureDataCommand.class);

    @Override
    public InstanceIdentifier<GpeFeatureData> getIid() {
        return VppIidFactory.getGpeFeatureDataIid();
    }

    @Override
    public GpeFeatureData getData() {
        LOG.debug("Delete commands should not invoke getData()");
        return null;
    }
}
