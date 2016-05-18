/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.BaseEndpointRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnreg;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

public class BaseEndpointRpcRegistry
        implements BaseEndpointService, BaseEndpointRendererAugmentationRegistry, AutoCloseable {

    static final ConcurrentMap<String, BaseEndpointRendererAugmentation> registeredRenderers =
            new ConcurrentHashMap<>();
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
        Preconditions.checkNotNull(dataProvider);
        Preconditions.checkNotNull(rpcRegistry);

        this.dataProvider = dataProvider;
        this.rpcRegistration = rpcRegistry.addRpcImplementation(BaseEndpointService.class, this);
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
        ListenableFuture<RpcResult<Void>> failResult = verifyRegisterEndpointInput(input);
        if (failResult == null) {
            for (ContainmentEndpointReg ce : nullToEmpty(endpoints)) {
                long stamp = (ce.getTimestamp() == null || ce.getTimestamp() == 0) ? timestamp : ce.getTimestamp();
                ContainmentEndpoint endpoint = buildContainmentEndpoint(ce).setTimestamp(stamp).build();
                t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(endpoint.getKey()), endpoint,
                        true);

                updateContainmentEndpointRegChilds(t, endpoint);
            }

            List<AddressEndpointReg> addressEndpoints = input.getAddressEndpointReg();
            for (AddressEndpointReg ae : nullToEmpty(addressEndpoints)) {
                long stamp = (ae.getTimestamp() == null || ae.getTimestamp() == 0) ? timestamp : ae.getTimestamp();
                AddressEndpoint endpoint = buildAddressEndpoint(ae).setTimestamp(stamp).build();
                t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(endpoint.getKey()), endpoint,
                        true);

                updateAddressEndpointRegChilds(t, endpoint);

                updateAddressEndpointRegParents(t, endpoint);
            }
        } else {
            return failResult;
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans);
    }

    private void updateContainmentEndpointRegChilds(WriteTransaction t, ContainmentEndpoint containmentEndpoint) {
        ReadOnlyTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        for (ChildEndpoint child : nullToEmpty(containmentEndpoint.getChildEndpoint())) {
            AddressEndpointKey key = new AddressEndpointKey(child.getAddress(), child.getAddressType(),
                    child.getContextId(), child.getContextType());
            Optional<AddressEndpoint> addressEndpointOptional = DataStoreHelper
                .readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key), readTransaction);

            if (addressEndpointOptional.isPresent()) {
                ParentEndpointChoice parentEndpointChoice = addressEndpointOptional.get().getParentEndpointChoice();
                List<ParentContainmentEndpoint> parentContainmentEndpoints =
                        getParentContainmentEndpoints(parentEndpointChoice);

                ParentContainmentEndpoint parentContainmentEndpoint =
                        new ParentContainmentEndpointBuilder().setContextId(containmentEndpoint.getContextId())
                            .setContextType(containmentEndpoint.getContextType())
                            .build();

                if (!nullToEmpty(parentContainmentEndpoints).contains(parentContainmentEndpoint)) {
                    parentContainmentEndpoints.add(parentContainmentEndpoint);
                    AddressEndpoint updatedAddressEndpoint = new AddressEndpointBuilder(addressEndpointOptional.get())
                        .setParentEndpointChoice(new ParentContainmentEndpointCaseBuilder()
                            .setParentContainmentEndpoint(parentContainmentEndpoints).build())
                        .build();

                    t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(key), updatedAddressEndpoint);
                }
            }
        }
    }

    private ListenableFuture<RpcResult<Void>> verifyRegisterEndpointInput(RegisterEndpointInput input) {
        ListenableFuture<RpcResult<Void>> result;
        List<AddressEndpointReg> addressEndpointRegs = nullToEmpty(input.getAddressEndpointReg());
        List<ContainmentEndpointReg> containmentEndpointRegs = nullToEmpty(input.getContainmentEndpointReg());

        for (AddressEndpointReg addressEndpointReg : nullToEmpty(addressEndpointRegs)) {
            result = verifyAddressEndpointChilds(addressEndpointRegs, addressEndpointReg);
            if (result != null) {
                return result;
            }

            result = verifyAddressEndpointParents(addressEndpointRegs, addressEndpointReg);
            if (result != null) {
                return result;
            }
        }

        result = verifyContainmentEndpointChilds(addressEndpointRegs, containmentEndpointRegs);
        if (result != null) {
            return result;
        }

        return null;
    }

    private ListenableFuture<RpcResult<Void>> verifyContainmentEndpointChilds(
            List<AddressEndpointReg> addressEndpointRegs, List<ContainmentEndpointReg> containmentEndpointRegs) {
        for (ContainmentEndpointReg containmentEndpointReg : nullToEmpty(containmentEndpointRegs)) {
            for (ChildEndpoint childEndpoint : nullToEmpty(containmentEndpointReg.getChildEndpoint())) {
                AddressEndpointRegKey key = new AddressEndpointRegKey(childEndpoint.getAddress(),
                        childEndpoint.getAddressType(), childEndpoint.getContextId(), childEndpoint.getContextType());
                AddressEndpointReg addressEndpointRegChild = findAddressEndpointReg(key, addressEndpointRegs);
                if (addressEndpointRegChild == null) {
                    // todo this can be optimized if we move this to
                    // updateContainmentEndpointRegChilds and verify child in one step with update.
                    Optional<AddressEndpoint> addressEndpointOptional =
                            DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                    IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                            key.getAddressType(), key.getContextId(), key.getContextType())),
                                    dataProvider.newReadOnlyTransaction());
                    if (!addressEndpointOptional.isPresent()) {
                        LOG.debug("Child AddressEndpoint {} does not exist in RPC and DS.", childEndpoint);
                        return buildFailFeature(
                                String.format("Child AddressEndpoint %s does not exist in RPC and DS.", childEndpoint));
                    }
                } else {

                    if (!containmentEndpointReg.getKey().equals(new ContainmentEndpointRegKey(
                            addressEndpointRegChild.getContextId(), addressEndpointRegChild.getContextType()))) {
                        LOG.debug(
                                "Child AddressEndpoint {} in ContainmentEndpoint->ChildEndpoints does not contain a valid ContainmentEndpointRegKey.",
                                addressEndpointRegChild);
                        return buildFailFeature(String
                            .format("AddressEndpoint in ContainmentEndpoint->ChildEndpoints does not contain a valid ContainmentEndpointRegKey."
                                    + "\nChild element: %s", addressEndpointRegChild));
                    }
                }
            }
        }
        return null;
    }

    private ListenableFuture<RpcResult<Void>> verifyAddressEndpointParents(List<AddressEndpointReg> addressEndpointRegs,
            AddressEndpointReg addressEndpointReg) {
        ParentEndpointChoice parentEndpointChoice = addressEndpointReg.getParentEndpointChoice();
        List<ParentEndpoint> parentEndpoints;
        parentEndpoints =
                (parentEndpointChoice instanceof ParentEndpointCase) ? ((ParentEndpointCase) parentEndpointChoice)
                    .getParentEndpoint() : null;

        for (ParentEndpoint parentEndpoint : nullToEmpty(parentEndpoints)) {
            AddressEndpointRegKey key = new AddressEndpointRegKey(parentEndpoint.getAddress(),
                    parentEndpoint.getAddressType(), parentEndpoint.getContextId(), parentEndpoint.getContextType());
            AddressEndpointReg addressEndpointRegParent = findAddressEndpointReg(key, addressEndpointRegs);

            if (addressEndpointRegParent == null) {
                // todo this can be optimized if we move this to updateAddressEndpointRegParents and
                // verify child in one step with update.
                Optional<AddressEndpoint> addressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                        key.getAddressType(), key.getContextId(), key.getContextType())),
                                dataProvider.newReadOnlyTransaction());
                if (!addressEndpointOptional.isPresent()) {
                    LOG.debug("Parent AddressEndpoint {} does not exist in RPC and DS.", parentEndpoint);
                    return buildFailFeature(
                            String.format("Parent AddressEndpoint %s does not exist in RPC and DS.", parentEndpoint));
                }
            } else {

                List<ChildEndpoint> childEndpoints = addressEndpointRegParent.getChildEndpoint();

                if (!nullToEmpty(childEndpoints).contains(new ChildEndpointBuilder(addressEndpointReg).build())) {
                    LOG.debug("Parent AddressEndpoint {} does not contain a valid child AddressEndpoint.",
                            addressEndpointRegParent);
                    return buildFailFeature(String.format(
                            "Parent AddressEndpoint does not contain a valid child AddressEndpoint."
                                    + "\nParent AddressEndpoint: %s" + "\nChild AddressEndpoint: %s",
                            addressEndpointRegParent, addressEndpointReg));
                }
            }
        }
        return null;
    }

    private ListenableFuture<RpcResult<Void>> verifyAddressEndpointChilds(List<AddressEndpointReg> addressEndpointRegs,
            AddressEndpointReg addressEndpointReg) {
        for (ChildEndpoint childEndpoint : nullToEmpty(addressEndpointReg.getChildEndpoint())) {
            AddressEndpointRegKey key = new AddressEndpointRegKey(childEndpoint.getAddress(),
                    childEndpoint.getAddressType(), childEndpoint.getContextId(), childEndpoint.getContextType());
            AddressEndpointReg addressEndpointRegChild = findAddressEndpointReg(key, addressEndpointRegs);
            if (addressEndpointRegChild == null) {
                // todo this can be optimized if we move this to updateAddressEndpointRegChilds and
                // verify child in one step with update.
                Optional<AddressEndpoint> addressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                        key.getAddressType(), key.getContextId(), key.getContextType())),
                                dataProvider.newReadOnlyTransaction());
                if (!addressEndpointOptional.isPresent()) {
                    LOG.debug("Child AddressEndpoint {} does not exist in RPC and DS.", childEndpoint);
                    return buildFailFeature(
                            String.format("Child AddressEndpoint %s does not exist in RPC and DS.", childEndpoint));
                }
            } else {

                ParentEndpointChoice parentEndpointChoice = addressEndpointRegChild.getParentEndpointChoice();

                if (!(parentEndpointChoice instanceof ParentEndpointCase)) {
                    LOG.debug("Child AddressEndpoint {} does not contain list of parent elements.", childEndpoint);
                    return buildFailFeature(String.format(
                            "Child AddressEndpoint does not contain list of parent elements." + "\nChild element: %s",
                            childEndpoint));
                }

                List<ParentEndpoint> parentEndpoints =
                        nullToEmpty(((ParentEndpointCase) parentEndpointChoice).getParentEndpoint());
                if (!parentEndpoints.contains(new ParentEndpointBuilder(addressEndpointReg).build())) {
                    LOG.debug("Child AddressEndpoint {} does not contain a valid parent AddressEndpoint.",
                            addressEndpointRegChild);
                    return buildFailFeature(String.format(
                            "Child AddressEndpoint does not contain a valid parent AddressEndpoint."
                                    + "\nChild element: %s" + "\nparent AddressEndpoint: %s",
                            addressEndpointRegChild, addressEndpointReg));
                }
            }
        }
        return null;
    }

    private AddressEndpointReg findAddressEndpointReg(AddressEndpointRegKey key, List<AddressEndpointReg> addressEndpointRegs) {
        for (AddressEndpointReg addressEndpointReg : addressEndpointRegs) {
            if (addressEndpointReg.getKey().equals(key)) {
                return addressEndpointReg;
            }
        }
        return null;
    }

    private void updateAddressEndpointRegParents(WriteTransaction t, AddressEndpoint endpoint) {
        ParentEndpointCase parentEndpointCase = (ParentEndpointCase) endpoint.getParentEndpointChoice();
        List<ParentEndpoint> parentEndpoints;
        ReadOnlyTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        parentEndpoints = getParentEndpoints(parentEndpointCase);

        for (ParentEndpoint parent : nullToEmpty(parentEndpoints)) {
            Optional<AddressEndpoint> addressEndpointOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                            IidFactory.addressEndpointIid(new AddressEndpointKey(parent.getAddress(),
                                    parent.getAddressType(), parent.getContextId(), parent.getContextType())),
                            readTransaction);

            if (addressEndpointOptional.isPresent()) {

                List<ChildEndpoint> childEndpoints;

                childEndpoints = (addressEndpointOptional.get() == null || addressEndpointOptional.get()
                    .getChildEndpoint() == null) ? new ArrayList<>() : addressEndpointOptional.get().getChildEndpoint();

                ChildEndpoint childEndpoint = new ChildEndpointBuilder(endpoint).build();
                if (!childEndpoints.contains(childEndpoint)) {
                    childEndpoints.add(childEndpoint);
                    AddressEndpoint parentAddressEndpoint = new AddressEndpointBuilder(addressEndpointOptional.get())
                        .setChildEndpoint(childEndpoints).build();
                    t.put(LogicalDatastoreType.OPERATIONAL,
                            IidFactory.addressEndpointIid(parentAddressEndpoint.getKey()), parentAddressEndpoint);
                }
            }
        }
    }

    private void updateAddressEndpointRegChilds(WriteTransaction t, AddressEndpoint endpoint) {
        ReadTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        for (ChildEndpoint child : nullToEmpty(endpoint.getChildEndpoint())) {
            Optional<AddressEndpoint> addressEndpointOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                            IidFactory.addressEndpointIid(new AddressEndpointKey(child.getAddress(),
                                    child.getAddressType(), child.getContextId(), child.getContextType())),
                            readTransaction);

            if (addressEndpointOptional.isPresent()) {
                ParentEndpointCase parentEndpointCase =
                        (ParentEndpointCase) addressEndpointOptional.get().getParentEndpointChoice();
                List<ParentEndpoint> parentEndpoints;

                parentEndpoints = getParentEndpoints(parentEndpointCase);

                ParentEndpoint parentEndpoint = new ParentEndpointBuilder(endpoint).build();
                if (!parentEndpoints.contains(parentEndpoint)) {
                    parentEndpoints.add(parentEndpoint);
                    AddressEndpoint childAddressEndpoint =
                            new AddressEndpointBuilder(addressEndpointOptional.get())
                                .setParentEndpointChoice(
                                        new ParentEndpointCaseBuilder().setParentEndpoint(parentEndpoints).build())
                                .build();
                    t.put(LogicalDatastoreType.OPERATIONAL,
                            IidFactory.addressEndpointIid(childAddressEndpoint.getKey()), childAddressEndpoint);
                }
            }
        }
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
            .setTimestamp(input.getTimestamp())
            .setChildEndpoint(input.getChildEndpoint());

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

            updateAddressEndpointUnregChilds(t, ae);

            updateAddressEndpointUnregParents(t, ae);
        }

        List<ContainmentEndpointUnreg> endpoints = input.getContainmentEndpointUnreg();
        for (ContainmentEndpointUnreg ce : nullToEmpty(endpoints)) {
            ContainmentEndpointKey key = new ContainmentEndpointKey(ce.getContextId(), ce.getContextType());
            t.delete(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(key));

            updateContainmentEndpointUnregChilds(t, ce);
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans);
    }

    private void updateAddressEndpointUnregParents(WriteTransaction t, AddressEndpointUnreg ae) {
        ReadTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        Optional<AddressEndpoint> addressEndpointOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(new AddressEndpointKey(ae.getAddress(), ae.getAddressType(),
                        ae.getContextId(), ae.getContextType())),
                readTransaction);

        if (addressEndpointOptional.isPresent()) {
            ParentEndpointCase parentEndpointCase =
                    (ParentEndpointCase) addressEndpointOptional.get().getParentEndpointChoice();
            List<ParentEndpoint> parentEndpoints;

            parentEndpoints = getParentEndpoints(parentEndpointCase);

            for (ParentEndpoint parentEndpoint : parentEndpoints) {
                Optional<AddressEndpoint> parentAddressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(parentEndpoint.getAddress(),
                                        parentEndpoint.getAddressType(), parentEndpoint.getContextId(),
                                        parentEndpoint.getContextType())),
                                readTransaction);

                AddressEndpoint parent =
                        parentAddressEndpointOptional.isPresent() ? parentAddressEndpointOptional.get() : null;

                ChildEndpoint endpointToRemove = new ChildEndpointBuilder(addressEndpointOptional.get()).build();

                if (parent != null && nullToEmpty(parent.getChildEndpoint()).contains(endpointToRemove)) {
                    parent.getChildEndpoint().remove(endpointToRemove);
                    t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(parent.getKey()), parent);
                }

            }
        }
    }

    private void updateAddressEndpointUnregChilds(WriteTransaction t, AddressEndpointUnreg ae) {
        ReadTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        Optional<AddressEndpoint> addressEndpointOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(new AddressEndpointKey(ae.getAddress(), ae.getAddressType(),
                        ae.getContextId(), ae.getContextType())),
                readTransaction);

        if (addressEndpointOptional.isPresent()) {
            AddressEndpoint endpoint = addressEndpointOptional.get();
            List<ChildEndpoint> childEndpoints = endpoint.getChildEndpoint();

            for (ChildEndpoint childEndpoint : nullToEmpty(childEndpoints)) {
                Optional<AddressEndpoint> childAddressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(childEndpoint.getAddress(),
                                        childEndpoint.getAddressType(), childEndpoint.getContextId(),
                                        childEndpoint.getContextType())),
                                readTransaction);

                AddressEndpoint child =
                        childAddressEndpointOptional.isPresent() ? childAddressEndpointOptional.get() : null;
                ParentEndpointCase parentEndpointCase =
                        (child != null) ? (ParentEndpointCase) child.getParentEndpointChoice() : null;
                List<ParentEndpoint> parentEndpoints;

                parentEndpoints = getParentEndpoints(parentEndpointCase);

                ParentEndpoint endpointToRemove = new ParentEndpointBuilder(endpoint).build();

                if (child != null && nullToEmpty(parentEndpoints).contains(endpointToRemove)) {
                    parentEndpoints.remove(endpointToRemove);
                    AddressEndpoint newChild =
                            new AddressEndpointBuilder(child)
                                .setParentEndpointChoice(
                                        new ParentEndpointCaseBuilder().setParentEndpoint(parentEndpoints).build())
                                .build();

                    t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(newChild.getKey()), newChild);
                }
            }
        }
    }

    private void updateContainmentEndpointUnregChilds(WriteTransaction t,
            ContainmentEndpointUnreg containmentEndpointUnreg) {
        ReadOnlyTransaction readTransaction = dataProvider.newReadOnlyTransaction();

        ContainmentEndpointKey key = new ContainmentEndpointKey(containmentEndpointUnreg.getContextId(),
                containmentEndpointUnreg.getContextType());
        Optional<ContainmentEndpoint> containmentEndpointOptional = DataStoreHelper
            .readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(key), readTransaction);

        if (!containmentEndpointOptional.isPresent()) {
            return;
        }

        for (ChildEndpoint child : nullToEmpty(containmentEndpointOptional.get().getChildEndpoint())) {
            AddressEndpointKey aeKey = new AddressEndpointKey(child.getAddress(), child.getAddressType(),
                    child.getContextId(), child.getContextType());
            Optional<AddressEndpoint> addressEndpointOptional = DataStoreHelper
                .readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(aeKey), readTransaction);

            if (addressEndpointOptional.isPresent()) {
                ParentEndpointChoice parentEndpointChoice = addressEndpointOptional.get().getParentEndpointChoice();
                List<ParentContainmentEndpoint> parentContainmentEndpoints;
                parentContainmentEndpoints = getParentContainmentEndpoints(parentEndpointChoice);

                ParentContainmentEndpoint parentContainmentEndpoint =
                        new ParentContainmentEndpointBuilder().setContextId(containmentEndpointUnreg.getContextId())
                            .setContextType(containmentEndpointUnreg.getContextType())
                            .build();
                if (nullToEmpty(parentContainmentEndpoints).contains(parentContainmentEndpoint)) {
                    t.delete(LogicalDatastoreType.OPERATIONAL,
                            IidFactory.parentContainmentEndpointIid(aeKey, parentContainmentEndpoint.getKey()));
                }
            }
        }
    }

    private List<ParentContainmentEndpoint> getParentContainmentEndpoints(ParentEndpointChoice parentEndpointChoice) {
        return (parentEndpointChoice instanceof ParentContainmentEndpointCase) ? ((ParentContainmentEndpointCase) parentEndpointChoice)
            .getParentContainmentEndpoint() : new ArrayList<>();
    }

    private List<ParentEndpoint> getParentEndpoints(ParentEndpointCase parentEndpointCase) {
        return (parentEndpointCase == null
                || parentEndpointCase.getParentEndpoint() == null) ? new ArrayList<>() : parentEndpointCase
                    .getParentEndpoint();
    }

    private ListenableFuture<RpcResult<Void>> buildFailFeature(String message) {
        return Futures
            .immediateFuture(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.PROTOCOL, message).build());
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
