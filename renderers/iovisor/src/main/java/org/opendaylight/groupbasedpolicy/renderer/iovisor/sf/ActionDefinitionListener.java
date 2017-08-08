/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.IovisorRenderer;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

public class ActionDefinitionListener extends DataTreeChangeHandler<ActionDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(ActionDefinitionListener.class);
    private static final InstanceIdentifier<Capabilities> CAPABILITIES_IID = InstanceIdentifier.builder(Renderers.class)
        .child(Renderer.class, new RendererKey(IovisorRenderer.RENDERER_NAME))
        .child(Capabilities.class)
        .build();
    private static final String PUT = "stored";
    private static final String DELETED = "removed";

    public ActionDefinitionListener(DataBroker dataBroker) {
        super(dataBroker);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class).build()));
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
                LOG.debug("Capability of renerer {} was {}: {}", IovisorRenderer.RENDERER_NAME.getValue(), putOrDeleted,
                        supportedActionDefinitionKey.getActionDefinitionId().getValue());
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.error("Capability of renderer {} was NOT {}: {}", IovisorRenderer.RENDERER_NAME.getValue(),
                        putOrDeleted, supportedActionDefinitionKey.getActionDefinitionId().getValue(), t);
            }
        };
    }

    @Override
    protected void onWrite(DataObjectModification<ActionDefinition> rootNode,
            InstanceIdentifier<ActionDefinition> rootIdentifier) {
        onSubtreeModified(rootNode, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<ActionDefinition> rootNode,
            InstanceIdentifier<ActionDefinition> rootIdentifier) {
        ActionDefinition actionDefinitionBefore = checkNotNull(rootNode.getDataBefore());
        Action ourAction = SubjectFeatures.getAction(actionDefinitionBefore.getId());
        if (ourAction != null) {
            SupportedActionDefinitionKey supportedActionDefinitionKey =
                    new SupportedActionDefinitionKey(ourAction.getId());
            WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
            wTx.delete(LogicalDatastoreType.OPERATIONAL,
                    CAPABILITIES_IID.child(SupportedActionDefinition.class, supportedActionDefinitionKey));
            Futures.addCallback(wTx.submit(), logDebugResult(supportedActionDefinitionKey, DELETED), MoreExecutors.directExecutor());
        }
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<ActionDefinition> rootNode,
            InstanceIdentifier<ActionDefinition> rootIdentifier) {
        ActionDefinition actionDefinitionAfter = checkNotNull(rootNode.getDataAfter());
        Action ourAction = SubjectFeatures.getAction(actionDefinitionAfter.getId());
        if (ourAction != null) {
            SupportedActionDefinition supportedActionDefinition = createSupportedActionDefinition(ourAction);
            WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.OPERATIONAL,
                    CAPABILITIES_IID.child(SupportedActionDefinition.class, supportedActionDefinition.getKey()),
                    supportedActionDefinition, true);
            Futures.addCallback(wTx.submit(), logDebugResult(supportedActionDefinition.getKey(), PUT), MoreExecutors.directExecutor());
        }
    }
}
