/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapResolverDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigureMapResolverCommand extends AbstractLispCommand<MapResolver>{
    private MapResolverDom mapResolverDom;

    public ConfigureMapResolverCommand(MapResolverDom mapResolverDom) {
        this.mapResolverDom = mapResolverDom;
    }

    @Override
    public InstanceIdentifier<MapResolver> getIid() {
        return VppIidFactory.getMapResolverIid(new MapResolverKey(mapResolverDom.getIpAddress()));
    }

    @Override
    public MapResolver getData() {
        return mapResolverDom.getSALObject();
    }

    @Override public String toString() {
        return "Operation: " + getOperation() + ", Iid: " + this.getIid() + ", " + mapResolverDom.toString();
    }
}
