/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.location.resolver.LocationResolver;
import org.opendaylight.groupbasedpolicy.resolver.ForwardingResolver;
import org.opendaylight.groupbasedpolicy.sf.SubjectFeatureDefinitionProvider;
import org.opendaylight.groupbasedpolicy.sf.SupportedActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.sf.SupportedClassifierDefinitionListener;

public class GroupbasedpolicyInstance implements AutoCloseable {

    private final SubjectFeatureDefinitionProvider sfdp;
    private final SupportedClassifierDefinitionListener supportedClassifierDefinitionListener;
    private final SupportedActionDefinitionListener supportedActionDefinitionListener;
    private final LocationResolver locationResolver;
    private final ForwardingResolver forwardingResolver;

    public GroupbasedpolicyInstance(DataBroker dataProvider, PolicyValidatorRegistry validatorRegistry)
            throws TransactionCommitFailedException {
        sfdp = new SubjectFeatureDefinitionProvider(dataProvider);
        supportedClassifierDefinitionListener =
                new SupportedClassifierDefinitionListener(dataProvider, validatorRegistry);
        supportedActionDefinitionListener = new SupportedActionDefinitionListener(dataProvider);
        locationResolver = new LocationResolver(dataProvider);
        forwardingResolver = new ForwardingResolver(dataProvider);
    }

    @Override
    public void close() throws Exception {
        sfdp.close();
        supportedClassifierDefinitionListener.close();
        supportedActionDefinitionListener.close();
        locationResolver.close();
        forwardingResolver.close();
    }
}
