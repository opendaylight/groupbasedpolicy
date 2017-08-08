/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

public class ClassifierDefinitionListener implements ClusteredDataTreeChangeListener<ClassifierDefinition>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierDefinitionListener.class);
    private static final InstanceIdentifier<Capabilities> RENDERER_CAPABILITIES_IID = InstanceIdentifier
        .builder(Renderers.class)
        .child(Renderer.class, new RendererKey(OFOverlayRenderer.RENDERER_NAME))
        .child(Capabilities.class)
        .build();
    private static final String PUT = "stored";
    private static final String DELETED = "removed";

    private final DataBroker dataProvider;
    private final ListenerRegistration<ClassifierDefinitionListener> registration;

    public ClassifierDefinitionListener(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        registration =
                dataProvider
                    .registerDataTreeChangeListener(
                            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier
                                .builder(SubjectFeatureDefinitions.class).child(ClassifierDefinition.class).build()),
                    this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ClassifierDefinition>> changes) {
        for (DataTreeModification<ClassifierDefinition> change : changes) {
            DataObjectModification<ClassifierDefinition> rootNode = change.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    ClassifierDefinition classifierDefinitionAfter = checkNotNull(rootNode.getDataAfter());
                    Classifier ourClassifier = SubjectFeatures.getClassifier(classifierDefinitionAfter.getId());
                    if (ourClassifier != null) {
                        SupportedClassifierDefinition supportedClassifierDefinition =
                                createSupportedClassifierDefinition(ourClassifier);
                        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                        wTx.put(LogicalDatastoreType.OPERATIONAL, RENDERER_CAPABILITIES_IID
                            .child(SupportedClassifierDefinition.class, supportedClassifierDefinition.getKey()),
                                supportedClassifierDefinition, true);
                        Futures.addCallback(wTx.submit(), logDebugResult(supportedClassifierDefinition.getKey(), PUT),
                            MoreExecutors.directExecutor());
                    }
                    break;

                case DELETE:
                    ClassifierDefinition classifierDefinitionBefore = checkNotNull(rootNode.getDataBefore());
                    ourClassifier = SubjectFeatures.getClassifier(classifierDefinitionBefore.getId());
                    if (ourClassifier != null) {
                        SupportedClassifierDefinitionKey supportedClassifierDefinitionKey =
                                new SupportedClassifierDefinitionKey(ourClassifier.getId());
                        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                        wTx.delete(LogicalDatastoreType.OPERATIONAL, RENDERER_CAPABILITIES_IID
                            .child(SupportedClassifierDefinition.class, supportedClassifierDefinitionKey));
                        Futures.addCallback(wTx.submit(), logDebugResult(supportedClassifierDefinitionKey, DELETED),
                            MoreExecutors.directExecutor());
                    }
                    break;
            }
        }
    }

    private SupportedClassifierDefinition createSupportedClassifierDefinition(Classifier classifier) {
        SupportedClassifierDefinitionBuilder scdBuilder =
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(classifier.getId())
                    .setSupportedParameterValues(classifier.getSupportedParameterValues());
        if (classifier.getParent() != null) {
            scdBuilder.setParentClassifierDefinitionId(classifier.getParent().getId());
        }
        return scdBuilder.build();
    }

    private FutureCallback<Void> logDebugResult(final SupportedClassifierDefinitionKey supportedClassifierDefinitionKey,
            final String putOrDeleted) {
        return new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Capability of renerer {} was {}: {}", OFOverlayRenderer.RENDERER_NAME.getValue(),
                        putOrDeleted, supportedClassifierDefinitionKey);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.error("Capability of renderer {} was NOT {}: {}", OFOverlayRenderer.RENDERER_NAME.getValue(),
                        putOrDeleted, supportedClassifierDefinitionKey, t);
            }
        };
    }

    @Override
    public void close() throws Exception {
        if (registration != null)
            registration.close();
    }
}
