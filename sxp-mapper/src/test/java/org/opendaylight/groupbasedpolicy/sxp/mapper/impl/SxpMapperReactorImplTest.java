/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Test for {@link SxpMapperReactorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SxpMapperReactorImplTest {

    private static final IpPrefix IP_PREFIX = new IpPrefix(new Ipv4Prefix("10.11.12.1/32"));

    @Mock
    private EndpointService l3EndpointService;
    @Mock
    private EndpointPolicyTemplateBySgt epPolicyTemplate;
    @Mock
    private MasterDatabaseBinding masterDBBinding;
    @Mock
    private EndpointForwardingTemplateBySubnet epForwardingTemplate;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadOnlyTransaction rTx;

    private SxpMapperReactorImpl sxpMapperReactor;

    @Before
    public void setUp() throws Exception {
        sxpMapperReactor = new SxpMapperReactorImpl(l3EndpointService, dataBroker);
        Mockito.when(l3EndpointService.registerEndpoint(Matchers.<RegisterEndpointInput>any()))
                .thenReturn(RpcResultBuilder.<Void>success().buildFuture());
        Mockito.when(masterDBBinding.getIpPrefix()).thenReturn(IP_PREFIX);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.same(LogicalDatastoreType.OPERATIONAL),
                Matchers.<InstanceIdentifier<EndpointL3>>any())).thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(l3EndpointService);
    }

    @Test
    public void testProcessPolicyAndSxpMasterDB() throws Exception {
        sxpMapperReactor.processPolicyAndSxpMasterDB(epPolicyTemplate, masterDBBinding);
        Mockito.verify(l3EndpointService).registerEndpoint(Matchers.<RegisterEndpointInput>any());
    }

    @Test
    public void testProcessForwardingAndSxpMasterDB() throws Exception {
        sxpMapperReactor.processForwardingAndSxpMasterDB(epForwardingTemplate, masterDBBinding);
        Mockito.verify(l3EndpointService).registerEndpoint(Matchers.<RegisterEndpointInput>any());
    }
}