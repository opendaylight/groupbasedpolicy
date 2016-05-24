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

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.EndpointUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegKey;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RegisterEndpointInputVerificator {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterEndpointInputVerificator.class);

    public static ListenableFuture<RpcResult<Void>> verifyRegisterEndpointInput(RegisterEndpointInput input,
            ReadTransaction rTx) {
        ListenableFuture<RpcResult<Void>> result;
        List<AddressEndpointReg> addressEndpointRegs = nullToEmpty(input.getAddressEndpointReg());
        List<ContainmentEndpointReg> containmentEndpointRegs = nullToEmpty(input.getContainmentEndpointReg());

        for (AddressEndpointReg addressEndpointReg : addressEndpointRegs) {
            result = verifyAddressEndpointChilds(addressEndpointRegs, addressEndpointReg, rTx);
            if (result != null) {
                return result;
            }

            result = verifyAddressEndpointParents(addressEndpointRegs, containmentEndpointRegs, addressEndpointReg,
                    rTx);
            if (result != null) {
                return result;
            }
        }

        result = verifyContainmentEndpointChilds(addressEndpointRegs, containmentEndpointRegs, rTx);
        if (result != null) {
            return result;
        }

        return null;
    }

    private static ListenableFuture<RpcResult<Void>> verifyAddressEndpointChilds(
            List<AddressEndpointReg> addressEndpointRegs, AddressEndpointReg addressEndpointReg, ReadTransaction rTx) {
        for (ChildEndpoint childEndpoint : nullToEmpty(addressEndpointReg.getChildEndpoint())) {
            AddressEndpointRegKey key = new AddressEndpointRegKey(childEndpoint.getAddress(),
                    childEndpoint.getAddressType(), childEndpoint.getContextId(), childEndpoint.getContextType());
            AddressEndpointReg addressEndpointRegChild = findAddressEndpointReg(key, addressEndpointRegs);
            if (addressEndpointRegChild == null) {
                Optional<AddressEndpoint> addressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                        key.getAddressType(), key.getContextId(), key.getContextType())),
                                rTx);
                if (!addressEndpointOptional.isPresent()) {
                    LOG.debug("Child AddressEndpoint {} does not exist in RPC and DS.", childEndpoint.getKey());
                    return buildFailFeature(String.format("Child AddressEndpoint %s does not exist in RPC and DS.",
                            childEndpoint.getKey()));
                }
            } else {
                ParentEndpointChoice parentEndpointChoice = addressEndpointRegChild.getParentEndpointChoice();
                List<ParentEndpoint> parentEndpoints = EndpointUtils.getParentEndpoints(parentEndpointChoice);
                if (!parentEndpoints.contains(new ParentEndpointBuilder(addressEndpointReg).build())) {
                    LOG.debug("Child AddressEndpoint {} does not contain a parent AddressEndpoint {}.\nChild: {}",
                            addressEndpointRegChild.getKey(), addressEndpointReg.getKey(), addressEndpointRegChild);
                    return buildFailFeature(String.format(
                            "Child AddressEndpoint does not contain a parent AddressEndpoint."
                                    + "\nChild AddressEndpoint: %s" + "\nParent AddressEndpoint: %s",
                            addressEndpointRegChild.getKey(), addressEndpointReg.getKey()));
                }
            }
        }
        return null;
    }

    private static ListenableFuture<RpcResult<Void>> verifyAddressEndpointParents(
            List<AddressEndpointReg> addressEndpointRegs, List<ContainmentEndpointReg> containmentEndpointRegs,
            AddressEndpointReg addressEndpointReg, ReadTransaction rTx) {
        ParentEndpointChoice parentEndpointChoice = addressEndpointReg.getParentEndpointChoice();
        for (ParentEndpoint parentEndpoint : EndpointUtils.getParentEndpoints(parentEndpointChoice)) {
            AddressEndpointRegKey key = new AddressEndpointRegKey(parentEndpoint.getAddress(),
                    parentEndpoint.getAddressType(), parentEndpoint.getContextId(), parentEndpoint.getContextType());
            AddressEndpointReg addressEndpointRegParent = findAddressEndpointReg(key, addressEndpointRegs);
            if (addressEndpointRegParent == null) {
                Optional<AddressEndpoint> addressEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                        key.getAddressType(), key.getContextId(), key.getContextType())),
                                rTx);
                if (!addressEndpointOptional.isPresent()) {
                    LOG.debug("Parent AddressEndpoint {} does not exist in RPC and DS.", parentEndpoint.getKey());
                    return buildFailFeature(String.format("Parent AddressEndpoint %s does not exist in RPC and DS.",
                            parentEndpoint.getKey()));
                }
            } else {
                List<ChildEndpoint> childEndpoints = addressEndpointRegParent.getChildEndpoint();
                if (!nullToEmpty(childEndpoints).contains(new ChildEndpointBuilder(addressEndpointReg).build())) {
                    LOG.debug("Parent AddressEndpoint {} does not contain a child AddressEndpoint {}.\nParent: {}",
                            addressEndpointRegParent.getKey(), addressEndpointReg.getKey(), addressEndpointRegParent);
                    return buildFailFeature(String.format(
                            "Parent AddressEndpoint does not contain a child AddressEndpoint."
                                    + "\nParent AddressEndpoint: %s" + "\nChild AddressEndpoint: %s",
                            addressEndpointRegParent.getKey(), addressEndpointReg.getKey()));
                }
            }
        }
        for (ParentContainmentEndpoint parentEndpoint : EndpointUtils
            .getParentContainmentEndpoints(parentEndpointChoice)) {
            ContainmentEndpointRegKey key =
                    new ContainmentEndpointRegKey(parentEndpoint.getContextId(), parentEndpoint.getContextType());
            ContainmentEndpointReg containmentEndpointRegParent =
                    findContainmentEndpointReg(key, containmentEndpointRegs);
            if (containmentEndpointRegParent == null) {
                Optional<ContainmentEndpoint> containmentEndpointOptional =
                        DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, IidFactory.containmentEndpointIid(
                                new ContainmentEndpointKey(key.getContextId(), key.getContextType())), rTx);
                if (!containmentEndpointOptional.isPresent()) {
                    LOG.debug("Parent ContainmentEndpoint {} does not exist in RPC and DS.", parentEndpoint.getKey());
                    return buildFailFeature(String.format("Parent ContainmentEndpoint %s does not exist in RPC and DS.",
                            parentEndpoint.getKey()));
                }
            } else {
                List<ChildEndpoint> childEndpoints = containmentEndpointRegParent.getChildEndpoint();
                if (!nullToEmpty(childEndpoints).contains(new ChildEndpointBuilder(addressEndpointReg).build())) {
                    LOG.debug("Parent ContainmentEndpoint {} does not contain a child AddressEndpoint {}.\nParent: {}",
                            containmentEndpointRegParent.getKey(), addressEndpointReg.getKey(),
                            containmentEndpointRegParent);
                    return buildFailFeature(String.format(
                            "Parent ContainmentEndpoint does not contain a child AddressEndpoint."
                                    + "\nParent ContainmentEndpoint: %s" + "\nChild AddressEndpoint: %s",
                            containmentEndpointRegParent.getKey(), addressEndpointReg.getKey()));
                }
            }
        }
        return null;
    }

    private static ListenableFuture<RpcResult<Void>> verifyContainmentEndpointChilds(
            List<AddressEndpointReg> addressEndpointRegs, List<ContainmentEndpointReg> containmentEndpointRegs,
            ReadTransaction rTx) {
        for (ContainmentEndpointReg containmentEndpointReg : nullToEmpty(containmentEndpointRegs)) {
            for (ChildEndpoint childEndpoint : nullToEmpty(containmentEndpointReg.getChildEndpoint())) {
                AddressEndpointRegKey key = new AddressEndpointRegKey(childEndpoint.getAddress(),
                        childEndpoint.getAddressType(), childEndpoint.getContextId(), childEndpoint.getContextType());
                AddressEndpointReg addressEndpointRegChild = findAddressEndpointReg(key, addressEndpointRegs);
                if (addressEndpointRegChild == null) {
                    Optional<AddressEndpoint> addressEndpointOptional =
                            DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                                    IidFactory.addressEndpointIid(new AddressEndpointKey(key.getAddress(),
                                            key.getAddressType(), key.getContextId(), key.getContextType())),
                                    rTx);
                    if (!addressEndpointOptional.isPresent()) {
                        LOG.debug("Child AddressEndpoint {} does not exist in RPC and DS.", childEndpoint.getKey());
                        return buildFailFeature(String.format("Child AddressEndpoint %s does not exist in RPC and DS.",
                                childEndpoint.getKey()));
                    }
                } else {
                    List<ParentContainmentEndpoint> parentContainmentEndpoints = EndpointUtils
                        .getParentContainmentEndpoints(addressEndpointRegChild.getParentEndpointChoice());
                    if (!parentContainmentEndpoints
                        .contains(new ParentContainmentEndpointBuilder(containmentEndpointReg).build())) {
                        LOG.debug(
                                "Child AddressEndpoint {} does not contain a parent ContainmentEndpoint {}.\nChild: {}",
                                addressEndpointRegChild.getKey(), containmentEndpointReg.getKey(),
                                addressEndpointRegChild);
                        return buildFailFeature(String.format(
                                "Child AddressEndpoint does not contain a parent ContainmentEndpoint."
                                        + "\nChild AddressEndpoint: %s" + "\nParent ContainmentEndpoint: %s",
                                addressEndpointRegChild.getKey(), containmentEndpointReg.getKey()));
                    }
                }
            }
        }
        return null;
    }

    private static AddressEndpointReg findAddressEndpointReg(AddressEndpointRegKey key,
            List<AddressEndpointReg> addressEndpointRegs) {
        for (AddressEndpointReg addressEndpointReg : addressEndpointRegs) {
            if (addressEndpointReg.getKey().equals(key)) {
                return addressEndpointReg;
            }
        }
        return null;
    }

    private static ContainmentEndpointReg findContainmentEndpointReg(ContainmentEndpointRegKey key,
            List<ContainmentEndpointReg> addressEndpointRegs) {
        for (ContainmentEndpointReg containmentEndpointReg : addressEndpointRegs) {
            if (containmentEndpointReg.getKey().equals(key)) {
                return containmentEndpointReg;
            }
        }
        return null;
    }

    private static ListenableFuture<RpcResult<Void>> buildFailFeature(String message) {
        return Futures
            .immediateFuture(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.PROTOCOL, message).build());
    }

    private static <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
