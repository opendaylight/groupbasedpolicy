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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.ConnectionService;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.discovery.definitions.PolicyRepository;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
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
 * @author tbachman
 *
 * TODO: Still too big - need to separate
 */
public class OpflexConnectionService
    implements ConnectionService, RpcBroker,
                RpcBroker.RpcCallback, DataChangeListener, AutoCloseable {
    protected static final Logger logger =
            LoggerFactory.getLogger(OpflexConnectionService.class);


    static final String OPFLEX_DOMAIN = "default";
    static final String INVALID_DOMAIN = "Domain mismatch";
    // Properties that can be set in config.ini
    static final String OPFLEX_LISTENPORT = "opflex.listenPort";
    private static final Integer defaultOpflexPort = 6670;
    static final String OPFLEX_LISTENIP = "opflex.listenIp";
    private static final String defaultOpflexIp = "0.0.0.0";

    private Integer opflexListenPort = defaultOpflexPort;
    private String opflexListenIp = defaultOpflexIp;

    private final ScheduledExecutorService executor;

    String domain;
    ConcurrentMap<String, OpflexAgent> opflexAgents = null;
    ConcurrentMap<String, OpflexRpcServer> opflexServers = null;

    ConcurrentMap<String, List<RpcCallback>> brokerMap = null;

    DiscoveryDefinitions currentIdentities;
    private DataBroker dataProvider;

    public static final InstanceIdentifier<DiscoveryDefinitions> DISCOVERY_IID =
            InstanceIdentifier.builder(DiscoveryDefinitions.class).build();

    public OpflexConnectionService() {
        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);
    }

    /**
     *
     * Set the data store provider for the OpFlex connection service, and
     * start the service using this data store.
     *
     * @param salDataProvider The MD-SAl data store provider
     */
    public void setDataProvider(DataBroker salDataProvider) {
        dataProvider = salDataProvider;

        start();
    }

    List<OpflexRpcServer> setDefaultIdentities() {

        /*
         * Create a single server, filling all roles
         */
        String identity = opflexListenIp + ":" + opflexListenPort.toString();
        List<OpflexRpcServer> srvList = new ArrayList<OpflexRpcServer>();
        List<Role> roles = new ArrayList<Role>();
        roles.add(Role.POLICY_REPOSITORY);
        roles.add(Role.ENDPOINT_REGISTRY);
        roles.add(Role.OBSERVER);

        OpflexRpcServer srv = new OpflexRpcServer(domain, identity, roles);
        srv.setConnectionService(this);
        srv.setRpcBroker(this);
        srvList.add(srv);
        return srvList;

    }

    private List<OpflexRpcServer> createServerList(DiscoveryDefinitions identities) {

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
        return null;
    }

    private void initializeServers() {

        readConfig();
        /*
         * Get the configured identities, if any. If lists are empty,
         * set up a single instance of each, using the default
         * interface, all inside a default domain
         */
        List<OpflexRpcServer> serverList = createServerList(currentIdentities);
        if (serverList != null && serverList.size() > 0) {
            addServers(serverList);
        }
        else {
            // TODO: should also write into config store?
            logger.info("Setting default identities");
            domain = OPFLEX_DOMAIN;
            addServers(setDefaultIdentities());
        }
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

    private void addServerList(Map<String, OpflexRpcServer> servers,
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
            srv = new OpflexRpcServer(domain, id, roles);
            srv.setConnectionService(this);
            srv.setRpcBroker(this);
            servers.put(id, srv);
        }

    }



    /**
     * We store the {@link OpflexDomain} in the {@link JsonRpcEndpoint}'s
     * context field when the {@link RpcServer} creates the new connection.
     *
     * @param endpoint The endpoint to look up
     * @return The OpflexDomain that owns this endpoint
     *
     * TODO: should throw an exception of there is no
     * OpflexDomain that contains this endpoint
     */
    public String getOpflexDomain(JsonRpcEndpoint endpoint) {
        if (endpoint.getContext() instanceof OpflexRpcServer) {
            OpflexRpcServer srv = (OpflexRpcServer)endpoint.getContext();
            return srv.getDomain();
        }
        logger.warn("endpoint {} does not have a domain", endpoint.getIdentifier());
        return null;
    }

    /**
     * Find the {@link OpflexAgent} that owns this
     * {@link JsonRpcEndpoint}.
     *
     * @param endpoint The endpoint to look up
     * @return The OpflexConnection that owns this endpoint
     *
     * TODO: should throw an exception of there is no
     * OpflexConnection that contains this endpoint
     */
    public OpflexAgent getOpflexConnection(JsonRpcEndpoint endpoint) {

        return getOpflexAgent(endpoint.getIdentifier());
    }

    /**
     * Get the OpflexRpcServer that spawned this endpoint.
     *
     * @param endpoint The endpoint to look up
     * @return The OpflexRpcServer that owns this endpoint, or
     * null if the server no longer exists
     *
     * TODO: exception if the endpoint is owned by anything
     */
    public OpflexRpcServer getOpflexServer(JsonRpcEndpoint endpoint) {
        if (endpoint.getContext() instanceof OpflexRpcServer) {
            return (OpflexRpcServer)endpoint.getContext();
        }
        logger.warn("Couldn't find OpflexConnection for endpoint {}",
                endpoint.getIdentifier());
        return null;
    }


    /**
     * Start the {@link OpflexConnectionService}
     */
    public void start() {
        opflexAgents = new ConcurrentHashMap<String, OpflexAgent>();
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

        initializeServers();
    }

    /**
     * Stop the OpFlex Connection Service. This shuts down all active
     * connections and servers.
     */
    public void stopping() {
        cleanup();
    }


    public ConcurrentMap<String, OpflexAgent> getOpflexAgents() {
        return opflexAgents;
    }

    public void setOpflexAgents(
            ConcurrentMap<String, OpflexAgent> opflexAgents) {
        this.opflexAgents = opflexAgents;
    }

    public ConcurrentMap<String, OpflexRpcServer> getOpflexServers() {
        return opflexServers;
    }

    public void setOpflexServers(
            ConcurrentMap<String, OpflexRpcServer> opflexServers) {
        this.opflexServers = opflexServers;
    }

    public void removeOpflexAgent(OpflexAgent agent) {
        opflexAgents.remove(agent.getIdentity());
    }

    public void removeOpflexServer(OpflexRpcServer server) {
        opflexServers.remove(server.getId());
    }

    public List<OpflexRpcServer> getOpflexServerList() {
        return new ArrayList<OpflexRpcServer>(opflexServers.values());
    }

    /**
     * Clean up all the entities contained by this domain. The
     * connection service also owns these references, so we
     * provide notifications to the connection service so that
     * it can clean up as well.
     */
    public void cleanup() {
        List<String> agents = new ArrayList<String>(opflexAgents.keySet());
        List<String> servers = new ArrayList<String>(opflexServers.keySet());
        for (String agent : agents) {
            OpflexAgent conn = opflexAgents.remove(agent);
            conn.getEndpoint().getChannel().disconnect();
        }
        for (String srv : servers) {
            OpflexRpcServer server = opflexServers.get(srv);
            if (server.getRpcServer().getChannel() != null) {
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    /**
     * Add an {@link OpflexAgent} to the domain
     *
     * @param agent The agent to add
     */
    public void addOpflexAgent(OpflexAgent agent) {
        opflexAgents.put(agent.getIdentity(), agent);
    }

    /**
     * Return the {@link OpflexAgent} associated
     * with this identity
     *
     * @param identity A string representing the connections identity
     * @return The connection represented by that key, or null if not found
     */
    public OpflexAgent getOpflexAgent(String identity) {
        return opflexAgents.get(identity);
    }

    /**
     * Add the List of servers to the domain
     *
     * @param serverList List of new servers to start
     */
    public void addServers(List<OpflexRpcServer> serverList) {

        if (serverList == null) return;

        /*
         * Check to see if there's already a server
         * with this identity, and if so, close it
         * and replace it with this one.
         */
        for ( OpflexRpcServer srv: serverList ) {
            OpflexRpcServer server = opflexServers.get(srv.getId());
            if (server != null) {
                if ( !server.sameServer(srv)) {
                    OpflexRpcServer oldServer = opflexServers.remove(srv.getId());
                    oldServer.getRpcServer().getChannel().disconnect();
                    opflexServers.put(srv.getId(), srv);
                    srv.start();
                }
            }
            else {
                opflexServers.put(srv.getId(), srv);
                srv.start();
            }
        }
    }

    /**
     * Drop the list of servers from the domain
     *
     * @param oldServers The list of servers to drop
     *
     * TODO: Should we provide notifications to or close
     *       the connections that were spawned by the
     *       deleted servers?
     */
    public void dropServers(List<String> oldServers) {
        OpflexRpcServer server;

        /*
         * Check to see if there's a server
         * with this identity, and if so, close it
         */
        for (String srv: oldServers) {
            if (opflexServers.containsKey(srv)) {
                server = opflexServers.remove(srv);
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    /**
     * Check the new configuration of the servers against the
     * existing, and if different, delete the old server and
     * replace it with a new server running the updated parameters.
     *
     * @param serverList The new server configurations
     */
    public void updateServers(List<OpflexRpcServer> serverList) {
        /* Get the new list of configured servers in this domain */
        List<OpflexRpcServer> updateServers = new ArrayList<OpflexRpcServer>();
        List<OpflexRpcServer> newServers = new ArrayList<OpflexRpcServer>();
        List<String> newList = new ArrayList<String>();

        for (OpflexRpcServer srv : serverList) {
            newList.add(srv.getId());
        }

        /* Get the list of currently configured servers in this domain*/
        List<String> currentList =
                new ArrayList<String>(opflexServers.keySet());

        /* Make the add/drop/update lists */
        List<String> addList = new ArrayList<String>(newList);
        List<String> dropList = new ArrayList<String>(currentList);
        List<String> updateList = new ArrayList<String>(newList);

        addList.removeAll(currentList);
        dropList.removeAll(newList);
        updateList.removeAll(addList);

        /*
         * Create add and update lists
         */
        for (OpflexRpcServer srv: serverList) {
            if (updateList.contains(srv.getId())) {
                updateServers.add(srv);
            }
            if (addList.contains(srv.getId())) {
                newServers.add(srv);
            }
        }


        dropServers(dropList);
        addServers(newServers);
        addServers(updateServers);
    }

     private void readConfig() {
         ListenableFuture<Optional<DiscoveryDefinitions>> dao =
                 dataProvider.newReadOnlyTransaction()
                     .read(LogicalDatastoreType.CONFIGURATION, DISCOVERY_IID);
         Futures.addCallback(dao, new FutureCallback<Optional<DiscoveryDefinitions>>() {
             @Override
             public void onSuccess(final Optional<DiscoveryDefinitions> result) {
                 if (!result.isPresent()) {
                     dropServers(new ArrayList<String>(opflexServers.keySet()));
                     addServers(setDefaultIdentities());             
                     return;
                 }
                 getNewConfig(result);
             }

             @Override
             public void onFailure(Throwable t) {
                 logger.error("Failed to read configuration", t);
             }
         }, executor);
     }

     void getNewConfig(final Optional<DiscoveryDefinitions> result) {
         /*
          * Get the new list of discovery definitions from the
          * configuration store, and convert to a list for manipulation
          */
         currentIdentities = result.get();
         if (currentIdentities == null) {
             dropServers(new ArrayList<String>(opflexServers.keySet()));             
             addServers(setDefaultIdentities());             
         }
         else {
             updateServers(createServerList(currentIdentities));
         }
     }

    @Override
    public void onDataChanged( final AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject>change) {

        readConfig();
    }
    
    /**
     * Close the connection service. Implemented from the
     * AutoCloseable interface.
     */
     @Override
     public void close() throws ExecutionException, InterruptedException {

         stopping();
         executor.shutdownNow();

         if (dataProvider != null) {
             WriteTransaction t = dataProvider.newWriteOnlyTransaction();
             t.delete(LogicalDatastoreType.CONFIGURATION, DISCOVERY_IID);
             t.submit().get();
         }
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

    /**
     * This notification handles the OpFlex Identity request messages.
     * 
     * TODO: implement Identity Response messages
     */
    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage message) {

        if (!(message instanceof IdentityRequest)) {
            logger.warn("message is not identity request {}", message);
            return;
        }
        OpflexRpcServer srv = getOpflexServer(endpoint);
        if (srv == null) return;

        IdentityRequest request = (IdentityRequest)message;
        IdentityResponse.Result result = new IdentityResponse.Result();

        List<IdentityResponse.Peer> peers =
                new ArrayList<IdentityResponse.Peer>();

        IdentityResponse response = new IdentityResponse();

        /*
         *  We inherit our role from the server that spawned
         *  the connection.
         */
        List<String> myRoles = new ArrayList<String>();
        List<Role> roles = srv.getRoles();
        if (roles != null) {
            for ( Role r : roles ) {
                myRoles.add(r.toString());
            }
        }
        result.setMy_role(myRoles);

        /*
         * The peers field contains the identifiers other than my_role
         */
        if (request.getParams() == null || request.getParams().size() <= 0) {
            return;
        }
        if (request.getParams() == null ||
            request.getParams().get(0) == null ||
            !request.getParams().get(0).getDomain().equals(domain)) {
            IdentityResponse.Error error = new IdentityResponse.Error();
            error.setMessage(INVALID_DOMAIN);
            response.setError(error);
            /* send domain mismatch */
        }
        else {
            for (OpflexRpcServer server : getOpflexServerList()) {
                /* Skip our server -- reported in my_role */
                if ( Objects.equals(server.getId(), srv.getId()))
                    continue;
                roles = server.getRoles();
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
            result.setName(srv.getId());
            result.setDomain(domain);
            response.setResult(result);
        }
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

    /**
     * This is the notification when a new endpoint
     * has been created. Since the endpoint is new,
     * we don't have a OpflexConnection for it yet. We
     * create the OpflexConnection, then retrieve the
     * OpflexRpcServer that created this connections
     * to inherit some of the fields we need (domain, server).
     */
    @Override
    public void addConnection(JsonRpcEndpoint endpoint) {

        /*
         * When the connection is added, we only have the 
         * JsonRpcEndpoint. We use the JsonRpcEndpoint's 
         * context field to store the server object that created this
         * connection, and can look up things like the domain,
         * etc. to create the containing connection object.
         */
        if (!(endpoint.getContext() instanceof OpflexRpcServer)) {
            logger.error("Connection for endpoint {} invalid",
                    endpoint.getIdentifier());
            // TODO: close connection?
            return;
        }

        OpflexRpcServer server = (OpflexRpcServer)endpoint.getContext();

        /*
         * The OpFlex domain is the same as the server 
         * that the agent connected to. Look up the OpFlex RPC
         * server using the server socket.
         *
         * It's possible that the server was closed or changed
         * between the connection establishment and now (race
         * condition). Treat that as a failure, closing the
         * connection.
         */
        OpflexAgent oc = new OpflexAgent();
        oc.setEndpoint(endpoint);
        oc.setIdentity(endpoint.getIdentifier());
        oc.setDomain(domain);
        oc.setOpflexServer(server);
        oc.setRoles(server.getRoles());

        logger.info("Adding agent {}", endpoint.getIdentifier());
        addOpflexAgent(oc);
        
        /*
         * Send an Identity Request
         */
        IdentityRequest ourId = new IdentityRequest();
        IdentityRequest.Params params = new IdentityRequest.Params();
        List<IdentityRequest.Params> paramList = new ArrayList<IdentityRequest.Params>();
        List<String> myRoles = new ArrayList<String>();
        List<Role> roles = server.getRoles();
        if (roles != null) {
            for ( Role r : roles ) {
                myRoles.add(r.toString());
            }
        }
        params.setMy_role(myRoles);
        params.setDomain(server.getDomain());
        params.setName(server.getId());
        paramList.add(params);
        ourId.setParams(paramList);

        try {
            endpoint.sendRequest(ourId);
        }
        catch (Throwable t) {
        }        
    }

    /**
     * This is the notification we receive when a connection 
     * is closed. Retrieve the domain from the {@link JsonRpcEndpoint}'s
     * context field to get the {@link OpflexRpcServer}, which contains
     * the OpFlex domain for this connection, then use the identity from 
     * the {@link JsonRpcEndpoint} and domain to remove the {@link OpflexAgent} 
     * from the domain
     */
    @Override
    public void channelClosed(JsonRpcEndpoint endpoint) throws Exception {
        logger.info("Connection to Node : {} closed", endpoint.getIdentifier());
        OpflexAgent agent = getOpflexConnection(endpoint);
        if (agent != null) {
            removeOpflexAgent(agent);
        }
    }

}
