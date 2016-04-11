/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Purpose: exclusively processes sxp master database changes and EGP templates changes
 */
public class SxpMapperReactorImpl implements SxpMapperReactor {

    private static final Logger LOG = LoggerFactory.getLogger(SxpMapperReactorImpl.class);

    private final BaseEndpointService l3EndpointService;
    private final DataBroker dataBroker;

    public SxpMapperReactorImpl(final BaseEndpointService l3EndpointService, final DataBroker dataBroker) {
        this.l3EndpointService = Preconditions.checkNotNull(l3EndpointService, "l3Endpoint service missing");
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "dataBroker missing");
    }

    @Override
    public ListenableFuture<RpcResult<Void>> processTemplatesAndSxpMasterDB(
            final EndpointPolicyTemplateBySgt epPolicyTemplate,
            final EndpointForwardingTemplateBySubnet epForwardingTemplate,
            final MasterDatabaseBinding masterDBBinding) {
        LOG.debug("processing ep-templates + sxpMasterDB entry: {} - {}",
                masterDBBinding.getSecurityGroupTag(), masterDBBinding.getIpPrefix());
        // apply sxpMasterDB to policy template
        final Ipv4Prefix address = new Ipv4Prefix(epForwardingTemplate.getIpPrefix().getIpv4Prefix().getValue());
        final NetworkContainment networkContainment = new NetworkContainmentBuilder()
            .setNetworkDomainType(epForwardingTemplate.getNetworkContainment().getNetworkDomainType())
            .setNetworkDomainId(epForwardingTemplate.getNetworkContainment().getNetworkDomainId())
            .build();
        final RegisterEndpointInput epInput = new RegisterEndpointInputBuilder()
                .setAddressEndpointReg(Collections.singletonList(new AddressEndpointRegBuilder()
                        .setAddressType(IpPrefixType.class)
                        .setAddress(address.getValue())
                        .setContextType(L3Context.class)
                        .setContextId(epForwardingTemplate.getL3Context())
                        .setNetworkContainment(networkContainment)
                        .setCondition(epPolicyTemplate.getConditions())
                        .setTenant(epPolicyTemplate.getTenant())
                        .setEndpointGroup(epPolicyTemplate.getEndpointGroups())
                        .build()))
                .build();
                epForwardingTemplate.getL3Context();

        return chainL3EPServiceIfEpAbsent(epInput);
    }

    private CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> findExistingEndPoint(final ContextId containment,
                                                                                          final String address) {
        KeyedInstanceIdentifier<AddressEndpoint, AddressEndpointKey> addressEndpointPath =
                InstanceIdentifier.create(Endpoints.class).child(AddressEndpoints.class).child(AddressEndpoint.class,
                        new AddressEndpointKey(address, IpPrefixType.class, containment, L3Context.class));
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> read = rTx.read(
                LogicalDatastoreType.OPERATIONAL, addressEndpointPath);
        Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));
        return read;
    }

    private ListenableFuture<RpcResult<Void>> chainL3EPServiceIfEpAbsent(final RegisterEndpointInput epInput) {
        AddressEndpointReg addressEndpoint = epInput.getAddressEndpointReg().get(0);
        CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> existingEndpointFuture =
                findExistingEndPoint(addressEndpoint.getContextId(), addressEndpoint.getAddress());

        return Futures.transform(existingEndpointFuture, new AsyncFunction<Optional<AddressEndpoint>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<AddressEndpoint> input) throws Exception {
                final ListenableFuture<RpcResult<Void>> nextResult;
                if (input == null || !input.isPresent()) {
                    // invoke service
                    return JdkFutureAdapters.listenInPoolThread(l3EndpointService.registerEndpoint(epInput));
                } else {
                    final String existingL3EpMsg = String.format("address-endpoint for given key already exists: %s | %s",
                            addressEndpoint.getContextId(), addressEndpoint.getAddress() );
                    nextResult = Futures.immediateFailedFuture(new IllegalStateException(existingL3EpMsg));
                }
                return nextResult;
            }
        });
    }

}

