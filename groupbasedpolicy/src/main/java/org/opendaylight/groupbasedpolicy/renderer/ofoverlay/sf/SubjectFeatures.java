/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Defines the subject features that are supported by the OF overlay renderer
 */
public class SubjectFeatures {
    private static final Map<ClassifierDefinitionId, Classifier> classifiers =
            ImmutableMap.<ClassifierDefinitionId, Classifier>
                of(EtherTypeClassifier.ID, new EtherTypeClassifier(),
                   IpProtoClassifier.ID, new IpProtoClassifier(),
                   L4Classifier.ID, new L4Classifier());

    private static final List<ClassifierDefinition> classifierDefs =
            ImmutableList.copyOf(Collections2.transform(classifiers.values(), 
                new Function<Classifier, ClassifierDefinition>() {
                    @Override
                    public ClassifierDefinition apply(Classifier input) {
                        return input.getClassDef();
                    }
                }
            ));
    
    public static final ActionDefinition ALLOW = 
            new ActionDefinitionBuilder()
                .setId(new ActionDefinitionId("f942e8fd-e957-42b7-bd18-f73d11266d17"))
                .setName(new ActionName("allow"))
                .setDescription(new Description("Allow the specified traffic to pass"))
                .build();

    public static final SubjectFeatureDefinitions OF_OVERLAY_FEATURES =
            new SubjectFeatureDefinitionsBuilder()
                .setActionDefinition(ImmutableList.of(ALLOW))
                .setClassifierDefinition(classifierDefs)
                .build();

    /**
     * Get the {@link Classifier} associated with the given 
     * {@link ClassifierDefinitionId}
     * @param id the {@link ClassifierDefinitionId} to look up
     * @return the {@link Classifier} if one exists, or <code>null</code> 
     * otherwise
     */
    public static Classifier getClassifier(ClassifierDefinitionId id) {
        return classifiers.get(id);
    }
                                           
}
