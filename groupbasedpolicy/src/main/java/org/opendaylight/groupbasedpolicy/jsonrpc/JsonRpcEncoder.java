/*
 * Copyright (C) 2013 EBay Software Foundation
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class JsonRpcEncoder extends MessageToMessageEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

    }
}
