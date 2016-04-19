/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Test for {@link SxpMapperReactorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SxpMapperReactorImplTest {

    @Mock
    private EndpointService l3EndpointService;
    @Mock
    private EndpointPolicyTemplateBySgt epPolicyTemplate;
    @Mock
    private MasterDatabaseBinding masterDBBinding;
    @Mock
    private EndpointForwardingTemplateBySubnet epForwardingTemplate;

    private SxpMapperReactorImpl sxpMapperReactor;

    @Before
    public void setUp() throws Exception {
        sxpMapperReactor = new SxpMapperReactorImpl(l3EndpointService);
        Mockito.when(l3EndpointService.registerEndpoint(Matchers.<RegisterEndpointInput>any()))
                .thenReturn(RpcResultBuilder.<Void>success().buildFuture());
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