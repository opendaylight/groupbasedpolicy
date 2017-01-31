/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.hostconfigs.VppNodeListener;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors.NeutronListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronVppMapper implements AutoCloseable {

    NeutronListener neutronListener;
    VppNodeListener vppNodeListener;
    private static final Logger LOG = LoggerFactory.getLogger(NeutronVppMapper.class);

    public NeutronVppMapper(String socketPath, String socketPrefix, String routingNode, DataBroker dataBroker) {
        SocketInfo socketInfo = new SocketInfo(socketPath, socketPrefix);
        vppNodeListener = new VppNodeListener(dataBroker, socketInfo);
        neutronListener = new NeutronListener(dataBroker, new NodeId(routingNode));
        LOG.info("Neutron VPP started!");
    }

    @Override
    public void close() {
        neutronListener.close();
        vppNodeListener.close();
    }
}
