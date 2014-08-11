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

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcDecoder;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.ObserverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepository;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 *
 * Test the serialization and deserialization of RPC Messages,
 * and check against expected structure and values.
 */
public class OpflexConnectionServiceTest implements RpcBroker.RpcCallback {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexMessageTest.class);

    static private final String TEST_EP_UUID = "85d53c32-47af-4eaf-82fd-ced653ff74da";
    static public final String TEST_IP = "127.0.0.1";

    static private final String ID_UUID = "2da9e3d7-0bbe-4099-b343-12783777452f";
    static private final String SEND_IDENTITY = "send_identity";
    static private final String DOMAIN_UUID = "default";
    static private final String NAME = "vm1";
    static private final String IDENTITY = "192.168.0.1:56732";
    static private final String opflexIdentityRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + SEND_IDENTITY + "\"," +
            "  \"params\": [ {" +
            "      \"name\":    \"" + NAME + "\"," +
            "      \"domain\":  \"" + DOMAIN_UUID + "\"," +
            "      \"my_role\": [\"" + Role.POLICY_ELEMENT.toString() + "\"]" +
            "   }] }";

    @Mock
    private DataBroker mockDataBroker;
    private DiscoveryDefinitionsBuilder discoveryBuilder;
    private EndpointRegistryBuilder eprBuilder;
    private PolicyRepositoryBuilder prBuilder;
    private ObserverBuilder oBuilder;
    private DiscoveryDefinitions dummyDefinitions;
    private List<EndpointRegistry> registries;
    private List<PolicyRepository> repositories;
    private List<Observer> observers;
    private OpflexConnectionService opflexService;
    @Mock
    private EmbeddedChannel mockChannel;
    @Mock
    private JsonRpcEndpoint mockEp;
    private JsonRpcDecoder decoder;
    @Mock
    private ReadOnlyTransaction mockRead;
    @Mock
    private WriteTransaction mockWrite;
    @Mock
    private CheckedFuture<Optional<DiscoveryDefinitions>, ReadFailedException> mockOption;
    @Mock
    CheckedFuture<Void, TransactionCommitFailedException> mockStatus;
    @Mock
    private Optional<DiscoveryDefinitions> mockDao;
    @Mock
    private OpflexRpcServer mockOpflexServer;
    @Mock
    private OpflexAgent mockAgent;

    @Mock
    private OpflexRpcServer mockServer;
    @Mock
    private RpcServer mockRpcServer;
    
    private ServerSocket create(int[] ports) throws IOException {
        for (int port : ports) {
            try {
                return new ServerSocket(port);
            } catch (IOException ex) {
                continue; // try next port
            }
        }

        // if the program gets here, no port in the range was found
        throw new IOException("no free port found");
    }
    
    private int getAvailableServerPort() {
        try {
            int freePort;
            ServerSocket s = create(new int[] 
                    { 6670, 6671, 6672, 6673, 6674, 6675, 6676, 6677, 6678 });
            freePort = s.getLocalPort();
            s.close();
            return freePort;
        } catch (IOException ex) {
            return 0;
        }
    }

    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage request) {
        opflexService.callback(endpoint, request);
    }
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        /*
         * Mocks
         */
        when(mockDataBroker.newReadOnlyTransaction()).thenReturn(mockRead);
        when(mockDataBroker.newWriteOnlyTransaction()).thenReturn(mockWrite);
        when(mockWrite.submit()).thenReturn(mockStatus);
        when(mockRead.read(LogicalDatastoreType.CONFIGURATION,
                OpflexConnectionService.DISCOVERY_IID)).thenReturn(mockOption);
        when(mockOption.get()).thenReturn(mockDao);
        when(mockDao.get()).thenReturn(dummyDefinitions);

        /*
         * Builders for creating our own discovery definitions
         */
        discoveryBuilder = new DiscoveryDefinitionsBuilder();
        eprBuilder = new EndpointRegistryBuilder();
        prBuilder = new PolicyRepositoryBuilder();
        oBuilder = new ObserverBuilder();

        int testPort = getAvailableServerPort();
        if ( testPort == 0) {
            assertTrue(1==0);
        }
        System.setProperty(OpflexConnectionService.OPFLEX_LISTENPORT, Integer.toString(testPort));
        System.setProperty(OpflexConnectionService.OPFLEX_LISTENIP, TEST_IP);
    }

    @Test
    public void testNoDefinitions() throws Exception {

        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        verify(mockDataBroker).newReadOnlyTransaction();
    }

    @Test
    public void testInitialSet() throws Exception {
        registries = new ArrayList<EndpointRegistry>();
        repositories = new ArrayList<PolicyRepository>();
        observers = new ArrayList<Observer>();
        int serverPort = getAvailableServerPort();
        EndpointRegistry epr = eprBuilder.setId(TEST_IP)
                .setPort(serverPort).build();
        PolicyRepository pr = prBuilder.setId(TEST_IP)
                .setPort(serverPort).build();
        Observer o = oBuilder.setId(TEST_IP)
                .setPort(serverPort).build();
        registries.add(epr);
        repositories.add(pr);
        observers.add(o);
        dummyDefinitions = discoveryBuilder.setObserver(observers)
                .setEndpointRegistry(registries)
                .setPolicyRepository(repositories).build();
        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        verify(mockDataBroker).newReadOnlyTransaction();

    }

    @Test
    public void testAddConnection() throws Exception {
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);
        when(mockEp.getContext()).thenReturn(mockOpflexServer);
        when(mockOpflexServer.getDomain()).thenReturn(DOMAIN_UUID);

        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        opflexService.addConnection(mockEp);
        verify(mockEp, Mockito.times(2)).getIdentifier();
    }

    @Test
    public void testChannelClosed() throws Exception {
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);
        when(mockEp.getContext()).thenReturn(mockOpflexServer);

        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        when(mockOpflexServer.getDomain()).
            thenReturn(OpflexConnectionService.OPFLEX_DOMAIN);
        opflexService.addConnection(mockEp);

        verify(mockEp, Mockito.times(2)).getIdentifier();

        assertTrue(opflexService.opflexAgents.size() > 0);
        when(mockAgent.getIdentity()).thenReturn(TEST_EP_UUID);
        opflexService.channelClosed(mockEp);
        assertTrue(opflexService.opflexAgents.size() <=0);
    }

    @Test
    public void testPublishSubscribeCallback() throws Exception {

        List<Role> testRoles = new ArrayList<Role>();
        testRoles.add(Role.POLICY_REPOSITORY);
        testRoles.add(Role.ENDPOINT_REGISTRY);
        testRoles.add(Role.OBSERVER);

        /*
         * This is *far* from UT, but worthwhile for now
         */
        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        List<RpcMessage> messages = Role.POLICY_REPOSITORY.getMessages();
        for (RpcMessage msg: messages) {
            opflexService.subscribe(msg, this);
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        decoder = new JsonRpcDecoder(1000);
        JsonRpcServiceBinderHandler binderHandler =
                new JsonRpcServiceBinderHandler(null);
        EmbeddedChannel channel = new EmbeddedChannel(decoder, binderHandler);

        RpcMessageMap messageMap = new RpcMessageMap();
        messageMap.addList(Role.POLICY_REPOSITORY.getMessages());

        JsonRpcEndpoint ep = new JsonRpcEndpoint(IDENTITY , opflexService,
                objectMapper, channel, messageMap, opflexService);
        ep.setContext(mockOpflexServer);
        binderHandler.setEndpoint(ep);

        when(mockOpflexServer.getRoles()).thenReturn(testRoles);
        when(mockOpflexServer.getDomain()).
            thenReturn(OpflexConnectionService.OPFLEX_DOMAIN);
        opflexService.addConnection(ep);
        channel.writeInbound(copiedBuffer(opflexIdentityRequest, CharsetUtil.UTF_8));
        Object result = channel.readOutbound();
        result = channel.readOutbound();
        assertTrue(result != null);
        IdentityResponse resp = objectMapper.readValue(result.toString(), IdentityResponse.class);
        assertTrue(resp != null);
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.ENDPOINT_REGISTRY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.POLICY_REPOSITORY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.OBSERVER.toString()));
    }
    
}
