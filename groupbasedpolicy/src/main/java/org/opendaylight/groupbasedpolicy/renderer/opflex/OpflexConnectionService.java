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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.Domains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.Domain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.DiscoveryDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.EndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.Observer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.domains.domain.discovery.definitions.PolicyRepository;
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

    List<Domain> domainList = null;
    ConcurrentMap<String, OpflexDomain> opflexDomains = null;
    ConcurrentMap<String, List<RpcCallback>> brokerMap = null;

    private DataBroker dataProvider;

    public static final InstanceIdentifier<Domains> DOMAINS_IID =
            InstanceIdentifier.builder(Domains.class).build();

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

    private List<OpflexRpcServer> setDefaultIdentities(OpflexDomain od) {

        /*
         * Create a single server, filling all roles
         */
        String identity = opflexListenIp + ":" + opflexListenPort.toString();
        List<OpflexRpcServer> srvList = new ArrayList<OpflexRpcServer>();
        List<Role> roles = new ArrayList<Role>();
        roles.add(Role.POLICY_REPOSITORY);
        roles.add(Role.ENDPOINT_REGISTRY);
        roles.add(Role.OBSERVER);

        OpflexRpcServer srv = new OpflexRpcServer(od, identity, roles);
        srv.setConnectionService(this);
        srv.setRpcBroker(this);
        srvList.add(srv);
        return srvList;

    }

    private List<OpflexRpcServer> createServerList(OpflexDomain d, Domain domain) {

        DiscoveryDefinitions identities = domain.getDiscoveryDefinitions();
        if (identities != null) {
            Map<String, OpflexRpcServer> servers =
                    new ConcurrentHashMap<String, OpflexRpcServer>();
            List<String> addList = getPolicyRepositories(identities.getPolicyRepository());
            addServerList(d, servers, addList, Role.POLICY_REPOSITORY);
            addList = getEndpointRegistries(identities.getEndpointRegistry());
            addServerList(d, servers, addList, Role.ENDPOINT_REGISTRY);
            addList = getObservers(identities.getObserver());
            addServerList(d, servers, addList, Role.OBSERVER);
            return(new ArrayList<OpflexRpcServer>(servers.values()));
        }
        return null;
    }

    private void initializeServers() {

        OpflexDomain od;

        readConfig();
        /*
         * Get the configured identities, if any. If lists are empty,
         * set up a single instance of each, using the localhost
         * interface, all inside a default domain
         */
        if (domainList != null && domainList.size() > 0) {
            for (Domain d : domainList) {
                od = opflexDomains.get(d.getId());
                if (od == null) continue;
                List<OpflexRpcServer> serverList = createServerList(od, d);
                od.addServers(serverList);
            }
        }
        else {
            // TODO: should also write into config store?
            logger.info("Setting default identities");
            od = new OpflexDomain();
            od.setDomain(OPFLEX_DOMAIN);
            od.addServers(setDefaultIdentities(od));
            opflexDomains.put(od.getDomain(), od);
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

    private void addServerList( OpflexDomain d, Map<String, OpflexRpcServer> servers,
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
            srv = new OpflexRpcServer(d, id, roles);
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
    public OpflexDomain getOpflexDomain(JsonRpcEndpoint endpoint) {
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

        OpflexDomain od = getOpflexDomain(endpoint);
        if (od != null) {
            return od.getOpflexAgent(endpoint.getIdentifier());
        }
        logger.warn("Couldn't find OpflexConnection for {}", endpoint.getIdentifier());
        return null;

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
        opflexDomains = new ConcurrentHashMap<String, OpflexDomain>();
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
        /* this class implements identity handlers */
        subscribe(new IdentityRequest(), this);

        initializeServers();
    }

    /**
     * Stop the OpFlex Connection Service. This shuts down all active
     * connections and servers.
     */
    public void stopping() {
        for (OpflexDomain d : opflexDomains.values()) {
            d.cleanup();
        }
    }


    private void deleteDomain(String domain) {
        OpflexDomain od = opflexDomains.remove(domain);
        if (od != null) {
            od.cleanup();
        }
    }

     private void readConfig() {
         ListenableFuture<Optional<Domains>> dao =
                 dataProvider.newReadOnlyTransaction()
                     .read(LogicalDatastoreType.CONFIGURATION, DOMAINS_IID);
         Futures.addCallback(dao, new FutureCallback<Optional<Domains>>() {
             @Override
             public void onSuccess(final Optional<Domains> result) {
                 if (!result.isPresent()) {
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

     void getNewConfig(final Optional<Domains> result) {

         List<String> currentDomains = new ArrayList<String>(opflexDomains.keySet());
         List<String> newDomains = new ArrayList<String>();


         /*
          * Get the new list of domains from the
          * configuration store, and convert to a
          * list of the actual domain names for list
          * manipulation
          */
         Domains domains = result.get();

         domainList = domains.getDomain();
         for (Domain domainObj : domainList) {
             newDomains.add(domainObj.getId());
         }

         /*
          * Find out what's changed at the domain level.
          * Classify as additions, deletions, and updates
          */
         List<String> addList = new ArrayList<String>(newDomains);
         List <String> dropList = new ArrayList<String>(currentDomains);
         List <String> updateList = new ArrayList<String>(newDomains);
         addList.removeAll(currentDomains);
         dropList.removeAll(newDomains);
         updateList.removeAll(addList);

         /*
          * Drop domains that were removed, along with all
          * of their servers and connections
          */
         for (String d : dropList) {
             deleteDomain(d);
         }

         /*
          * These are entirely new domains -- get the
          * information for each new domain and configure
          */
         for (String d : addList) {
             OpflexDomain od = new OpflexDomain();
             od.setDomain(d);
             opflexDomains.put(od.getDomain(), od );

             /* Spawn the servers for this domain */
             for (Domain dl : domainList) {
                 if (dl.getId().equals(d)) {
                     od.addServers(createServerList(od, dl));
                     break;
                 }
             }
         }

         /*
          * These are domains with updates
          */
         for (String d : updateList) {
             OpflexDomain od = opflexDomains.get(d);
             for (Domain domainObj : domainList) {
                 if (domainObj.getId().equals(d)) {
                     od.updateServers(createServerList(od, domainObj));
                     break;
                 }
             }
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

         executor.shutdownNow();

         if (dataProvider != null) {
             WriteTransaction t = dataProvider.newWriteOnlyTransaction();
             t.delete(LogicalDatastoreType.CONFIGURATION, DOMAINS_IID);
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
        OpflexDomain od = getOpflexDomain(endpoint);
        if (request.getParams() == null || request.getParams().size() <= 0) {
            return;
        }
        if (request.getParams() == null ||
            request.getParams().get(0) == null ||
            !request.getParams().get(0).getDomain().equals(od.getDomain())) {
            IdentityResponse.Error error = new IdentityResponse.Error();
            error.setMessage(INVALID_DOMAIN);
            response.setError(error);
            /* send domain mismatch */
        }
        else {
            for (OpflexRpcServer server : od.getOpflexServerList()) {
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
            result.setDomain(od.getDomain());
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
        OpflexDomain domain = server.getDomain();

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
        oc.setDomain(domain.getDomain());
        oc.setOpflexServer(server);
        oc.setRoles(server.getRoles());

        logger.info("Adding agent {}", endpoint.getIdentifier());
        domain.addOpflexAgent(oc);
        
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
        params.setDomain(server.getDomain().getDomain());
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
            OpflexDomain od = opflexDomains.get(agent.getDomain());
            if (od != null) {
                od.removeOpflexAgent(agent);
            }
        }
    }

}
