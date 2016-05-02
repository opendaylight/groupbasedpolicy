/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.endpoints.AddressEndpointsByContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.endpoints.AddressEndpointsByContainmentKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.endpoints.address.endpoints.by.containment.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.endpoints.address.endpoints.by.containment.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.register.endpoint.input.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final RegisterEndpointInput epInput = new RegisterEndpointInputBuilder()
                .setAddressEndpoint(Collections.singletonList(new AddressEndpointBuilder()
                        .setNetworkContainment(epForwardingTemplate.getNetworkContainment())
                        .setCondition(epPolicyTemplate.getConditions())
                        .setTenant(epPolicyTemplate.getTenant())
                        .setContainment(epForwardingTemplate.getL3Context())
                        .setEndpointGroup(epPolicyTemplate.getEndpointGroups())
                        .setAddress(address)
                        .build()))
                .build();
                epForwardingTemplate.getL3Context();

        return chainL3EPServiceIfEpAbsent(epInput);
    }

    private CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> findExistingEndPoint(final ContextId containment,
                                                                                          final Address address) {
        KeyedInstanceIdentifier<AddressEndpoint, AddressEndpointKey> addressEndpointPath =
                InstanceIdentifier.create(Endpoints.class)
                .child(AddressEndpointsByContainment.class, new AddressEndpointsByContainmentKey(containment))
                .child(AddressEndpoint.class, new AddressEndpointKey(address));
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> read = rTx.read(
                LogicalDatastoreType.OPERATIONAL, addressEndpointPath);
        Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));
        return read;
    }

    private ListenableFuture<RpcResult<Void>> chainL3EPServiceIfEpAbsent(final RegisterEndpointInput epInput) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoints.rev160427.register.endpoint.input.AddressEndpoint addressEndpoint = epInput.getAddressEndpoint().get(0);
        CheckedFuture<Optional<AddressEndpoint>, ReadFailedException> existingEndpointFuture =
                findExistingEndPoint(addressEndpoint.getContainment(), addressEndpoint.getAddress());

        return Futures.transform(existingEndpointFuture, new AsyncFunction<Optional<AddressEndpoint>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<AddressEndpoint> input) throws Exception {
                final ListenableFuture<RpcResult<Void>> nextResult;
                if (input == null || !input.isPresent()) {
                    // invoke service
                    return JdkFutureAdapters.listenInPoolThread(l3EndpointService.registerEndpoint(epInput));
                } else {
                    final String existingL3EpMsg = String.format("address-endpoint for given key already exists: %s | %s",
                            addressEndpoint.getContainment(), addressEndpoint.getAddress() );
                    nextResult = Futures.immediateFailedFuture(new IllegalStateException(existingL3EpMsg));
                }
                return nextResult;
            }
        });
    }

}

