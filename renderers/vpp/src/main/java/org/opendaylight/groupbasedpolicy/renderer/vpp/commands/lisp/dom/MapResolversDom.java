/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolver;

import java.util.List;

public class MapResolversDom implements CommandModel {
    private List<MapResolver> mapResolvers;

    public List<MapResolver> getMapResolvers() {
        return mapResolvers;
    }

    public void setMapResolvers(List<MapResolver> mapResolvers) {
        this.mapResolvers = mapResolvers;
    }

    @Override
    public MapResolvers getSALObject() {
        return new MapResolversBuilder().setMapResolver(mapResolvers).build();
    }

    @Override public String toString() {
        return "MapResolvers{" + "mapResolvers=" + mapResolvers + '}';
    }
}
