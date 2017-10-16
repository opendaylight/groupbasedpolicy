/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util.HandlerUtil;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/3/17.
 */
public class SubnetHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SubnetHandler.class);

    private final DataBroker dataBroker;

    public SubnetHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void processCreatedNeutronDto(Subnet subnet) {
        GbpSubnet gbpSubnet = HandlerUtil.toGbpSubnet(subnet);

        if (gbpSubnet != null) {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                     HandlerUtil.getInstanceIdentifier(gbpSubnet.getId()),
                     gbpSubnet, true);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    public void processUpdatedNeutronDto(Subnet oldSubnet, Subnet delta) {
        Preconditions.checkState(oldSubnet.getUuid().equals(delta.getUuid()),
                "Uuid change not allowed!");
        GbpSubnet gbpSubnet = HandlerUtil.toGbpSubnet(delta);

        if (gbpSubnet != null) {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    HandlerUtil.getInstanceIdentifier(gbpSubnet.getId()),
                    gbpSubnet, true);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    public void processDeletedNeutronDto(Subnet subnet) {
        GbpSubnet gbpSubnet = HandlerUtil.toGbpSubnet(subnet);
        if (gbpSubnet != null) {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    HandlerUtil.getInstanceIdentifier(gbpSubnet.getId()),
                    gbpSubnet, true);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                                           HandlerUtil.getInstanceIdentifier(gbpSubnet.getId()),
                                           rwTx);
            DataStoreHelper.submitToDs(rwTx);
        }
    }
}