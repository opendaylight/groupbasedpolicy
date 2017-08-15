/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.ContainmentEndpointUnreg;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class BaseEndpointServiceImpl implements BaseEndpointService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BaseEndpointServiceImpl.class);
    private final DataBroker dataProvider;
    private final EndpointAugmentorRegistryImpl epAugRegistry;

    private static final Function<Void, RpcResult<Void>> TO_SUCCESS_RPC_RESULT = new Function<Void, RpcResult<Void>>() {

        @Override
        public RpcResult<Void> apply(Void input) {
            return RpcResultBuilder.<Void>success().build();
        }
    };

    public BaseEndpointServiceImpl(DataBroker dataProvider, EndpointAugmentorRegistryImpl epAugRegistry) {
        this.epAugRegistry = Preconditions.checkNotNull(epAugRegistry);
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
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

        ReadWriteTransaction t = dataProvider.newReadWriteTransaction();

        ListenableFuture<RpcResult<Void>> failResult =
                RegisterEndpointInputVerificator.verifyRegisterEndpointInput(input, t);
        if (failResult != null) {
            t.cancel();
            return failResult;
        }

        List<ContainmentEndpointReg> endpoints = input.getContainmentEndpointReg();
        for (ContainmentEndpointReg ce : nullToEmpty(endpoints)) {
            long stamp = (ce.getTimestamp() == null || ce.getTimestamp() == 0) ? timestamp : ce.getTimestamp();
            ContainmentEndpoint endpoint = buildContainmentEndpoint(ce).setTimestamp(stamp).build();
            t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(endpoint.getKey()), endpoint,
                    true);
            addContainmentEndpointToChilds(t, endpoint);
        }

        List<AddressEndpointReg> addressEndpoints = input.getAddressEndpointReg();
        for (AddressEndpointReg ae : nullToEmpty(addressEndpoints)) {
            long stamp = (ae.getTimestamp() == null || ae.getTimestamp() == 0) ? timestamp : ae.getTimestamp();
            AddressEndpoint endpoint = buildAddressEndpoint(ae).setTimestamp(stamp).build();
            t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.addressEndpointIid(endpoint.getKey()), endpoint, true);
            addAddressEndpointToChilds(t, endpoint);
            addAddressEndpointToParents(t, endpoint);
        }

        return Futures.transform(t.submit(), TO_SUCCESS_RPC_RESULT);
    }

    private void addContainmentEndpointToChilds(ReadWriteTransaction t, ContainmentEndpoint endpoint) {
        ParentContainmentEndpoint epAsParent = new ParentContainmentEndpointBuilder(endpoint).build();
        for (ChildEndpoint child : nullToEmpty(endpoint.getChildEndpoint())) {
            InstanceIdentifier<AddressEndpoint> childIid =
                    IidFactory.addressEndpointIid(EndpointUtils.createAddressEndpointKey(child));
            Optional<AddressEndpoint> childAddrEpOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, childIid, t);
            if (childAddrEpOptional.isPresent()) {
                KeyedInstanceIdentifier<ParentContainmentEndpoint, ParentContainmentEndpointKey> parentInChildIid =
                        childIid.child(ParentContainmentEndpoint.class, epAsParent.getKey());
                t.put(LogicalDatastoreType.OPERATIONAL, parentInChildIid, epAsParent, true);
            }
        }
    }

    private void addAddressEndpointToChilds(ReadWriteTransaction t, AddressEndpoint endpoint) {
        ParentEndpoint epAsParent = new ParentEndpointBuilder(endpoint).build();
        for (ChildEndpoint child : nullToEmpty(endpoint.getChildEndpoint())) {
            InstanceIdentifier<AddressEndpoint> childIid =
                    IidFactory.addressEndpointIid(EndpointUtils.createAddressEndpointKey(child));
            Optional<AddressEndpoint> childAddrEpOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, childIid, t);
            if (childAddrEpOptional.isPresent()) {
                KeyedInstanceIdentifier<ParentEndpoint, ParentEndpointKey> parentInChildIid =
                        childIid.child(ParentEndpoint.class, epAsParent.getKey());
                t.put(LogicalDatastoreType.OPERATIONAL, parentInChildIid, epAsParent, true);
            }
        }
    }

    private void addAddressEndpointToParents(ReadWriteTransaction t, AddressEndpoint endpoint) {
        ChildEndpoint epAsChild = new ChildEndpointBuilder(endpoint).build();
        for (ParentEndpoint parent : EndpointUtils.getParentEndpoints(endpoint.getParentEndpointChoice())) {
            InstanceIdentifier<AddressEndpoint> parentIid =
                    IidFactory.addressEndpointIid(EndpointUtils.createAddressEndpointKey(parent));
            Optional<AddressEndpoint> parentAddrEpOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, parentIid, t);
            if (parentAddrEpOptional.isPresent()) {
                KeyedInstanceIdentifier<ChildEndpoint, ChildEndpointKey> childInParentIid =
                        parentIid.child(ChildEndpoint.class, epAsChild.getKey());
                t.put(LogicalDatastoreType.OPERATIONAL, childInParentIid, epAsChild, true);
            }
        }
        for (ParentContainmentEndpoint parent : EndpointUtils
            .getParentContainmentEndpoints(endpoint.getParentEndpointChoice())) {
            InstanceIdentifier<ContainmentEndpoint> parentIid = IidFactory
                .containmentEndpointIid(new ContainmentEndpointKey(parent.getContextId(), parent.getContextType()));
            Optional<ContainmentEndpoint> parentContEpOptional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, parentIid, t);
            if (parentContEpOptional.isPresent()) {
                KeyedInstanceIdentifier<ChildEndpoint, ChildEndpointKey> childInParentIid =
                        parentIid.child(ChildEndpoint.class, epAsChild.getKey());
                t.put(LogicalDatastoreType.OPERATIONAL, childInParentIid, epAsChild, true);
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

        for (EndpointAugmentor epAugmentor : epAugRegistry.getEndpointAugmentors()) {
            try {
                Map.Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> augmentationEntry =
                        epAugmentor.buildContainmentEndpointAugmentation(input);
                if (augmentationEntry != null) {
                    eb.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("AddressEndpoint Augmentation error while {} was processing {}",
                        epAugmentor.getClass().getSimpleName(), input, e);
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

        for (EndpointAugmentor epAugmentor : epAugRegistry.getEndpointAugmentors()) {
            try {
                Map.Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> augmentationEntry =
                        epAugmentor.buildAddressEndpointAugmentation(ae);
                if (augmentationEntry != null) {
                    builder.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("AddressEndpoint Augmentation error while {} was processing {}",
                        epAugmentor.getClass().getSimpleName(), ae, e);
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
        ReadWriteTransaction t = dataProvider.newReadWriteTransaction();

        List<AddressEndpointUnreg> addrEndpoints = input.getAddressEndpointUnreg();
        for (AddressEndpointUnreg aeUnreg : nullToEmpty(addrEndpoints)) {
            AddressEndpointKey key = new AddressEndpointKey(aeUnreg.getAddress(), aeUnreg.getAddressType(),
                    aeUnreg.getContextId(), aeUnreg.getContextType());
            Optional<AddressEndpoint> aeOptional = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.addressEndpointIid(key), t);
            if (aeOptional.isPresent()) {
                deleteAddressEndpointFromParents(t, aeOptional.get());
                deleteAddressEndpointFromChilds(t, aeOptional.get());
            }
        }

        List<ContainmentEndpointUnreg> contEndpoints = input.getContainmentEndpointUnreg();
        for (ContainmentEndpointUnreg ceUnreg : nullToEmpty(contEndpoints)) {
            ContainmentEndpointKey key = new ContainmentEndpointKey(ceUnreg.getContextId(), ceUnreg.getContextType());
            Optional<ContainmentEndpoint> ceOptional = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.containmentEndpointIid(key), t);
            if (ceOptional.isPresent()) {
                deleteContainmentEndpointFromChilds(t, ceOptional.get());
            }
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, TO_SUCCESS_RPC_RESULT);
    }

    private void deleteAddressEndpointFromParents(ReadWriteTransaction t, AddressEndpoint endpoint) {
        ParentEndpointChoice parentEndpointChoice = endpoint.getParentEndpointChoice();
        for (ParentEndpoint parentEndpoint : EndpointUtils.getParentEndpoints(parentEndpointChoice)) {
            KeyedInstanceIdentifier<ChildEndpoint, ChildEndpointKey> childEp =
                    IidFactory.addressEndpointIid(EndpointUtils.createAddressEndpointKey(parentEndpoint))
                        .child(ChildEndpoint.class, new ChildEndpointKey(endpoint.getAddress(),
                                endpoint.getAddressType(), endpoint.getContextId(), endpoint.getContextType()));
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, childEp, t);
        }
        for (ParentContainmentEndpoint parentContEndpoint : EndpointUtils
            .getParentContainmentEndpoints(parentEndpointChoice)) {
            KeyedInstanceIdentifier<ChildEndpoint, ChildEndpointKey> childEp = IidFactory
                .containmentEndpointIid(new ContainmentEndpointKey(parentContEndpoint.getContextId(),
                        parentContEndpoint.getContextType()))
                .child(ChildEndpoint.class, new ChildEndpointKey(endpoint.getAddress(), endpoint.getAddressType(),
                        endpoint.getContextId(), endpoint.getContextType()));
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, childEp, t);
        }
    }

    private void deleteAddressEndpointFromChilds(ReadWriteTransaction t, AddressEndpoint endpoint) {
        for (ChildEndpoint childEndpoint : nullToEmpty(endpoint.getChildEndpoint())) {
            KeyedInstanceIdentifier<ParentEndpoint, ParentEndpointKey> parentEp =
                    IidFactory.addressEndpointIid(EndpointUtils.createAddressEndpointKey(childEndpoint))
                        .child(ParentEndpoint.class, new ParentEndpointKey(endpoint.getAddress(),
                                endpoint.getAddressType(), endpoint.getContextId(), endpoint.getContextType()));
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, parentEp, t);
        }
    }

    private void deleteContainmentEndpointFromChilds(ReadWriteTransaction t, ContainmentEndpoint endpoint) {
        for (ChildEndpoint child : nullToEmpty(endpoint.getChildEndpoint())) {
            AddressEndpointKey aeKey = new AddressEndpointKey(child.getAddress(), child.getAddressType(),
                    child.getContextId(), child.getContextType());
            KeyedInstanceIdentifier<ParentContainmentEndpoint, ParentContainmentEndpointKey> parentEp =
                    IidFactory.addressEndpointIid(aeKey).child(ParentContainmentEndpoint.class,
                            new ParentContainmentEndpointKey(endpoint.getContextId(), endpoint.getContextType()));
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, parentEp, t);
        }
    }

    @Override
    public void close() {}

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

}
