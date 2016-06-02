/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppEndpointConfEventTest {

    private final static String ADDRESS = "1.1.1.1/32";
    private final static ContextId CONTEXT_ID = new ContextId("ctx1");
    private final static VppEndpointKey BASIC_VPP_EP_KEY =
            new VppEndpointKey(ADDRESS, AddressType.class, CONTEXT_ID, ContextType.class);
    private final static InstanceIdentifier<VppEndpoint> BASIC_VPP_EP_IID =
            InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, BASIC_VPP_EP_KEY).build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_vppEpCreated() {
        VppEndpoint vppEndpoint = basicVppEpBuilder().build();
        VppEndpointConfEvent event = new VppEndpointConfEvent(BASIC_VPP_EP_IID, null, vppEndpoint);
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertFalse(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_vppEpDeleted() {
        VppEndpoint vppEndpoint = basicVppEpBuilder().build();
        VppEndpointConfEvent event = new VppEndpointConfEvent(BASIC_VPP_EP_IID, vppEndpoint, null);
        Assert.assertFalse(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_vppEpUpdated() {
        VppEndpoint vppEndpoint = basicVppEpBuilder().build();
        VppEndpointConfEvent event = new VppEndpointConfEvent(BASIC_VPP_EP_IID, vppEndpoint, vppEndpoint);
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_nullVppEp_Exception() {
        thrown.expect(IllegalArgumentException.class);
        new VppEndpointConfEvent(BASIC_VPP_EP_IID, null, null);
    }

    private VppEndpointBuilder basicVppEpBuilder() {
        return new VppEndpointBuilder().setAddress(ADDRESS)
            .setAddressType(AddressType.class)
            .setContextId(CONTEXT_ID)
            .setContextType(ContextType.class);
    }

}
