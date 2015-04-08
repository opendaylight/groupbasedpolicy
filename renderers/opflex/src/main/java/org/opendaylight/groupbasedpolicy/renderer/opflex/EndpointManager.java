/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexAgent;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexConnectionService;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.Role;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointIdentity;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointResolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUnresolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointResolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyUri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3Builder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep track of endpoints on the system. Maintain an index of endpoints and
 * their locations for queries from agents. The endpoint manager will maintain
 * appropriate indexes only for agents that are attached to the current
 * controller node. In order to render the policy, we need to be able to
 * efficiently enumerate all endpoints on a particular agent and also all the
 * agents containing each particular endpoint group
 *
 */
public class EndpointManager implements AutoCloseable, DataChangeListener, RpcBroker.RpcCallback,
        EprContext.EprCtxCallback {

    protected static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);

    private static final InstanceIdentifier<Endpoint> endpointsIid = InstanceIdentifier.builder(Endpoints.class)
            .child(Endpoint.class)
            .build();
    private static final InstanceIdentifier<EndpointL3> endpointsL3Iid = InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3.class)
            .build();

    final ListenerRegistration<DataChangeListener> listenerReg;
    final ListenerRegistration<DataChangeListener> listenerL3Reg;

    private final OpflexConnectionService connectionService;
    private final MitLib mitLibrary;

    final Map<EpKey, Endpoint> endpoints = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Set<String>> epSubscriptions = new ConcurrentHashMap<>();
    private RpcMessageMap messageMap = null;

    final private OfEndpointAug endpointRpcAug = new OfEndpointAug();

    final private ScheduledExecutorService executor;

    final private DataBroker dataProvider;

    public EndpointManager(DataBroker dataProvider, RpcProviderRegistry rpcRegistry, ScheduledExecutorService executor,
            OpflexConnectionService connectionService, MitLib opflexLibrary) {
        this.executor = executor;
        this.dataProvider = dataProvider;
        EndpointRpcRegistry.register(dataProvider, rpcRegistry, executor, endpointRpcAug);

        if (dataProvider != null) {
            listenerReg = dataProvider.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, endpointsIid, this,
                    DataChangeScope.ONE);
            listenerL3Reg = dataProvider.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, endpointsL3Iid,
                    this, DataChangeScope.ONE);
        } else {
            listenerReg = null;
            listenerL3Reg = null;
        }

        this.connectionService = connectionService;
        this.mitLibrary = opflexLibrary;

        /* Subscribe to EPR messages */
        messageMap = new RpcMessageMap();
        List<RpcMessage> messages = Role.ENDPOINT_REGISTRY.getMessages();
        messageMap.addList(messages);
        for (RpcMessage msg : messages) {
            this.connectionService.subscribe(msg, this);
        }
        LOG.trace("Initialized OpFlex endpoint manager");
    }

    /**
     * Shut down the {@link EndpointManager}
     */
    public void shutdown() {

    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Get the endpoint object for the given key
     *
     * @param epKey
     *            the key
     * @return the {@link Endpoint} corresponding to the key
     */
    public Endpoint getEndpoint(EpKey epKey) {
        return endpoints.get(epKey);
    }

    // ************************
    // Endpoint Augmentation
    // ************************
    private class OfEndpointAug implements EpRendererAugmentation {

        @Override
        public void buildEndpointAugmentation(EndpointBuilder eb, RegisterEndpointInput input) {
            OpflexOverlayContextInput ictx = input.getAugmentation(OpflexOverlayContextInput.class);
            eb.addAugmentation(OpflexOverlayContext.class, new OpflexOverlayContextBuilder(ictx).build());
        }

        @Override
        public void buildEndpointL3Augmentation(EndpointL3Builder eb, RegisterEndpointInput input) {
            OpflexOverlayContextInput ictx = input.getAugmentation(OpflexOverlayContextInput.class);
            eb.addAugmentation(OpflexOverlayContextL3.class, new OpflexOverlayContextL3Builder(ictx).build());
        }

        @Override
        public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
            // TODO These methods will be replaced by getAugmentation and
            // augmentation applied at caller.

        }
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null)
            listenerReg.close();
        EndpointRpcRegistry.unregister(endpointRpcAug);
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint)
                updateEndpoint(null, dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint(old, null);
        }
        Map<InstanceIdentifier<?>, DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if ((!(entry.getValue() instanceof Endpoint)) && (!(entry.getValue() instanceof EndpointL3)))
                continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            DataObject oldEp = null;
            if (entry instanceof Endpoint || entry instanceof EndpointL3) {
                if (old != null && old instanceof Endpoint)
                    oldEp = old;
                updateEndpoint(oldEp, entry.getValue());
            }
        }
    }

    // **************
    // Implementation
    // **************

    private Identity getIdentity(DataObject obj) {
        Identity id = null;
        if (obj instanceof Endpoint) {
            Endpoint ep = (Endpoint) obj;
            id = new Identity(ep);
            id.setContext(ep.getL2Context().getValue());
        }

        if (obj instanceof EndpointL3) {
            EndpointL3 ep = (EndpointL3) obj;
            id = new Identity(ep);
            id.setContext(ep.getL3Context().getValue());
        }
        if (id != null && !id.valid()) {
            return null;
        }
        return id;
    }

    private synchronized Set<String> getEpSubscriptions(String id) {
        return epSubscriptions.get(id);
    }

    /**
     * Provide endpoint policy update messages based on changes
     */
    protected void updateEndpoint(DataObject oldEp, DataObject newEp) {
        Identity oldId = getIdentity(oldEp);
        Identity newId = getIdentity(newEp);
        /*
         * If an endpoint has changed, we need to provide notifications to
         * agents that have subscribed to that endpoint. Batch up the
         * notifications to be sent to the agents.
         */
        Queue<EndpointUpdate> updateQ = new ConcurrentLinkedQueue<EndpointUpdate>();

        /* This covers additions or updates */
        if (newId != null) {
            Set<String> agentList = getEpSubscriptions(newId.identityAsString());
            if (agentList != null) {
                for (String agentId : agentList) {
                    OpflexAgent agent = connectionService.getOpflexAgent(agentId);
                    if (agent != null) {
                        updateQ.add(new EndpointUpdate(EndpointUpdate.UpdateType.ADD_CHANGE, agent.getEndpoint(), newEp));
                    }
                }
            }
        }
        /* this covers deletions */
        if ((newId == null) && (oldId != null)) {
            Set<String> agentList = getEpSubscriptions(oldId.identityAsString());
            if (agentList != null) {
                for (String agentId : agentList) {
                    OpflexAgent agent = connectionService.getOpflexAgent(agentId);
                    if (agent != null) {
                        updateQ.add(new EndpointUpdate(EndpointUpdate.UpdateType.DELETE, agent.getEndpoint(), oldEp));
                    }
                }
            }
        }

        sendEpUpdates(updateQ);
    }

    private static class EndpointUpdate implements Runnable {

        public static enum UpdateType {
            ADD_CHANGE("add_change"), DELETE("delete");

            private final String updateType;

            UpdateType(String updateType) {
                this.updateType = updateType;
            }

            @Override
            public String toString() {
                return this.updateType;
            }
        }

        private final UpdateType type;
        private final JsonRpcEndpoint agent;
        private final ManagedObject mo;

        EndpointUpdate(UpdateType type, JsonRpcEndpoint agent, DataObject obj) {
            this.type = type;
            this.agent = agent;
            mo = MessageUtils.getMoFromEp(obj);
        }

        @Override
        public void run() {
            EndpointUpdateRequest request = new EndpointUpdateRequest();
            EndpointUpdateRequest.Params params = new EndpointUpdateRequest.Params();
            List<EndpointUpdateRequest.Params> paramList = new ArrayList<EndpointUpdateRequest.Params>();

            // TODO: how do we get delete URIs from the
            // normalized policy?
            List<Uri> delete_uri = new ArrayList<Uri>();
            List<ManagedObject> replace = new ArrayList<ManagedObject>();
            if (mo != null) {
                replace.add(mo);
                delete_uri.add(mo.getUri());
            }
            if (type == EndpointUpdate.UpdateType.ADD_CHANGE) {
                params.setReplace(replace);
            } else if (type == EndpointUpdate.UpdateType.DELETE) {
                params.setDelete_uri(delete_uri);
            }

            paramList.add(params);
            request.setParams(paramList);
            try {
                agent.sendRequest(request);
            } catch (Throwable t) {

            }

        }

    }

    private void sendEpUpdates(Queue<EndpointUpdate> updateQ) {
        while (!updateQ.isEmpty()) {
            executor.execute(updateQ.remove());
        }
    }

    /**
     * Create an Endpoint Registry Context for an OpFlex Request message.
     *
     * @param agent
     * @param message
     * @param dataProvider
     * @param executor
     * @return
     */
    public EprContext create(JsonRpcEndpoint agent, RpcMessage message, DataBroker dataProvider,
            ScheduledExecutorService executor) {

        EprContext ec = null;

        if (message instanceof EndpointDeclareRequest) {
            EndpointDeclareRequest request = (EndpointDeclareRequest) message;
            /*
             * There theoretically could be a list of parameters, but we'll
             * likely only ever see one element.
             */
            ec = new EprContext(agent, request, dataProvider, executor);
            for (EndpointDeclareRequest.Params params : request.getParams()) {

                int prr = params.getPrr();

                /*
                 * We have a list of endpoints, so iterate through the list and
                 * register each one, extracting the identities for
                 * registration.
                 */
                List<ManagedObject> endpoints = params.getEndpoint();
                if (endpoints != null) {
                    for (ManagedObject mo : endpoints) {
                        EprOperation eo = MessageUtils.getEprOpFromEpMo(mo, prr, agent.getIdentifier());
                        ec.addOperation(eo);
                    }
                }
            }
        } else if (message instanceof EndpointUndeclareRequest) {
            EndpointUndeclareRequest request = (EndpointUndeclareRequest) message;
            ec = new EprContext(agent, request, dataProvider, executor);
            for (EndpointUndeclareRequest.Params params : request.getParams()) {

                /*
                 * A single URI is provided per param in Undeclare messages
                 */
                String subject = params.getSubject();
                Uri uri = params.getEndpoint_uri();
                if (uri != null) {
                    EprOperation op = MessageUtils.getEprOpFromUri(uri, subject);
                    ec.addOperation(op);
                }
            }
        } else if (message instanceof EndpointResolveRequest) {
            EndpointResolveRequest request = (EndpointResolveRequest) message;
            ec = new EprContext(agent, request, dataProvider, executor);
            for (EndpointResolveRequest.Params params : request.getParams()) {
                /*
                 * The resolve message contains either the URI or a context/URI
                 * and an identifier. There is only one of these per param array
                 * entry.
                 */
                EndpointIdentity eid = params.getEndpoint_ident();

                String subject = params.getSubject();

                if (eid != null) {

                    EprOperation op = MessageUtils.getEprOpFromEpId(eid, subject);
                    ec.addOperation(op);

                } else {
                    /*
                     * Extract the list to add the EP to from the URI
                     */
                    Uri uri = params.getEndpoint_uri();
                    if (uri != null) {
                        EprOperation op = MessageUtils.getEprOpFromUri(uri, subject);
                        ec.addOperation(op);
                    }
                }
            }
        }
        return ec;
    }

    private synchronized void addEpSubscription(JsonRpcEndpoint agent, String id) {
        Set<String> agents = epSubscriptions.get(id);
        if (agents == null) {
            agents = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            Set<String> result = epSubscriptions.putIfAbsent(id, agents);
            if (result != null) {
                agents = result;
            }
        }
        agents.add(agent.getIdentifier());
    }

    private synchronized void removeEpSubscription(JsonRpcEndpoint agent, String id) {
        Set<String> agents = epSubscriptions.get(id);
        if (agents != null) {
            agents.remove(id);
        }
    }

    /**
     * This notification handles the OpFlex Endpoint messages. We should only
     * receive request messages. Responses are sent in a different context, as
     * all requests result in a Future to access the data store.
     *
     * @param agent
     *            The JsonRpcEndpoint that received the request
     * @param request
     *            The request message from the endpoint
     */
    @Override
    public void callback(JsonRpcEndpoint agent, RpcMessage request) {

        if (messageMap.get(request.getMethod()) == null) {
            LOG.warn("message {} was not subscribed to, but was delivered.", request);
            return;
        }

        /*
         * For declaration requests, we need to make sure that this EP is in our
         * registry. Since we can have multiple identifiers, we create a Set of
         * endpoints.
         */

        if (request instanceof EndpointDeclareRequest) {
            EndpointDeclareRequest req = (EndpointDeclareRequest) request;

            /*
             * valid() ensures presence of params and MOs, so we know those
             * won't be null
             */
            if (!req.valid()) {
                LOG.warn("Invalid declaration request: {}", req);
                // TODO: should return error reply?
                return;
            }

            /*
             * OpFlex EP declaration/registration is different from REST EP
             * declaration/registration -- REST only allows a single EP to be
             * registered at a time. Since each MO represents an Endpoint that's
             * being declared, we need add each one up separately,yet provide a
             * single response. We also want to know the result of the
             * registration so we can provide the appropriate response. We
             * create a context for the Endpoint Registry interaction, where we
             * can track the status of all the EP registrations, and provide a
             * response when all have completed.
             */
            EprContext ctx = create(agent, req, dataProvider, executor);
            ctx.setCallback(this);
            ctx.createEp();
        } else if (request instanceof EndpointUndeclareRequest) {
            EndpointUndeclareRequest req = (EndpointUndeclareRequest) request;

            /*
             * valid() ensures presence of params and URIs, so we know those
             * won't be null
             */
            if (!req.valid()) {
                LOG.warn("Invalid declaration request: {}", req);
                // TODO: should return error reply?
                return;
            }

            /*
             * OpFlex EP undeclaration/unregistration is different from REST EP
             * declaration/registration -- REST only allows a single EP to be
             * unregistered at a time. Since each MO represents an Endpoint
             * that's being undeclared, we need add each one up separately,yet
             * provide a single response. We also want to know the result of the
             * unregistration so we can provide the appropriate response. We
             * create a context for the Endpoint Registry interaction, where we
             * can track the status of all the EP unregistrations, and provide a
             * response when all have completed.
             */
            EprContext ctx = create(agent, req, dataProvider, executor);
            ctx.setCallback(this);
            ctx.deleteEp();
        } else if (request instanceof EndpointResolveRequest) {
            EndpointResolveRequest req = (EndpointResolveRequest) request;

            if (!req.valid()) {
                LOG.warn("Invalid endpoint request: {}", req);
                // TODO: should return error reply?
                return;
            }
            List<EndpointResolveRequest.Params> paramList = req.getParams();

            for (EndpointResolveRequest.Params param : paramList) {
                EprContext ctx = create(agent, req, dataProvider, executor);

                /*
                 * We query the EPR for the EP. This is an asynchronous
                 * operation, so we send the response in the callback
                 */
                ctx.setCallback(this);
                ctx.lookupEndpoint();

                /*
                 * A request is effectively a subscription. Add this agent to
                 * the set of listeners.
                 */
                Identity id;
                if (param.getEndpoint_ident() != null) {
                    id = new Identity(param.getEndpoint_ident().getIdentifier());
                } else if (param.getEndpoint_uri() != null) {
                    PolicyUri puri = new PolicyUri(param.getEndpoint_uri().getValue());
                    id = new Identity(puri.pop());
                } else {
                    // TOOD: should return error reply
                    return;
                }
                addEpSubscription(agent, id.identityAsString());
            }
        } else if (request instanceof EndpointUnresolveRequest) {
            EndpointUnresolveRequest req = (EndpointUnresolveRequest) request;

            if (!req.valid()) {
                LOG.warn("Invalid endpoint request: {}", req);
                // TODO: should return error reply?
                return;
            }

            List<EndpointUnresolveRequest.Params> params = ((EndpointUnresolveRequest) request).getParams();

            for (EndpointUnresolveRequest.Params param : params) {
                /*
                 * No interaction with the Data Store is required -- just cancel
                 * the notification subscription for this EP..
                 */
                Identity id = null;
                if (param.getEndpoint_ident() != null) {
                    id = new Identity(param.getEndpoint_ident().getIdentifier());
                } else if (param.getEndpoint_uri() != null) {
                    PolicyUri puri = new PolicyUri(param.getEndpoint_uri().getValue());
                    id = new Identity(puri.pop());
                } else {
                    // TOODO: should return an error
                    return;
                }
                removeEpSubscription(agent, id.identityAsString());
            }

            /*
             * No EprContext is used in unresolve -- so just send the response
             * directly
             */
            EndpointUnresolveResponse resp = new EndpointUnresolveResponse();
            EndpointUnresolveResponse.Result result = new EndpointUnresolveResponse.Result();
            resp.setResult(result);
            resp.setId(req.getId());
            try {
                agent.sendResponse(resp);
            } catch (Throwable t) {
                LOG.warn("Response {} could not be sent to {}", resp, agent);
            }

        } else {
            LOG.warn("Unexpected callback, {}", request);
        }

    }

    private class EndpointResponse implements Runnable {

        private final EprContext ctx;
        private final RpcMessage resp;

        public EndpointResponse(EprContext ctx, RpcMessage resp) {
            this.ctx = ctx;
            this.resp = resp;
        }

        @Override
        public void run() {
            try {
                ctx.getPeer().sendResponse(resp);
            } catch (Throwable t) {
                // TODO: what to do here
            }

        }

    }

    /**
     * This notification handles the callback from an interaction with the
     * Endpoint Registry. The context for the callback is a notification from
     * the data store, so so the code has to ensure that it won't block.
     * Responses are sent using an executor
     */
    @Override
    public void callback(EprContext ctx) {
        RpcMessage resp = null;
        if (ctx.getRequest() == null)
            return;

        if (!(ctx.getRequest() instanceof EndpointDeclareRequest)
                && !(ctx.getRequest() instanceof EndpointUndeclareRequest)
                && !(ctx.getRequest() instanceof EndpointResolveRequest)) {
            return;
        }

        if (ctx.getRequest() instanceof EndpointDeclareRequest) {
            EndpointDeclareRequest req = (EndpointDeclareRequest) ctx.getRequest();
            EndpointDeclareResponse response = new EndpointDeclareResponse();
            EndpointDeclareResponse.Result result = new EndpointDeclareResponse.Result();
            response.setResult(result);
            response.setId(req.getId());
            response.setError(null); // TODO: real errors
            resp = response;
        } else if (ctx.getRequest() instanceof EndpointUndeclareRequest) {
            EndpointUndeclareRequest req = (EndpointUndeclareRequest) ctx.getRequest();
            EndpointUndeclareResponse response = new EndpointUndeclareResponse();
            EndpointUndeclareResponse.Result result = new EndpointUndeclareResponse.Result();
            response.setResult(result);
            response.setId(req.getId());
            response.setError(null); // TODO: real errors
            resp = response;
        } else {
            EndpointResolveRequest req = (EndpointResolveRequest) ctx.getRequest();
            EndpointResolveResponse response = new EndpointResolveResponse();
            response.setId(req.getId());
            EndpointResolveResponse.Result result = new EndpointResolveResponse.Result();
            List<ManagedObject> epList = new ArrayList<ManagedObject>();

            if (ctx.getOperations().size() > 0) {

                /*
                 * If we get any EP, then we can provide a response to the
                 * original request Note that we could potentially have multiple
                 * requests outstanding for the same EP, and even using
                 * different context types (L2 or L3).
                 */
                for (EprOperation op : ctx.getOperations()) {

                    ManagedObject mo = MessageUtils.getMoFromOp(op);
                    if (mo != null) {
                        epList.add(mo);
                    }
                    /*
                     * For EPs on a different agent, we need to look up the VTEP
                     * information. For now, we're only supporting VXLAN VTEPs,
                     * so we look up the destination tunnel IP, and provide that
                     * in the data field of the response
                     */
                    // TODO: Need to look this up in op store
                    // endpoint.setData();
                }
                result.setEndpoint(epList);
                response.setResult(result);
                resp = response;
            }
        }
        if (resp != null) {
            executor.execute(new EndpointResponse(ctx, resp));
        }
    }
}
