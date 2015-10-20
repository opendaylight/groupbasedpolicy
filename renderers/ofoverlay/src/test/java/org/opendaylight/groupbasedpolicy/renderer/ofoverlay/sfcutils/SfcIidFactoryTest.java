/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcIidFactoryTest {

    @Test
    public void sfcIidTest() {
        SfcName sfcName = new SfcName("sfcName");
        InstanceIdentifier<ServiceFunctionChain> identifier = SfcIidFactory.sfcIid(sfcName);
        Assert.assertFalse(identifier.isWildcarded());
        Assert.assertEquals(sfcName, InstanceIdentifier.keyOf(identifier).getName());
    }

    @Test
    public void sfpIidTest() {
        SfpName sfpName = new SfpName("sfpName");
        InstanceIdentifier<ServiceFunctionPath> identifier = SfcIidFactory.sfpIid(sfpName);
        Assert.assertFalse(identifier.isWildcarded());
        Assert.assertEquals(sfpName, InstanceIdentifier.keyOf(identifier).getName());
    }

    @Test
    public void rspIidTest() {
        RspName rspName = new RspName("rspName");
        InstanceIdentifier<RenderedServicePath> identifier = SfcIidFactory.rspIid(rspName);
        Assert.assertFalse(identifier.isWildcarded());
        Assert.assertEquals(rspName, InstanceIdentifier.keyOf(identifier).getName());
    }
}
