/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import io.netty.channel.Channel;

import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;

public class Connection {
    private String identifier;
    private Channel channel;
    private JsonRpcEndpoint endpoint;

    public Long getIdCounter() {
        return idCounter;
    }

    public void setIdCounter(Long idCounter) {
        this.idCounter = idCounter;
    }

    private Long idCounter;

    public Connection(String identifier, Channel channel) {

        super();

        this.identifier = identifier;
        this.channel = channel;
        this.idCounter = 0L;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setEndpoint(JsonRpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public JsonRpcEndpoint getEndpoint() {
        return this.endpoint;
    }


    public void sendMessage(String message) {
        channel.writeAndFlush(message);
        this.idCounter++;
    }

    public void disconnect() {
        channel.close();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Connection other = (Connection) obj;
        if (identifier == null) {
            if (other.identifier != null) return false;
        } else if (!identifier.equals(other.identifier)) return false;
        return true;
    }
}
