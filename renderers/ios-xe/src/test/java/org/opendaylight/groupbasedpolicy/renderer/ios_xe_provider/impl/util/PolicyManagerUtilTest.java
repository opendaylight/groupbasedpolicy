/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link PolicyManagerUtil}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyManagerUtilTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadOnlyTransaction roTx;
    @Captor
    private ArgumentCaptor<InstanceIdentifier<RenderedServicePath>> rendererServicePathIICaptor;

    private ServiceFunctionPath serviceFunctionPath;

    @Before
    public void setUp() throws Exception {
        serviceFunctionPath = new ServiceFunctionPathBuilder()
                .setServiceChainName(new SfcName("sfc-chain-name-42"))
                .setName(new SfpName("sfp-name-01"))
                .build();

        new OpendaylightSfc().setDataProvider(dataBroker);
    }

    @Test
    public void testGetServicePath() throws Exception {
        final ParameterValue paramValueSfc = new ParameterValueBuilder()
                .setName(new ParameterName(ChainActionDefinition.SFC_CHAIN_NAME))
                .setStringValue("sfc-chain-name-42")
                .build();

        final ServiceFunctionPaths sfPaths = new ServiceFunctionPathsBuilder()
                .setServiceFunctionPath(Collections.singletonList(serviceFunctionPath))
                .build();

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(ServiceFunctionPaths.class)))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(sfPaths)));

        final ServiceFunctionPath servicePath = PolicyManagerUtil.getServicePath(Collections.singletonList(paramValueSfc));
        Assert.assertEquals(serviceFunctionPath, servicePath);
    }

    @Test
    public void testCreateRenderedPath() throws Exception {
        final TenantId tenantId = new TenantId("tenant-id-01");
        final RenderedServicePath renderedSP = new RenderedServicePathBuilder().build();

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(Matchers.eq(LogicalDatastoreType.OPERATIONAL), rendererServicePathIICaptor.capture()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(renderedSP)));

        final RenderedServicePath renderedPath = PolicyManagerUtil.createRenderedPath(serviceFunctionPath, tenantId);
        Assert.assertEquals(renderedSP, renderedPath);
        final InstanceIdentifier<RenderedServicePath> ii = rendererServicePathIICaptor.getValue();
        Assert.assertEquals("sfp-name-01tenant-id-01-gbp-rsp", ii.firstKeyOf(RenderedServicePath.class).getName().getValue());
    }

    @Test
    public void testCreateSymmetricRenderedPath() throws Exception {
        final RenderedServicePath renderedServicePath = new RenderedServicePathBuilder().build();
        final TenantId tenantId = new TenantId("tenant-id-02");

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(Matchers.eq(LogicalDatastoreType.OPERATIONAL), rendererServicePathIICaptor.capture()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(renderedServicePath)));


        final RenderedServicePath symmetricRenderedPath = PolicyManagerUtil.createSymmetricRenderedPath(
                serviceFunctionPath, renderedServicePath, tenantId);
        Assert.assertEquals(renderedServicePath, symmetricRenderedPath);
        final InstanceIdentifier<RenderedServicePath> ii = rendererServicePathIICaptor.getValue();
        Assert.assertEquals("sfp-name-01tenant-id-02-gbp-rsp-Reverse", ii.firstKeyOf(RenderedServicePath.class).getName().getValue());
    }
}