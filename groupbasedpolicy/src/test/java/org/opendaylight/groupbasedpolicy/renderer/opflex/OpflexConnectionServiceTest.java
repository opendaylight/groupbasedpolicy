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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcDecoder;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.ObserverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepository;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepositoryBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Test the serialization and deserialization of RPC Messages,
 * and check against expected structure and values.
 */
public class OpflexConnectionServiceTest {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexMessageTest.class);

    static private final String TEST_RPC_MESSAGE_NAME = "test_message";

    static private final String TEST_EP_UUID = "85d53c32-47af-4eaf-82fd-ced653ff74da";
    static private final String TEST_ID_UUID = "788950f6-2279-4ae1-820e-d277cea3623c";
    static public final String TEST_IP = "127.0.0.1";
    static public final String TEST_PORT = "57563";

    static private final String ID_UUID = "2da9e3d7-0bbe-4099-b343-12783777452f";
    static private final String SEND_IDENTITY = "send_identity";
    static private final String POLICY_REQUEST = "resolve_policy";
    static private final String DOMAIN_UUID = "75caaff2-cb4f-4509-b45e-47b447cb35a9";
    static private final String NAME = "vm1";
    static private final String IDENTITY = "192.168.0.1:56732";
    static private final String opflexIdentityRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + SEND_IDENTITY + "\"," +
            "  \"params\": [ {" +
            "      \"name\":    \"" + NAME + "\"," +
            "      \"domain\":  \"" + DOMAIN_UUID + "\"," +
            "      \"my_role\": [\"" + OpflexConnectionService.Role.POLICY_ELEMENT.toString() + "\"]" +
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
    private ListenableFuture<Optional<DataObject>> mockOption;
    @Mock
    private Optional<DataObject> mockDao;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        /*
         * Mocks
         */
        when(mockDataBroker.newReadOnlyTransaction()).thenReturn(mockRead);
        when(mockRead.read(LogicalDatastoreType.CONFIGURATION, OpflexConnectionService.
                DISCOVERY_DEFINITIONS_IID)).thenReturn(mockOption);
        when(mockOption.get()).thenReturn(mockDao);
        when(mockDao.get()).thenReturn(dummyDefinitions);

        /*
         * Builders for creating our own discovery definitions
         */
        discoveryBuilder = new DiscoveryDefinitionsBuilder();
        eprBuilder = new EndpointRegistryBuilder();
        prBuilder = new PolicyRepositoryBuilder();
        oBuilder = new ObserverBuilder();


        // TODO: needs deterministic way of finding available socket
        System.setProperty(OpflexConnectionService.OPFLEX_LISTENPORT, TEST_PORT);
        System.setProperty(OpflexConnectionService.OPFLEX_LISTENIP, TEST_IP);
    }


    //@Test
    public void testNoDefinitions() throws Exception {

        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        verify(mockDataBroker).newReadOnlyTransaction();
    }

    //@Test
    public void testInitialSet() throws Exception {
        registries = new ArrayList<EndpointRegistry>();
        repositories = new ArrayList<PolicyRepository>();
        observers = new ArrayList<Observer>();
        EndpointRegistry epr = eprBuilder.setId(TEST_IP)
                .setPort(Integer.valueOf(TEST_PORT)).build();
        PolicyRepository pr = prBuilder.setId(TEST_IP)
                .setPort(Integer.valueOf(TEST_PORT)).build();
        Observer o = oBuilder.setId(TEST_IP)
                .setPort(Integer.valueOf(TEST_PORT)).build();
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

    //@Test
    public void testAddConnection() throws Exception {
        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);

        when(mockEp.supportsMessages(opflexService.
                policyRepositoryMessages)).thenReturn(true);
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);


        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        opflexService.addConnection(mockEp);
        verify(mockEp, Mockito.times(3)).supportsMessages(opflexService.policyRepositoryMessages);
        verify(mockEp, Mockito.times(3)).getIdentifier();
        assertTrue(opflexService.opflexAgents.size() == 1);
    }

    //@Test
    public void testChannelClosed() throws Exception {
        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);

        JsonRpcEndpoint mockEp = mock(JsonRpcEndpoint.class);

        when(mockEp.supportsMessages(opflexService.
                policyRepositoryMessages)).thenReturn(true);
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);


        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        opflexService.addConnection(mockEp);
        assertTrue(opflexService.opflexAgents.size() == 1);
        opflexService.channelClosed(mockEp);
        assertTrue(opflexService.opflexAgents.size() == 0);
    }

    //@Test
    public void testPublishSubscribeCallback() throws Exception {

        /*
         * This is *far* from UT, but worthwhile for now
         */
        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        decoder = new JsonRpcDecoder(1000);
        JsonRpcServiceBinderHandler binderHandler =
                new JsonRpcServiceBinderHandler(null);
        EmbeddedChannel channel = new EmbeddedChannel(decoder, binderHandler);

        RpcMessageMap messageMap = new RpcMessageMap();
        IdentityRequest rpcMsg = new IdentityRequest();
        messageMap.add(rpcMsg);
        JsonRpcEndpoint ep = new JsonRpcEndpoint(IDENTITY , opflexService,
                objectMapper, channel, messageMap, opflexService);
        binderHandler.setEndpoint(ep);
        opflexService.addConnection(ep);
        channel.writeInbound(copiedBuffer(opflexIdentityRequest, CharsetUtil.UTF_8));
        Object result = channel.readOutbound();
        assertTrue(result != null);
        IdentityResponse resp = objectMapper.readValue(result.toString(), IdentityResponse.class);
        assertTrue(result != null);
        assertTrue(resp.getResult().getMy_role()
                .contains(OpflexConnectionService.Role.ENDPOINT_REGISTRY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(OpflexConnectionService.Role.POLICY_REPOSITORY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(OpflexConnectionService.Role.OBSERVER.toString()));
    }
}
