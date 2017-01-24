/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointRegistrator {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointRegistrator.class);
    private final EndpointService epService;
    private final BaseEndpointService baseEpService;

    public EndpointRegistrator(EndpointService epService, BaseEndpointService baseEpService) {
        this.epService = Preconditions.checkNotNull(epService);
        this.baseEpService = Preconditions.checkNotNull(baseEpService);
    }

    public boolean registerEndpoint(AddressEndpointReg regEndpointInput) {
        RegisterEndpointInput regBaseEpInput = new RegisterEndpointInputBuilder().setAddressEndpointReg(
                ImmutableList.<AddressEndpointReg>of(regEndpointInput))
            .build();
        return registerEndpoint(regBaseEpInput);
    }

    public boolean registerEndpoint(RegisterEndpointInput regBaseEpInput) {
        try {
            RpcResult<Void> rpcResult = baseEpService.registerEndpoint(regBaseEpInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - registerEndpoint was not successful. Input of RPC: {}", regBaseEpInput);
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Base endpoint registration failed. {}", regBaseEpInput, e);
            return false;
        }
    }

    public boolean unregisterEndpoint(AddressEndpointUnreg addrEpUnreg) {
        UnregisterEndpointInput input = new UnregisterEndpointInputBuilder().setAddressEndpointUnreg(
                ImmutableList.<AddressEndpointUnreg>of(new AddressEndpointUnregBuilder().setKey(addrEpUnreg.getKey())
                    .build())).build();
        return unregisterEndpoint(input);
    }

    public boolean unregisterEndpoint(UnregisterEndpointInput input) {
        try {
            RpcResult<Void> rpcResult = baseEpService.unregisterEndpoint(input).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - unregisterEndpoint was not successful. Input of RPC: {}", input);
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("unregisterEndpoint failed. {}", input, e);
            return false;
        }
    }

    @Deprecated
    public boolean registerEndpoint(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput regEndpointInput) {
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

    @Deprecated
    public boolean unregisterEndpoint(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput unregEndpointInput) {
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

    @Deprecated
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

    @Deprecated
    public boolean registerL3EpAsExternalGateway(TenantId tenantId, IpAddress ipAddress, L3ContextId l3Context,
            NetworkDomainId networkContainment) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput registerEndpointInput =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder()
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

    @Deprecated
    public boolean unregisterL3EpAsExternalGateway(IpAddress ipAddress, L3ContextId l3Context) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput unregisterEndpointInput =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder()
                .setL3(ImmutableList.of(new L3Builder().setL3Context(l3Context)
                    .setIpAddress(ipAddress)
                    .build()))
                .build();

        try {
            RpcResult<Void> rpcResult = epService.unregisterEndpoint(unregisterEndpointInput).get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("Illegal state - unregisterL3EndpointAsExternalGateway was not successful. Input of RPC: {}",
                    unregisterEndpointInput);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("unregisterL3EndpointAsExternalGateway failed. {}", unregisterEndpointInput, e);
            return false;
        }
        return true;
    }
}
