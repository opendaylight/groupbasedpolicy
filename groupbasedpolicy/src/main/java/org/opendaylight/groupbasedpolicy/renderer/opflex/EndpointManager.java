/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.AbstractEndpointRegistry;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Keep track of endpoints on the system.  Maintain an index of endpoints
 * and their locations for queries from agents.  The endpoint manager will maintain
 * appropriate indexes only for agents that are attached to the current
 * controller node.
 * 
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular agent and also all the agents containing 
 * each particular endpoint group
 * @author tbachman
 */
public class EndpointManager 
        extends AbstractEndpointRegistry 
        implements AutoCloseable, DataChangeListener, 
        RpcBroker.RpcCallback, 
        L2EprContext.Callback, L3EprContext.Callback {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(EndpointManager.class);
    
    private static final InstanceIdentifier<Endpoint> endpointsIid = 
            InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class).build();

    // TODO: hacks for now :(
    private static final String NO_ENDPOINTS = "No endpoints found.";
    private static final int DEFAULT_PRR = 1000;
    
    final ListenerRegistration<DataChangeListener> listenerReg;
    
    private OpflexConnectionService connectionService;
    final ConcurrentHashMap<EpKey, Endpoint> endpoints =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, Set<EpKey>> endpointsByNode =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup = 
            new ConcurrentHashMap<>();
    private RpcMessageMap messageMap = null;      

    Set<L2EprContext> l2RpcCtxts = 
            Collections.newSetFromMap(new ConcurrentHashMap<L2EprContext, Boolean>());            
    Set<L3EprContext> l3RpcCtxts = 
            Collections.newSetFromMap(new ConcurrentHashMap<L3EprContext, Boolean>());            
    
    private List<EndpointListener> listeners = new CopyOnWriteArrayList<>();

    public EndpointManager(DataBroker dataProvider,
                           RpcProviderRegistry rpcRegistry,
                           ScheduledExecutorService executor,
                           OpflexConnectionService connectionService) {
        super(dataProvider, rpcRegistry, executor);
        
        if (dataProvider != null) {
            listenerReg = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, 
                                                endpointsIid, 
                                                this, 
                                                DataChangeScope.ONE);
        } else
            listenerReg = null;

        this.connectionService = connectionService;

        /* Subscribe to EPR messages */
        messageMap = new RpcMessageMap();
        List<RpcMessage> messages = Role.ENDPOINT_REGISTRY.getMessages();
        messageMap.addList(messages);
        for (RpcMessage msg: messages) {
            this.connectionService.subscribe(msg, this);
        }
        LOG.warn("Initialized OpFlex endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Add a {@link EndpointListener} to get notifications of switch events
     * @param listener the {@link EndpointListener} to add
     */
    public void registerListener(EndpointListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Get a collection of endpoints attached to a particular switch
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    public Collection<Endpoint> getEndpointsForNode(NodeId nodeId) {
        Collection<EpKey> ebn = endpointsByNode.get(nodeId);
        if (ebn == null) return Collections.emptyList();
        return Collections2.transform(ebn, indexTransform);
    }

    /**
     * Get the endpoint object for the given key
     * @param epKey the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }

    // ************************
    // AbstractEndpointRegistry
    // ************************
    
    @Override
    protected EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        // TODO: implement
        return null;
    }

    @Override
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        return super.buildEndpointL3(input);
    }
    
    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null) listenerReg.close();
        super.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint)
                updateEndpoint(null, (Endpoint)dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint((Endpoint)old, null);
        }
        Map<InstanceIdentifier<?>,DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if (!(entry.getValue() instanceof Endpoint)) continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            Endpoint oldEp = null;
            if (old != null && old instanceof Endpoint)
                oldEp = (Endpoint)old;
            updateEndpoint(oldEp, (Endpoint)entry.getValue());
        }
    }
    // **************
    // Implementation
    // **************

    private void notifyEndpointUpdated(EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.endpointUpdated(epKey);
        }
    }

    private void notifyNodeEndpointUpdated(NodeId nodeId, EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.nodeEndpointUpdated(nodeId, epKey);
        }
    }

    private void notifyGroupEndpointUpdated(EgKey egKey, EpKey epKey) {
        for (EndpointListener l : listeners) {
            l.groupEndpointUpdated(egKey, epKey);
        }
    }

    private Function<EpKey, Endpoint> indexTransform = 
            new Function<EpKey, Endpoint>() {
        @Override
        public Endpoint apply(EpKey input) {
            return endpoints.get(input);
        }
    };
    
    private boolean validEp(Endpoint endpoint) {
        return (endpoint != null && endpoint.getTenant() != null && 
                endpoint.getEndpointGroup() != null &&
                endpoint.getL2Context() != null &&
                endpoint.getMacAddress() != null);
    }
    
    private NodeId getLocation(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;

        // TODO: implement

        return null;
    }
    
    private EpKey getEpKey(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;
        return new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
    }
    
    private EgKey getEgKey(Endpoint endpoint) {
        if (!validEp(endpoint)) 
            return null;
        return new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup());
    }
    
    private Set<EpKey> getEpNSet(NodeId location) {
        return SetUtils.getNestedSet(location, endpointsByNode);
    }

    private Set<EpKey> getEpGSet(EgKey eg) {
        return SetUtils.getNestedSet(eg, endpointsByGroup);
    }
    
    /**
     * Update the endpoint indexes.  Set newEp to null to remove.
     */
    protected void updateEndpoint(Endpoint oldEp, Endpoint newEp) {
        // XXX TODO only keep track of endpoints that are attached 
        // to switches that are actually connected to us
        NodeId oldLoc = getLocation(oldEp);
        NodeId newLoc = getLocation(newEp);

        EgKey oldKey = getEgKey(oldEp);
        EgKey newKey = getEgKey(newEp);

        EpKey epKey = getEpKey(oldEp);
        if (epKey == null) epKey = getEpKey(newEp);
        if (epKey == null) return;

        boolean notifyOldLoc = false;
        boolean notifyNewLoc = false;
        boolean notifyOldEg = false;
        boolean notifyNewEg = false;
        
        if (newEp != null)
            endpoints.put(epKey, newEp);

        if (oldLoc != null && 
            (newLoc == null || !oldLoc.equals(newLoc))) {
            Set<EpKey> eps = getEpNSet(oldLoc);
            eps.remove(epKey);
            notifyOldLoc = true;
        }
        if (oldKey != null &&
            (newKey == null || !oldKey.equals(newKey))) {
            Set<EpKey> gns = getEpGSet(oldKey);
            gns.remove(epKey);
            notifyOldEg = true;
        }

        if (newLoc != null) {
            Set<EpKey> eps = getEpNSet(newLoc);
            eps.add(epKey);
            LOG.debug("Endpoint {} added to node {}", epKey, newLoc);
            notifyNewLoc = true;
        }
        if (newKey != null) {
            Set<EpKey> gns = getEpGSet(newKey);
            gns.add(epKey);
            LOG.debug("Endpoint {} added to group {}", epKey, newKey);
            notifyNewEg = true;
        }

        if (newEp == null)
            endpoints.remove(epKey);
        
        notifyEndpointUpdated(epKey);

        if (notifyOldLoc)
            notifyNodeEndpointUpdated(oldLoc,epKey);
        if (notifyNewLoc)
            notifyNodeEndpointUpdated(newLoc,epKey);
        if (notifyOldEg)
            notifyGroupEndpointUpdated(oldKey, epKey);
        if (notifyNewEg)
            notifyGroupEndpointUpdated(newKey, epKey);
    }
    
    /**
     * This notification handles the OpFlex Endpoint messages.
     * We should only receive quest messages. Responses are
     * sent in a different context, as all requests result 
     * in a Future to access the data store.
     * 
     * @param endpoint The JsonRpcEndpoint that received the request
     * @param request The request message from the endpoint
     */
    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage request) {

        RpcMessage response = null;
        if (messageMap.get(request.getMethod()) == null) {
            LOG.warn("message {} was not subscribed to, but was delivered.", request);
            return;
        }
        if (request instanceof IdentityRequest) {
            connectionService.callback(endpoint, request);
        }                
        /*
         * For declaration requests, we need to make sure that this
         * EP is in our registry. Since we can have multiple identifiers,
\        * we create a Set of endpoints.
         */
        
        if (request instanceof EndpointDeclarationRequest) {
            EndpointDeclarationRequest req = (EndpointDeclarationRequest)request;
            EndpointDeclarationResponse msg = new EndpointDeclarationResponse();
            msg.setId(request.getId());
            response = msg;
            
            if (!req.valid() ||
                (req.getParams().get(0).getIdentifier() == null) ||
                (req.getParams().get(0).getIdentifier().size() <= 0)) {
                LOG.warn("Invalid declaration request: {}", req);
                // TODO: should return error reply?
                return;
            }
            EndpointDeclarationRequest.Params params = req.getParams().get(0);
            
            /*
             * Use the first identifier to determine the type of 
             * identifier being passed to us, so we can install the
             * EP into the appropriate EPR list
             */
            Identity id = 
                    new Identity(req.getParams().get(0).getIdentifier().get(0));
            if (id.isL2()) {
                L2EprContext ctx = 
                        new L2EprContext(endpoint, request, 
                        params.getIdentifier().size(),
                        dataProvider, executor);
                ctx.setCallback(this);
                ctx.createL2Ep(req.getParams().get(0).getContext(), id);
            }
            else if (id.isL3()) {
                L3EprContext ctx = 
                        new L3EprContext(endpoint, request, 
                        params.getIdentifier().size(),
                        dataProvider, executor);                
                ctx.setCallback(this);
                ctx.createL3Ep(req.getParams().get(0).getContext(), 
                               req.getParams().get(0).getIdentifier(), id);
            }
        }
        else if (request instanceof EndpointRequestRequest) {
            EndpointRequestRequest req = (EndpointRequestRequest)request;

            /*
             * We query the EPR for the EP. This is an asynchronous
             * operation, so we send the response in the callback
             */
            if (req.valid()) {
                EndpointRequestRequest.Params params = req.getParams().get(0);

                for (String id: params.getIdentifier()) {
                    Identity i = new Identity(id);
                    
                    if (i.isL2()) {
                        L2EprContext ctx = 
                                new L2EprContext(endpoint, request, 
                                params.getIdentifier().size(),
                                dataProvider, executor);
                        this.l2RpcCtxts.add(ctx);
                        ctx.setCallback(this);
                        ctx.lookupEndpoint(params.getContext(), id);
                    }
                    else if (i.isL3()) {
                        L3EprContext ctx = 
                                new L3EprContext(endpoint, request, 
                                params.getIdentifier().size(),
                                dataProvider, executor);
                        this.l3RpcCtxts.add(ctx);                                                
                        ctx.setCallback(this);
                        ctx.lookupEndpoint(params.getContext(), id);                        
                    }
                }
            }
        }
        
        if (response != null) {
            try {
                endpoint.sendResponse(response);
            }
            catch (Throwable t) {
                LOG.warn("Response {} could not be sent to {}", response, endpoint);
            }
        }
    }

    /**
     * This notification handles the callback from a query 
     * of the L2 Endpoint Registry
     */
    @Override
    public void callback(L2EprContext ctx) {
        if (!(ctx.getRequest() instanceof EndpointRequestRequest)) {
            return;
        }
        EndpointRequestRequest req = 
                (EndpointRequestRequest)ctx.getRequest();
        EndpointRequestResponse response = new EndpointRequestResponse();
        EndpointRequestResponse.Result result = 
                new EndpointRequestResponse.Result();
        EndpointRequestResponse.Endpoint endpoint = 
                new EndpointRequestResponse.Endpoint();
        List<EndpointRequestResponse.Endpoint> epList = 
                new ArrayList<EndpointRequestResponse.Endpoint>();
        
        /*
         * If we didn't find any EPs, send the 
         * error response
         */
        if ((ctx.getEps() == null) || (ctx.getEps().size() <= 0)) {
            EndpointRequestResponse.Error error = 
                    new EndpointRequestResponse.Error();
            error.setMessage(NO_ENDPOINTS);
            response.setError(error);
        }
        else {
            EndpointRequestRequest.Params params = req.getParams().get(0);

            /*
             * If we get any EP, then we can
             * provide a response to the original request
             * Note that we could potentially have multiple
             * requests outstanding for the same EP, and 
             * even using different context types (L2 or L3).
             */
            for (Endpoint e : ctx.getEps()) {
                List<String> ids = new ArrayList<String>();

                L2BridgeDomainId l2Context = 
                        e.getL2Context();
                if (l2Context != null && 
                        l2Context.getValue().equals(params.getContext())) {
                    ids.add(e.getMacAddress().getValue());
                    endpoint.setIdentifier(ids);
                    endpoint.setContext(l2Context.getValue());
                }
                /* TODO: Need to look this up in op store */
                //endpoint.setLocation("");
                //endpoint.setPolicy_name("");
                //endpoint.setStatus("");
                //endpoint.setSubject("");
                endpoint.setPrr(DEFAULT_PRR);
                epList.add(endpoint);
                /*
                 * For EPs on a different agent, we need to look up the 
                 * VTEP information. For now, we're only supporting 
                 * VXLAN VTEPs, so we look up the destination tunnel IP,
                 * and provide that in the data field of the response
                 */
                // TODO: Need to look this up in op store
                //endpoint.setData();
            }
            result.setEndpoint(epList);
            response.setResult(result);
        }
        try {
            ctx.getEp().sendResponse(response);
        }
        catch (Throwable t) {
            // TODO: implement
        }
        this.l2RpcCtxts.remove(ctx);
    }

    /**
     * This notification handles the callback from a query 
     * of the L3 Endpoint Registry
     */
    
    @Override
    public void callback(L3EprContext ctx) {        
        if (!(ctx.getRequest() instanceof EndpointRequestRequest)) {
            return;
        }
        EndpointRequestRequest req = 
                (EndpointRequestRequest)ctx.getRequest();
        EndpointRequestResponse response = new EndpointRequestResponse();
        response.setId(ctx.getRequest().getId());
        EndpointRequestResponse.Result result = 
                new EndpointRequestResponse.Result();
        EndpointRequestResponse.Endpoint endpoint = 
                new EndpointRequestResponse.Endpoint();
        List<EndpointRequestResponse.Endpoint> epList = 
                new ArrayList<EndpointRequestResponse.Endpoint>();
        
        /*
         * If we didn't find any EPs, send the 
         * error response
         */
        if ((ctx.getEps() == null) || (ctx.getEps().size() <= 0)) {
            EndpointRequestResponse.Error error = 
                    new EndpointRequestResponse.Error();
            error.setMessage(NO_ENDPOINTS);
            response.setError(error);
        }
        else {
            EndpointRequestRequest.Params params = req.getParams().get(0);

            /*
             * If we get any EP, then we can
             * provide a response to the original request
             * Note that we could potentially have multiple
             * requests outstanding for the same EP, and 
             * even using different context types (L2 or L3).
             */
            for (EndpointL3 e : ctx.getEps()) {
                List<String> ids = new ArrayList<String>();

                String l3Context = "";
                
                /* 
                 * The OpFlex RFC indicates that a single 
                 * Endpoint Request can match on multiple
                 * Endpoints, as the identifiers may not
                 * be unique (e.g. multiple IP addresses). 
                 * However, GBP scopes the endpoint's 
                 * identifier with the L3 context, which
                 * means there will only be a single match.
                 * As a result, send the response once we
                 * get a single EP
                 */
                for (L3Address l3Addr : e.getL3Address()) {
                    if (l3Addr.getL3Context().getValue()
                            .equals(params.getContext())) {
                        if (l3Addr.getIpAddress().getIpv4Address() != null) { 
                        ids.add(l3Addr.
                                getIpAddress()
                                .getIpv4Address().getValue().toString());
                        }
                        else if (l3Addr.getIpAddress().getIpv6Address() != null) {
                            ids.add(l3Addr.getIpAddress().
                                    getIpv6Address().getValue().toString());
                        }
                        l3Context = l3Addr.getL3Context().getValue();
                    }
                }
                if (ids.size() > 0) {
                    endpoint.setIdentifier(ids);
                }
                endpoint.setContext(l3Context);
                /* TODO: get these from the op store */
                //endpoint.setLocation("");
                //endpoint.setPolicy_name("");
                //endpoint.setStatus("");
                //endpoint.setSubject("");
                endpoint.setPrr(DEFAULT_PRR);
                epList.add(endpoint);
                /*
                 * For EPs on a different agent, we need to look up the 
                 * VTEP information. For now, we're only supporting 
                 * VXLAN VTEPs, so we look up the destination tunnel IP,
                 * and provide that in the data field of the response
                 */
                // TODO: get this from the op store
                //endpoint.setData();
            }
            result.setEndpoint(epList);
            response.setResult(result);
        }
        try {
            ctx.getEp().sendResponse(response);
        }
        catch (Throwable t) {
            // TODO: implement
        }
        this.l3RpcCtxts.remove(ctx);
    }    
    
}