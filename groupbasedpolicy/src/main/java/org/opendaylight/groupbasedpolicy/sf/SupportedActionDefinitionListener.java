/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class SupportedActionDefinitionListener
        implements DataTreeChangeListener<SupportedActionDefinition>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SupportedActionDefinitionListener.class);

    private final DataBroker dataProvider;
    private final ListenerRegistration<SupportedActionDefinitionListener> registration;
    @VisibleForTesting
    final SetMultimap<ClassifierDefinitionId, InstanceIdentifier<SupportedClassifierDefinition>> supportedCdIidByCdId =
            HashMultimap.create();
    @VisibleForTesting
    final Map<InstanceIdentifier<SupportedClassifierDefinition>, ClassifierInstanceValidator> ciValidatorBySupportedCdIid =
            new HashMap<>();

    public SupportedActionDefinitionListener(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        registration =
                dataProvider.registerDataTreeChangeListener(
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                InstanceIdentifier.builder(Renderers.class)
                                    .child(Renderer.class)
                                    .child(Capabilities.class)
                                    .child(SupportedActionDefinition.class)
                                    .build()),
                        this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<SupportedActionDefinition>> changes) {
        for (DataTreeModification<SupportedActionDefinition> change : changes) {
            DataObjectModification<SupportedActionDefinition> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    ActionDefinitionId actionDefinitionId = rootNode.getDataAfter().getActionDefinitionId();
                    ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
                    Optional<ActionDefinition> potentialAdFromConfDs = DataStoreHelper.readFromDs(
                            LogicalDatastoreType.CONFIGURATION, IidFactory.actionDefinitionIid(actionDefinitionId), rwTx);
                    if (!potentialAdFromConfDs.isPresent()) {
                        LOG.error("Action-definition with ID {} does not exist in CONF datastore.", actionDefinitionId);
                        return;
                    }
                    // TODO union and validation need to be finished
                    ActionDefinition ad = potentialAdFromConfDs.get();
                    rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.actionDefinitionIid(actionDefinitionId), ad);
                    DataStoreHelper.submitToDs(rwTx);
                    break;
                case DELETE:
                    throw new UnsupportedOperationException("Not implemented yet.");
            }
        }
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
