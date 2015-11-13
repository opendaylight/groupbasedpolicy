/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class SupportedClassifierDefinitionListener
        implements DataTreeChangeListener<SupportedClassifierDefinition>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SupportedClassifierDefinitionListener.class);

    private final DataBroker dataProvider;
    private final ListenerRegistration<SupportedClassifierDefinitionListener> registration;
    @VisibleForTesting
    final SetMultimap<ClassifierDefinitionId, InstanceIdentifier<SupportedClassifierDefinition>> supportedCdIidByCdId =
            HashMultimap.create();
    @VisibleForTesting
    final Map<InstanceIdentifier<SupportedClassifierDefinition>, ClassifierInstanceValidator> ciValidatorBySupportedCdIid =
            new HashMap<>();

    public SupportedClassifierDefinitionListener(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        registration =
                dataProvider.registerDataTreeChangeListener(
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                InstanceIdentifier.builder(Renderers.class)
                                    .child(Renderer.class)
                                    .child(Capabilities.class)
                                    .child(SupportedClassifierDefinition.class)
                                    .build()),
                        this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<SupportedClassifierDefinition>> changes) {
        for (DataTreeModification<SupportedClassifierDefinition> change : changes) {
            DataObjectModification<SupportedClassifierDefinition> rootNode = change.getRootNode();
            InstanceIdentifier<SupportedClassifierDefinition> rootIdentifier = change.getRootPath().getRootIdentifier();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    ClassifierDefinitionId classifierDefinitionId = rootNode.getDataAfter().getClassifierDefinitionId();
                    if (containsParameters(rootNode.getDataAfter())) {
                        ClassifierInstanceValidator ciValidator =
                                new ClassifierInstanceValidator(rootNode.getDataAfter());
                        ciValidatorBySupportedCdIid.put(rootIdentifier, ciValidator);
                        // TODO register validator to Policy Resolver service
                    }
                    supportedCdIidByCdId.put(classifierDefinitionId, rootIdentifier);
                    putOrRemoveClassifierDefinitionInOperDs(classifierDefinitionId);
                    break;
                case DELETE:
                    classifierDefinitionId = rootNode.getDataBefore().getClassifierDefinitionId();
                    // TODO unregister validator from Policy Resolver service
                    supportedCdIidByCdId.remove(classifierDefinitionId, rootIdentifier);
                    ciValidatorBySupportedCdIid.remove(rootIdentifier);
                    putOrRemoveClassifierDefinitionInOperDs(classifierDefinitionId);
                    break;
                case SUBTREE_MODIFIED:
                    classifierDefinitionId = rootNode.getDataAfter().getClassifierDefinitionId();
                    if (containsParameters(rootNode.getDataAfter())) {
                        ClassifierInstanceValidator ciValidator =
                                new ClassifierInstanceValidator(rootNode.getDataAfter());
                        ClassifierInstanceValidator oldCiValidator =
                                ciValidatorBySupportedCdIid.put(rootIdentifier, ciValidator);
                        // TODO unregister old validator from Policy Resolver service and register
                        // new one
                    }
                    putOrRemoveClassifierDefinitionInOperDs(classifierDefinitionId);
                    break;
            }
        }
    }

    private boolean containsParameters(SupportedClassifierDefinition supportedClassifierDefinition) {
        return supportedClassifierDefinition.getSupportedParameterValues() != null
                && !supportedClassifierDefinition.getSupportedParameterValues().isEmpty();
    }

    private void putOrRemoveClassifierDefinitionInOperDs(ClassifierDefinitionId classifierDefinitionId) {
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        ClassifierDefinition cd = createClassifierDefinitionWithUnionOfParams(classifierDefinitionId, rwTx);
        if (cd != null) {
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(classifierDefinitionId), cd);
        } else {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.classifierDefinitionIid(classifierDefinitionId), rwTx);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    @VisibleForTesting
    ClassifierDefinition createClassifierDefinitionWithUnionOfParams(ClassifierDefinitionId classifierDefinitionId,
            ReadTransaction rTx) {
        Optional<ClassifierDefinition> potentialCdFromDs = DataStoreHelper.readFromDs(
                LogicalDatastoreType.CONFIGURATION, IidFactory.classifierDefinitionIid(classifierDefinitionId), rTx);
        if (!potentialCdFromDs.isPresent()) {
            LOG.error("Classifier-definition with ID {} does not exist in CONF datastore.", classifierDefinitionId);
            return null;
        }
        ClassifierDefinition cdFromDs = potentialCdFromDs.get();
        Set<InstanceIdentifier<SupportedClassifierDefinition>> supportedCdIids =
                supportedCdIidByCdId.get(classifierDefinitionId);
        if (supportedCdIids.isEmpty()) {
            LOG.debug("Classifier-definition with ID {} is not supported by any renderer.", classifierDefinitionId);
            return null;
        }
        if (cdFromDs.getParameter() == null || cdFromDs.getParameter().isEmpty()) {
            LOG.debug("Classifier-definition with ID {} does not contain any parameter", classifierDefinitionId);
            return cdFromDs;
        }
        List<Parameter> params = new ArrayList<>();
        for (InstanceIdentifier<SupportedClassifierDefinition> supportedCdIid : supportedCdIids) {
            ClassifierInstanceValidator ciValidator = ciValidatorBySupportedCdIid.get(supportedCdIid);
            Set<ParameterName> supportedParams = ciValidator.getSupportedParameters();
            for (ParameterName supportedParamName : supportedParams) {
                for (Parameter param : cdFromDs.getParameter()) {
                    if (param.getName().equals(supportedParamName)) {
                        params.add(param);
                    }
                }
            }
        }
        ClassifierDefinitionBuilder cdBuilder = new ClassifierDefinitionBuilder(cdFromDs);
        return cdBuilder.setParameter(params).build();
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
