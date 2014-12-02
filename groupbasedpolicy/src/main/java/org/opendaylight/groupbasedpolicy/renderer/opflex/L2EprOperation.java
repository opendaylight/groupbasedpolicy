/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * A context for mapping OpFlex messaging to asynchronous
 * requests to the Endpoint Registry's list of L2 Endpoints.
 *
 * @author tbachman
 *
 */
public class L2EprOperation implements EprOperation, FutureCallback<Optional<Endpoint>>{

	private EprOpCallback cb;
    private Endpoint ep;
    private InstanceIdentifier<Endpoint> iid;

    private String agentId;
    private TenantId tid;
    private EndpointGroupId egid;
    private L2BridgeDomainId l2bdid;
    private MacAddress mac;
    private List<L3Address> l3al;
    private Long timeout;


    public L2EprOperation(int prr) {
        this.timeout = Long.valueOf(prr);
        this.l3al = new ArrayList<L3Address>();
    }

    public L2EprOperation() {
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void setTenantId(TenantId tid) {
    	this.tid = tid;
    }

    public void setEndpointGroupId(EndpointGroupId egid) {
    	this.egid = egid;
    }

    public void setContextId(L2BridgeDomainId l2bdid) {
    	this.l2bdid = l2bdid;
    }

    public void setMacAddress(MacAddress mac) {
    	this.mac = mac;
    }

    public void setL3AddressList(List<L3Address> l3al) {
    	this.l3al = l3al;
    }

    public void addL3Address(L3Address l3a) {
    	this.l3al.add(l3a);
    }

    public Endpoint getEp() {
        return ep;
    }

    public void setEp(Endpoint ep) {
        this.ep = ep;
    }

    public Endpoint buildEp() {
        EndpointBuilder epBuilder = new EndpointBuilder();
        OpflexOverlayContextBuilder oocb = new OpflexOverlayContextBuilder();
        oocb.setAgentId(this.agentId);

        epBuilder.setTenant(this.tid)
                 .setEndpointGroup(this.egid)
                 .setL2Context(this.l2bdid)
                 .setL3Address(l3al)
                 .setMacAddress(this.mac)
                 .setTimestamp(this.timeout)
                 .addAugmentation(OpflexOverlayContext.class, oocb.build());

        // TODO: add support for conditions
        //epBuilder.setCondition(new List<ConditionName>());

        return epBuilder.build();
    }

    /**
     * Create or update an L2 Endpoint in the Endpoint Registry
     *
     * @param wt The Write Transaction
     */
    @Override
    public void put(WriteTransaction wt) {

        ep = buildEp();
        this.iid = InstanceIdentifier.builder(Endpoints.class)
            	.child(Endpoint.class, ep.getKey())
            	.build();
        wt.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
    }

    @Override
    public void delete(WriteTransaction wt) {

        ep = buildEp();
        this.iid = InstanceIdentifier.builder(Endpoints.class)
            	.child(Endpoint.class, ep.getKey())
            	.build();
        wt.delete(LogicalDatastoreType.OPERATIONAL, iid);
    }

    /**
     * Get/read an L2 endpoint in the registry, given a context
     * and an identifier.
     * .
     * @param rot The read transaction
     */
    @Override
    public void read(ReadOnlyTransaction rot,
    		ScheduledExecutorService executor) {

    	ep = buildEp();
    	this.iid = InstanceIdentifier.builder(Endpoints.class)
            	.child(Endpoint.class, ep.getKey())
            	.build();

    	ListenableFuture<Optional<Endpoint>> dao =
                rot.read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(dao, this, executor);
    }

    @Override
    public void setCallback(EprOpCallback callback) {
    	this.cb = callback;
    }


    @Override
    public void onSuccess(final Optional<Endpoint> result) {
        if (!result.isPresent()) {
            /*
             * This EP doesn't exist in the registry. If
             * all of the data store queries have been made,
             * and we still don't have any EPs, then provide
             * an error result.
             */
            this.ep = null;
            cb.callback(this);
            return;
        }
        setEp(result.get());
        cb.callback(this);
    }


    @Override
    public void onFailure(Throwable t) {
        cb.callback(this);
    }

}
