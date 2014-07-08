/*
 * Copyright (C) 2013 Red Hat, Inc.
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Evan Zeller, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.ConnectionService;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepository;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/*
 * Manages the different OpFlex entity connections. It does this
 * on behalf of each logical OpFlex entity:
 *    o  Policy Repositories
 *    o  Endpoint Registries
 *    o  Observers
 *
 * Each OpFlex entity defines the JSON RPC methods supported, and
 * manages their connection/discovery using dedicated servers.
 * Servers and connections are maintained in dedicated client and
 * server maps.
 *
 * TODO: calls to add messages to policy repository, EP registry , and observer
 * TODO: incorporate OpFlex domain
 * TODO: break into smaller pieces?
 */
public class OpflexConnectionService
    implements ConnectionService, RpcBroker,
                RpcBroker.RpcCallback, DataChangeListener, AutoCloseable {
    protected static final Logger logger =
            LoggerFactory.getLogger(OpflexConnectionService.class);

    public enum Role {
        POLICY_REPOSITORY("policy_repository"),
        ENDPOINT_REGISTRY("endpoint_registry"),
        OBSERVER("observer"),
        POLICY_ELEMENT("policy_element");

        private String role;
        Role(String role) {
            this.role = role;
        }
        @Override
        public String toString() {
            return this.role;
        }
    }

    private static class OpflexConnection {
        String identity;
        List<Role> roles;
        JsonRpcEndpoint endpoint;

        public OpflexConnection() {
        }

        public String getIdentity() {
            return identity;
        }

        public void setIdentity(String identity) {
            this.identity = identity;
        }

        public List<Role> getRoles() {
            return roles;
        }

        public void setRoles(List<Role> roles) {
            this.roles = roles;
        }

        public JsonRpcEndpoint getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(JsonRpcEndpoint endpoint) {
            this.endpoint = endpoint;
        }

    }

    public static class OpflexRpcServer {
        private String identity;
        private List<Role> roles;
        private RpcServer server;

        public OpflexRpcServer() {
            roles = new ArrayList<Role>();
        }

        public OpflexRpcServer(String identity) {
            this.identity = identity;
        }

        public OpflexRpcServer(String identity, List<Role> roles) {
            this.identity = identity;
            this.roles = roles;
        }

        public String getId() {
            return this.identity;
        }

        public void setRpcServer(RpcServer server) {
            this.server = server;
        }

        public RpcServer getRpcServer() {
            return this.server;
        }

        public void addRole(Role role) {
            if (!this.roles.contains(role))
                this.roles.add(role);
        }

        public List<Role> getRoles() {
            return this.roles;
        }

        public boolean sameServer(OpflexRpcServer srv) {
            if (this == srv)
                return true;
            if (srv == null)
                return false;
            if (!this.identity.equals(srv.identity))
                return false;
            if (this.roles == null && srv.roles == null)
                return true;
            if (this.roles == null || srv.roles == null)
                return false;
            if (this.roles.size() == srv.roles.size() && this.roles.containsAll(srv.roles))
                return true;
            return false;
        }
    }

    // Properties that can be set in config.ini
    static final String OPFLEX_LISTENPORT = "opflex.listenPort";
    private static final Integer defaultOpflexPort = 6670;
    static final String OPFLEX_LISTENIP = "opflex.listenIp";
    private static final String defaultOpflexIp = "0.0.0.0";

    private Integer opflexListenPort = defaultOpflexPort;
    private String opflexListenIp = defaultOpflexIp;

    ConcurrentMap<String, OpflexConnection> opflexAgents = null;
    ConcurrentMap<String, OpflexRpcServer> opflexServers = null;
    ConcurrentMap<String, List<RpcCallback>> brokerMap = null;
    List<RpcMessage> policyRepositoryMessages;
    List<RpcMessage> endpointRegistryMessages;
    List<RpcMessage> observerMessages;
    private DataBroker dataProvider;

    public static final InstanceIdentifier<DiscoveryDefinitions>  DISCOVERY_DEFINITIONS_IID =
            InstanceIdentifier.builder(DiscoveryDefinitions.class).build();


    /**
     *
     * Set the data store provider for the OpFlex connection service, and
     * start the service using this data store.
     *
     * @param salDataProvider The MD-SAl data store provider
     */
    public void setDataProvider(DataBroker salDataProvider) {
        dataProvider = salDataProvider;

        startOpflexManager();
    }

    private DiscoveryDefinitions getDiscoveryDefinitions() {

        ReadTransaction t = dataProvider.newReadOnlyTransaction();
        ListenableFuture<Optional<DataObject>> f = t.read(LogicalDatastoreType.CONFIGURATION, DISCOVERY_DEFINITIONS_IID);
        try {
            Optional<DataObject> dao = f.get();
            if (dao.get() != null && dao.get() instanceof DiscoveryDefinitions) {
                return (DiscoveryDefinitions)dao.get();
            }
        }
        catch ( Exception e ) {
            logger.warn("Not sure what happens here");
        }
        return null;

    }

    private List<OpflexRpcServer> setDefaultIdentities() {

        /*
         * Create a single server, filling all roles
         */
        String identity = opflexListenIp + ":" + opflexListenPort.toString();
        List<OpflexRpcServer> srvList = new ArrayList<OpflexRpcServer>();
        List<Role> roles = new ArrayList<Role>();
        roles.add(Role.POLICY_REPOSITORY);
        roles.add(Role.ENDPOINT_REGISTRY);
        roles.add(Role.OBSERVER);
        OpflexRpcServer srv = new OpflexRpcServer(identity, roles);
        srvList.add(srv);
        return srvList;

    }

    private List<OpflexRpcServer> createServerList() {
        DiscoveryDefinitions identities = getDiscoveryDefinitions();
        if (identities != null) {
            Map<String, OpflexRpcServer> servers =
                    new ConcurrentHashMap<String, OpflexRpcServer>();
            List<String> addList = getPolicyRepositories(identities.getPolicyRepository());
            addServerList(servers, addList, Role.POLICY_REPOSITORY);
            addList = getEndpointRegistries(identities.getEndpointRegistry());
            addServerList(servers, addList, Role.ENDPOINT_REGISTRY);
            addList = getObservers(identities.getObserver());
            addServerList(servers, addList, Role.OBSERVER);
            return(new ArrayList<OpflexRpcServer>(servers.values()));
        }
        else {
            return setDefaultIdentities();
        }
    }

    private void initializeServers() {

        /*
         * Get the configured identities, if any. If lists are empty,
         * set up a single instance of each, using the localhost
         * interface
         */
        List<OpflexRpcServer> serverList = createServerList();
        addServers(serverList);
    }


    private List<String> getPolicyRepositories(List<PolicyRepository> repositories ) {
        List<String> identityList = new ArrayList<String>() ;
        for ( PolicyRepository pr: repositories.toArray(new PolicyRepository[0]) ) {
            String identity = pr.getId() + ":" + pr.getPort().toString();
            identityList.add(identity);
        }
        return identityList;
    }

    private List<String> getEndpointRegistries(List<EndpointRegistry> registries ) {
        List<String> identityList = new ArrayList<String>() ;
        for ( EndpointRegistry epr: registries.toArray(new EndpointRegistry[0]) ) {
            String identity = epr.getId() + ":" + epr.getPort().toString();
            identityList.add(identity);
        }
        return identityList;
    }

    private List<String> getObservers(List<Observer> observers ) {
        List<String> identityList = new ArrayList<String>() ;
        for ( Observer o: observers.toArray(new Observer[0]) ) {
            String identity = o.getId() + ":" + o.getPort().toString();
            identityList.add(identity);
        }
        return identityList;
    }

    private void addServerList( Map<String, OpflexRpcServer> servers,
            List<String> idList, Role role ) {
        if (idList == null || idList.size() <= 0)
            return;

        for ( String id : idList ) {
            List<Role> roles = new ArrayList<Role>();
            OpflexRpcServer srv = servers.get(id);
            if (srv != null ) {
                roles = srv.getRoles();
                servers.remove(id);
            }

            roles.add(role);
            srv = new OpflexRpcServer(id, roles);
            servers.put(id, srv);
        }

    }

    private void launchRpcServer(OpflexRpcServer srv) {
        RpcServer rpcSrv = new RpcServer(srv.getId().split(":")[0],
                Integer.parseInt(srv.getId().split(":")[1]));
        rpcSrv.setConnectionService(this);
        rpcSrv.setRpcBroker(this);

        /*
         * Make sure the server is configured for the proper messages
         */
        List<Role> roles = srv.getRoles();
        for ( Role role : roles ) {
            switch (role) {
                case POLICY_REPOSITORY:
                {
                    rpcSrv.addMessageList(this.policyRepositoryMessages);
                }
                break;
                case ENDPOINT_REGISTRY:
                {
                    rpcSrv.addMessageList(this.endpointRegistryMessages);
                }
                break;
                case OBSERVER:
                {
                    rpcSrv.addMessageList(this.observerMessages);
                }
                break;
                default:
                {
                    logger.warn("Invalid Role {}", role );
                }
                break;
            }
        }

        srv.setRpcServer(rpcSrv);
        opflexServers.put(srv.getId(), srv);

        new Thread() {
            private RpcServer server;

            public Thread initializeServerParams(RpcServer server) {
                this.server = server;
                return this;
            }
            @Override
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                    logger.warn("Exception starting new server {}", e);
                }
            }
        }.initializeServerParams(rpcSrv).start();

    }

    private void addServers(List<OpflexRpcServer> idMap) {
        /*
         * Check to see if there's already a server
         * with this identity, and if so, close it
         * and replace it with this one.
         */
        for ( OpflexRpcServer srv: idMap ) {
            OpflexRpcServer server = opflexServers.get(srv.getId());
            if (server != null) {
                if ( !server.sameServer(srv)) {
                    OpflexRpcServer oldServer = opflexServers.remove(srv.getId());
                    oldServer.getRpcServer().getChannel().disconnect();
                    launchRpcServer(srv);
                }
            }
            else {
                launchRpcServer(srv);
            }
        }
    }

    private void dropServers(List<String> oldServers) {
        OpflexRpcServer server;

        /*
         * Check to see if there's already a server
         * with this identity, and if so, close it
         * and replace it with this one.
         */
        for (String identity: oldServers) {
            if (opflexServers.containsKey(identity)) {
                server = opflexServers.remove(identity);
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    public void startOpflexManager() {
        opflexAgents = new ConcurrentHashMap<String, OpflexConnection>();
        opflexServers = new ConcurrentHashMap<String, OpflexRpcServer>();
        brokerMap = new ConcurrentHashMap<String, List<RpcCallback>>();

        /*
         * Check configuration to see which listeners we should be creating
         */
        int listenPort = defaultOpflexPort;
        String portString = System.getProperty(OPFLEX_LISTENPORT);
        if (portString != null) {
            listenPort = Integer.decode(portString).intValue();
        }
        opflexListenPort = listenPort;
        String listenIp = defaultOpflexIp;
        String ipString = System.getProperty(OPFLEX_LISTENIP);
        if (ipString != null) {
            listenIp = ipString;
        }
        opflexListenIp = listenIp;

        /*
         * Set up the messages supported by each OpFlex policy
         * component
         */
        policyRepositoryMessages = new ArrayList<RpcMessage>();
        endpointRegistryMessages = new ArrayList<RpcMessage>();
        observerMessages = new ArrayList<RpcMessage>();

        IdentityRequest idRequest = new IdentityRequest();
        policyRepositoryMessages.add(idRequest);
        endpointRegistryMessages.add(idRequest);
        observerMessages.add(idRequest);

        /* this class implements identity handlers */
        subscribe(idRequest, this);

        IdentityResponse idResponse = new IdentityResponse();
        policyRepositoryMessages.add(idResponse);
        endpointRegistryMessages.add(idResponse);
        observerMessages.add(idResponse);

        initializeServers();
    }

    /**
     * Stop the OpFlex Connection Service. This shuts down all active
     * connections and servers.
     */
    public void stopping() {
        for (OpflexConnection connection : opflexAgents.values()) {
            connection.getEndpoint().getChannel().disconnect();
        }
        for (OpflexRpcServer server : opflexServers.values() ) {
            if (server.getRpcServer().getChannel() != null) {
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    /**
     * Remove the OpFlex connection/agent from the map
     *
     * @param identifier The identity of the connection that was closed
     */
    public void removeConnection(String identifier) {
        opflexAgents.remove(identifier);
    }

    /**
     * Add a server with the given identity
     *
     * @param identity The IP address/socket pair for the server
     * @param server The instantiated server
     */
    public void addServer(String identity, OpflexRpcServer server) {
        opflexServers.put(identity, server);
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
     @Override
     public void close() throws ExecutionException, InterruptedException {

         if (dataProvider != null) {
             WriteTransaction t = dataProvider.newWriteOnlyTransaction();
             t.delete(LogicalDatastoreType.CONFIGURATION, DISCOVERY_DEFINITIONS_IID);
             t.commit().get();
         }
     }

    @Override
    public void onDataChanged( final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>change) {

        List<String> addList = new ArrayList<String>();
        List <String> dropList = new ArrayList<String>();

        /* Get the new list of configured servers */
        List<OpflexRpcServer> serverList = createServerList();

        /*
         * Create a list of new servers by skipping any servers in the
         * list that are already configured (i.e. same IP/socket and set
         * of roles) -- no need to take them down
         */
        for ( OpflexRpcServer srv : serverList ) {
            OpflexRpcServer s = opflexServers.get(srv.getId());
            if (s != null && s.getRoles().containsAll(srv.getRoles())) {
                continue;
            }
            addList.add(srv.getId());
        }

        /*
         * We need to find out if there are any servers that
         * we have to drop. This is the set of servers that
         * are already running but don't appear in the configured
         * list. This just requires a check against the IP/port
         * (i.e. no need to check role).
         */
        Set <String> dropSet = opflexServers.keySet();
        dropSet.removeAll(addList);
        dropList.addAll(dropSet);

        /* remove deleted servers first */
        dropServers(dropList);
        addServers(serverList);
    }

    @Override
    public void subscribe(RpcMessage message, RpcCallback callback) {

        /*
         * Create a new list, replacing the old
         */
        List<RpcCallback> cbList = brokerMap.get(message.getName());
        if(cbList == null) {
            cbList = new ArrayList<RpcCallback>();
            cbList.add(callback);
            brokerMap.put(message.getName(), cbList);
        }
        else
        if(!cbList.contains(callback)) {
            cbList.add(callback);
            brokerMap.replace(message.getName(), cbList);
        }
    }

    @Override
    public void publish(JsonRpcEndpoint endpoint, RpcMessage message) {
        List <RpcCallback> cbList = brokerMap.get(message.getName());
        if (cbList == null) {
            System.out.println("Unhandled Message name is " + message.getName());
            return;
        }

        for (RpcCallback cb : cbList ) {
            cb.callback(endpoint, message);
        }
    }

    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage message) {

        IdentityResponse.Result result = new IdentityResponse.Result();

        List<IdentityResponse.Peer> peers =
                new ArrayList<IdentityResponse.Peer>();

        IdentityResponse response = new IdentityResponse();

        /*
         * We find our role by matching the parent Channel (couldn't
         * come up with an easier way to do this, as we're trying to
         * match against the configured identity -- decided against
         * using the channel's connection b/c things like wildcard
         * addresses make this comparison tricky). There's also a
         * minute possibility that the parent socket has been deleted
         * (e.g. due to reconfiguration) in which case, the peers list
         * will provide the updated information.
         */
        OpflexRpcServer srv = null;
        List<String> myRoles = new ArrayList<String>();
        List<OpflexRpcServer> servers =
                new ArrayList<OpflexRpcServer>(opflexServers.values());
        for (OpflexRpcServer server : servers) {
            if (server.getRpcServer().getChannel() == endpoint.getChannel().parent()) {
                /* this is our server */
                List<Role> roles = server.getRoles();
                if (roles != null) {
                    for ( Role r : roles ) {
                        myRoles.add(r.toString());
                    }
                }
                srv = server;
                break;
            }
        }
        result.setMy_role(myRoles);

        /*
         * The peers field contains the identifiers other than my_role
         */
        for (OpflexRpcServer server : servers) {
            /* Skip our server -- reported in my_role */
            if ( Objects.equals(server.getId(), srv.getId()))
                continue;
            List<Role> roles = server.getRoles();
            if (roles != null) {
                for ( Role r : roles ) {
                    IdentityResponse.Peer peer = new IdentityResponse.Peer();
                    peer.setConnectivity_info(server.getId());
                    peer.setRole(r.toString());
                    peers.add(peer);
                }
            }
        }
        result.setPeers(peers);
        response.setResult(result);

        response.setId(message.getId());

        /*
         * Collect the set of severs and send in the response
         */
        try {
            endpoint.sendResponse(response);
        }
        catch ( Throwable e ) {
            logger.error("Throwable for sending {}, {}", message, e);
        }
    }

    @Override
    public void addConnection(JsonRpcEndpoint endpoint) {
        List<Role> roles = new ArrayList<Role>();
        OpflexConnection agent = new OpflexConnection();
        agent.setEndpoint(endpoint);
        agent.setIdentity(endpoint.getIdentifier());

        if (endpoint.supportsMessages(policyRepositoryMessages)) {
            roles.add(Role.POLICY_REPOSITORY);
        }
        if (endpoint.supportsMessages(endpointRegistryMessages)) {
            roles.add(Role.ENDPOINT_REGISTRY);
        }
        if (endpoint.supportsMessages(observerMessages)) {
            roles.add(Role.OBSERVER);
        }
        agent.setRoles(roles);
        logger.warn("Adding agent {}", endpoint.getIdentifier());
        opflexAgents.put(endpoint.getIdentifier(), agent);
    }

    @Override
    public void channelClosed(JsonRpcEndpoint peer) throws Exception {
        logger.info("Connection to Node : {} closed", peer.getIdentifier());
        opflexAgents.remove(peer.getIdentifier());
    }

}
