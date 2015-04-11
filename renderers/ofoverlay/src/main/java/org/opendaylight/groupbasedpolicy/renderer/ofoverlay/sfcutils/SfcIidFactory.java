/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcIidFactory {

    private SfcIidFactory() {
        throw new UnsupportedOperationException();
    }

    public static InstanceIdentifier<ServiceFunctionChain> sfcIid(String sfcName) {

        ServiceFunctionChainKey serviceFunctionChainKey =
                new ServiceFunctionChainKey(sfcName);
        return InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey).build();
    }

    public static InstanceIdentifier<ServiceFunctionPath> sfpIid(String sfpName) {

        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(sfpName);
        return InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

    }
}
