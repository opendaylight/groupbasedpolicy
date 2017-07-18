/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

/**
 * Created by Shakib Ahmed on 7/18/17.
 */
public class InterfaceUtil {

    public static List<Interface> getOperationalInterfaces(DataBroker vppDataBroker) {
        final Optional<InterfacesState> opInterfaceState = GbpNetconfTransaction.read(vppDataBroker,
                LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(InterfacesState.class),
                GbpNetconfTransaction.RETRY_COUNT);

        if (!opInterfaceState.isPresent()) {
            return null;
        }

        return opInterfaceState.get().getInterface();
    }
}
