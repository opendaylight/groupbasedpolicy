/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvedPolicyListener extends DataTreeChangeHandler<ResolvedPolicy> {

    protected ResolvedPolicyListener(DataBroker dataProvider, DataTreeIdentifier<ResolvedPolicy> pointOfInterest) {
        super(dataProvider, pointOfInterest);
        LOG.info("Initialised ResolvedPolicyListener");
    }

    private static final Logger LOG = LoggerFactory.getLogger(ResolvedPolicyListener.class);

    @Override
    protected void onWrite(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        LOG.info("Not implemented yet");

    }

    @Override
    protected void onDelete(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        LOG.info("Not implemented yet");

    }

    @Override
    protected void onSubreeModified(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        LOG.info("Not implemented yet");

    }

}
