/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer;

import org.opendaylight.groupbasedpolicy.renderer.lib.jsonrpc.JsonRpcEndpoint;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class ChannelConnectionHandler implements ChannelFutureListener {
    Connection peer;
    OpflexConnectionService connectionService;
    public Connection getPeer() {
        return peer;
    }
    public void setPeer(Connection peer) {
        this.peer = peer;
    }
    public OpflexConnectionService getConnectionService() {
        return connectionService;
    }
    public void setConnectionService(OpflexConnectionService connectionService) {
        this.connectionService = connectionService;
    }
    @Override
    public void operationComplete(ChannelFuture arg0) throws Exception {
        connectionService.channelClosed(peer);
    }
}
