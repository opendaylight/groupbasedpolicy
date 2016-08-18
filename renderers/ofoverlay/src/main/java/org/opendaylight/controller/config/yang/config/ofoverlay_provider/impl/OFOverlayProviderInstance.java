/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

public class OFOverlayProviderInstance implements AutoCloseable {


    public OFOverlayRenderer renderer;

    public OFOverlayProviderInstance(DataBroker dataBroker,
            PacketProcessingService packetProcessingService,
            SalFlowService flowService,
            NotificationService notificationService,
            EpRendererAugmentationRegistry epRendererAugmentationRegistry,
            PolicyValidatorRegistry policyValidatorRegistry,
            StatisticsManager statisticsManager,
            short tableOffset
            ) {
        renderer = new OFOverlayRenderer(dataBroker,
                packetProcessingService,
                flowService,
                notificationService,
                epRendererAugmentationRegistry,
                policyValidatorRegistry,
                statisticsManager,
                tableOffset);
    }

    @Override
    public void close() throws Exception {
        renderer.close();
    }

}
