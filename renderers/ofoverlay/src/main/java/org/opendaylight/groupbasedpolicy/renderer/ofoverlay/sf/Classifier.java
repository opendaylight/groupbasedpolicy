/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;

import com.google.common.base.Strings;

/**
 * Represent a classifier definition, and provide tools for generating flow
 * rules based on the classifier
 */
public abstract class Classifier {

    protected final Classifier parent;

    public static final EtherTypeClassifier ETHER_TYPE_CL = new EtherTypeClassifier(null);
    public static final IpProtoClassifier IP_PROTO_CL = new IpProtoClassifier(ETHER_TYPE_CL);
    public static final L4Classifier L4_CL = new L4Classifier(IP_PROTO_CL);

    protected Classifier(Classifier parent) {
        this.parent = parent;
    }

    /**
     * Get the classifier definition id for this classifier
     *
     * @return the {@link ClassifierDefinitionId} for this classifier
     */
    public abstract ClassifierDefinitionId getId();

    /**
     * Get the classifier definition for this classifier
     *
     * @return the {@link ClassifierDefinition} for this classifier
     */
    public abstract ClassifierDefinition getClassDef();

    /**
     * @return parent classifier, see {@link Classifier}
     */
    public final Classifier getParent() {
        return parent;
    }

    /**
     * Template method for resolving {@code matches}.
     *
     * @param matches list of builders containing {@code matches} to update
     * @param params parameters of classifier-instance inserted by user
     * @return result, which indicates if all the matching fields were updated successfully and
     *         contain updated {@code matches}, see {@link ClassificationResult}
     */
    public final ClassificationResult updateMatch(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        if (params == null) {
            return new ClassificationResult("Classifier: {" + this.getClassDef().getName() + "} No parameters present.");
        }
        List<MatchBuilder> matchBuilders = matches;
        try {
            checkPresenceOfRequiredParams(params);
            matchBuilders = this.update(matchBuilders, params);
            Classifier parent = this.getParent();
            List<Classifier> updatedClassifiers = new ArrayList<>();
            updatedClassifiers.add(this);
            while (parent != null) {
                boolean hasReqParams = true;
                try {
                    parent.checkPresenceOfRequiredParams(params);
                } catch (IllegalArgumentException e) {
                    hasReqParams = false;
                }
                if (hasReqParams == true) {
                    matchBuilders = parent.update(matchBuilders, params);
                    updatedClassifiers.add(parent);
                }
                parent = parent.getParent();
            }
            for (Classifier updatedClassifier : updatedClassifiers) {
                updatedClassifier.checkPrereqs(matchBuilders);
            }
        } catch (IllegalArgumentException e) {
            if (!Strings.isNullOrEmpty(e.getMessage())) {
                return new ClassificationResult(e.getMessage());
            } else
                return new ClassificationResult("Error while processing data of " + this.getClassDef().getName()
                        + " classifier. Classification was not successful.");
        }
        return new ClassificationResult(matchBuilders);
    }

    /**
     * Checks presence of required {@code params} in order to decide if classifier can update {@code matches} properly
     * in  method {@link #update(List, Map)}
     * @param  params  inserted parameters, not null
     * @throws  IllegalArgumentException when any of required {@code params} is not present, see {@link #updateMatch(List, Map)}
     */
    protected abstract void checkPresenceOfRequiredParams(Map<String, ParameterValue> params);

    /**
     * Resolves {@code matches} from inserted {@code params} and updates them.
     * <p>
     * Updates fields in {@code matches} or it can creates new matches. If it creates new matches it
     * has to always use match from {@code matches} as parameter in constructor
     * {@code MatchBuilder(Match base)}
     *
     * @param matches - fields to update
     * @param params - input parameters
     * @return updated {@code matches}. It is allowed to return new object.
     * @throws IllegalArgumentException when update fails because of bad input
     *         (e.g. overriding existing matches with different values is not permitted)
     */
    protected abstract List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params);

    /**
     * Checks whether prerequisites (required {@code matches}) for the match that this classifier
     * updates are present
     * according to Openflow specifications.
     *
     * @param matches input list of matches to check
     * @throws IllegalArgumentException when any of prerequisites is not present
     */
    protected abstract void checkPrereqs(List<MatchBuilder> matches);
}
