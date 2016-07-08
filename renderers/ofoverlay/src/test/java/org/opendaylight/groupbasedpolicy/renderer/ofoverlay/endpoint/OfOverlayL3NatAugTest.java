/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressInputBuilder;

public class OfOverlayL3NatAugTest {

    private OfOverlayL3NatAug ofOverlayL3NatAug;
    private EpRendererAugmentationRegistry epRendererAugmentationRegistry;

    @Before
    public void init() {
        epRendererAugmentationRegistry = mock(EpRendererAugmentationRegistry.class);
        ofOverlayL3NatAug = new OfOverlayL3NatAug(epRendererAugmentationRegistry);
    }

    @Test
    public void testConstructor() throws Exception {
        OfOverlayL3NatAug other = new OfOverlayL3NatAug(epRendererAugmentationRegistry);
        other.close();
    }

    @Test
    public void testBuildEndpointAugmentation() {
        RegisterEndpointInput input = new RegisterEndpointInputBuilder().build();
        // no op
        assertNull(ofOverlayL3NatAug.buildEndpointAugmentation(input));
    }

    @Test
    public void testBuildEndpointL3Augmentation() {
        NatAddressInput natAddressInput = new NatAddressInputBuilder().setNatAddress(
                new IpAddress(new Ipv4Address("10.0.0.2"))).build();
        RegisterEndpointInput input =
                new RegisterEndpointInputBuilder().addAugmentation(NatAddressInput.class,
                        natAddressInput).build();
        assertNotNull(ofOverlayL3NatAug.buildEndpointL3Augmentation(input));
    }

    @Test
    public void testBuildEndpointL3Augmentation_noAug() {
        RegisterEndpointInput input = new RegisterEndpointInputBuilder().build();
        assertNull(ofOverlayL3NatAug.buildEndpointL3Augmentation(input));
    }

    @Test
    public void testBuildL3PrefixEndpointAugmentation() {
        RegisterL3PrefixEndpointInput input = new RegisterL3PrefixEndpointInputBuilder().build();
        // no op

        assertNull(ofOverlayL3NatAug.buildL3PrefixEndpointAugmentation(input));
    }

    @Test
    public void testClose() {
        //        fail("Not yet implemented");
    }

}
