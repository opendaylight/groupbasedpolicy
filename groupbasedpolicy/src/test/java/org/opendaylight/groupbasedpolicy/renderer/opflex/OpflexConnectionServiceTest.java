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
import static org.mockito.Matchers.anyObject;
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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcDecoder;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.Domains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.Domain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.DiscoveryDefinitionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.EndpointRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.ObserverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.PolicyRepository;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.PolicyRepositoryBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
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

    static private final String TEST_EP_UUID = "85d53c32-47af-4eaf-82fd-ced653ff74da";
    static public final String TEST_IP = "127.0.0.1";
    static public final String TEST_PORT = "57563";

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
    private ListenableFuture<Optional<Domains>> mockOption;
    @Mock
    ListenableFuture<RpcResult<TransactionStatus>> mockStatus;
    @Mock
    private Optional<Domains> mockDao;
    @Mock
    private Domains mockDomains;
    @Mock
    private Domain mockDomain;
    @Mock
    private OpflexDomain mockOpflexDomain;
    @Mock
    private OpflexRpcServer mockOpflexServer;
    @Mock
    private OpflexAgent mockAgent;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        /*
         * Mocks
         */
        when(mockDataBroker.newReadOnlyTransaction()).thenReturn(mockRead);
        when(mockDataBroker.newWriteOnlyTransaction()).thenReturn(mockWrite);
        when(mockWrite.commit()).thenReturn(mockStatus);
        when(mockRead.read(LogicalDatastoreType.CONFIGURATION,
                OpflexConnectionService.DOMAINS_IID)).thenReturn(mockOption);
        when(mockOption.get()).thenReturn(mockDao);
        when(mockDao.get()).thenReturn(mockDomains);
        when(mockDomains.getDomain())
        .thenReturn(new ArrayList<Domain>() {{ add(mockDomain); }});
        when(mockDomain.getDiscoveryDefinitions()).thenReturn(dummyDefinitions);

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
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);
        when(mockEp.getContext()).thenReturn(mockOpflexServer);
        when(mockOpflexServer.getDomain()).thenReturn(mockOpflexDomain);
        when(mockOpflexDomain.getDomain()).thenReturn(DOMAIN_UUID);

        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        opflexService.addConnection(mockEp);
        verify(mockEp, Mockito.times(2)).getIdentifier();
        verify(mockOpflexDomain, Mockito.times(1)).addOpflexAgent((OpflexAgent)anyObject());
    }

    //@Test
    public void testChannelClosed() throws Exception {
        when(mockEp.getIdentifier()).thenReturn(TEST_EP_UUID);
        when(mockEp.getContext()).thenReturn(mockOpflexServer);
        when(mockOpflexDomain.getDomain()).thenReturn(DOMAIN_UUID);
        when(mockAgent.getDomain()).thenReturn(OpflexConnectionService.OPFLEX_DOMAIN);


        opflexService = new OpflexConnectionService();
        opflexService.setDataProvider(mockDataBroker);
        when(mockOpflexServer.getDomain()).
            thenReturn(opflexService.opflexDomains.get(OpflexConnectionService.OPFLEX_DOMAIN));
        opflexService.addConnection(mockEp);

        verify(mockEp, Mockito.times(2)).getIdentifier();

        when(mockOpflexServer.getDomain()).thenReturn(mockOpflexDomain);
        when(mockOpflexDomain.getOpflexAgent(TEST_EP_UUID)).thenReturn(mockAgent);
        when(mockAgent.getDomain()).thenReturn(OpflexConnectionService.OPFLEX_DOMAIN);
        when(mockAgent.getIdentity()).thenReturn(TEST_EP_UUID);
        opflexService.channelClosed(mockEp);
        verify(mockAgent).getIdentity();
    }

    //@Test
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
            thenReturn(opflexService.opflexDomains.get(OpflexConnectionService.OPFLEX_DOMAIN));
        opflexService.addConnection(ep);
        channel.writeInbound(copiedBuffer(opflexIdentityRequest, CharsetUtil.UTF_8));
        Object result = channel.readOutbound();
        assertTrue(result != null);
        IdentityResponse resp = objectMapper.readValue(result.toString(), IdentityResponse.class);
        assertTrue(result != null);
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.ENDPOINT_REGISTRY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.POLICY_REPOSITORY.toString()));
        assertTrue(resp.getResult().getMy_role()
                .contains(Role.OBSERVER.toString()));
    }
}
