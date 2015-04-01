/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.opflex.rev140528.OpflexOverlayContextL3Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A context for mapping OpFlex messaging to asynchronous
 * requests to the Endpoint Registry's list of L3 Endpoints.
 *
 * @author tbachman
 */
public class L3EprOperation implements EprOperation, FutureCallback<Optional<EndpointL3>> {

    private EprOpCallback cb;
    private EndpointL3 ep;
    private InstanceIdentifier<EndpointL3> iid;

    private String agentId;
    private TenantId tid;
    private EndpointGroupId egid;
    private MacAddress mac;
    private List<L3Address> l3al;
    private L2BridgeDomainId l2bdid;
    private L3ContextId l3cid;
    private IpAddress ip;
    private Long timeout;

    public L3EprOperation(int prr) {
        this.timeout = Long.valueOf(prr);
    }

    public L3EprOperation() {}

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void setTenantId(TenantId tid) {
        this.tid = tid;
    }

    public void setEndpointGroupId(EndpointGroupId egid) {
        this.egid = egid;
    }

    public void setContextId(L3ContextId l3cid) {
        this.l3cid = l3cid;
    }

    public void setL2BridgDomainId(L2BridgeDomainId l2bdid) {
        this.l2bdid = l2bdid;
    }

    public void setMacAddress(MacAddress mac) {
        this.mac = mac;
    }

    public void setIpAddress(IpAddress ip) {
        this.ip = ip;
    }

    public void setL3AddressList(List<L3Address> l3al) {
        this.l3al = l3al;
    }

    public void addL3Address(L3Address l3a) {
        this.l3al.add(l3a);
    }

    public EndpointL3 getEp() {
        return ep;
    }

    public void setEp(EndpointL3 ep) {
        this.ep = ep;
    }

    public EndpointL3 buildEp() {
        EndpointL3Builder epBuilder = new EndpointL3Builder();
        OpflexOverlayContextL3Builder oocb = new OpflexOverlayContextL3Builder();
        oocb.setAgentId(this.agentId);

        epBuilder.setTenant(this.tid)
            .setEndpointGroup(this.egid)
            .setL2Context(this.l2bdid)
            .setL3Context(this.l3cid)
            .setL3Address(l3al)
            .setMacAddress(this.mac)
            .setIpAddress(this.ip)
            .setTimestamp(this.timeout)
            .addAugmentation(OpflexOverlayContextL3.class, oocb.build());

        // TODO: add support for conditions
        // epBuilder.setCondition(new List<ConditionName>());

        return epBuilder.build();
    }

    @Override
    public void onSuccess(final Optional<EndpointL3> result) {
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
        // TODO: implement another callback
    }

    @Override
    public void put(WriteTransaction wt) {
        ep = buildEp();
        this.iid = InstanceIdentifier.builder(Endpoints.class).child(EndpointL3.class, ep.getKey()).build();
        wt.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
    }

    @Override
    public void delete(WriteTransaction wt) {
        ep = buildEp();
        this.iid = InstanceIdentifier.builder(Endpoints.class).child(EndpointL3.class, ep.getKey()).build();
        wt.delete(LogicalDatastoreType.OPERATIONAL, iid);
    }

    @Override
    public void read(ReadOnlyTransaction rot, ScheduledExecutorService executor) {
        ep = buildEp();
        this.iid = InstanceIdentifier.builder(Endpoints.class).child(EndpointL3.class, ep.getKey()).build();

        ListenableFuture<Optional<EndpointL3>> dao = rot.read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(dao, this, executor);

    }

    @Override
    public void setCallback(EprOpCallback callback) {
        this.cb = callback;
    }

}
