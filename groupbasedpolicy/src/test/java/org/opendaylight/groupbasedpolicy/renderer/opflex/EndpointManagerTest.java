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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class EndpointManagerTest implements DataChangeListener {
    protected static final Logger logger = LoggerFactory.getLogger(EndpointManagerTest.class);
    private static final String TEST_MSG_ID = "7b77f5f7-b078-4276-b220-6fed8596c6aa";
    private static final String TEST_CONTEXT = "604f7808-21d3-424c-a2f0-934ac34780c0";
    private static final String TEST_LOCATION = "area54";
    private static final String TEST_POLICY =
            "/policy/tenants/tenant/95ef18d6-0ae4-4157-803e-03a38b58ccd8/" +
            "endpoint-group/d9c5625e-a489-42b4-bbe7-a1ca9502b5ef";
    private static final int TEST_PRR = 1000;
    private static final String TEST_IDENTIFIER = "00:11:22:33:44:55";
    private static final String TEST_AGENT_ID = "192.168.194.132:6742";

    @Mock
    private DataBroker mockBroker;
    @Mock
    private RpcProviderRegistry mockRpcRegistry;
    @Mock
    private ScheduledExecutorService mockExecutor;
    @Mock
    private OpflexConnectionService mockConnService;
    @Mock
    private ListenerRegistration<DataChangeListener> mockListener;
    @Mock
    private ListenerRegistration<DataChangeListener> mockL3Listener;
    @Mock
    private JsonRpcEndpoint mockAgent;
    @Mock
    private EndpointDeclarationRequest mockRpcMessage;
    @Mock
    private BindingAwareBroker.RpcRegistration<EndpointService> mockRpcRegistration;
    @Mock
    private WriteTransaction mockWriteTransaction;
    @Mock
    private ReadOnlyTransaction mockReadTransaction;
    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockWriteFuture;
    @Mock
    private CheckedFuture<Optional<Endpoint>,ReadFailedException> mockReadFuture;
    @Mock
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> mockChange;
    @Mock
    private Map<InstanceIdentifier<?>, DataObject> mockDaoMap;
    @Mock
    private Set<InstanceIdentifier<?>> mockDaoSet;
    @Mock
    private DataObject mockDao;
    @Mock
    private InstanceIdentifier<?> mockIid;

    private EndpointManager epManager;


    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mockBroker.registerDataChangeListener(Matchers.<LogicalDatastoreType>any(),
                Matchers.<InstanceIdentifier<Endpoint>>any(), Matchers.<DataChangeListener>any(),
                Matchers.<DataChangeScope>any()))
        .thenReturn(mockListener);

        // The following is needed to satisfy the AbstractEndpointRegistry
        when(mockRpcRegistry.addRpcImplementation(Matchers.<Class<EndpointService>>any(),
                Matchers.<EndpointService>any())).thenReturn(mockRpcRegistration);
        when(mockBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTransaction);
        when(mockBroker.newReadOnlyTransaction()).thenReturn(mockReadTransaction);
        when(mockReadTransaction.read(Matchers.<LogicalDatastoreType>any(),
                Matchers.<InstanceIdentifier<Endpoint>>any())).thenReturn(mockReadFuture);
        when(mockWriteTransaction.submit()).thenReturn(mockWriteFuture);
        epManager = new EndpointManager(mockBroker,
                mockRpcRegistry, mockExecutor, mockConnService);
    }


    @Test
    public void testConstructor() throws Exception {
        verify(mockBroker, times(2)).registerDataChangeListener(Matchers.<LogicalDatastoreType>any(),
                Matchers.<InstanceIdentifier<Endpoint>>any(), Matchers.<DataChangeListener>any(),
                Matchers.<DataChangeScope>any());
    }

    @Test
    public void testCallbackEpAttach() throws Exception {
        JsonRpcEndpoint agent = mock(JsonRpcEndpoint.class);
        EndpointDeclarationRequest mockReq =
                mock(EndpointDeclarationRequest.class);
        EndpointDeclarationRequest.Params mockParams =
                mock(EndpointDeclarationRequest.Params.class);
        List<EndpointDeclarationRequest.Params> paramList =
                new ArrayList<EndpointDeclarationRequest.Params>();
        paramList.add(mockParams);
        List<String> idList =
                new ArrayList<String>();
        idList.add(TEST_IDENTIFIER);

        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointDeclarationRequest.DECLARATION_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockParams.getLocation()).thenReturn(TEST_LOCATION);
        when(mockParams.getPolicy_name()).thenReturn(TEST_POLICY);
        when(mockParams.getPrr()).thenReturn(TEST_PRR);
        when(mockParams.getStatus()).thenReturn(EpStatus.EP_STATUS_ATTACH.toString());
        when(mockParams.getIdentifier()).thenReturn(idList);
        epManager.callback(agent, mockReq);
        verify(mockParams).getStatus();

    }

    @Test
    public void testCallbackEpDetach() throws Exception {
        JsonRpcEndpoint agent = mock(JsonRpcEndpoint.class);
        EndpointDeclarationRequest mockReq =
                mock(EndpointDeclarationRequest.class);
        EndpointDeclarationRequest.Params mockParams =
                mock(EndpointDeclarationRequest.Params.class);
        List<EndpointDeclarationRequest.Params> paramList =
                new ArrayList<EndpointDeclarationRequest.Params>();
        paramList.add(mockParams);
        List<String> idList =
                new ArrayList<String>();
        idList.add(TEST_IDENTIFIER);

        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointDeclarationRequest.DECLARATION_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockParams.getLocation()).thenReturn(TEST_LOCATION);
        when(mockParams.getPolicy_name()).thenReturn(TEST_POLICY);
        when(mockParams.getPrr()).thenReturn(TEST_PRR);
        when(mockParams.getStatus()).thenReturn(EpStatus.EP_STATUS_DETACH.toString());
        when(mockParams.getIdentifier()).thenReturn(idList);
        epManager.callback(agent, mockReq);
        verify(mockParams).getStatus();

    }

    @Test
    public void testCallbackEpRequest() throws Exception {
        JsonRpcEndpoint mockRpcEp = mock(JsonRpcEndpoint.class);
        EndpointRequestRequest mockReq =
                mock(EndpointRequestRequest.class);
        EndpointRequestRequest.Params mockParams =
                mock(EndpointRequestRequest.Params.class);
        List<EndpointRequestRequest.Params> paramList =
                new ArrayList<EndpointRequestRequest.Params>();
        paramList.add(mockParams);
        List<String> idList =
                new ArrayList<String>();
        idList.add(TEST_IDENTIFIER);

        when(mockRpcEp.getIdentifier()).thenReturn(TEST_IDENTIFIER);
        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointDeclarationRequest.DECLARATION_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getContext()).thenReturn(TEST_CONTEXT);
        when(mockParams.getIdentifier()).thenReturn(idList);
        epManager.callback(mockRpcEp, mockReq);
        verify(mockParams, times(4)).getIdentifier();
    }

    @Test
    public void testOnDataChangedCreated() throws Exception {
        List<DataObject> daoList =
                new ArrayList<DataObject>();
        Endpoint mockEp = mock(Endpoint.class);
        EndpointGroupId mockEpgId = mock(EndpointGroupId.class);
        TenantId mockTid = mock(TenantId.class);
        L2BridgeDomainId mockBdId = mock(L2BridgeDomainId.class);
        MacAddress mockMac = mock(MacAddress.class);
        OpflexOverlayContext mockCtx = mock(OpflexOverlayContext.class);
        Set<InstanceIdentifier<?>> emptySet =
                Collections.emptySet();
        Map<InstanceIdentifier<?>,DataObject> emptyMap =
                new ConcurrentHashMap<InstanceIdentifier<?>, DataObject>();

        daoList.add(mockEp);
        when(mockEp.getEndpointGroup()).thenReturn(mockEpgId);
        when(mockEp.getTenant()).thenReturn(mockTid);
        when(mockEp.getL2Context()).thenReturn(mockBdId);
        when(mockEp.getMacAddress()).thenReturn(mockMac);
        when(mockEp.getAugmentation(OpflexOverlayContext.class)).thenReturn(mockCtx);
        when(mockCtx.getAgentId()).thenReturn(TEST_AGENT_ID);
        when(mockMac.getValue()).thenReturn(TEST_IDENTIFIER);

        when(mockChange.getCreatedData()).thenReturn(mockDaoMap);
        when(mockDaoMap.values()).thenReturn(daoList);
        when(mockChange.getRemovedPaths()).thenReturn(emptySet);
        when(mockChange.getUpdatedData()).thenReturn(emptyMap);
        epManager.onDataChanged(mockChange);
        verify(mockChange).getCreatedData();
    }

    @Test
    public void testOnDataChangedRemoved() throws Exception {
        List<DataObject> daoList =
                new ArrayList<DataObject>();
        Endpoint mockEp = mock(Endpoint.class);
        EndpointGroupId mockEpgId = mock(EndpointGroupId.class);
        TenantId mockTid = mock(TenantId.class);
        L2BridgeDomainId mockBdId = mock(L2BridgeDomainId.class);
        MacAddress mockMac = mock(MacAddress.class);
        OpflexOverlayContext mockCtx = mock(OpflexOverlayContext.class);
        Map<InstanceIdentifier<?>,DataObject> emptyMap =
                new ConcurrentHashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>,Boolean> dummyMap =
                new ConcurrentHashMap<InstanceIdentifier<?>, Boolean>();
        Set<InstanceIdentifier<?>> dummySet =
                Collections.newSetFromMap(dummyMap);
        dummySet.add(mockIid);
        daoList.add(mockEp);
        when(mockEp.getEndpointGroup()).thenReturn(mockEpgId);
        when(mockEp.getTenant()).thenReturn(mockTid);
        when(mockEp.getL2Context()).thenReturn(mockBdId);
        when(mockEp.getMacAddress()).thenReturn(mockMac);
        when(mockEp.getAugmentation(OpflexOverlayContext.class)).thenReturn(mockCtx);
        when(mockCtx.getAgentId()).thenReturn(TEST_AGENT_ID);
        when(mockMac.getValue()).thenReturn(TEST_IDENTIFIER);

        when(mockChange.getCreatedData()).thenReturn(emptyMap);
        when(mockChange.getRemovedPaths()).thenReturn(dummySet);
        when(mockChange.getOriginalData()).thenReturn(mockDaoMap);
        when(mockChange.getUpdatedData()).thenReturn(emptyMap);
        when(mockDaoMap.get(Matchers.<InstanceIdentifier<?>>any())).thenReturn(mockEp);

        epManager.onDataChanged(mockChange);
        verify(mockChange).getOriginalData();
    }

    @Test
    public void testOnDataChangedUpdated() throws Exception {
        List<DataObject> daoList =
                new ArrayList<DataObject>();
        Endpoint mockEp = mock(Endpoint.class);
        EndpointGroupId mockEpgId = mock(EndpointGroupId.class);
        TenantId mockTid = mock(TenantId.class);
        L2BridgeDomainId mockBdId = mock(L2BridgeDomainId.class);
        MacAddress mockMac = mock(MacAddress.class);
        OpflexOverlayContext mockCtx = mock(OpflexOverlayContext.class);
        Set<InstanceIdentifier<?>> emptySet =
                Collections.emptySet();
        Map<InstanceIdentifier<?>,DataObject> emptyMap =
                new ConcurrentHashMap<InstanceIdentifier<?>, DataObject>();
        Map<InstanceIdentifier<?>,DataObject> dummyMap =
                new ConcurrentHashMap<InstanceIdentifier<?>, DataObject>();
        dummyMap.put(mockIid, mockEp);

        daoList.add(mockEp);
        when(mockEp.getEndpointGroup()).thenReturn(mockEpgId);
        when(mockEp.getTenant()).thenReturn(mockTid);
        when(mockEp.getL2Context()).thenReturn(mockBdId);
        when(mockEp.getMacAddress()).thenReturn(mockMac);
        when(mockEp.getAugmentation(OpflexOverlayContext.class)).thenReturn(mockCtx);
        when(mockCtx.getAgentId()).thenReturn(TEST_AGENT_ID);
        when(mockMac.getValue()).thenReturn(TEST_IDENTIFIER);

        when(mockChange.getCreatedData()).thenReturn(emptyMap);
        when(mockChange.getRemovedPaths()).thenReturn(emptySet);
        when(mockChange.getUpdatedData()).thenReturn(dummyMap);
        when(mockChange.getOriginalData()).thenReturn(mockDaoMap);
        when(mockDaoMap.get(Matchers.<InstanceIdentifier<?>>any())).thenReturn(mockEp);

        when(mockDaoMap.values()).thenReturn(daoList);
        epManager.onDataChanged(mockChange);
        verify(mockChange).getOriginalData();
    }

}
