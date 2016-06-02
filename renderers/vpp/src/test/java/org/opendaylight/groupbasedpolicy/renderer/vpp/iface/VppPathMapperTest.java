/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;


import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class VppPathMapperTest {

    private static final String INTERFACE_PATH = "/ietf-interfaces:interfaces/ietf-interfaces:interface";
    private static final String INTERFACE_KEY_START = "[ietf-interfaces:name='";
    private static final String INTERFACE_KEY_END = "']";
    private static final String INTERFACE_NAME = "12345";
    private static final InterfaceKey INTERFACE_KEY = new InterfaceKey(INTERFACE_NAME);

    @Test
    public void testToInstanceIdentifier() {
        String restPath = INTERFACE_PATH + INTERFACE_KEY_START + INTERFACE_NAME + INTERFACE_KEY_END;
        Optional<InstanceIdentifier<Interface>> optIid = VppPathMapper.interfaceToInstanceIdentifier(restPath);
        Assert.assertTrue(optIid.isPresent());
        InstanceIdentifier<Interface> iid = optIid.get();
        Assert.assertNotNull(iid);
        Assert.assertEquals(INTERFACE_KEY, iid.firstKeyOf(Interface.class));
    }
}
