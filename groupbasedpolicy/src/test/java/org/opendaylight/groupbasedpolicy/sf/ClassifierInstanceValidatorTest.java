/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ValidationResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.IntBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRangeBuilder;

public class ClassifierInstanceValidatorTest {

    private static final String RENDERER = "renderer";
    private static final String PARENT_CLASSIFIER_DEFINITION_ID = "cdId-0";
    private static final String CLASSIFIER_DEFINITION_ID = "cdId-1";
    private static final String PARAM_NAME = "param_name";
    private static final String CLASSIFIER1 = "classifier1";
    private ClassifierDefinitionId cdId;
    private ClassifierDefinitionId parentCdId;
    private List<SupportedParameterValues> supportedParameterValues;

    @Before
    public void init() {
        cdId = new ClassifierDefinitionId(CLASSIFIER_DEFINITION_ID);
        parentCdId = new ClassifierDefinitionId(PARENT_CLASSIFIER_DEFINITION_ID);
        supportedParameterValues =
                ImmutableList.of(
                        new SupportedParameterValuesBuilder().setParameterName(new ParameterName(PARAM_NAME))
                            .setParameterType(new IntBuilder()
                                .setSupportedIntValueInRange(ImmutableList
                                    .of(new SupportedIntValueInRangeBuilder().setMin(0L).setMax(1000L).build()))
                                .build())
                            .build());
    }

    @Test
    public void testConstructor() {

        SupportedClassifierDefinition constraint = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(cdId)
            .setParentClassifierDefinitionId(parentCdId)
            .setSupportedParameterValues(supportedParameterValues)
            .build();
        RendererName rendererName = new RendererName(RENDERER);

        ClassifierInstanceValidator validator = new ClassifierInstanceValidator(constraint, rendererName);

        assertEquals(CLASSIFIER_DEFINITION_ID, validator.getClassifierDefinitionId().getValue());
        assertNotNull(validator.getParentClassifierDefinitionId());
        assertEquals(PARENT_CLASSIFIER_DEFINITION_ID, validator.getParentClassifierDefinitionId().getValue());
        assertEquals(RENDERER, validator.getRendererName().getValue());
        assertEquals(supportedParameterValues.size(), validator.getSupportedParameters().size());
    }

    @Test
    public void testConstructor_noSupportedParameters() {

        SupportedClassifierDefinition constraint = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(cdId).setParentClassifierDefinitionId(parentCdId).build();
        RendererName rendererName = new RendererName(RENDERER);

        ClassifierInstanceValidator validator = new ClassifierInstanceValidator(constraint, rendererName);

        assertEquals(CLASSIFIER_DEFINITION_ID, validator.getClassifierDefinitionId().getValue());
        assertNotNull(validator.getParentClassifierDefinitionId());
        assertEquals(PARENT_CLASSIFIER_DEFINITION_ID, validator.getParentClassifierDefinitionId().getValue());
        assertEquals(RENDERER, validator.getRendererName().getValue());
        assertTrue(validator.getSupportedParameters().isEmpty());
    }

    @Test
    public void testValidate() {
        ClassifierInstanceBuilder ciBuilder = new ClassifierInstanceBuilder();
        ciBuilder.setName(new ClassifierName(CLASSIFIER1));
        ciBuilder.setParameterValue(ImmutableList
            .of(new ParameterValueBuilder().setName(new ParameterName(PARAM_NAME)).setIntValue(100L).build()));

        SupportedClassifierDefinition constraint = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(cdId)
            .setParentClassifierDefinitionId(parentCdId)
            .setSupportedParameterValues(supportedParameterValues)
            .build();
        RendererName rendererName = new RendererName(RENDERER);

        ClassifierInstanceValidator validator = new ClassifierInstanceValidator(constraint, rendererName);

        assertEquals(new ValidationResultBuilder().success().build(), validator.validate(ciBuilder.build()));
    }

}
