/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class SupportedClassifierDefinitionListener extends DataTreeChangeHandler<SupportedClassifierDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(SupportedClassifierDefinitionListener.class);

    @VisibleForTesting
    final Table<RendererName, ClassifierDefinitionId, ClassifierInstanceValidator> validatorByRendererAndCd =
            HashBasedTable.create();
    private final PolicyValidatorRegistry validatorRegistry;

    public SupportedClassifierDefinitionListener(DataBroker dataProvider, PolicyValidatorRegistry validatorRegistry) {
        super(dataProvider);
        this.validatorRegistry = validatorRegistry;
        if (validatorRegistry == null) {
            LOG.info(
                    "{} service was NOT found. Automatic registration of simple classifier-instance validators is NOT available for renderers.",
                    PolicyValidatorRegistry.class.getCanonicalName());
        } else {
            LOG.info(
                    "{} service was found. Automatic registration of simple classifier-instance validators is available for renderers.",
                    PolicyValidatorRegistry.class.getCanonicalName());
        }
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                IidFactory.supportedClassifierDefinitionIidWildcard()));
    }

    @Override
    protected void onWrite(DataObjectModification<SupportedClassifierDefinition> rootNode,
            InstanceIdentifier<SupportedClassifierDefinition> createdSupportedCdIid) {
        SupportedClassifierDefinition createdSupportedCd = rootNode.getDataAfter();
        RendererName rendererName = createdSupportedCdIid.firstKeyOf(Renderer.class).getName();
        ClassifierInstanceValidator ciValidator = new ClassifierInstanceValidator(createdSupportedCd, rendererName);
        setParentValidators(ciValidator, false);
        ClassifierDefinitionId cdId = createdSupportedCd.getClassifierDefinitionId();
        validatorByRendererAndCd.put(rendererName, cdId, ciValidator);
        if (validatorRegistry != null) {
            validatorRegistry.register(cdId, ciValidator);
        }
        putOrRemoveClassifierDefinitionInOperDs(cdId, getAllSupportedParams(cdId));
    }

    @Override
    protected void onDelete(DataObjectModification<SupportedClassifierDefinition> rootNode,
            InstanceIdentifier<SupportedClassifierDefinition> removedSupportedCdIid) {
        SupportedClassifierDefinition removedSupportedCd = rootNode.getDataBefore();
        ClassifierDefinitionId cdId = removedSupportedCd.getClassifierDefinitionId();
        RendererName rendererName = removedSupportedCdIid.firstKeyOf(Renderer.class).getName();
        ClassifierInstanceValidator removedCiValidator = validatorByRendererAndCd.remove(rendererName, cdId);
        if (validatorRegistry != null) {
            validatorRegistry.unregister(cdId, removedCiValidator);
        }
        setParentValidators(removedCiValidator, true);
        putOrRemoveClassifierDefinitionInOperDs(cdId, getAllSupportedParams(cdId));
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<SupportedClassifierDefinition> rootNode,
            InstanceIdentifier<SupportedClassifierDefinition> modifiedSupportedCdIid) {
        SupportedClassifierDefinition beforeSupportedCd = rootNode.getDataBefore();
        ClassifierDefinitionId cdId = beforeSupportedCd.getClassifierDefinitionId();
        RendererName rendererName = modifiedSupportedCdIid.firstKeyOf(Renderer.class).getName();
        ClassifierInstanceValidator oldCiValidator = validatorByRendererAndCd.remove(rendererName, cdId);
        if (validatorRegistry != null) {
            validatorRegistry.unregister(cdId, oldCiValidator);
        }
        SupportedClassifierDefinition afterSupportedCd = rootNode.getDataAfter();
        ClassifierInstanceValidator newCiValidator = new ClassifierInstanceValidator(afterSupportedCd, rendererName);
        setParentValidators(newCiValidator, false);
        validatorByRendererAndCd.put(rendererName, cdId, newCiValidator);
        if (validatorRegistry != null) {
            validatorRegistry.register(cdId, newCiValidator);
        }
        putOrRemoveClassifierDefinitionInOperDs(cdId, getAllSupportedParams(cdId));
    }

    @VisibleForTesting
    void setParentValidators(ClassifierInstanceValidator ciValidator, boolean setParentToNull) {
        if (ciValidator.getParentClassifierDefinitionId() != null && !setParentToNull) {
            ClassifierInstanceValidator parentCiValidator = validatorByRendererAndCd.get(ciValidator.getRendererName(),
                    ciValidator.getParentClassifierDefinitionId());
            if (parentCiValidator != null) {
                ciValidator.setParentValidator(parentCiValidator);
            }
        }
        for (ClassifierInstanceValidator existingCiValidator : getValidatorsWithParentCdForRenderer(
                ciValidator.getClassifierDefinitionId(), ciValidator.getRendererName())) {
            if (setParentToNull) {
                existingCiValidator.setParentValidator(null);
            } else {
                existingCiValidator.setParentValidator(ciValidator);
            }
        }
    }

    @VisibleForTesting
    Collection<ClassifierInstanceValidator> getValidatorsWithParentCdForRenderer(
            final ClassifierDefinitionId parentCdId, RendererName renderer) {
        return Collections2.filter(validatorByRendererAndCd.row(renderer).values(),
                new Predicate<ClassifierInstanceValidator>() {

                    @Override
                    public boolean apply(ClassifierInstanceValidator ciValidator) {
                        if (parentCdId.equals(ciValidator.getParentClassifierDefinitionId())) {
                            return true;
                        }
                        return false;
                    }
                });
    }

    @VisibleForTesting
    List<ParameterName> getAllSupportedParams(final ClassifierDefinitionId cdId) {
        return FluentIterable.from(validatorByRendererAndCd.column(cdId).values())
            .transformAndConcat(new Function<ClassifierInstanceValidator, Set<ParameterName>>() {

                @Override
                public Set<ParameterName> apply(ClassifierInstanceValidator input) {
                    return input.getSupportedParameters();
                }
            })
            .toList();
    }

    @VisibleForTesting
    void putOrRemoveClassifierDefinitionInOperDs(ClassifierDefinitionId cdId, List<ParameterName> supportedParams) {
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        Optional<ClassifierDefinition> potentialCdFromConfDs = DataStoreHelper
            .readFromDs(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierDefinitionIid(cdId), rwTx);
        if (!potentialCdFromConfDs.isPresent()) {
            LOG.error("Classifier-definition with ID {} does not exist in CONF datastore.", cdId);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(cdId),
                    rwTx);
            DataStoreHelper.submitToDs(rwTx);
            return;
        }
        ClassifierDefinition cd =
                createClassifierDefinitionWithUnionOfParams(potentialCdFromConfDs.get(), supportedParams);
        if (cd != null) {
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(cdId), cd);
        } else {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(cdId),
                    rwTx);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    @VisibleForTesting
    static ClassifierDefinition createClassifierDefinitionWithUnionOfParams(ClassifierDefinition cd,
            List<ParameterName> supportedParams) {
        if (supportedParams == null || supportedParams.isEmpty()) {
            LOG.debug("Classifier-definition with ID {} is not supported by any renderer.", cd.getId().getValue());
            return null;
        }
        if (cd.getParameter() == null || cd.getParameter().isEmpty()) {
            LOG.trace("Classifier-definition with ID {} does not contain any parameter in CONF datastore.",
                    cd.getId().getValue());
            return cd;
        }
        List<Parameter> params = new ArrayList<>();
        for (ParameterName supportedParam : supportedParams) {
            for (Parameter param : cd.getParameter()) {
                if (param.getName().equals(supportedParam)) {
                    params.add(param);
                }
            }
        }
        ClassifierDefinitionBuilder cdBuilder = new ClassifierDefinitionBuilder(cd);
        return cdBuilder.setParameter(params).build();
    }

}
