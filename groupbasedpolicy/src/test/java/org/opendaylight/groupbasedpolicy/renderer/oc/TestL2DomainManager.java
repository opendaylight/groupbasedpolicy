/*
 * Copyright (C) 2015 Juniper Networks, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.groupbasedpolicy.renderer.oc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.FloatingIpPool;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Test Class for L2 Flood Domain.
 */

public class TestL2DomainManager {
    L2DomainManager domainmanager;
    ApiConnector apiconnector;
    ApiConnector mockedApiConnector = mock(ApiConnector.class);
    VirtualNetwork mockedVirtualNetwork = mock(VirtualNetwork.class);
    Project mockedProject = mock(Project.class);
    FloatingIpPool mockedFloatingIpPool = mock(FloatingIpPool.class);
    L2FloodDomain mockFd = mock(L2FloodDomain.class);
    L2FloodDomain mockFdNew = mock(L2FloodDomain.class);
    @Mock
    private DataBroker mockBroker;
    @Mock
    private RpcProviderRegistry mockRpcRegistry;
    @Mock
    private ScheduledExecutorService mockExecutor;
    @Mock
    private ListenerRegistration<DataChangeListener> mockListener;
    @Mock
    private ListenerRegistration<DataChangeListener> mockL3Listener;
    @Mock
    private CheckedFuture<Optional<L2FloodDomain>,ReadFailedException> mockReadFuture;
    @Mock
    private JsonRpcEndpoint mockAgent;
    @Mock
    private OcRenderer mockOcRenderer;
    @Mock
    private WriteTransaction mockWriteTransaction;
    @Mock
    private ReadOnlyTransaction mockReadTransaction;
    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockWriteFuture;
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
    protected static final Logger logger = LoggerFactory.getLogger(L2DomainManager.class);
    private static final String TENANT_ID = "22282cca-9a13-4d0c-a67e-a933ebb0b0ae";
    private static final String TENANT_ID_VALUE = "[_value=22282cca-9a13-4d0c-a67e-a933ebb0b0ae]";
    private static final L2FloodDomainId FLOOD_DOMAIN_ID = new L2FloodDomainId("08e3904c-7850-41c0-959a-1a646a83598a");
    private static final Name FLOOD_DOMAIN_NAME = new Name("MOCK_DOMAIN");

    List<DataObject> daoList =
            new ArrayList<DataObject>();

    @Before
    public void beforeTest() {
    	domainmanager = new L2DomainManager(mockBroker, mockRpcRegistry, mockExecutor);
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedVirtualNetwork);
        assertNotNull(mockedProject);
        assertNotNull(mockedFloatingIpPool);
    }

    @After
    public void AfterTest() {
    	domainmanager = null;
        apiconnector = null;
    }

   /*  Test method to check if flood domain id is null*/
    @Test
    public void testCanCreateFloodDomainIDNull() {
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getId()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,domainmanager.canCreateFloodDomain(mockFd, TENANT_ID));
    }

    /* Test method to check if flood domain name is null*/
    @Test
    public void testCanCreateFloodDomainNameNull() {
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getName()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,domainmanager.canCreateFloodDomain(mockFd, TENANT_ID));
    }

     /*Test method to check if FloodDomain ProjctUUID is not found */
    @Test
    public void testCanCreateFloodDomainProjctUUIDNotFound() throws IOException {
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFd.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(null);
    	assertEquals(HttpURLConnection.HTTP_NOT_FOUND,domainmanager.canCreateFloodDomain(mockFd ,TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomain ProjctUUID is forbidden */
    @Test
    public void testCanCreateFloodDomainForbidden() throws IOException {
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFd.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	String mockL2FloodDomainUUID = "08e3904c-7850-41c0-959a-1a646a83598a";
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(mockedProject);
    	when(mockedApiConnector.findById(VirtualNetwork.class, mockL2FloodDomainUUID)).thenReturn(mockedVirtualNetwork);
    	assertEquals(HttpURLConnection.HTTP_FORBIDDEN,domainmanager.canCreateFloodDomain(mockFd ,TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomain is created ok. */
    @Test
    public void testCanCreateFloodDomainOk() throws IOException {
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFd.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	String mockL2FloodDomainUUID = "08e3904c-7850-41c0-959a-1a646a83598a";
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(mockedProject);
    	when(mockedApiConnector.findById(VirtualNetwork.class, mockL2FloodDomainUUID)).thenReturn(null);
    	assertEquals(HttpURLConnection.HTTP_OK,domainmanager.canCreateFloodDomain(mockFd ,TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomain Name is null */
    @Test
    public void testCanUpdateFloodDomainNameisNull(){
    	OcRenderer.apiConnector = mockedApiConnector;
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFdNew.getName()).thenReturn(null);
    	assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, domainmanager.canUpdateFloodDomain(mockFdNew, mockFd, TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomainName already exists */
    @Test
    public void testCanUpdateFloodDomainNameALreadyExist() throws IOException{
    	OcRenderer.apiConnector = mockedApiConnector;
    	String mockL2FloodDomainUUID = "08e3904c-7850-41c0-959a-1a646a83598a";
    	String name = "MOCK_DOMAIN";
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFdNew.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(mockedProject);
    	when(mockedApiConnector.findById(VirtualNetwork.class, mockL2FloodDomainUUID)).thenReturn(mockedVirtualNetwork);
    	when(mockedApiConnector.findByName(VirtualNetwork.class, mockedProject, mockFdNew.getName().toString())).thenReturn(name);
    	assertEquals(HttpURLConnection.HTTP_FORBIDDEN, domainmanager.canUpdateFloodDomain(mockFdNew, mockFd, TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomain ID is null */
    @Test
    public void testCanUpdateFloodDomainIDisNull() throws IOException{
    	OcRenderer.apiConnector = mockedApiConnector;
    	String mockL2FloodDomainUUID = "08e3904c-7850-41c0-959a-1a646a83598a";
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFdNew.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(mockedProject);
    	when(mockedApiConnector.findById(VirtualNetwork.class, mockL2FloodDomainUUID)).thenReturn(null);
    	when(mockedApiConnector.findByName(VirtualNetwork.class, mockedProject, mockFdNew.getName().toString())).thenReturn(null);
    	assertEquals(HttpURLConnection.HTTP_FORBIDDEN, domainmanager.canUpdateFloodDomain(mockFdNew, mockFd, TENANT_ID_VALUE));
    }

    /*Test method to check if FloodDomain Name can be updated */
    @Test
    public void testCanUpdateFloodDomainOK() throws IOException{
    	OcRenderer.apiConnector = mockedApiConnector;
    	String mockL2FloodDomainUUID = "08e3904c-7850-41c0-959a-1a646a83598a";
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFdNew.getName()).thenReturn(FLOOD_DOMAIN_NAME);
    	when(mockedApiConnector.findById(Project.class, TENANT_ID)).thenReturn(mockedProject);
    	when(mockedApiConnector.findById(VirtualNetwork.class, mockL2FloodDomainUUID)).thenReturn(mockedVirtualNetwork);
    	when(mockedApiConnector.findByName(VirtualNetwork.class, mockedProject, mockFdNew.getName().toString())).thenReturn(null);
    	assertEquals(HttpURLConnection.HTTP_OK, domainmanager.canUpdateFloodDomain(mockFdNew, mockFd, TENANT_ID_VALUE));
    }
}