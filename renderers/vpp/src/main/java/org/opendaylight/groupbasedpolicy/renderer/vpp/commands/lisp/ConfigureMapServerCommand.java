/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapServerDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 3/20/17.
 */
public class ConfigureMapServerCommand extends AbstractLispCommand<MapServer> {
    private MapServerDom mapServerDom;

    public ConfigureMapServerCommand(MapServerDom mapServerDom) {
        this.mapServerDom = mapServerDom;
    }

    @Override
    public InstanceIdentifier<MapServer> getIid() {
        return VppIidFactory.getMapServerIid(new MapServerKey(mapServerDom.getIpAddress()));
    }

    @Override
    public MapServer getData() {
        return mapServerDom.getSALObject();
    }
}
