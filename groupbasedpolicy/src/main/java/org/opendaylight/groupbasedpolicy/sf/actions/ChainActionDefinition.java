/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf.actions;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

public class ChainActionDefinition {

    public static final ActionDefinitionId ID = new ActionDefinitionId("Action-Chain");

    public static final Integer CHAIN_CONDITION_GROUP = 0xfffffe;

    // protected static final String TYPE = "type";

    // the chain action
    public static final String SFC_CHAIN_ACTION = "chain";

    // the parameter used for storing the chain name
    public static final String SFC_CHAIN_NAME = "sfc-chain-name";

    public static final ActionDefinition DEFINITION = new ActionDefinitionBuilder().setId(ID)
        .setName(new ActionName(SFC_CHAIN_ACTION))
        .setDescription(new Description("Send the traffic through a Service Function Chain"))
        .setParameter((ImmutableList.of(new ParameterBuilder().setName(new ParameterName(SFC_CHAIN_NAME))
            .setDescription(new Description("The named chain to match against"))
            .setIsRequired(IsRequired.Required)
            .setType(Type.String)
            .build())))
        .build();

    public static final InstanceIdentifier<ActionDefinition> IID = InstanceIdentifier
        .builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class, DEFINITION.getKey()).build();

}
