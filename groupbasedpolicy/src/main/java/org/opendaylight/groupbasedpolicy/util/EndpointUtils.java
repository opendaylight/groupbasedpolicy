/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentContainmentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.containment.endpoint._case.ParentContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;

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
        if (parentEndpointChoice instanceof ParentEndpointCase) {
            ParentEndpointCase parentEpCase = (ParentEndpointCase) parentEndpointChoice;
            List<ParentEndpoint> parentEndpoints = parentEpCase.getParentEndpoint();
            if (parentEndpoints != null) {
                return parentEndpoints;
            }
        }
        return Collections.emptyList();
    }
}
