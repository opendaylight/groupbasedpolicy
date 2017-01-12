/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.GbpAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent a classifier definition.
 */
public abstract class Classifier {

    private static final Logger LOG = LoggerFactory.getLogger(Classifier.class);
    protected final Classifier parent;

    protected Classifier(Classifier parent) {
        this.parent = parent;
    }

    /**
     * Template method for resolving {@code matches}.
     *
     * @param aclRuleBuilder list of builders containing {@code matches} to update
     * @param params parameters of classifier-instance inserted by user
     * @return result, which indicates if all the matching fields were updated successfully
     */
    public final boolean updateMatch(GbpAceBuilder aclRuleBuilder, Map<String, ParameterValue> params) {
        LOG.debug("Updating ACE entries {} with parameters {}",aclRuleBuilder, params);
        if (params == null) {
                return false;
        }
        GbpAceBuilder matchBuilders = aclRuleBuilder;
        try {
            checkPresenceOfRequiredParams(params);
            matchBuilders = this.update(matchBuilders, params);
            Classifier clParent = this.getParent();
            List<Classifier> updatedClassifiers = new ArrayList<>();
            updatedClassifiers.add(this);
            while (clParent != null) {
                boolean hasReqParams = true;
                try {
                    clParent.checkPresenceOfRequiredParams(params);
                } catch (IllegalArgumentException e) {
                    LOG.error("Missing required params for classifier {}. {}", clParent.getId(), e);
                    hasReqParams = false;
                }
                if (hasReqParams) {
                    matchBuilders = clParent.update(matchBuilders, params);
                    updatedClassifiers.add(clParent);
                }
                clParent = clParent.getParent();
            }
            for (Classifier updatedClassifier : updatedClassifiers) {
                updatedClassifier.checkPrereqs(matchBuilders);
            }
        } catch (IllegalArgumentException e) {
                LOG.error("Failed to update matches {}", e);
                return false;
        }
        return true;
    }

    abstract GbpAceBuilder update(GbpAceBuilder ruleBuilder, Map<String, ParameterValue> params);

    abstract void checkPrereqs(GbpAceBuilder matchBuilders);

    /**
     * Get the classifier definition id for this classifier.
     *
     * @return the {@link ClassifierDefinitionId} for this classifier
     */
    public abstract ClassifierDefinitionId getId();

    /**
     * Get the classifier definition for this classifier.
     *
     * @return the {@link ClassifierDefinition} for this classifier
     */
    public abstract ClassifierDefinition getClassifierDefinition();

    /**
     * Get parent for this classifier.
     *
     * @return parent classifier, see {@link Classifier}
     */
    public final Classifier getParent() {
        return parent;
    }

    /**
     * The result represents supported parameters for the classifier by renderer.
     *
     * @return list of supported parameters by the classifier
     */
    public abstract List<SupportedParameterValues> getSupportedParameterValues();

    /**
     * Checks presence of required {@code params} in order to decide if classifier can update
     * {@code matches} properly.
     *
     * @param params inserted parameters, not null
     * @throws IllegalArgumentException when any of required {@code params} is not present
     */
    protected abstract void checkPresenceOfRequiredParams(Map<String, ParameterValue> params);

}
