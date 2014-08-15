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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * A context for mapping OpFlex messaging to asynchronous
 * requests to the Endpoint Registry's list of L3 Endpoints.
 *
 * @author tbachman
 *
 */
public class L3EprContext implements EprContext, FutureCallback<Optional<EndpointL3>>{

    private final DataBroker dataProvider;
    private final ScheduledExecutorService executor;
    private final JsonRpcEndpoint peer;
    private final Identity id;
    private final RpcMessage request;
    private final int numIdentifiers;
    private Callback cb;
    private AtomicReference<Integer> calls;
    private Set<EndpointL3> eps;
    public L3EprContext(JsonRpcEndpoint peer, Identity id, RpcMessage request,
            int numIdentifiers, DataBroker dataProvider, ScheduledExecutorService executor) {
        this.peer = peer;
        this.id = id;
        this.request = request;
        this.numIdentifiers = numIdentifiers;
        this.dataProvider = dataProvider;
        this.executor = executor;
        this.calls = new AtomicReference<Integer>(Integer.valueOf(numIdentifiers));
        eps = Collections.newSetFromMap(new ConcurrentHashMap<EndpointL3, Boolean>());
    }
    @Override
    public void setCallback(Callback callback) {
        this.cb = callback;
    }
    public JsonRpcEndpoint getEp() {
        return peer;
    }
    @Override
    public RpcMessage getRequest() {
        return request;
    }

    public int getNumIdentifiers() {
        return numIdentifiers;
    }

    public Set<EndpointL3> getEps() {
        return eps;
    }
    public void setEp(EndpointL3 ep) {
        eps.add(ep);
    }

    /**
     * Create an L3 Endpoint in the Endopint Registry
     *
     * @param req The OpFlex EP Declaration Request message
     * @param id The identity of the EP to create
     */
    @Override
    public void createEp() {
        EndpointDeclarationRequest req =
                (EndpointDeclarationRequest)request;
        if (!req.valid()) {
            return;
        }
        EndpointDeclarationRequest.Params params =
                req.getParams().get(0);
        String context = params.getContext();
        List<String> ids = params.getIdentifier();
        EndpointL3Builder epBuilder = new EndpointL3Builder();

        for ( String l3Addr : ids ) {
            if (InetAddresses.isInetAddress(l3Addr)) {
                id.addId(l3Addr);
            }
        }
        id.setContext(context);
        epBuilder.setIpAddress(id.getL3Identity());
        epBuilder.setL3Context(id.getL3Context());
        epBuilder.setL3Address(id.getL3Addresses());
        long timestamp = System.currentTimeMillis() +
                Long.valueOf(params.getPrr());
        epBuilder.setTimestamp(timestamp);

        // TODO: add support for conditions
        //epBuilder.setCondition(new List<ConditionName>());

        if (params.getPolicy_name() != null) {
            String ts = MessageUtils.getTenantFromUri(params.getPolicy_name());
            String epgs = MessageUtils.getEndpointGroupFromUri(params.getPolicy_name());
            if (ts != null) {
                TenantId tid = new TenantId(ts);
                epBuilder.setTenant(tid);
            }
            if (epgs != null) {
                EndpointGroupId eid = new EndpointGroupId(epgs);
                epBuilder.setEndpointGroup(eid);
            }
        }

        OpflexOverlayContextL3Builder oocb = new OpflexOverlayContextL3Builder();
        // TODO: how to divine the location type?
        //oocb.setLocationType(value);
        oocb.setAgentId(peer.getIdentifier());
        oocb.setAgentEpLocation(params.getLocation());
        epBuilder.addAugmentation(OpflexOverlayContextL3.class, oocb.build());

        EndpointL3 ep = epBuilder.build();
        InstanceIdentifier<EndpointL3> iid =
                InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, ep.getKey())
                .build();
        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        wt.submit();
    }

    @Override
    public void deleteEp() {
        EndpointDeclarationRequest req =
                (EndpointDeclarationRequest)request;
        if (!req.valid()) {
            return;
        }
        EndpointDeclarationRequest.Params params =
                req.getParams().get(0);
        String context = params.getContext();
        List<String> ids = params.getIdentifier();
        EndpointL3Builder epBuilder = new EndpointL3Builder();

        for ( String l3Addr : ids ) {
            if (InetAddresses.isInetAddress(l3Addr)) {
                id.addId(l3Addr);
            }
        }
        id.setContext(context);
        epBuilder.setIpAddress(id.getL3Identity());
        epBuilder.setL3Context(id.getL3Context());
        epBuilder.setL3Address(id.getL3Addresses());
        epBuilder.setTimestamp(Long.valueOf(params.getPrr()));

        if (params.getPolicy_name() != null) {
            String ts = MessageUtils.getTenantFromUri(params.getPolicy_name());
            String epgs = MessageUtils.getEndpointGroupFromUri(params.getPolicy_name());
            if (ts != null) {
                TenantId tid = new TenantId(ts);
                epBuilder.setTenant(tid);
            }
            if (epgs != null) {
                EndpointGroupId eid = new EndpointGroupId(epgs);
                epBuilder.setEndpointGroup(eid);
            }
        }

        EndpointL3 ep = epBuilder.build();
        InstanceIdentifier<EndpointL3> iid =
                InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, ep.getKey())
                .build();
        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.delete(LogicalDatastoreType.OPERATIONAL, iid);
        wt.submit();
    }

    /**
     * Look up an L3 endpoint in the registry, given a context
     * and an identifier.
     * .
     * @param context The L3 Context
     * @param identifier The L3 identifier
     */
    @Override
    public void lookupEndpoint() {
        EndpointRequestRequest req =
                (EndpointRequestRequest)request;
        if (!req.valid()) {
            return;
        }
        EndpointRequestRequest.Params params =
                req.getParams().get(0);
        String context = params.getContext();
        String identity = params.getIdentifier().get(0);
        if (context == null || identity == null) return;
        id.setContext(context);

        EndpointL3Key key =
                new EndpointL3Key(id.getL3Identity(),
                        id.getL3Context());
        InstanceIdentifier<EndpointL3> iid =
                InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, key)
                .build();
        ListenableFuture<Optional<EndpointL3>> dao =
                dataProvider.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(dao, this, executor);
    }

    @Override
    public void sendResponse() {
        if (!(getRequest() instanceof EndpointRequestRequest)) {
            return;
        }
        EndpointRequestRequest req =
                (EndpointRequestRequest)getRequest();
        EndpointRequestResponse response = new EndpointRequestResponse();
        response.setId(req.getId());
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
        if ((getEps() == null) || (getEps().size() <= 0)) {
            EndpointRequestResponse.Error error =
                    new EndpointRequestResponse.Error();
            error.setMessage(EpStatus.NO_ENDPOINTS);
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
            for (EndpointL3 e : getEps()) {
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
                endpoint.setPolicy_name(MessageUtils.
                        createEpgUri(e.getTenant().getValue(), e.getEndpointGroup().getValue()));
                endpoint.setStatus(EpStatus.EP_STATUS_ATTACH.toString());
                OpflexOverlayContextL3 context =
                        e.getAugmentation(OpflexOverlayContextL3.class);
                if (context != null) {
                    endpoint.setLocation(context.getAgentEpLocation());
                }
                //endpoint.setSubject("");
                endpoint.setPrr(Long.
                        valueOf(e.getTimestamp().
                        longValue()/1000).
                        intValue());
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
            getEp().sendResponse(response);
        }
        catch (Throwable t) {
            // TODO: implement
        }

    }
    @Override
    public void onSuccess(final Optional<EndpointL3> result) {
        calls.set(calls.get().intValue()-1);
        if (!result.isPresent()) {
            /*
             * This EP doesn't exist in the registry. If
             * all of the data store queries have been made,
             * and we still don't have any EPs, then provide
             * an error result.
             */
            if (calls.get().intValue() <= 0) {
                cb.callback(this);
            }
            return;
        }
        setEp(result.get());

        cb.callback(this);
    }


    @Override
    public void onFailure(Throwable t) {
        // TODO: implement another callback
    }

}
