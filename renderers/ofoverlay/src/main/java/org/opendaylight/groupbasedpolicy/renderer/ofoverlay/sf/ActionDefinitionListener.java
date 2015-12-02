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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class ActionDefinitionListener implements DataTreeChangeListener<ActionDefinition>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ActionDefinitionListener.class);
    private static final InstanceIdentifier<Capabilities> CAPABILITIES_IID = InstanceIdentifier.builder(Renderers.class)
        .child(Renderer.class, new RendererKey(OFOverlayRenderer.RENDERER_NAME))
        .child(Capabilities.class)
        .build();
    private static String PUT = "stored";
    private static String DELETED = "removed";

    private final DataBroker dataProvider;
    private final ListenerRegistration<ActionDefinitionListener> registration;

    public ActionDefinitionListener(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        registration = dataProvider
            .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class).build()),
                    this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ActionDefinition>> changes) {
        for (DataTreeModification<ActionDefinition> change : changes) {
            DataObjectModification<ActionDefinition> rootNode = change.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    ActionDefinition actionDefinitionAfter = checkNotNull(rootNode.getDataAfter());
                    Action ourAction = SubjectFeatures.getAction(actionDefinitionAfter.getId());
                    if (ourAction != null) {
                        SupportedActionDefinition supportedActionDefinition =
                                createSupportedActionDefinition(ourAction);
                        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                        wTx.put(LogicalDatastoreType.OPERATIONAL, CAPABILITIES_IID
                            .child(SupportedActionDefinition.class, supportedActionDefinition.getKey()),
                                supportedActionDefinition, true);
                        Futures.addCallback(wTx.submit(), logDebugResult(supportedActionDefinition.getKey(), PUT));
                    }
                    break;

                case DELETE:
                    ActionDefinition actionDefinitionBefore = checkNotNull(rootNode.getDataBefore());
                    ourAction = SubjectFeatures.getAction(actionDefinitionBefore.getId());
                    if (ourAction != null) {
                        SupportedActionDefinitionKey supportedActionDefinitionKey =
                                new SupportedActionDefinitionKey(ourAction.getId());
                        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                        wTx.delete(LogicalDatastoreType.OPERATIONAL,
                                CAPABILITIES_IID.child(SupportedActionDefinition.class, supportedActionDefinitionKey));
                        Futures.addCallback(wTx.submit(), logDebugResult(supportedActionDefinitionKey, DELETED));
                    }
                    break;
            }

        }
    }

    private SupportedActionDefinition createSupportedActionDefinition(Action action) {
        return new SupportedActionDefinitionBuilder().setActionDefinitionId(action.getId())
            .setSupportedParameterValues(action.getSupportedParameterValues())
            .build();
    }

    private FutureCallback<Void> logDebugResult(final SupportedActionDefinitionKey supportedActionDefinitionKey,
            final String putOrDeleted) {
        return new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Capability of renerer {} was {}: {}", OFOverlayRenderer.RENDERER_NAME.getValue(),
                        putOrDeleted, supportedActionDefinitionKey);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Capability of renderer {} was NOT {}: {}", OFOverlayRenderer.RENDERER_NAME.getValue(),
                        putOrDeleted, supportedActionDefinitionKey, t);
            }
        };
    }

    @Override
    public void close() throws Exception {
        if (registration != null)
            registration.close();
    }
}
