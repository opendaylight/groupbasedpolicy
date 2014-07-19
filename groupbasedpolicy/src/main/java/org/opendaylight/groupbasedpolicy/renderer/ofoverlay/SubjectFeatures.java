/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Defines the subject features that are supported by the OF overlay renderer
 */
public class SubjectFeatures {
    public static final ActionDefinition ALLOW = 
            new ActionDefinitionBuilder()
                .setId(new ActionDefinitionId("f942e8fd-e957-42b7-bd18-f73d11266d17"))
                .setName(new ActionName("allow"))
                .setDescription(new Description("Allow the specified traffic to pass"))
                .build();

    public static final ClassifierDefinition ETHER_TYPE = 
            new ClassifierDefinitionBuilder()
                .setId(new ClassifierDefinitionId("79c6fdb2-1e1a-4832-af57-c65baf5c2335"))
                .setName(new ClassifierName("ether_type"))
                .setDescription(new Description("Match on the ether type of the traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName("type"))
                    .setDescription(new Description("The ethertype to match against"))
                    .setType(Type.Int)
                    .build()))
                .build();

    public static final ClassifierDefinition IP_PROTO = 
            new ClassifierDefinitionBuilder()
                .setId(new ClassifierDefinitionId("79c6fdb2-1e1a-4832-af57-c65baf5c2335"))
                .setParent(ETHER_TYPE.getId())
                .setName(new ClassifierName("ip_proto"))
                .setDescription(new Description("Match on the IP protocol of IP traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName("proto"))
                    .setDescription(new Description("The IP protocol to match against"))
                    .setType(Type.Int)
                    .build()))
                .build();

    public static final ClassifierDefinition UDP_PORT = 
            new ClassifierDefinitionBuilder()
                .setId(new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f"))
                .setParent(IP_PROTO.getId())
                .setName(new ClassifierName("udp_port"))
                .setDescription(new Description("Match on the port number of UDP traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName("port"))
                    .setDescription(new Description("The port number to match against"))
                    .setType(Type.Int)
                    .build()))
                .build();

    public static final ClassifierDefinition TCP_PORT = 
            new ClassifierDefinitionBuilder()
                .setId(new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f"))
                .setParent(IP_PROTO.getId())
                .setName(new ClassifierName("tcp_port"))
                .setDescription(new Description("Match on the port number of TCP traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName("port"))
                    .setDescription(new Description("The port number to match against"))
                    .setType(Type.Int)
                    .build()))
                .build();

    public static final SubjectFeatureDefinitions OF_OVERLAY_FEATURES =
            new SubjectFeatureDefinitionsBuilder()
                .setActionDefinition(ImmutableList.of(ALLOW))
                .setClassifierDefinition(ImmutableList.of(ETHER_TYPE,
                                                          IP_PROTO,
                                                          UDP_PORT,
                                                          TCP_PORT))
                .build();
}
