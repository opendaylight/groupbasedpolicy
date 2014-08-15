/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.groupbasedpolicy.endpoint.AbstractEndpointRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointPolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
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
        implements AutoCloseable, DataChangeListener, RpcBroker.RpcCallback,
        EprContext.Callback {
    protected static final Logger LOG =
            LoggerFactory.getLogger(EndpointManager.class);

    private static final InstanceIdentifier<Endpoint> endpointsIid =
            InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class).build();
    private static final InstanceIdentifier<EndpointL3> endpointsL3Iid =
            InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class).build();

    final ListenerRegistration<DataChangeListener> listenerReg;
    final ListenerRegistration<DataChangeListener> listenerL3Reg;

    private OpflexConnectionService connectionService;
    final ConcurrentHashMap<EpKey, Endpoint> endpoints =
            new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> epSubscriptions =
            new ConcurrentHashMap<>();

    private RpcMessageMap messageMap = null;

    Set<L2EprContext> l2RpcCtxts =
            Collections.newSetFromMap(new ConcurrentHashMap<L2EprContext, Boolean>());
    Set<L3EprContext> l3RpcCtxts =
            Collections.newSetFromMap(new ConcurrentHashMap<L3EprContext, Boolean>());

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
            listenerL3Reg = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                                                endpointsL3Iid,
                                                this,
                                                DataChangeScope.ONE);
        } else {
            listenerReg = null;
            listenerL3Reg = null;
        }

        this.connectionService = connectionService;

        /* Subscribe to EPR messages */
        messageMap = new RpcMessageMap();
        List<RpcMessage> messages = Role.ENDPOINT_REGISTRY.getMessages();
        messageMap.addList(messages);
        for (RpcMessage msg: messages) {
            this.connectionService.subscribe(msg, this);
        }
        LOG.trace("Initialized OpFlex endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

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
        OpflexOverlayContextInput ictx =
                input.getAugmentation(OpflexOverlayContextInput.class);
        return super.buildEndpoint(input)
                .addAugmentation(OpflexOverlayContext.class,
                                 new OpflexOverlayContextBuilder(ictx).build());
    }

    @Override
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        OpflexOverlayContextInput ictx =
                input.getAugmentation(OpflexOverlayContextInput.class);
        return super.buildEndpointL3(input)
                .addAugmentation(OpflexOverlayContextL3.class,
                                 new OpflexOverlayContextL3Builder(ictx).build());
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
                updateEndpoint(null, dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint(old, null);
        }
        Map<InstanceIdentifier<?>,DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if ((!(entry.getValue() instanceof Endpoint)) &&
                (!(entry.getValue() instanceof EndpointL3))) continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            DataObject oldEp = null;
            if (entry instanceof Endpoint ||
                entry instanceof EndpointL3) {
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
            Endpoint ep = (Endpoint)obj;
            id = new Identity(ep);
        }

        if (obj instanceof EndpointL3) {
            EndpointL3 ep = (EndpointL3)obj;
            id = new Identity(ep);
        }
        if (id != null && !id.valid()) {
            return null;
        }
        return id;
    }


    /**
     *  Provide endpoint policy update messages based on changes
     */
    protected void updateEndpoint(DataObject oldEp, DataObject newEp) {
        Identity oldId = getIdentity(oldEp);
        Identity newId = getIdentity(newEp);
        /*
         * If an endpoint has changed, we need to provide notifications
         * to agents that have subscribed to that endpoint. Batch up
         * the notifications to be sent to the agents.
         */
        Queue<EndpointUpdate> updateQ = new ConcurrentLinkedQueue<EndpointUpdate>();

        /* This covers additions or updates */
        if (newId != null) {
            Set<String> agentList = epSubscriptions.get(newId.identityAsString());
            if (agentList != null) {
                for (String agentId : agentList) {
                    OpflexAgent agent = connectionService.getOpflexAgent(agentId);
                    if (agent != null) {
                        EpStatus epStatus;
                        if (oldId == null) {
                            epStatus = EpStatus.EP_STATUS_ATTACH;
                        }
                        else {
                            epStatus = EpStatus.EP_STATUS_MODIFY;
                        }
                        updateQ.add(new EndpointUpdate(agent.getEndpoint(),
                                newId, newEp, epStatus));
                    }
                }
            }
        }
        /* this covers deletions */
        if ((newId == null) && (oldId != null)) {
            Set<String> agentList = epSubscriptions.get(oldId.identityAsString());
            if (agentList != null) {
                for (String agentId : agentList) {
                    OpflexAgent agent = connectionService.getOpflexAgent(agentId);
                    if (agent != null) {
                        updateQ.add(new EndpointUpdate(agent.getEndpoint(),
                                oldId, oldEp,
                                EpStatus.EP_STATUS_DETACH));
                    }
                }
            }
        }

        sendEpUpdates(updateQ);
    }

    private static class EndpointUpdate implements Runnable {
        private final JsonRpcEndpoint agent;
        private final Identity id;
        private final EpStatus status;
        private int ttl;
        private String tid;
        private String epgId;
        private String location;
        EndpointUpdate(JsonRpcEndpoint agent, Identity id, DataObject obj, EpStatus status) {
            this.agent = agent;
            this.id = id;
            this.status = status;
            if (obj instanceof Endpoint) {
                Endpoint ep = (Endpoint)obj;
                tid = ep.getTenant().getValue();
                epgId = ep.getEndpointGroup().getValue();
                ttl = ep.getTimestamp().intValue();
                OpflexOverlayContext context =
                        ep.getAugmentation(OpflexOverlayContext.class);
                if (context != null) {
                    location = context.getAgentEpLocation();
                }
            }
            if (obj instanceof EndpointL3) {
                EndpointL3 ep = (EndpointL3)obj;
                tid = ep.getTenant().getValue();
                epgId = ep.getEndpointGroup().getValue();
                ttl = ep.getTimestamp().intValue();
                OpflexOverlayContextL3 context =
                        ep.getAugmentation(OpflexOverlayContextL3.class);
                if (context != null) {
                    location = context.getAgentEpLocation();
                }
            }
        }

        @Override
        public void run() {
            EndpointPolicyUpdateRequest request =
                    new EndpointPolicyUpdateRequest();
            EndpointPolicyUpdateRequest.Params params =
                    new EndpointPolicyUpdateRequest.Params();
            List<EndpointPolicyUpdateRequest.Params> paramList =
                    new ArrayList<EndpointPolicyUpdateRequest.Params>();
            List<String> idList = new ArrayList<String>();

            params.setContext(id.contextAsString());
            idList.add(id.identityAsString());
            params.setIdentifier(idList);
            params.setStatus(status.toString());
            params.setTtl(ttl);
            params.setPolicy_name(MessageUtils.createEpgUri(tid, epgId));
            params.setLocation(location);
            //params.setSubject(new String());

            paramList.add(params);
            request.setParams(paramList);
            try {
                agent.sendRequest(request);
            }
            catch (Throwable t) {

            }

        }

    }

    private void sendEpUpdates(Queue<EndpointUpdate> updateQ) {
        while (!updateQ.isEmpty()) {
            executor.execute(updateQ.remove());
        }
    }

    /**
     * This notification handles the OpFlex Endpoint messages.
     * We should only receive request messages. Responses are
     * sent in a different context, as all requests result
     * in a Future to access the data store.
     *
     * @param agent The JsonRpcEndpoint that received the request
     * @param request The request message from the endpoint
     */
    @Override
    public void callback(JsonRpcEndpoint agent, RpcMessage request) {

        RpcMessage response = null;
        if (messageMap.get(request.getMethod()) == null) {
            LOG.warn("message {} was not subscribed to, but was delivered.", request);
            return;
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

            if (!req.valid()) {
                LOG.warn("Invalid declaration request: {}", req);
                // TODO: should return error reply?
                return;
            }

            EprContext ctx = EprContextFactory.create(agent, req, dataProvider, executor);
            ctx.setCallback(this);

            EndpointDeclarationRequest.Params params = req.getParams().get(0);
            String epStatus = params.getStatus();
            if (epStatus != null &&
                    (epStatus.equals(EpStatus.EP_STATUS_ATTACH.toString()) ||
                            epStatus.equals(EpStatus.EP_STATUS_MODIFY.toString()))) {
                ctx.createEp();
            }
            else if (epStatus != null &&
                    epStatus.equals(EpStatus.EP_STATUS_DETACH.toString())) {
                ctx.deleteEp();
            }

        }
        else if (request instanceof EndpointRequestRequest) {
            EndpointRequestRequest req = (EndpointRequestRequest)request;

            if (!req.valid()) {
                LOG.warn("Invalid endpoint request: {}", req);
                // TODO: should return error reply?
                return;
            }
            EprContext ctx = EprContextFactory.create(agent, req, dataProvider, executor);

            /*
             * We query the EPR for the EP. This is an asynchronous
             * operation, so we send the response in the callback
             */
            ctx.setCallback(this);
            ctx.lookupEndpoint();

            /*
             * A request is effectively a subscription. Add this agent
             * to the set of listeners.
             */
            Identity id =
                    new Identity(req.getParams().get(0).getIdentifier().get(0));
            Set<String> agents = epSubscriptions.get(id.identityAsString());
            if (agents == null) {
                agents = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            }
            agents.add(agent.getIdentifier());
            epSubscriptions.put(id.identityAsString(), agents);
        }
        else {
            LOG.warn("Unexpected callback, {}", request);
        }

        if (response != null) {
            try {
                agent.sendResponse(response);
            }
            catch (Throwable t) {
                LOG.warn("Response {} could not be sent to {}", response, agent);
            }
        }
    }

    /**
     * This notification handles the callback from a query
     * of the Endpoint Registry
     */
    @Override
    public void callback(EprContext ctx) {
        ctx.sendResponse();
    }
}
