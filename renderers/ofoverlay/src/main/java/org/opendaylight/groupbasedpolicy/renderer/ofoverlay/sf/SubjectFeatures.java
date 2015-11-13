/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.actions.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.sf.actions.ChainActionDefinition;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Defines the subject features that are supported by the OF overlay renderer
 */
public class SubjectFeatures {

    private static final Map<ClassifierDefinitionId, Classifier> classifiers = ImmutableMap.
            of(EtherTypeClassifierDefinition.ID, Classifier.ETHER_TYPE_CL,
                    IpProtoClassifierDefinition.ID, Classifier.IP_PROTO_CL,
                    L4ClassifierDefinition.ID, Classifier.L4_CL);

    private static final List<ClassifierDefinition> classifierDefs =
            ImmutableList.copyOf(Collections2.transform(classifiers.values(),
                new Function<Classifier, ClassifierDefinition>() {
                    @Override
                    public ClassifierDefinition apply(Classifier input) {
                        return input.getClassifierDefinition();
                    }
                }
            ));

    private static final Map<ActionDefinitionId, Action> actions = ImmutableMap.
            of(AllowActionDefinition.ID, new AllowAction(), ChainActionDefinition.ID,
                    new ChainAction());

    private static final List<ActionDefinition> actionDefs =
            ImmutableList.copyOf(Collections2.transform(actions.values(),
                new Function<Action, ActionDefinition>() {
                    @Override
                    public ActionDefinition apply(Action input) {
                        return input.getActionDef();
                    }
                }
             ));

    public static final SubjectFeatureDefinitions OF_OVERLAY_FEATURES =
            new SubjectFeatureDefinitionsBuilder()
                .setActionDefinition(actionDefs)
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


    public static Map<ClassifierDefinitionId, Classifier> getClassifiers() {
        return classifiers;
    }

    public static Map<ActionDefinitionId, Action> getActions() {
        return actions;
    }

    /**
     * Get the {@link Action} associated with the given
     * {@link ActionDefinitionId}
     * @param id the {@link ActionDefinitionId} to look up
     * @return the {@link Action} if one exists, or <code>null</code>
     * otherwise
     */
    public static Action getAction(ActionDefinitionId id) {
        return actions.get(id);
    }
}
