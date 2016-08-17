/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

public class PolicyValidatorRegistryInstance implements PolicyValidatorRegistry, AutoCloseable {

    private final PolicyResolver policyResolver;

    public PolicyValidatorRegistryInstance (DataBroker dataBroker) {
        this.policyResolver = new PolicyResolver(dataBroker);
    }

    @Override
    public void close() throws Exception {
        policyResolver.close();
    }

    @Override
    public void register(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        policyResolver.register(actionDefinitionId, validator);
    }

    @Override
    public void unregister(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        policyResolver.unregister(actionDefinitionId, validator);
    }

    @Override
    public void register(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        policyResolver.register(classifierDefinitionId, validator);
    }

    @Override
    public void unregister(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        policyResolver.unregister(classifierDefinitionId, validator);
    }
}
