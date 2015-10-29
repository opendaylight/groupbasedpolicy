/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf.classifiers;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match on the IP protocol of IP traffic
 */
public class IpProtoClassifierDefinition {

    /**
     * Protocol parameter name
     */
    public static final String PROTO_PARAM = "proto";
    /**
     * TCP protocol value
     */
    public static final Long TCP_VALUE = Long.valueOf(6);
    /**
     * UDP protocol value
     */
    public static final Long UDP_VALUE = Long.valueOf(17);
    /**
     * ICMP protocol value
     */
    public static final Long ICMP_VALUE = Long.valueOf(1);
    /**
     * SCTP protocol value
     */
    public static final Long SCTP_VALUE = Long.valueOf(132);

    public static final ClassifierDefinitionId ID =
            new ClassifierDefinitionId("79c6fdb2-1e1a-4832-af57-c65baf5c2335");
    /**
     * Protocol classifier-definition
     */
    public static final ClassifierDefinition DEFINITION =
            new ClassifierDefinitionBuilder().setId(ID)
                    .setName(new ClassifierName("ip_proto"))
                    .setDescription(new Description("Match on the IP protocol of IP traffic"))
                    .setParameter(ImmutableList.of(
                                    new ParameterBuilder().setName(new ParameterName(PROTO_PARAM))
                                            .setDescription(new Description(
                                                    "The IP protocol to match against"))
                                            .setIsRequired(IsRequired.Required)
                                            .setType(Type.Int)
                                            .build()))
                    .build();

    public static final InstanceIdentifier IID =
            InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                    .child(ClassifierDefinition.class, DEFINITION.getKey())
                    .build();

}
