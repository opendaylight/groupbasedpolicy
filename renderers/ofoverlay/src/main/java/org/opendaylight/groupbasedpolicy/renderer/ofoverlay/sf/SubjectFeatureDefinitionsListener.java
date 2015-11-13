/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFeatureDefinitionsListener
        implements DataTreeChangeListener<ClassifierDefinition>, AutoCloseable {

    private static final Logger LOG =
            LoggerFactory.getLogger(SubjectFeatureDefinitionsListener.class);

    private final DataBroker dataProvider;
    private final ListenerRegistration<SubjectFeatureDefinitionsListener> registration;

    private static final Map<ClassifierDefinitionId, Classifier> OF_CLASSIFIERS =
            SubjectFeatures.getClassifiers();

    public SubjectFeatureDefinitionsListener(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        registration = dataProvider.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                                .child(ClassifierDefinition.class)
                                .build()), this);

    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ClassifierDefinition>> changes) {

        for (DataTreeModification<ClassifierDefinition> change : changes) {
            DataObjectModification<ClassifierDefinition> rootNode = change.getRootNode();

            List<SupportedClassifierDefinition> supportedClassifierDefinitions = new ArrayList<>();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    ClassifierDefinition classifierDefinitionAfter =
                            checkNotNull(rootNode.getDataAfter());
                    Classifier ourClassifier =
                            OF_CLASSIFIERS.get(classifierDefinitionAfter.getId());
                    if (ourClassifier != null) {
                        List<SupportedParameterValues> spValues =
                                ourClassifier.getSupportedParameterValues();

                        SupportedClassifierDefinitionBuilder scdBuilder =
                                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(
                                        ourClassifier.getClassifierDefinition().getId())
                                        .setSupportedParameterValues(spValues);
                        if (ourClassifier.getParent() != null) {
                            scdBuilder.setParentClassifierDefinitionId(
                                    ourClassifier.getParent().getId());
                        }
                        supportedClassifierDefinitions.add(scdBuilder.build());
                    }

                    if (!supportedClassifierDefinitions.isEmpty()) {
                        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
                        for (SupportedClassifierDefinition def : supportedClassifierDefinitions) {
                            wt.put(LogicalDatastoreType.OPERATIONAL,
                                    InstanceIdentifier.builder(Renderers.class)
                                            .child(Renderer.class)
                                            .child(Capabilities.class)
                                            .child(SupportedClassifierDefinition.class)
                                            .build(), def, true);
                        }
                        wt.submit();
                    }
                    break;

                case DELETE:
                    ClassifierDefinition classifierDefinitionBefore =
                            checkNotNull(rootNode.getDataAfter());
                    ClassifierDefinitionId id = classifierDefinitionBefore.getId();
                    WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
                    wt.delete(LogicalDatastoreType.OPERATIONAL,
                            InstanceIdentifier.builder(Renderers.class)
                                    .child(Renderer.class)
                                    .child(Capabilities.class)
                                    .child(SupportedClassifierDefinition.class,
                                            new SupportedClassifierDefinitionKey(id))
                                    .build());
                    wt.submit();
                    break;
            }

        }
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
