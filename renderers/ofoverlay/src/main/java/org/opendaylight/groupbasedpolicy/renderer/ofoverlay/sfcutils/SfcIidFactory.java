/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcIidFactory {

    private SfcIidFactory() {
        throw new UnsupportedOperationException();
    }

    public static InstanceIdentifier<ServiceFunctionChain> sfcIid(SfcName sfcName) {

        ServiceFunctionChainKey serviceFunctionChainKey = new ServiceFunctionChainKey(sfcName);
        return InstanceIdentifier.builder(ServiceFunctionChains.class)
            .child(ServiceFunctionChain.class, serviceFunctionChainKey)
            .build();
    }

    public static InstanceIdentifier<ServiceFunctionPath> sfpIid(SfpName sfpName) {

        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(sfpName);
        return InstanceIdentifier.builder(ServiceFunctionPaths.class)
            .child(ServiceFunctionPath.class, serviceFunctionPathKey)
            .build();
    }

    public static InstanceIdentifier<RenderedServicePath> rspIid(RspName rspName) {

        RenderedServicePathKey rspKey = new RenderedServicePathKey(rspName);
        return InstanceIdentifier.builder(RenderedServicePaths.class).child(RenderedServicePath.class, rspKey).build();
    }

    public static InstanceIdentifier<SfcOfRendererConfig> sfcOfRendererConfigIid() {
        return InstanceIdentifier.builder(SfcOfRendererConfig.class).build();

    }
}
