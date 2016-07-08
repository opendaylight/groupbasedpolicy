/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.utils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;

import com.google.common.base.Optional;

public class IovisorModuleUtils {

    private IovisorModuleUtils() {
    }

    /**
     * Make sure the specified IOvisor module Uri exists in the datastore.
     * @param dataBroker An instance of the {@link DataBroker}
     * @param iovisorModuleUri The Uri of the {@link IovisorModule} we want to validate
     * @return <code>true</code> if validated, else, <code>false</code>
     */
    public static boolean validateIovisorModuleInstance(DataBroker dataBroker, Uri iovisorModuleUri) {
        Optional<IovisorModuleInstances> res = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                                                                         IovisorIidFactory.iovisorModuleInstanceWildCardIid(),
                                                                         dataBroker.newReadOnlyTransaction());
        if (res.isPresent()) {
            for (IovisorModuleInstance instance : res.get().getIovisorModuleInstance()) {
                if (instance.getUri().equals(iovisorModuleUri)) {
                    return true;
                }
            }
        }
        return false;
    }
}
