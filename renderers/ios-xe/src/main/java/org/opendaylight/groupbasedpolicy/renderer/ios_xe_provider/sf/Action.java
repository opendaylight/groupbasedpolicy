/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf;

import java.util.List;

import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.action.definition.SupportedParameterValues;

public abstract class Action implements Validator<ActionInstance> {

    /**
     * Get the action definition for this action.
     *
     * @return the {@link ActionDefinition} for this action
     */
    public abstract ActionDefinitionId getId();

    /**
     * The result represents supported parameters for the action by renderer.
     *
     * @return list of supported parameters by the action
     */
    public abstract List<SupportedParameterValues> getSupportedParameterValues();

}
