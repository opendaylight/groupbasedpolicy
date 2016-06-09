/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;

/**
 * Represent a classifier definition.
 */
public abstract class Classifier {

    protected final ClassifierDefinitionId parent;

    protected Classifier(ClassifierDefinitionId parent) {
        this.parent = parent;
    }

    /**
     * Get the classifier definition id for this classifier.
     *
     * @return the {@link ClassifierDefinitionId} for this classifier
     */
    public abstract ClassifierDefinitionId getId();

    /**
     * Get parent for this classifier.
     *
     * @return parent classifier, see {@link Classifier}
     */
    public final ClassifierDefinitionId getParent() {
        return parent;
    }

    /**
     * The result represents supported parameters for the classifier by renderer.
     *
     * @return list of supported parameters by the classifier
     */
    public abstract List<SupportedParameterValues> getSupportedParameterValues();

}
