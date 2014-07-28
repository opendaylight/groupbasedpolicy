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

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;

/**
 * Represent a classifier definition, and provide tools for generating flow
 * rules based on the classifier
 * @author readams
 */
public abstract class Classifier {
    /**
     * Get the classifier definition for this classifier
     * @return the {@link ClassifierDefinition} for this classifier
     */
    public abstract ClassifierDefinitionId getId();

    /**
     * Get the classifier definition for this classifier
     * @return the {@link ClassifierDefinition} for this classifier
     */
    public abstract ClassifierDefinition getClassDef();
    
    /**
     * Construct a set of matches that will apply to the traffic.  Augment
     * the existing list of matches or add new matches.  It's important
     * that the order of the returned list be consistent however
     * @param matches The existing matches
     * @param params the parameters for the classifier instance
     * @return the updated list of matches (may be a different length)
     */
    public abstract List<MatchBuilder> updateMatch(List<MatchBuilder> matches,
                                                   Map<String, Object> params);
}
