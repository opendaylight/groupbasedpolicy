/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class EndpointUtils {

    public static AddressEndpointKey createAddressEndpointKey(ChildEndpoint child) {
        return new AddressEndpointKey(child.getAddress(), child.getAddressType(), child.getContextId(),
                child.getContextType());
    }

    public static AddressEndpointKey createAddressEndpointKey(ParentEndpoint parent) {
        return new AddressEndpointKey(parent.getAddress(), parent.getAddressType(), parent.getContextId(),
                parent.getContextType());
    }

    public static @Nonnull List<ParentContainmentEndpoint> getParentContainmentEndpoints(
            @Nullable ParentEndpointChoice parentEndpointChoice) {
        if (parentEndpointChoice instanceof ParentContainmentEndpointCase) {
            ParentContainmentEndpointCase parentCeCase = (ParentContainmentEndpointCase) parentEndpointChoice;
            List<ParentContainmentEndpoint> parentContainmentEndpoints = parentCeCase.getParentContainmentEndpoint();
            if (parentContainmentEndpoints != null) {
                return parentContainmentEndpoints;
            }
        }
        return Collections.emptyList();
    }

    public static @Nonnull List<ParentEndpoint> getParentEndpoints(
            @Nullable ParentEndpointChoice parentEndpointChoice) {
        if (parentEndpointChoice != null && parentEndpointChoice instanceof ParentEndpointCase) {
            ParentEndpointCase parentEpCase = (ParentEndpointCase) parentEndpointChoice;
            List<ParentEndpoint> parentEndpoints = parentEpCase.getParentEndpoint();
            if (parentEndpoints != null) {
                return parentEndpoints;
            }
        }
        return Collections.emptyList();
    }

    public static boolean isExternalEndpoint(@Nonnull DataBroker dataBroker, @Nonnull AddressEndpoint addrEp) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        List<ListenableFuture<Boolean>> results = new ArrayList<>();
        if(addrEp.getEndpointGroup() == null) {
            return false;
        }
        for (EndpointGroupId epgId : addrEp.getEndpointGroup()) {
            results.add(Futures.transform(
                    rTx.read(LogicalDatastoreType.CONFIGURATION,
                            IidFactory.externalImplicitGroupIid(addrEp.getTenant(), epgId)),
                    new Function<Optional<ExternalImplicitGroup>, Boolean>() {

                        @Override
                        public Boolean apply(Optional<ExternalImplicitGroup> input) {
                            return input.isPresent();
                        }
                    }));
        }
        rTx.close();
        try {
            List<Boolean> list = Futures.allAsList(results).get();
            return list.stream().anyMatch(Boolean::booleanValue);
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    public static Optional<ExternalLocationCase> getExternalLocationFrom(AddressEndpointWithLocation input) {
        if (input.getAbsoluteLocation() != null && input.getAbsoluteLocation().getLocationType() != null
                && input.getAbsoluteLocation().getLocationType() instanceof ExternalLocationCase) {
            return Optional.of((ExternalLocationCase) input.getAbsoluteLocation().getLocationType());
        }
        return Optional.absent();
    }
}
