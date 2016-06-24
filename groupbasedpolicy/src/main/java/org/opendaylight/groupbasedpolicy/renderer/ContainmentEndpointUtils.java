/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerExternalContainmentEndpointKey;

public class ContainmentEndpointUtils {

    public static PeerExternalContainmentEndpointKey toPeerExtContEpKey(ContainmentEndpointKey peerContEpKey) {
        return new PeerExternalContainmentEndpointKey(peerContEpKey.getContextId(), peerContEpKey.getContextType());
    }

    public static ContainmentEndpointKey fromPeerExtContEpKey(PeerExternalContainmentEndpointKey peerExtContEpKey) {
        return new ContainmentEndpointKey(peerExtContEpKey.getContextId(), peerExtContEpKey.getContextType());
    }
}
