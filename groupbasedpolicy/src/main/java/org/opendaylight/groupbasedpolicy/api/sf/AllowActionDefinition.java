/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api.sf;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Allow action
 */
public class AllowActionDefinition {

    public static final ActionDefinitionId ID = new ActionDefinitionId("Action-Allow");
    /**
     * Access control - allow action-definition
     */
    public static final ActionDefinition DEFINITION = new ActionDefinitionBuilder().setId(ID)
        .setName(new ActionName("allow"))
        .setDescription(new Description("Allow the specified traffic to pass"))
        .build();

    public static final InstanceIdentifier<ActionDefinition> IID = InstanceIdentifier
        .builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class, DEFINITION.getKey()).build();

}
