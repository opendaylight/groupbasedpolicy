/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class GbpSubnetEvent extends DtoChangeEvent<GbpSubnet>{

    public GbpSubnetEvent(@Nonnull InstanceIdentifier<GbpSubnet> iid, @Nullable GbpSubnet before, @Nullable GbpSubnet after) {
        super(iid, before, after);
    }
}
