/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;

public class FaasEndpointAugCovrgTest {

    EpRendererAugmentationRegistry epRendererAugmentationRegistry;

    @Before
    public void init() {
        epRendererAugmentationRegistry = mock(EpRendererAugmentationRegistry.class);
    }

    @Test
    public void testBuildEndpointAugmentation() throws Exception {
        FaasEndpointAug aug = new FaasEndpointAug(epRendererAugmentationRegistry);
        RegisterEndpointInput registerEndpointInput = mock(RegisterEndpointInput.class);
        FaasEndpointContextInput pix = mock(FaasEndpointContextInput.class);

        when(registerEndpointInput.getAugmentation(FaasEndpointContextInput.class)).thenReturn(pix);
        aug.buildEndpointAugmentation(registerEndpointInput);

        when(registerEndpointInput.getAugmentation(FaasEndpointContextInput.class)).thenReturn(null);
        aug.buildEndpointAugmentation(registerEndpointInput);
        aug.close();
    }

    @Test
    public void testBuildEndpointL3Augmentation() throws Exception {
        FaasEndpointAug aug = new FaasEndpointAug(epRendererAugmentationRegistry);
        RegisterEndpointInput registerEndpointInput = mock(RegisterEndpointInput.class);

        aug.buildEndpointL3Augmentation(registerEndpointInput);
        aug.close();
    }

    @Test
    public void testBuildL3PrefixEndpointAugmentation() throws Exception {
        FaasEndpointAug aug = new FaasEndpointAug(epRendererAugmentationRegistry);
        RegisterL3PrefixEndpointInput registerL3PrefixEndpointInput = mock(RegisterL3PrefixEndpointInput.class);

        aug.buildL3PrefixEndpointAugmentation(registerL3PrefixEndpointInput);
        aug.close();
    }

}
