/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RendererPolicyConfEvent extends DtoChangeEvent<RendererPolicy> {

    public RendererPolicyConfEvent(InstanceIdentifier<RendererPolicy> iid, RendererPolicy before,
            RendererPolicy after) {
        super(iid, before, after);
    }

}
