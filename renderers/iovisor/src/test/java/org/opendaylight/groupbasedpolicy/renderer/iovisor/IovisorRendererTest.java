/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;

public class IovisorRendererTest extends GbpIovisorDataBrokerTest {

    private DataBroker dataBroker;
    private EpRendererAugmentationRegistry epRendererAugReg;
    private IovisorRenderer iovisorRenderer;
    private PolicyValidatorRegistry policyValidatorRegistry;

    @Before
    public void iovisorInit() {
        dataBroker = getDataBroker();
        epRendererAugReg = mock(EpRendererAugmentationRegistry.class);
        policyValidatorRegistry = mock(PolicyValidatorRegistry.class);
        iovisorRenderer = new IovisorRenderer(dataBroker, epRendererAugReg, policyValidatorRegistry);

    }
}
