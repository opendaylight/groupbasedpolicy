/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.servers.grouping.MapServersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.servers.grouping.map.servers.MapServer;

import java.util.List;

/**
 * Created by Shakib Ahmed on 3/20/17.
 */
public class MapServersDom implements CommandModel {
    List<MapServer> mapServers;

    public List<MapServer> getMapServers() {
        return mapServers;
    }

    public void setMapServers(List<MapServer> mapServers) {
        this.mapServers = mapServers;
    }

    @Override
    public MapServers getSALObject() {
        return new MapServersBuilder().setMapServer(mapServers).build();
    }
}
