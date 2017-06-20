/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.api;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Bridge domain manager interface.
 */
public interface BridgeDomainManager {

    /**
     * Creates a bridge domain on VPP node and it also adds tunnels of the bridge domain to VXLAN
     * full mesh topology
     *
     * @param bridgeDomainName name of bridge domain
     * @param vni VXLAN VNI used in full mesh topology for the given bridge domain
     * @param vppNodeId VPP node where the bridge domain should be created
     * @return {@link ListenableFuture}
     */
    ListenableFuture<Void> createVxlanBridgeDomainOnVppNode(@Nonnull String bridgeDomainName, @Nonnull VxlanVni vni, @Nonnull NodeId vppNodeId);

    /**
     * Creates a bridge domain on VPP node and it also adds tunnels of the bridge domain to VLAN
     * full mesh topology
     *
     * @param bridgeDomainName name of bridge domain
     * @param vlanId VLAN ID used in full mesh topology for the given bridge domain
     * @param vppNodeId VPP node where the bridge domain should be created
     * @return {@link ListenableFuture}
     */

    ListenableFuture<Void> createVlanBridgeDomainOnVppNode(@Nonnull String bridgeDomainName, @Nonnull VlanId vlanId, @Nonnull NodeId vppNodeId);

    /**
     * Removes a bridge domain from VPP node and it also removes tunnels of the bridge domain from
     * VXLAN full mesh topology
     *
     * @param bridgeDomainName name of bridge domain
     * @param vppNode VPP node where the bridge domain should be removed from
     * @return {@link ListenableFuture}
     */
    ListenableFuture<Void> removeBridgeDomainFromVppNode(@Nonnull String bridgeDomainName, NodeId vppNode);

}
