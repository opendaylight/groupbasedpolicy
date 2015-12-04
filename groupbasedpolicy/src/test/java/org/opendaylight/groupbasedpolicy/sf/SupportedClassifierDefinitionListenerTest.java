package org.opendaylight.groupbasedpolicy.sf;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class SupportedClassifierDefinitionListenerTest extends GbpDataBrokerTest {

    private SupportedClassifierDefinitionListener listener;
    private PolicyValidatorRegistry policyValidatorRegistry;

    @Before
    public void init() {
        policyValidatorRegistry = Mockito.mock(PolicyValidatorRegistry.class);
        listener = new SupportedClassifierDefinitionListener(getDataBroker(), policyValidatorRegistry);
    }

    @Test
    public void testSetParentValidators_newValidatorHasParent_parentValidatorExists() {
        RendererName rendererFoo = new RendererName("Foo");
        SupportedClassifierDefinition supportedIpProtoPort = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(IpProtoClassifierDefinition.ID).build();
        ClassifierInstanceValidator rendererFooIpProtoParentValidator =
                new ClassifierInstanceValidator(supportedIpProtoPort, rendererFoo);
        listener.validatorByRendererAndCd.put(rendererFoo, IpProtoClassifierDefinition.ID,
                rendererFooIpProtoParentValidator);
        SupportedClassifierDefinition supportedL4SrcPort =
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(L4ClassifierDefinition.ID)
                    .setParentClassifierDefinitionId(IpProtoClassifierDefinition.ID)
                    .build();
        ClassifierInstanceValidator rendererFooL4SrcPortNewValidator =
                new ClassifierInstanceValidator(supportedL4SrcPort, rendererFoo);
        listener.validatorByRendererAndCd.put(rendererFoo, L4ClassifierDefinition.ID, rendererFooL4SrcPortNewValidator);
        listener.setParentValidators(rendererFooL4SrcPortNewValidator, false);
        Assert.assertEquals(rendererFooIpProtoParentValidator, rendererFooL4SrcPortNewValidator.getParentValidator());

        listener.setParentValidators(rendererFooL4SrcPortNewValidator, true);
        Assert.assertNull(rendererFooIpProtoParentValidator.getParentValidator());
    }

    @Test
    public void testSetParentValidators_newValidatorHasParent_parentValidatorNotExists() {
        RendererName rendererFoo = new RendererName("Foo");
        SupportedClassifierDefinition supportedL4SrcPort =
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(L4ClassifierDefinition.ID)
                    .setParentClassifierDefinitionId(IpProtoClassifierDefinition.ID)
                    .build();
        ClassifierInstanceValidator rendererFooL4SrcPortNewValidator =
                new ClassifierInstanceValidator(supportedL4SrcPort, rendererFoo);
        listener.validatorByRendererAndCd.put(rendererFoo, L4ClassifierDefinition.ID, rendererFooL4SrcPortNewValidator);

        listener.setParentValidators(rendererFooL4SrcPortNewValidator, false);
        Assert.assertNull(rendererFooL4SrcPortNewValidator.getParentValidator());

        SupportedClassifierDefinition supportedIpProtoPort = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(IpProtoClassifierDefinition.ID).build();
        ClassifierInstanceValidator rendererFooIpProtoParentValidator =
                new ClassifierInstanceValidator(supportedIpProtoPort, rendererFoo);
        listener.validatorByRendererAndCd.put(rendererFoo, IpProtoClassifierDefinition.ID,
                rendererFooIpProtoParentValidator);

        listener.setParentValidators(rendererFooL4SrcPortNewValidator, false);
        Assert.assertEquals(rendererFooIpProtoParentValidator, rendererFooL4SrcPortNewValidator.getParentValidator());

        listener.setParentValidators(rendererFooL4SrcPortNewValidator, true);
        Assert.assertNull(rendererFooIpProtoParentValidator.getParentValidator());
    }

    @Test
    public void testGetValidatorsWithParentCdForRenderer() {
        List<SupportedParameterValues> srcPortParam = ImmutableList.of(new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM)).build());
        SupportedClassifierDefinition supportedL4SrcPort =
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(L4ClassifierDefinition.ID)
                    .setParentClassifierDefinitionId(IpProtoClassifierDefinition.ID)
                    .setSupportedParameterValues(srcPortParam)
                    .build();
        RendererName rendererFoo = new RendererName("Foo");
        ClassifierInstanceValidator rendererFooL4SrcPortValidator =
                new ClassifierInstanceValidator(supportedL4SrcPort, rendererFoo);
        listener.validatorByRendererAndCd.put(rendererFoo, supportedL4SrcPort.getClassifierDefinitionId(),
                rendererFooL4SrcPortValidator);
        List<SupportedParameterValues> dstPortParam = ImmutableList.of(new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).build());
        SupportedClassifierDefinition supportedL4DstPort =
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(L4ClassifierDefinition.ID)
                    .setParentClassifierDefinitionId(IpProtoClassifierDefinition.ID)
                    .setSupportedParameterValues(dstPortParam)
                    .build();
        RendererName rendererBar = new RendererName("Bar");
        ClassifierInstanceValidator rendererBarL4DstPortValidator =
                new ClassifierInstanceValidator(supportedL4DstPort, rendererBar);
        listener.validatorByRendererAndCd.put(rendererBar, supportedL4DstPort.getClassifierDefinitionId(),
                rendererBarL4DstPortValidator);

        Collection<ClassifierInstanceValidator> validatorsWithParentIpProtoCdForRendererFoo =
                listener.getValidatorsWithParentCdForRenderer(IpProtoClassifierDefinition.ID, rendererFoo);
        Assert.assertNotNull(validatorsWithParentIpProtoCdForRendererFoo);
        Assert.assertEquals(validatorsWithParentIpProtoCdForRendererFoo.size(), 1);
        Assert.assertTrue(validatorsWithParentIpProtoCdForRendererFoo.contains(rendererFooL4SrcPortValidator));
    }

    @Test
    public void testGetAllSupportedParams() {
        List<SupportedParameterValues> srcPortParam = ImmutableList.of(new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM)).build());
        SupportedClassifierDefinition supportedL4SrcPort = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(L4ClassifierDefinition.ID).setSupportedParameterValues(srcPortParam).build();
        RendererName rendererFoo = new RendererName("Foo");
        listener.validatorByRendererAndCd.put(rendererFoo, supportedL4SrcPort.getClassifierDefinitionId(),
                new ClassifierInstanceValidator(supportedL4SrcPort, rendererFoo));
        List<SupportedParameterValues> dstPortParam = ImmutableList.of(new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).build());
        SupportedClassifierDefinition supportedL4DstPort = new SupportedClassifierDefinitionBuilder()
            .setClassifierDefinitionId(L4ClassifierDefinition.ID).setSupportedParameterValues(dstPortParam).build();
        RendererName rendererBar = new RendererName("Bar");
        listener.validatorByRendererAndCd.put(rendererBar, supportedL4DstPort.getClassifierDefinitionId(),
                new ClassifierInstanceValidator(supportedL4DstPort, rendererBar));

        List<ParameterName> allSupportedParams = listener.getAllSupportedParams(L4ClassifierDefinition.ID);
        Assert.assertTrue(allSupportedParams.contains(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM)));
        Assert.assertTrue(allSupportedParams.contains(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)));
    }

    @Test
    public void testCreateClassifierDefinitionWithUnionOfParams_allSupportedParams() {
        List<ParameterName> supportedParams =
                ImmutableList.of(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));
        ClassifierDefinition classifierDefinitionWithUnionOfParams = listener
            .createClassifierDefinitionWithUnionOfParams(EtherTypeClassifierDefinition.DEFINITION, supportedParams);
        assertEquals(EtherTypeClassifierDefinition.DEFINITION, classifierDefinitionWithUnionOfParams);
    }

    @Test
    public void testCreateClassifierDefinitionWithUnionOfParams_emptySupportedParams() {
        List<ParameterName> supportedParams = ImmutableList.of();
        ClassifierDefinition classifierDefinitionWithUnionOfParams = listener
            .createClassifierDefinitionWithUnionOfParams(EtherTypeClassifierDefinition.DEFINITION, supportedParams);
        Assert.assertNull(classifierDefinitionWithUnionOfParams);
    }

    @Test
    public void testCreateClassifierDefinitionWithUnionOfParams_someSupportedParams() {
        ImmutableList<ParameterName> supportedParams =
                ImmutableList.of(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM),
                        new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM));
        ClassifierDefinition classifierDefinitionWithUnionOfParams = listener
            .createClassifierDefinitionWithUnionOfParams(L4ClassifierDefinition.DEFINITION, supportedParams);
        ClassifierDefinition expectedCd = new ClassifierDefinitionBuilder(L4ClassifierDefinition.DEFINITION)
            .setParameter(ImmutableList.<Parameter>of(
                    getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
                            L4ClassifierDefinition.DST_PORT_PARAM),
                    getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
                            L4ClassifierDefinition.DST_PORT_RANGE_PARAM)))
            .build();
        assertEquals(expectedCd, classifierDefinitionWithUnionOfParams);
    }

    @Test
    public void testPutOrRemoveClassifierDefinitionInOperDs_cdIsInConfDs_withSupportedParams() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID),
                EtherTypeClassifierDefinition.DEFINITION, true);
        wTx.submit().get();
        List<ParameterName> supportedParams =
                ImmutableList.of(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));

        listener.putOrRemoveClassifierDefinitionInOperDs(EtherTypeClassifierDefinition.ID, supportedParams);
        Optional<ClassifierDefinition> potentialCd = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID))
            .get();
        Assert.assertTrue(potentialCd.isPresent());
        assertEquals(EtherTypeClassifierDefinition.DEFINITION, potentialCd.get());
    }

    @Test
    public void testPutOrRemoveClassifierDefinitionInOperDs_cdIsNotInConfDs_withSupportedParams() throws Exception {
        List<ParameterName> supportedParams =
                ImmutableList.of(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));

        listener.putOrRemoveClassifierDefinitionInOperDs(EtherTypeClassifierDefinition.ID, supportedParams);
        Optional<ClassifierDefinition> potentialCd = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID))
            .get();
        Assert.assertFalse(potentialCd.isPresent());
    }

    @Test
    public void testPutOrRemoveClassifierDefinitionInOperDs_cdIsInConfDs_emptySupportedParams() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID),
                EtherTypeClassifierDefinition.DEFINITION, true);
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID),
                EtherTypeClassifierDefinition.DEFINITION, true);
        wTx.submit().get();
        List<ParameterName> supportedParams = ImmutableList.of();

        listener.putOrRemoveClassifierDefinitionInOperDs(EtherTypeClassifierDefinition.ID, supportedParams);
        Optional<ClassifierDefinition> potentialCd = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID))
            .get();
        Assert.assertFalse(potentialCd.isPresent());
    }

    @Test
    public void testPutOrRemoveClassifierDefinitionInOperDs_cdIsInConfDs_changedSupportedParams() throws Exception {
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierDefinitionIid(L4ClassifierDefinition.ID),
                L4ClassifierDefinition.DEFINITION, true);
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(L4ClassifierDefinition.ID),
                L4ClassifierDefinition.DEFINITION, true);
        wTx.submit().get();
        ImmutableList<ParameterName> supportedParams =
                ImmutableList.of(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM),
                        new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM));

        listener.putOrRemoveClassifierDefinitionInOperDs(L4ClassifierDefinition.ID, supportedParams);
        Optional<ClassifierDefinition> potentialCd = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL, IidFactory.classifierDefinitionIid(L4ClassifierDefinition.ID))
            .get();
        ClassifierDefinition expectedCd = new ClassifierDefinitionBuilder(L4ClassifierDefinition.DEFINITION)
            .setParameter(ImmutableList.<Parameter>of(
                    getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
                            L4ClassifierDefinition.DST_PORT_PARAM),
                    getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
                            L4ClassifierDefinition.DST_PORT_RANGE_PARAM)))
            .build();
        Assert.assertTrue(potentialCd.isPresent());
        assertEquals(expectedCd, potentialCd.get());
    }

    private void assertEquals(ClassifierDefinition expectedCd, ClassifierDefinition actualCd) {
        Assert.assertEquals(expectedCd.getId(), actualCd.getId());
        Assert.assertEquals(expectedCd.getName(), actualCd.getName());
        Assert.assertEquals(expectedCd.getDescription(), actualCd.getDescription());
        Assert.assertEquals(expectedCd.getFallbackBehavior(), actualCd.getFallbackBehavior());
        List<Parameter> expectedParams = getNonNullList(expectedCd.getParameter());
        List<Parameter> actualParams = getNonNullList(actualCd.getParameter());
        Assert.assertTrue(expectedParams.containsAll(actualParams) && actualParams.containsAll(expectedParams));
    }

    private <T> List<T> getNonNullList(List<T> list) {
        return MoreObjects.firstNonNull(list, ImmutableList.<T>of());
    }

    // @Test
    // public void testCreateClassifierDefinitionWithUnionOfParams_allParamsSupportedByRenderer()
    // throws Exception {
    // WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
    // wTx.put(LogicalDatastoreType.CONFIGURATION,
    // IidFactory.classifierDefinitionIid(EtherTypeClassifierDefinition.ID),
    // EtherTypeClassifierDefinition.DEFINITION, true);
    // wTx.submit().get();
    //
    // SupportedClassifierDefinition supportedClassifierDefinition = new
    // SupportedClassifierDefinitionBuilder()
    // .setClassifierDefinitionId(EtherTypeClassifierDefinition.ID)
    // .setSupportedParameterValues(
    // ImmutableList.<SupportedParameterValues>of(new SupportedParameterValuesBuilder()
    // .setParameterName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM)).build()))
    // .build();
    // Renderer renderer = createRenderer("renderer1");
    // registerSupportedClassifierDefByRenderer(supportedClassifierDefinition, renderer);
    //
    // ClassifierDefinition newCd = listener.createClassifierDefinitionWithUnionOfParams(
    // EtherTypeClassifierDefinition.ID, getDataBroker().newReadOnlyTransaction());
    // Assert.assertEquals(EtherTypeClassifierDefinition.DEFINITION, newCd);
    // }

    // @Test
    // public void testCreateClassifierDefinitionWithUnionOfParams_someParamsSupportedByRenderer()
    // throws Exception {
    // WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
    // wTx.put(LogicalDatastoreType.CONFIGURATION,
    // IidFactory.classifierDefinitionIid(L4ClassifierDefinition.ID),
    // L4ClassifierDefinition.DEFINITION, true);
    // wTx.submit().get();
    //
    // SupportedClassifierDefinition supportedClassifierDefinition = new
    // SupportedClassifierDefinitionBuilder()
    // .setClassifierDefinitionId(L4ClassifierDefinition.ID)
    // .setSupportedParameterValues(
    // ImmutableList.<SupportedParameterValues>of(new SupportedParameterValuesBuilder()
    // .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM)).build()))
    // .build();
    // Renderer renderer = createRenderer("renderer1");
    // registerSupportedClassifierDefByRenderer(supportedClassifierDefinition, renderer);
    //
    // ClassifierDefinition newCd =
    // listener.createClassifierDefinitionWithUnionOfParams(L4ClassifierDefinition.ID,
    // getDataBroker().newReadOnlyTransaction());
    // ClassifierDefinition expectedCd = new
    // ClassifierDefinitionBuilder(L4ClassifierDefinition.DEFINITION)
    // .setParameter(ImmutableList.<Parameter>of(getParameterFromDefinition(L4ClassifierDefinition.DEFINITION,
    // L4ClassifierDefinition.DST_PORT_PARAM)))
    // .build();
    // Assert.assertEquals(expectedCd, newCd);
    // }
    //
    // private Renderer createRenderer(String rendererName) {
    // return new RendererBuilder().setName(new RendererName(rendererName)).build();
    // }
    //
    private Parameter getParameterFromDefinition(ClassifierDefinition cd, String parameter) {
        for (Parameter param : cd.getParameter()) {
            if (param.getName().getValue().equals(parameter)) {
                return param;
            }
        }
        throw new IllegalArgumentException("Parameter " + parameter + " is not located in " + cd);
    }
    //
    // private void registerSupportedClassifierDefByRenderer(SupportedClassifierDefinition
    // supportedClassifierDefinition,
    // Renderer renderer) {
    // InstanceIdentifier<SupportedClassifierDefinition> scdIid =
    // InstanceIdentifier.builder(Renderers.class)
    // .child(Renderer.class, renderer.getKey())
    // .child(Capabilities.class)
    // .child(SupportedClassifierDefinition.class, supportedClassifierDefinition.getKey())
    // .build();
    // listener.ciValidatorBySupportedCdKey.put(new SupportedCdKey(scdIid, null),
    // new ClassifierInstanceValidator(supportedClassifierDefinition));
    // listener.supportedCdIidByCdId.put(supportedClassifierDefinition.getClassifierDefinitionId(),
    // scdIid);
    // }

}
