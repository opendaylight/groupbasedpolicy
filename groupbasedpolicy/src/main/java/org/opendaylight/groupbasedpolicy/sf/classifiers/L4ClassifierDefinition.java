/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf.classifiers;

import com.google.common.collect.ImmutableList;
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

/**
 * Match against TCP or UDP, and source and/or destination ports
 */
public class L4ClassifierDefinition {

    /**
     * Source port parameter name
     */
    public static final String SRC_PORT_PARAM = "sourceport";
    /**
     * Source port range parameter name
     */
    public static final String SRC_PORT_RANGE_PARAM = "sourceport_range";
    /**
     * Destination port parameter name
     */
    public static final String DST_PORT_PARAM = "destport";
    /**
     * Destination port range parameter name
     */
    public static final String DST_PORT_RANGE_PARAM = "destport_range";

    public static final ClassifierDefinitionId ID = new ClassifierDefinitionId(
            "4250ab32-e8b8-445a-aebb-e1bd2cdd291f");
    /**
     * Layer 4 classifier-definition
     */
    public static final ClassifierDefinition DEFINITION = new ClassifierDefinitionBuilder().setId(
            new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f"))
        .setName(new ClassifierName("l4"))
        .setDescription(new Description("Match on the port number of UDP or TCP traffic"))
        .setParameter(
                ImmutableList.of(
                        new ParameterBuilder().setName(new ParameterName(SRC_PORT_PARAM))
                            .setDescription(new Description("The source port number to match against"))
                            .setType(Type.Int)
                            .setIsRequired(IsRequired.Optional)
                            .build(),
                        new ParameterBuilder().setName(new ParameterName(SRC_PORT_RANGE_PARAM))
                            .setDescription(new Description("The source port range to match against"))
                            .setType(Type.Range)
                            .setIsRequired(IsRequired.Optional)
                            .build(),
                        new ParameterBuilder().setName(new ParameterName(DST_PORT_PARAM))
                            .setDescription(new Description("The destination port number to match against"))
                            .setType(Type.Int)
                            .setIsRequired(IsRequired.Optional)
                            .build(), 
                        new ParameterBuilder().setName(new ParameterName(DST_PORT_RANGE_PARAM))
                            .setDescription(new Description("The destination port range to match against"))
                            .setType(Type.Range)
                            .setIsRequired(IsRequired.Optional)
                            .build()))
        .build();

    public static final InstanceIdentifier<ClassifierDefinition> IID =
            InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                    .child(ClassifierDefinition.class, DEFINITION.getKey())
                    .build();

}
