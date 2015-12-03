/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api.sf;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

/**
 * Match on the ether type of the traffic
 */
public class EtherTypeClassifierDefinition {

    /**
     * Ethertype parameter name
     */
    public static final String ETHERTYPE_PARAM = "ethertype";
    /**
     * ARP ethertype value
     */
    public static final Long ARP_VALUE = Long.valueOf(0x0806);
    /**
     * IPv4 ethertype value
     */
    public static final Long IPv4_VALUE = Long.valueOf(0x0800);
    /**
     * IPv6 ethertype value
     */
    public static final Long IPv6_VALUE = Long.valueOf(0x86DD);

    public static final ClassifierDefinitionId ID = new ClassifierDefinitionId("Classifier-EtherType");
    /**
     * Ethertype classifier-definition
     */
    public static final ClassifierDefinition DEFINITION = new ClassifierDefinitionBuilder().setId(ID)
        .setName(new ClassifierName("ether_type"))
        .setDescription(new Description("Match on the ether type of the traffic"))
        .setParameter(ImmutableList.of(new ParameterBuilder().setName(new ParameterName(ETHERTYPE_PARAM))
            .setDescription(new Description("The ethertype to match against"))
            .setIsRequired(IsRequired.Required)
            .setType(Type.Int)
            .build()))
        .build();

    public static final InstanceIdentifier<ClassifierDefinition> IID = InstanceIdentifier
        .builder(SubjectFeatureDefinitions.class).child(ClassifierDefinition.class, DEFINITION.getKey()).build();

}
