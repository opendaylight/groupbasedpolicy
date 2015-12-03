package org.opendaylight.groupbasedpolicy.sf;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

public class SupportedClassifierDefinitionListenerTest extends GbpDataBrokerTest {

    private SupportedClassifierDefinitionListener listener;

    @Before
    public void init() {
        listener = new SupportedClassifierDefinitionListener(getDataBroker());
    }

    @Test
    public void testCreateClassifierDefinitionWithUnionOfParams_allParamsSupportedByRenderer() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID),
                EtherTypeClassifierDefinition.DEFINITION, true);
        wTx.submit().get();

        SupportedClassifierDefinition supportedClassifierDefinition = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(EtherTypeClassifierDefinition.ID)
            .setSupportedParameterValues(
                    ImmutableList.<SupportedParameterValues>of(new SupportedParameterValuesBuilder()
                        .setParameterName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM)).build()))
            .build();
        Renderer renderer = createRenderer("renderer1");
        registerSupportedClassifierDefByRenderer(supportedClassifierDefinition, renderer);

        ClassifierDefinition newCd = listener.createClassifierDefinitionWithUnionOfParams(
                EtherTypeClassifierDefinition.ID, getDataBroker().newReadOnlyTransaction());
        Assert.assertEquals(EtherTypeClassifierDefinition.DEFINITION, newCd);
    }

    @Test
    public void testCreateClassifierDefinitionWithUnionOfParams_someParamsSupportedByRenderer() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierDefinitionIid(L4ClassifierDefinition.ID),
                L4ClassifierDefinition.DEFINITION, true);
        wTx.submit().get();

        SupportedClassifierDefinition supportedClassifierDefinition = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(L4ClassifierDefinition.ID)
            .setSupportedParameterValues(
                    ImmutableList.<SupportedParameterValues>of(new SupportedParameterValuesBuilder()
                        .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).build()))
            .build();
        Renderer renderer = createRenderer("renderer1");
        registerSupportedClassifierDefByRenderer(supportedClassifierDefinition, renderer);

        ClassifierDefinition newCd = listener.createClassifierDefinitionWithUnionOfParams(L4ClassifierDefinition.ID,
                getDataBroker().newReadOnlyTransaction());
        ClassifierDefinition expectedCd = new ClassifierDefinitionBuilder(L4ClassifierDefinition.DEFINITION)
            .setParameter(ImmutableList.<Parameter>of(getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
                    L4ClassifierDefinition.DST_PORT_PARAM)))
            .build();
        Assert.assertEquals(expectedCd, newCd);
    }

    private Renderer createRenderer(String rendererName) {
        return new RendererBuilder().setName(new RendererName(rendererName)).build();
    }

    private Parameter getParameterFromDefinition(ClassifierDefinition cd, String parameter) {
        for (Parameter param : cd.getParameter()) {
            if (param.getName().getValue().equals(parameter)) {
                return param;
            }
        }
        throw new IllegalArgumentException("Parameter " + parameter + " is not located in " + cd);
    }

    private void registerSupportedClassifierDefByRenderer(SupportedClassifierDefinition supportedClassifierDefinition,
            Renderer renderer) {
        InstanceIdentifier<SupportedClassifierDefinition> scdIid = InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, renderer.getKey())
            .child(Capabilities.class)
            .child(SupportedClassifierDefinition.class, supportedClassifierDefinition.getKey())
            .build();
        listener.ciValidatorBySupportedCdIid.put(scdIid,
                new ClassifierInstanceValidator(supportedClassifierDefinition));
        listener.supportedCdIidByCdId.put(supportedClassifierDefinition.getClassifierDefinitionId(), scdIid);
    }

}
