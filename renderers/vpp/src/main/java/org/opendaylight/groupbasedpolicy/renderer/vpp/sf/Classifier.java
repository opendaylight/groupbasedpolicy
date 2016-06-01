/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

/**
 * Represent a classifier definition.
 */
public abstract class Classifier {

    protected final Classifier parent;

    public static final EtherTypeClassifier ETHER_TYPE_CL = new EtherTypeClassifier(null);
    public static final IpProtoClassifier IP_PROTO_CL = new IpProtoClassifier(ETHER_TYPE_CL);

    protected Classifier(Classifier parent) {
        this.parent = parent;
    }

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
