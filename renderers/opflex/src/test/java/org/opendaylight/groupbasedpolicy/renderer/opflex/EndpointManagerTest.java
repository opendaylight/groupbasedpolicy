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
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexConnectionService;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointIdentity;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointResolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyUri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class EndpointManagerTest implements DataChangeListener {
    protected static final Logger logger = LoggerFactory.getLogger(EndpointManagerTest.class);
    private static final String TEST_CONTEXT = "3de31df2-5a65-4d5a-b42b-01fa3bdd82ea";
    private static final int TEST_PRR = 1000;
    private static final String TEST_IDENTIFIER = "00:11:22:33:44:55";
    private static final String TEST_AGENT_ID = "192.168.194.132:6742";
    private static final String TEST_EP_URI = "/EprL2Universe/EprL2Ep/3de31df2-5a65-4d5a-b42b-01fa3bdd82ea/00:11:22:33:44:55";
    private static final String TEST_SUBJECT = "EprL2Ep";

    @Mock
    private JsonNode TEST_MSG_ID;
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
    private EndpointDeclareRequest mockRpcMessage;
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
    @Mock
    private MitLib mockOpflexLib;

    private EndpointIdentity testIdentity;
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
                mockRpcRegistry, mockExecutor, mockConnService, mockOpflexLib);
        MessageUtils.init();
        PolicyUri puri = new PolicyUri(TEST_EP_URI);
        testIdentity = new EndpointIdentity();
        testIdentity.setIdentifier(puri.pop());
        testIdentity.setContext(puri.getUri());
    }


    @Test
    public void testConstructor() throws Exception {
        verify(mockBroker, times(2)).registerDataChangeListener(Matchers.<LogicalDatastoreType>any(),
                Matchers.<InstanceIdentifier<Endpoint>>any(), Matchers.<DataChangeListener>any(),
                Matchers.<DataChangeScope>any());
    }

    @Test
    public void testCallbackEpDeclare() throws Exception {
        JsonRpcEndpoint agent = mock(JsonRpcEndpoint.class);
        EndpointDeclareRequest mockReq =
                mock(EndpointDeclareRequest.class);
        EndpointDeclareRequest.Params mockParams =
                mock(EndpointDeclareRequest.Params.class);
        List<EndpointDeclareRequest.Params> paramList =
                new ArrayList<EndpointDeclareRequest.Params>();
        paramList.add(mockParams);
        List<String> idList =
                new ArrayList<String>();
        idList.add(TEST_IDENTIFIER);

        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointDeclareRequest.DECLARE_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint()).thenReturn(null);
        when(mockParams.getPrr()).thenReturn(TEST_PRR);
        epManager.callback(agent, mockReq);
        verify(mockParams).getEndpoint();

    }

    @Test
    public void testCallbackEpUndeclare() throws Exception {
        JsonRpcEndpoint agent = mock(JsonRpcEndpoint.class);
        EndpointUndeclareRequest mockReq =
                mock(EndpointUndeclareRequest.class);
        EndpointUndeclareRequest.Params mockParams =
                mock(EndpointUndeclareRequest.Params.class);
        List<EndpointUndeclareRequest.Params> paramList =
                new ArrayList<EndpointUndeclareRequest.Params>();
        paramList.add(mockParams);
        List<String> idList =
                new ArrayList<String>();
        idList.add(TEST_IDENTIFIER);

        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointUndeclareRequest.UNDECLARE_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint_uri()).thenReturn(null);
        when(mockParams.getSubject()).thenReturn(TEST_SUBJECT);
        epManager.callback(agent, mockReq);
        verify(mockParams).getEndpoint_uri();

    }

    @Test
    public void testCallbackEpResolve1() throws Exception {
        JsonRpcEndpoint mockRpcEp = mock(JsonRpcEndpoint.class);
        EndpointResolveRequest mockReq =
                mock(EndpointResolveRequest.class);
        EndpointResolveRequest.Params mockParams =
                mock(EndpointResolveRequest.Params.class);
        List<EndpointResolveRequest.Params> paramList =
                new ArrayList<EndpointResolveRequest.Params>();
        paramList.add(mockParams);

        when(mockRpcEp.getIdentifier()).thenReturn(TEST_IDENTIFIER);
        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointResolveRequest.EP_RESOLVE_REQUEST_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint_ident()).thenReturn(null);
        when(mockParams.getEndpoint_uri()).thenReturn(new Uri(TEST_EP_URI));
        when(mockParams.getSubject()).thenReturn(TEST_SUBJECT);
        epManager.callback(mockRpcEp, mockReq);
        verify(mockParams, times(3)).getEndpoint_uri();
    }

    @Test
    public void testCallbackEpResolve2() throws Exception {
        JsonRpcEndpoint mockRpcEp = mock(JsonRpcEndpoint.class);
        EndpointResolveRequest mockReq =
                mock(EndpointResolveRequest.class);
        EndpointResolveRequest.Params mockParams =
                mock(EndpointResolveRequest.Params.class);
        List<EndpointResolveRequest.Params> paramList =
                new ArrayList<EndpointResolveRequest.Params>();
        paramList.add(mockParams);

        when(mockRpcEp.getIdentifier()).thenReturn(TEST_IDENTIFIER);
        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointResolveRequest.EP_RESOLVE_REQUEST_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint_ident()).thenReturn(testIdentity);
        when(mockParams.getEndpoint_uri()).thenReturn(null);
        when(mockParams.getSubject()).thenReturn(TEST_SUBJECT);
        epManager.callback(mockRpcEp, mockReq);
        verify(mockParams, times(3)).getEndpoint_ident();
    }

    @Test
    public void testCallbackEpUnresolve1() throws Exception {
        JsonRpcEndpoint mockRpcEp = mock(JsonRpcEndpoint.class);
        EndpointUnresolveRequest mockReq =
                mock(EndpointUnresolveRequest.class);
        EndpointUnresolveRequest.Params mockParams =
                mock(EndpointUnresolveRequest.Params.class);
        List<EndpointUnresolveRequest.Params> paramList =
                new ArrayList<EndpointUnresolveRequest.Params>();
        paramList.add(mockParams);

        when(mockRpcEp.getIdentifier()).thenReturn(TEST_IDENTIFIER);
        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointUnresolveRequest.EP_UNRESOLVE_REQUEST_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint_ident()).thenReturn(null);
        when(mockParams.getEndpoint_uri()).thenReturn(new Uri(TEST_EP_URI));
        when(mockParams.getSubject()).thenReturn(TEST_SUBJECT);
        epManager.callback(mockRpcEp, mockReq);
        verify(mockParams, times(2)).getEndpoint_uri();

    }

    @Test
    public void testCallbackEpUnresolve2() throws Exception {
        JsonRpcEndpoint mockRpcEp = mock(JsonRpcEndpoint.class);
        EndpointUnresolveRequest mockReq =
                mock(EndpointUnresolveRequest.class);
        EndpointUnresolveRequest.Params mockParams =
                mock(EndpointUnresolveRequest.Params.class);
        List<EndpointUnresolveRequest.Params> paramList =
                new ArrayList<EndpointUnresolveRequest.Params>();
        paramList.add(mockParams);

        when(mockRpcEp.getIdentifier()).thenReturn(TEST_IDENTIFIER);
        when(mockReq.valid()).thenReturn(true);
        when(mockReq.getId()).thenReturn(TEST_MSG_ID);
        when(mockReq.getMethod()).thenReturn(EndpointUnresolveRequest.EP_UNRESOLVE_REQUEST_MESSAGE);
        when(mockReq.getParams()).thenReturn(paramList);
        when(mockParams.getEndpoint_ident()).thenReturn(testIdentity);
        when(mockParams.getEndpoint_uri()).thenReturn(null);
        when(mockParams.getSubject()).thenReturn(TEST_SUBJECT);
        epManager.callback(mockRpcEp, mockReq);
        verify(mockParams, times(2)).getEndpoint_ident();

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
        when(mockBdId.getValue()).thenReturn(TEST_CONTEXT);
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
        when(mockBdId.getValue()).thenReturn(TEST_CONTEXT);
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
