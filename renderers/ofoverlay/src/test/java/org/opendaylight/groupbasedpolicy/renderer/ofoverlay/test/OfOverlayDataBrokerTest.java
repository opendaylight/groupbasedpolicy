/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test;

import java.util.Collection;

import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;

import com.google.common.collect.ImmutableList;

public class OfOverlayDataBrokerTest extends CustomDataBrokerTest{

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(Nodes.class, OfOverlayNodeConfig.class, FlowCapableNodeConnector.class,
                MacAddress.class, Tenants.class, SubnetId.class, Endpoints.class);
    }

}
