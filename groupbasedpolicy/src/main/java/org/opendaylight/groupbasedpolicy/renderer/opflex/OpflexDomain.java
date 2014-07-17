/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 *
 * An OpFlex domain is a logical grouping of OpFlex entities.
 * The domain aggregates entites and provides methods so that they
 * can be looked up or referenced by domain.
 *
 * The domain field is only present in the OpFlex Identity request message.
 *
 * @author tbachman
 *
 */
public class OpflexDomain {
    String domain;
    ConcurrentMap<String, OpflexAgent> opflexAgents = null;
    ConcurrentMap<String, OpflexRpcServer> opflexServers = null;

    OpflexDomain() {
        opflexAgents = new ConcurrentHashMap<String, OpflexAgent>();
        opflexServers = new ConcurrentHashMap<String, OpflexRpcServer>();
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public ConcurrentMap<String, OpflexAgent> getOpflexAgents() {
        return opflexAgents;
    }

    public void setOpflexAgents(
            ConcurrentMap<String, OpflexAgent> opflexAgents) {
        this.opflexAgents = opflexAgents;
    }

    public ConcurrentMap<String, OpflexRpcServer> getOpflexServers() {
        return opflexServers;
    }

    public void setOpflexServers(
            ConcurrentMap<String, OpflexRpcServer> opflexServers) {
        this.opflexServers = opflexServers;
    }

    public void removeOpflexAgent(OpflexAgent agent) {
        opflexAgents.remove(agent.getIdentity());
    }

    public void removeOpflexServer(OpflexRpcServer server) {
        opflexServers.remove(server.getId());
    }

    public List<OpflexRpcServer> getOpflexServerList() {
        return new ArrayList<OpflexRpcServer>(opflexServers.values());
    }

    /**
     * Clean up all the entities contained by this domain. The
     * connection service also owns these references, so we
     * provide notifications to the connection service so that
     * it can clean up as well.
     */
    public void cleanup() {
        List<String> agents = new ArrayList<String>(opflexAgents.keySet());
        List<String> servers = new ArrayList<String>(opflexServers.keySet());
        for (String agent : agents) {
            OpflexAgent conn = opflexAgents.remove(agent);
            conn.getEndpoint().getChannel().disconnect();
        }
        for (String srv : servers) {
            OpflexRpcServer server = opflexServers.get(srv);
            if (server.getRpcServer().getChannel() != null) {
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    /**
     * Add an {@link OpflexAgent} to the domain
     *
     * @param agent The agent to add
     */
    public void addOpflexAgent(OpflexAgent agent) {
        opflexAgents.put(agent.getIdentity(), agent);
    }

    /**
     * Return the {@link OpflexAgent} associated
     * with this identity
     *
     * @param identity A string representing the connections identity
     * @return The connection represented by that key, or null if not found
     */
    public OpflexAgent getOpflexAgent(String identity) {
        return opflexAgents.get(identity);
    }

    /**
     * Add the List of servers to the domain
     *
     * @param serverList List of new servers to start
     */
    public void addServers(List<OpflexRpcServer> serverList) {

        if (serverList == null) return;

        /*
         * Check to see if there's already a server
         * with this identity, and if so, close it
         * and replace it with this one.
         */
        for ( OpflexRpcServer srv: serverList ) {
            OpflexRpcServer server = opflexServers.get(srv.getId());
            if (server != null) {
                if ( !server.sameServer(srv)) {
                    OpflexRpcServer oldServer = opflexServers.remove(srv.getId());
                    oldServer.getRpcServer().getChannel().disconnect();
                    opflexServers.put(srv.getId(), srv);
                    srv.start();
                }
            }
            else {
                opflexServers.put(srv.getId(), srv);
                srv.start();
            }
        }
    }

    /**
     * Drop the list of servers from the domain
     *
     * @param oldServers The list of servers to drop
     *
     * TODO: Should we provide notifications to or close
     *       the connections that were spawned by the
     *       deleted servers?
     */
    public void dropServers(List<String> oldServers) {
        OpflexRpcServer server;

        /*
         * Check to see if there's already a server
         * with this identity, and if so, close it
         * and replace it with this one.
         */
        for (String srv: oldServers) {
            if (opflexServers.containsKey(srv)) {
                server = opflexServers.remove(srv);
                server.getRpcServer().getChannel().disconnect();
            }
        }
    }

    /**
     * Check the new configuration of the servers against the
     * existing, and if different, delete the old server and
     * replace it with a new server running the updated parameters.
     *
     * @param serverList The new server configurations
     */
    public void updateServers(List<OpflexRpcServer> serverList) {
        /* Get the new list of configured servers in this domain */
        List<OpflexRpcServer> updateServers = new ArrayList<OpflexRpcServer>();
        List<OpflexRpcServer> newServers = new ArrayList<OpflexRpcServer>();
        List<String> newList = new ArrayList<String>();

        for (OpflexRpcServer srv : serverList) {
            newList.add(srv.getId());
        }

        /* Get the list of currently configured servers in this domain*/
        List<String> currentList =
                new ArrayList<String>(opflexServers.keySet());

        /* Make the add/drop/update lists */
        List<String> addList = new ArrayList<String>(newList);
        List<String> dropList = new ArrayList<String>(currentList);
        List<String> updateList = new ArrayList<String>(newList);

        addList.removeAll(currentList);
        dropList.removeAll(newList);
        updateList.removeAll(addList);

        /*
         * Create a list of new servers by skipping any servers in the
         * list that are already configured (i.e. same IP/socket and set
         * of roles) -- no need to take them down
         */
        for (OpflexRpcServer srv: serverList) {
            /*
             * If this in our update list, check parameters
             * to see if we really need to update it
             */
            if (updateList.contains(srv.getId())) {
                OpflexRpcServer s = opflexServers.get(srv.getId());
                if (s != null && s.getRoles().containsAll(srv.getRoles())) {
                    continue;
                }
                updateServers.add(srv);

            }
            if (addList.contains(srv.getId())) {
                newServers.add(srv);
            }
        }


        dropServers(dropList);
        addServers(newServers);
        addServers(updateServers);
    }


}
