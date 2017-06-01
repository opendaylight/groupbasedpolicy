/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GpeEnableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 6/1/17.
 */
public class ConfigureGpeCommand extends AbstractLispCommand<GpeFeatureData> {

    private GpeEnableDom gpeEnableDom;

    public ConfigureGpeCommand(GpeEnableDom gpeEnableDom) {
        this.gpeEnableDom = gpeEnableDom;
    }

    @Override
    public InstanceIdentifier<GpeFeatureData> getIid() {
        return VppIidFactory.getGpeFeatureDataIid();
    }

    @Override
    public GpeFeatureData getData() {
        return gpeEnableDom.getSALObject();
    }
}
