/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3GatewaysBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class EndpointRegistrator {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointRegistrator.class);
    private final EndpointService epService;

    public EndpointRegistrator(EndpointService epService) {
        this.epService = Preconditions.checkNotNull(epService);
    }

    public boolean registerEndpoint(RegisterEndpointInput regEndpointInput) {
        try {
            RpcResult<Void> rpcResult = epService.registerEndpoint(regEndpointInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - registerEndpoint was not successful. Input of RPC: {}", regEndpointInput);
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("registerEndpoint failed. {}", regEndpointInput, e);
            return false;
        }
    }

    public boolean unregisterEndpoint(UnregisterEndpointInput unregEndpointInput) {
        try {
            RpcResult<Void> rpcResult = epService.unregisterEndpoint(unregEndpointInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - unregisterEndpoint was not successful. Input of RPC: {}", unregEndpointInput);
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("unregisterEndpoint failed. {}", unregEndpointInput, e);
            return false;
        }
    }

    public boolean registerExternalL3PrefixEndpoint(IpPrefix ipPrefix, L3ContextId l3Context,
            @Nullable IpAddress gatewayIp, TenantId tenantId) {
        List<EndpointL3Gateways> l3Gateways = new ArrayList<EndpointL3Gateways>();
        if (gatewayIp != null) {
            EndpointL3Gateways l3Gateway =
                    new EndpointL3GatewaysBuilder().setIpAddress(gatewayIp).setL3Context(l3Context).build();
            l3Gateways.add(l3Gateway);
        }
        RegisterL3PrefixEndpointInput registerL3PrefixEpRpcInput = new RegisterL3PrefixEndpointInputBuilder()
            .setL3Context(l3Context)
            .setIpPrefix(ipPrefix)
            .setEndpointGroup(MappingUtils.EPG_EXTERNAL_ID)
            .setTenant(tenantId)
            .setEndpointL3Gateways(l3Gateways)
            .setTimestamp(System.currentTimeMillis())
            .build();
        try {
            RpcResult<Void> rpcResult = epService.registerL3PrefixEndpoint(registerL3PrefixEpRpcInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - registerExternalL3PrefixEndpoint was not successful. Input of RPC: {}",
                        registerL3PrefixEpRpcInput);
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("registerExternalL3PrefixEndpoint failed. {}", registerL3PrefixEpRpcInput, e);
            return false;
        }
    }

    public boolean registerL3EndpointAsExternalGateway(TenantId tenantId, IpAddress ipAddress, L3ContextId l3Context,
            NetworkDomainId networkContainment) {
        RegisterEndpointInput registerEndpointInput =
                new RegisterEndpointInputBuilder()
                    .setL3Address(ImmutableList
                        .of(new L3AddressBuilder().setL3Context(l3Context).setIpAddress(ipAddress).build()))
                    .setTenant(tenantId)
                    .setNetworkContainment(networkContainment)
                    .setEndpointGroups(ImmutableList.of(MappingUtils.EPG_EXTERNAL_ID))
                    .setTimestamp(System.currentTimeMillis())
                    .build();
        try {
            RpcResult<Void> rpcResult = epService.registerEndpoint(registerEndpointInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - registerL3EndpointAsExternalGateway was not successful. Input of RPC: {}",
                        registerEndpointInput);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("registerL3EndpointAsExternalGateway failed. {}", registerEndpointInput, e);
            return false;
        }
        return true;
    }

}
