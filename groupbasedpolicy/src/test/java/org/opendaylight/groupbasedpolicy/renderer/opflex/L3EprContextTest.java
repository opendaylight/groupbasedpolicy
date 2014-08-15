/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class L3EprContextTest implements L3EprContext.Callback {
    protected static final Logger logger = LoggerFactory.getLogger(L3EprContextTest.class);

    private static final int TEST_SIZE = 1;
    L3EprContext ctx = null;
    private int callbacks;
    @Mock
    private DataBroker mockProvider;
    private ScheduledExecutorService executor;
    @Mock
    private JsonRpcEndpoint mockPeer;
    @Mock
    private WriteTransaction mockWriter;
    @Mock
    private ReadOnlyTransaction mockReader;
    @Mock
    private EndpointDeclarationRequest mockDeclRequest;
    @Mock
    private EndpointRequestRequest mockReqRequest;
    @Mock
    private Identity mockId;
    @Mock
    private L3ContextId mockL3Context;
    @Mock
    private IpAddress mockIp;
    @Mock
    private List<L3Address> mockAddresses;
    @Mock
    private CheckedFuture<Optional<Endpoint>,ReadFailedException> mockFuture;
    @Mock
    private Optional<EndpointL3> mockOption;
    @Mock
    private EndpointL3 mockEp;
    @Mock
    private List<EndpointDeclarationRequest.Params> mockDeclParamsList;
    @Mock
    private EndpointDeclarationRequest.Params mockDeclParams;
    @Mock
    private List<EndpointRequestRequest.Params> mockReqParamsList;
    @Mock
    private EndpointRequestRequest.Params mockReqParams;
    @Mock
    private List<String> mockIdentityList;
    private List<String> dummyList;

    private static final String TEST_CONTEXT = "foo";
    private static final String TEST_ID = "bar";
    private static final String TEST_IP1 = "192.168.194.131";
    private static final String TEST_IP2 = "192.168.194.132";
    private static final String TEST_IP3 = "192.168.194.133";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Override
    public void callback(EprContext ctx) {
        callbacks++;
    }


    @Test
    public void testEpCreate() throws Exception {
        ctx =  new L3EprContext(mockPeer, mockId, mockDeclRequest,
                TEST_SIZE,
                mockProvider, executor);
        ctx.setCallback(this);
        dummyList = new ArrayList<String>();
        dummyList.add(TEST_IP1);
        dummyList.add(TEST_IP2);
        dummyList.add(TEST_IP3);
        when(mockId.getL3Context()).thenReturn(mockL3Context);
        when(mockId.getL3Identity()).thenReturn(mockIp);
        when(mockId.getL3Addresses()).thenReturn(mockAddresses);
        when(mockDeclRequest.valid()).thenReturn(true);
        when(mockDeclRequest.getParams()).thenReturn(mockDeclParamsList);
        when(mockDeclParamsList.get(0)).thenReturn(mockDeclParams);
        when(mockDeclParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockDeclParams.getIdentifier()).thenReturn(dummyList);
        when(mockProvider.newWriteOnlyTransaction()).thenReturn(mockWriter);

        ctx.createEp();
        verify(mockProvider).newWriteOnlyTransaction();
        verify(mockWriter).submit();
    }


    @Test
    public void testEpDelete() throws Exception {
        ctx =  new L3EprContext(mockPeer, mockId, mockDeclRequest,
                TEST_SIZE,
                mockProvider, executor);
        ctx.setCallback(this);
        dummyList = new ArrayList<String>();
        dummyList.add(TEST_IP1);
        dummyList.add(TEST_IP2);
        dummyList.add(TEST_IP3);
        when(mockId.getL3Context()).thenReturn(mockL3Context);
        when(mockId.getL3Identity()).thenReturn(mockIp);
        when(mockId.getL3Addresses()).thenReturn(mockAddresses);
        when(mockDeclRequest.valid()).thenReturn(true);
        when(mockDeclRequest.getParams()).thenReturn(mockDeclParamsList);
        when(mockDeclParamsList.get(0)).thenReturn(mockDeclParams);
        when(mockDeclParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockDeclParams.getIdentifier()).thenReturn(dummyList);
        when(mockProvider.newWriteOnlyTransaction()).thenReturn(mockWriter);

        ctx.deleteEp();
        verify(mockProvider).newWriteOnlyTransaction();
        verify(mockWriter).submit();
    }

    @Test
    public void testLookupEndpoint() throws Exception {
        ctx =  new L3EprContext(mockPeer, mockId, mockReqRequest,
                TEST_SIZE,
                mockProvider, executor);
        ctx.setCallback(this);
        when(mockId.getL3Context()).thenReturn(mockL3Context);
        when(mockId.getL3Identity()).thenReturn(mockIp);
        when(mockId.getL3Addresses()).thenReturn(mockAddresses);
        when(mockReqRequest.valid()).thenReturn(true);
        when(mockReqRequest.getParams()).thenReturn(mockReqParamsList);
        when(mockReqParamsList.get(0)).thenReturn(mockReqParams);
        when(mockReqParams.getIdentifier()).thenReturn(mockIdentityList);
        when(mockIdentityList.get(0)).thenReturn(TEST_ID);
        when(mockReqParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockProvider.newReadOnlyTransaction()).thenReturn(mockReader);
        when(mockReader.read(eq(LogicalDatastoreType.OPERATIONAL),
                Matchers.<InstanceIdentifier<Endpoint>>any())).thenReturn(mockFuture);
        ctx.lookupEndpoint();
        verify(mockProvider).newReadOnlyTransaction();
    }

    @Test
    public void testCallback() throws Exception {
        Identity id =
                new Identity(TEST_ID);
        ctx =  new L3EprContext(mockPeer, id, mockDeclRequest,
                TEST_SIZE,
                mockProvider, executor);
        callbacks = 0;
        ctx.setCallback(this);
        when(mockOption.get()).thenReturn(mockEp);
        ctx.onSuccess(mockOption);
        assertTrue(callbacks == 1);
    }

}
