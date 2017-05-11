/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;

import com.google.common.util.concurrent.ListenableFuture;

public interface VPPLocationProvider {

    ListenableFuture<Void> createLocationForVppEndpoint(@Nonnull VppEndpoint vppEndpoint);

    ListenableFuture<Void> deleteLocationForVppEndpoint(@Nonnull VppEndpoint vppEndpoint);

    ListenableFuture<Void> replaceLocationForEndpoint(@Nonnull ExternalLocationCase location,
            @Nonnull AddressEndpointWithLocationKey addrEpWithLocKey);

}
