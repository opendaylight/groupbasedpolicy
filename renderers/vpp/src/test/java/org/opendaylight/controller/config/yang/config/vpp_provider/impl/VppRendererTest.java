/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.vpp_provider.impl;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class VppRendererTest  extends VppRendererDataBrokerTest {
    private final String CLASSIFIER = "Classifier-EtherType";
    private final String ACTION_ALLOW = "Action-Allow";
    private final String SUPPORTED_PARAM_NAME = "ethertype";

    private DataBroker dataBroker;
    @Mock
    DataBroker dataBroker2;
    @Mock
    BindingAwareBroker bindingAwareBroker;
    @Mock
    MountPointService mountPointService;
    @Mock
    MountPoint mountPoint;
    @Mock
    BindingAwareBroker.ProviderContext providerContext;

    @Before
    public void init(){
        dataBroker =  getDataBroker();
        Mockito.when(providerContext.getSALService(Matchers.<Class<MountPointService>>any()))
                .thenReturn(mountPointService);
        Mockito.when(mountPointService.getMountPoint(Matchers.<InstanceIdentifier<Node>>any()))
                .thenReturn(Optional.of(mountPoint));
        Mockito.when(mountPoint.getService(Matchers.<Class<DataBroker>>any())).thenReturn(Optional.of(dataBroker2));


    }

    @Test
    public void testCreateVppRenderer() throws Exception {
        VppRenderer vppRenderer = new VppRenderer(dataBroker, bindingAwareBroker);

        vppRenderer.onSessionInitiated(providerContext);

        Optional<Renderer> rendererOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL,
                VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)),
                dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(rendererOptional.isPresent());
        Renderer renderer = rendererOptional.get();
        Assert.assertEquals(VppRenderer.NAME, renderer.getName());
        List<SupportedClassifierDefinition> definition = renderer.getCapabilities().getSupportedClassifierDefinition();
        Assert.assertEquals(1, definition.size());
        Assert.assertEquals(CLASSIFIER, definition.get(0).getClassifierDefinitionId().getValue());
        Assert.assertEquals(SUPPORTED_PARAM_NAME, definition.get(0).getSupportedParameterValues().get(0).getParameterName().getValue());

        List<SupportedActionDefinition> actionDefinition = renderer.getCapabilities().getSupportedActionDefinition();
        Assert.assertEquals(1, actionDefinition.size());
        Assert.assertEquals(ACTION_ALLOW, actionDefinition.get(0).getActionDefinitionId().getValue());
        Assert.assertTrue(actionDefinition.get(0).getSupportedParameterValues().isEmpty());

        vppRenderer.close();
        rendererOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL,
                VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)),
                dataBroker.newReadOnlyTransaction());
        Assert.assertFalse(rendererOptional.isPresent());
    }
}
