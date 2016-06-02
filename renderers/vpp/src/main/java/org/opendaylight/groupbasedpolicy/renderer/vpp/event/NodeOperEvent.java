/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class NodeOperEvent extends DtoChangeEvent<Node> {

    public NodeOperEvent(InstanceIdentifier<Node> iid, Node before, Node after) {
        super(iid, before, after);
        if (before != null) {
            Preconditions.checkArgument(before.getAugmentation(NetconfNode.class) != null);
        }
        if (after != null) {
            Preconditions.checkArgument(after.getAugmentation(NetconfNode.class) != null);
        }
    }

    public boolean isAfterConnected() {
        return isConnected(after);
    }

    public boolean isBeforeConnected() {
        return isConnected(before);
    }

    private static boolean isConnected(Optional<Node> potentialNode) {
        if (!potentialNode.isPresent()) {
            return false;
        }
        NetconfNode netconfNode = potentialNode.get().getAugmentation(NetconfNode.class);
        if (ConnectionStatus.Connected == netconfNode.getConnectionStatus()) {
            return true;
        }
        return false;
    }

}
