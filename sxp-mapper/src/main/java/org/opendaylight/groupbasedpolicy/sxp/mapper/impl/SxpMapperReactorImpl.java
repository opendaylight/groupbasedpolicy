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
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: exclusively processes sxp master database changes and EGP templates changes
 */
public class SxpMapperReactorImpl implements SxpMapperReactor {

    private static final Logger LOG = LoggerFactory.getLogger(SxpMapperReactorImpl.class);

    private final EndpointService l3EndpointService;
    private final DataBroker dataBroker;

    public SxpMapperReactorImpl(final EndpointService l3EndpointService, final DataBroker dataBroker) {
        this.l3EndpointService = Preconditions.checkNotNull(l3EndpointService, "l3Endpoint service missing");
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "dataBroker missing");
    }

    @Override
    public ListenableFuture<RpcResult<Void>> processPolicyAndSxpMasterDB(final EndpointPolicyTemplateBySgt template, final MasterDatabaseBinding masterDBBinding) {
        LOG.debug("processing ep-policy-template + sxpMasterDB entry: {} - {}",
                masterDBBinding.getSecurityGroupTag(), masterDBBinding.getIpPrefix());
        // apply sxpMasterDB to policy template
        final L3Address l3Address = buildL3Address(masterDBBinding.getIpPrefix());
        final RegisterEndpointInput epInput = new RegisterEndpointInputBuilder()
                .setCondition(template.getConditions())
                .setTenant(template.getTenant())
                .setEndpointGroups(template.getEndpointGroups())
                .setL3Address(Collections.singletonList(l3Address))
                .build();

        return chainL3EPServiceIfEpAbsent(l3Address, epInput);
    }

    @Override
    public ListenableFuture<RpcResult<Void>> processForwardingAndSxpMasterDB(final EndpointForwardingTemplateBySubnet template, final MasterDatabaseBinding masterDBBinding) {
        LOG.debug("processing ep-forwarding-template + sxpMasterDB entry: {} - {}",
                masterDBBinding.getSecurityGroupTag(), masterDBBinding.getIpPrefix());
        // apply sxpMasterDB to policy template
        final L3Address l3Address = buildL3Address(masterDBBinding.getIpPrefix());
        final RegisterEndpointInput epInput = new RegisterEndpointInputBuilder()
                .setNetworkContainment(template.getNetworkContainment())
                .setL3Address(Collections.singletonList(l3Address))
                .build();

        return chainL3EPServiceIfEpAbsent(l3Address, epInput);
    }

    private L3Address buildL3Address(final IpPrefix ipPrefix) {
        final String ipv4PrefixValue = ipPrefix.getIpv4Prefix().getValue();
        final IpAddress ipv4Value = new IpAddress(new Ipv4Address(ipv4PrefixValue.replaceFirst("/.+", "")));
        return new L3AddressBuilder()
                .setIpAddress(ipv4Value)
                .setL3Context(new L3ContextId(ipv4PrefixValue))
                .build();
    }

    private CheckedFuture<Optional<EndpointL3>, ReadFailedException> findExistingEndPoint(final L3Address l3Address) {
        EndpointL3Key epL3key = new EndpointL3Key(l3Address.getIpAddress(), l3Address.getL3Context());
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<EndpointL3>, ReadFailedException> read = rTx.read(
                LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointIid(epL3key));
        Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));
        return read;
    }

    private ListenableFuture<RpcResult<Void>> chainL3EPServiceIfEpAbsent(final L3Address l3Address, final RegisterEndpointInput epInput) {
        CheckedFuture<Optional<EndpointL3>, ReadFailedException> existingEndpointFuture = findExistingEndPoint(l3Address);

        return Futures.transform(existingEndpointFuture, new AsyncFunction<Optional<EndpointL3>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<EndpointL3> input) throws Exception {
                final ListenableFuture<RpcResult<Void>> nextResult;
                if (input == null || !input.isPresent()) {
                    // invoke service
                    return JdkFutureAdapters.listenInPoolThread(l3EndpointService.registerEndpoint(epInput));
                } else {
                    final String existingL3EpMsg = String.format("L3Endpoint for given key already exists: %s", l3Address);
                    nextResult = Futures.immediateFailedFuture(new IllegalStateException(existingL3EpMsg));
                }
                return nextResult;
            }
        });
    }
}

