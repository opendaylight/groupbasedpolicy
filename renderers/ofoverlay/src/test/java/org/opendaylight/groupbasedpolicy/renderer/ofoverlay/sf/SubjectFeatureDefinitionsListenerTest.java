/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubjectFeatureDefinitionsListenerTest extends AbstractDataBrokerTest {

    private static final Parameter PARAM_INT =
            new ParameterBuilder().setName(new ParameterName("paramInt"))
                    .setType(Parameter.Type.Int)
                    .build();
    private static final ClassifierDefinition CD_WITH_PARAM_INT =
            new ClassifierDefinitionBuilder().setId(
                    new ClassifierDefinitionId(EtherTypeClassifierDefinition.ID.getValue()))
                    .setDescription(new Description("some description"))
                    .setName(new ClassifierName("cd_with_one_param"))
                    .setParameter(ImmutableList.of(PARAM_INT))
                    .build();

    @Test
    public void testPutEtherTypeClassifierDefinition() throws Exception {
        Renderer renderer1 = new RendererBuilder().setName(new RendererName("renderer1")).build();
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Renderers.class).child(Renderer.class, renderer1.getKey()).build(),
                renderer1);

        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.classifierDefinitionIid(CD_WITH_PARAM_INT.getId()), CD_WITH_PARAM_INT,
                true);
        wTx.submit().get();

        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        CheckedFuture<Optional<SupportedClassifierDefinition>, ReadFailedException> f =
                rTx.read(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Renderers.class)
                                .child(Renderer.class, renderer1.getKey())
                                .child(Capabilities.class)
                                .child(SupportedClassifierDefinition.class,
                                        new SupportedClassifierDefinitionKey(
                                                EtherTypeClassifierDefinition.ID))
                                .build());
        Futures.addCallback(f, new FutureCallback<Optional<SupportedClassifierDefinition>>() {

            @Override
            public void onSuccess(Optional<SupportedClassifierDefinition> result) {
                if (result.isPresent()) {
                    SupportedClassifierDefinition def = result.get();
                    Assert.assertEquals(CD_WITH_PARAM_INT.getId(), def.getClassifierDefinitionId());
                    Assert.assertEquals(SubjectFeatures.getClassifier(CD_WITH_PARAM_INT.getId())
                            .getSupportedParameterValues(), def.getSupportedParameterValues());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                throw new RuntimeException(t);
            }
        });
        f.checkedGet();
    }

}
