package org.opendaylight.groupbasedpolicy.renderer.oc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.concurrent.ScheduledExecutorService;

import net.juniper.contrail.api.ApiConnector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( Utils.class )
public class TestUtilsStaticMethod{
	L2DomainManager domainmanager;
	ApiConnector mockedApiConnector = mock(ApiConnector.class);
    L2FloodDomain mockFd = mock(L2FloodDomain.class);
    Utils mockUtils = mock(Utils.class);
    private static final L2FloodDomainId FLOOD_DOMAIN_ID = new L2FloodDomainId("08e3904c-7850-41c0-959a-1a646a83598a");
    private static final Name FLOOD_DOMAIN_NAME = new Name("MOCK_NETWORK");
    private static final String TENANT_ID = "22282cca-9a13-4d0c-a67e-a933ebb0b0ae";
    private static final String TENANT_ID_VALUE = "[_value=22282cca-9a13-4d0c-a67e-a933ebb0b0ae]";
    @Mock
    private DataBroker mockBroker;
    @Mock
    private RpcProviderRegistry mockRpcRegistry;
    @Mock
    private ScheduledExecutorService mockExecutor;

    @Before
    public void beforeTest() {
    	domainmanager = new L2DomainManager(mockBroker, mockRpcRegistry, mockExecutor);
    }

    /*Test method to check if FloodDomain id is invalid */
    @Test
    public void testCanCreateInvalidFloodDomainId(){
    	OcRenderer.apiConnector = mockedApiConnector;
    	String InvalidFloodDOmainID = "08e3904c-7850-41c0-9";
    	when(mockFd.getId()).thenReturn(FLOOD_DOMAIN_ID);
    	when(mockFd.getName()).thenReturn(FLOOD_DOMAIN_NAME);
        PowerMockito.mockStatic(Utils.class);
        PowerMockito.when(Utils.uuidNameFormat(mockFd.getId().toString())).thenReturn(InvalidFloodDOmainID);
        PowerMockito.when(Utils.uuidNameFormat(TENANT_ID_VALUE)).thenReturn(TENANT_ID);
        PowerMockito.when(Utils.isValidHexNumber(InvalidFloodDOmainID)).thenReturn(false);
    	assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, domainmanager.canCreateFloodDomain(mockFd, TENANT_ID_VALUE));
    }
}