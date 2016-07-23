/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.base_endpoint;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;

public class EndpointAugmentorRegistryImplTest {

    private EndpointAugmentorRegistryImpl epAugmentorRegistry;
    private EndpointAugmentor epAugmentor;

    @Before
    public void init() {
        epAugmentorRegistry = new EndpointAugmentorRegistryImpl();
        epAugmentor = mock(EndpointAugmentor.class);
    }

    @Test
    public void testRegisterUnregister() throws Exception {
        epAugmentorRegistry.register(epAugmentor);
        Assert.assertEquals(1, epAugmentorRegistry.getEndpointAugmentors().size());

        epAugmentorRegistry.unregister(epAugmentor);
        Assert.assertEquals(0, epAugmentorRegistry.getEndpointAugmentors().size());
    }
}
