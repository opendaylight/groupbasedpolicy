/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;

public interface PolicyValidatorRegistrar {

    /**
     * Registers validator of {@link ActionInstance} for {@link ActionDefinitionId}.
     * 
     * @param actionDefinitionId cannot be {@code null}
     * @param validator cannot be {@code null}
     * @throws NullPointerException
     */
    void register(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator);

    /**
     * Unregisters validator of {@link ActionInstance} for {@link ActionDefinitionId}.
     * 
     * @param actionDefinitionId cannot be {@code null}
     * @param validator cannot be {@code null}
     * @throws NullPointerException
     */
    void unregister(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator);

    /**
     * Registers validator of {@link ClassifierInstance} for {@link ClassifierDefinitionId}.
     * 
     * @param classifierDefinitionId cannot be {@code null}
     * @param validator cannot be {@code null}
     * @throws NullPointerException
     */
    void register(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator);

    /**
     * Unregisters validator of {@link ClassifierInstance} for {@link ClassifierDefinitionId}.
     * 
     * @param classifierDefinitionId cannot be {@code null}
     * @param validator cannot be {@code null}
     * @throws NullPointerException
     */
    void unregister(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator);

}
