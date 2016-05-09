/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import com.google.common.base.Function;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnreg;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class BaseEndpointRpcRegistry
    implements BaseEndpointService, BaseEndpointRendererAugmentationRegistry, AutoCloseable {

    static final ConcurrentMap<String, BaseEndpointRendererAugmentation> registeredRenderers = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(BaseEndpointRpcRegistry.class);
    private final DataBroker dataProvider;
    private final BindingAwareBroker.RpcRegistration<BaseEndpointService> rpcRegistration;

    private Function<Void, RpcResult<Void>> futureTrans = new Function<Void, RpcResult<Void>>() {

        @Override
        public RpcResult<Void> apply(Void input) {
            return RpcResultBuilder.<Void>success().build();
        }
    };

    public BaseEndpointRpcRegistry(DataBroker dataProvider, RpcProviderRegistry rpcRegistry) {
        this.dataProvider = dataProvider;

        if (rpcRegistry != null) {
            rpcRegistration = rpcRegistry.addRpcImplementation(BaseEndpointService.class, this);
            LOG.debug("Added Endpoints RPC Implementation Correctly");
        } else {
            rpcRegistration = null;
        }

        if (dataProvider != null) {
            InstanceIdentifier<Endpoints> iid = InstanceIdentifier.builder(Endpoints.class).build();
            WriteTransaction t = this.dataProvider.newWriteOnlyTransaction();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, new EndpointsBuilder().build());
            CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
            Futures.addCallback(f, new FutureCallback<Void>() {

                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Could not write Endpoints base container", t);
                }

                @Override
                public void onSuccess(Void result) {
                    LOG.info("Endpoints container write successful");
                }
            });
        }
    }

    /**
     * Registers renderer's endpoints augmentation.
     *
     * @param baseEndpointRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    @Override
    public void register(BaseEndpointRendererAugmentation baseEndpointRendererAugmentation) {
        if (baseEndpointRendererAugmentation != null) {
            registeredRenderers.putIfAbsent(baseEndpointRendererAugmentation.getClass().getName(),
                baseEndpointRendererAugmentation);
            LOG.info("Registered {}", baseEndpointRendererAugmentation.getClass().getName());
        }
    }

    /**
     * Unregisters renderer's endpoints augmentation.
     *
     * @param baseEndpointRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    @Override
    public void unregister(BaseEndpointRendererAugmentation baseEndpointRendererAugmentation) {
        if (baseEndpointRendererAugmentation == null
                || !registeredRenderers.containsKey(baseEndpointRendererAugmentation.getClass().getName())) {
            return;
        }
        registeredRenderers.remove(baseEndpointRendererAugmentation.getClass().getName());
        LOG.info("Unregistered {}", baseEndpointRendererAugmentation.getClass().getName());
    }

    /**
     * Register a new endpoint into the registry. If there is already an existing
     * endpoint with the same keys, they will be overwritten with the new information.
     *
     * @param input Endpoint to register
     */
    @Override
    public Future<RpcResult<Void>> registerEndpoint(RegisterEndpointInput input) {
        long timestamp = System.currentTimeMillis();

        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        List<ContainmentEndpointReg> endpoints = input.getContainmentEndpointReg();
        for (ContainmentEndpointReg ce : nullToEmpty(endpoints)) {
            long stamp = (ce.getTimestamp() == null || ce.getTimestamp() == 0) ? timestamp : ce.getTimestamp();
            ContainmentEndpoint endpoint = buildContainmentEndpoint(ce).setTimestamp(stamp).build();
            t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(endpoint.getKey()), endpoint,
                    true);
        }

        List<AddressEndpointReg> addressEndpoints = input.getAddressEndpointReg();
        for (AddressEndpointReg ae : nullToEmpty(addressEndpoints)) {
            long stamp = (ae.getTimestamp() == null || ae.getTimestamp() == 0) ? timestamp : ae.getTimestamp();
            AddressEndpoint endpoint = buildAddressEndpoint(ae).setTimestamp(stamp).build();
            t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(endpoint.getKey()), endpoint, true);
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans);
    }

    private ContainmentEndpointBuilder buildContainmentEndpoint(ContainmentEndpointReg input) {
        ContainmentEndpointBuilder eb = new ContainmentEndpointBuilder().setChildEndpoint(input.getChildEndpoint())
            .setCondition(input.getCondition())
            .setContextType(input.getContextType())
            .setContextId(input.getContextId())
            .setEndpointGroup(input.getEndpointGroup())
            .setKey(new ContainmentEndpointKey(input.getContextId(), input.getContextType()))
            .setNetworkContainment(input.getNetworkContainment())
            .setTenant(input.getTenant())
            .setTimestamp(input.getTimestamp());

        for (Map.Entry<String, BaseEndpointRendererAugmentation> entry : registeredRenderers.entrySet()) {
            try {
                Map.Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> augmentationEntry =
                        entry.getValue().buildContainmentEndpointAugmentation(input);
                if (augmentationEntry != null) {
                    eb.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("AddressEndpoint Augmentation error while processing " + entry.getKey() + ". Reason: ", e);
            }
        }
        return eb;
    }

    private AddressEndpointBuilder buildAddressEndpoint(AddressEndpointReg ae) {
        AddressEndpointBuilder builder = new AddressEndpointBuilder().setTenant(ae.getTenant())
            .setNetworkContainment(ae.getNetworkContainment())
            .setEndpointGroup(ae.getEndpointGroup())
            .setAddress(ae.getAddress())
            .setAddressType(ae.getAddressType())
            .setChildEndpoint(ae.getChildEndpoint())
            .setCondition(ae.getCondition())
            .setKey(new AddressEndpointKey(ae.getAddress(), ae.getAddressType(), ae.getContextId(),
                    ae.getContextType()))
            .setParentEndpointChoice(ae.getParentEndpointChoice())
            .setTimestamp(ae.getTimestamp())
            .setContextId(ae.getContextId())
            .setContextType(ae.getContextType())
            .setTenant(ae.getTenant());

        for (Map.Entry<String, BaseEndpointRendererAugmentation> entry : registeredRenderers.entrySet()) {
            try {
                Map.Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> augmentationEntry =
                        entry.getValue().buildAddressEndpointAugmentation(ae);
                if (augmentationEntry != null) {
                    builder.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("AddressEndpoint Augmentation error while processing " + entry.getKey() + ". Reason: ", e);
            }
        }
        return builder;
    }

    /**
     * Unregister an endpoint or endpoints from the registry.
     *
     * @param input Endpoint/endpoints to unregister
     */
    @Override
    public Future<RpcResult<Void>> unregisterEndpoint(UnregisterEndpointInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        List<AddressEndpointUnreg> addressEndpoints = input.getAddressEndpointUnreg();
        for (AddressEndpointUnreg ae : nullToEmpty(addressEndpoints)) {
            AddressEndpointKey key = new AddressEndpointKey(ae.getAddress(), ae.getAddressType(), ae.getContextId(),
                    ae.getContextType());
            t.delete(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key));
        }

        List<ContainmentEndpointUnreg> endpoints = input.getContainmentEndpointUnreg();
        for (ContainmentEndpointUnreg ce : nullToEmpty(endpoints)) {
            ContainmentEndpointKey key = new ContainmentEndpointKey(ce.getContextId(), ce.getContextType());
            t.delete(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(key));
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans);
    }

    @Override
    public void close() throws Exception {
        if (rpcRegistration != null) {
            rpcRegistration.close();
        }
    }

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

}
