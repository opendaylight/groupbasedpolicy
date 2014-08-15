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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionRequest;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class PolicyManagerTest implements DataChangeListener {
    protected static final Logger logger = LoggerFactory.getLogger(PolicyManagerTest.class);
    private static final String TEST_AGENT_ID = "192.168.194.11:6723";
    private static final String TEST_MSG_ID = "e62303a0-baed-4d0e-af70-8d589bde252c";
    private static final int TEST_TIMEOUT = 500;
    private static final String TEST_POLICY = "foo-boo";

    @Mock
    private PolicyResolver mockResolver;
    @Mock
    private OpflexConnectionService mockConnService;
    @Mock
    private ListenerRegistration<DataChangeListener> mockListener;
    @Mock
    private ListenerRegistration<DataChangeListener> mockL3Listener;
    @Mock
    private EndpointDeclarationRequest mockRpcMessage;
    @Mock
    private BindingAwareBroker.RpcRegistration<EndpointService> mockRpcRegistration;
    @Mock
    private WriteTransaction mockWriteTransaction;
    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockFuture;
    @Mock
    private PolicyInfo mockPolicyInfo;
    @Mock
    private OpflexAgent mockAgent;
    @Mock
    private PolicyScope mockScope;
    @Mock
    private List<PolicyResolutionRequest.Params> mockParamsList;


    private JsonRpcEndpoint dummyEndpoint;

    private PolicyManager policyManager;
    private ScheduledExecutorService executor;

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);
        dummyEndpoint =
                new JsonRpcEndpoint(null, null,
                        null, null, new RpcMessageMap(), null);
        when(mockResolver
                .registerListener(Matchers.<PolicyListener>any())).thenReturn(mockScope);

        policyManager = new PolicyManager(mockResolver,
                mockConnService, executor);
    }


    //@Test
    public void testPolicyUpdated() throws Exception {
        EgKey sepgKey = mock(EgKey.class);
        EgKey depgKey = mock(EgKey.class);
        Policy mockPolicy = mock(Policy.class);

        Set<EgKey> degKeySet = Collections.
                newSetFromMap(new ConcurrentHashMap<EgKey, Boolean>());
        degKeySet.add(depgKey);
        Set<EgKey> segKeySet = Collections.
                newSetFromMap(new ConcurrentHashMap<EgKey, Boolean>());
        segKeySet.add(sepgKey);

        when(mockResolver.getCurrentPolicy()).thenReturn(mockPolicyInfo);
        when(mockConnService.getOpflexAgent(anyString())).thenReturn(mockAgent);
        when(mockPolicyInfo.getPeers(sepgKey)).thenReturn(degKeySet);
        when(mockPolicyInfo.getPolicy(sepgKey, depgKey)).thenReturn(mockPolicy);
        when(mockAgent.getEndpoint()).thenReturn(dummyEndpoint);

        /*
         * Add some EPGs to enable messaging
         */
        //policyManager.dirty.get().addEndpointGroup(sepgKey);
        //policyManager.dirty.get().addEndpointGroup(depgKey);

        /*
         * Add a single agent
         */
        //policyManager.dirty.get().addAgent(TEST_AGENT_ID);

        policyManager.policyUpdated(segKeySet);

        verify(mockAgent, timeout(TEST_TIMEOUT)).getEndpoint();

    }

    //@Test
    public void testGroupEndpointUpdated() throws Exception {
        EgKey sepgKey = mock(EgKey.class);
        EgKey depgKey = mock(EgKey.class);
        Policy mockPolicy = mock(Policy.class);
        TenantId tId = mock(TenantId.class);
        EndpointGroupId epgId = mock(EndpointGroupId.class);

        Set<EgKey> egKeySet = Collections.
                newSetFromMap(new ConcurrentHashMap<EgKey, Boolean>());
        egKeySet.add(depgKey);

        when(mockResolver.getCurrentPolicy()).thenReturn(mockPolicyInfo);
        when(mockConnService.getOpflexAgent(anyString())).thenReturn(mockAgent);
        when(mockPolicyInfo.getPeers(sepgKey)).thenReturn(egKeySet);
        when(mockPolicyInfo.getPolicy(sepgKey, depgKey)).thenReturn(mockPolicy);
        when(mockAgent.getEndpoint()).thenReturn(dummyEndpoint);
        when(sepgKey.getTenantId()).thenReturn(tId);
        when(sepgKey.getEgId()).thenReturn(epgId);

        /*
         * Add some EPGs to enable messaging
         */
        //policyManager.dirty.get().addEndpointGroup(sepgKey);
        //policyManager.dirty.get().addEndpointGroup(depgKey);

        /*
         * Add a single agent
         */
        //policyManager.dirty.get().addAgent(TEST_AGENT_ID);

        //policyManager.groupEndpointUpdated(sepgKey, epKey);

        verify(mockAgent, timeout(TEST_TIMEOUT)).getEndpoint();

    }


    @Test
    public void testCallback() throws Exception {
        JsonRpcEndpoint mockEp = mock(JsonRpcEndpoint.class);
        PolicyResolutionRequest request =
                mock(PolicyResolutionRequest.class);
        PolicyResolutionRequest.Params mockParams =
                mock(PolicyResolutionRequest.Params.class);

        when(request.getId()).thenReturn(TEST_MSG_ID);
        when(request.valid()).thenReturn(true);
        when(request.getMethod()).thenReturn(PolicyResolutionRequest.RESOLVE_MESSAGE);
        when(request.getParams()).thenReturn(mockParamsList);
        when(mockParamsList.get(0)).thenReturn(mockParams);
        when(mockParams.getPolicy_name()).thenReturn(TEST_POLICY);
        when(mockEp.getIdentifier()).thenReturn(TEST_AGENT_ID);

        policyManager.callback(mockEp,  request);

        verify(mockEp, timeout(TEST_TIMEOUT).times(2)).getIdentifier();
    }

}
