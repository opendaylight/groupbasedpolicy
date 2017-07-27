/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class VppPathMapper {

    private static final String INTERFACE_PATH_BEFORE_KEY =
            "/ietf-interfaces:interfaces/ietf-interfaces:interface[ietf-interfaces:name='";
    private static final String INTERFACE_PATH_AFTER_KEY = "']";
    private static final int INTERFACE_PATH_MIN_LENGTH =
            INTERFACE_PATH_BEFORE_KEY.length() + INTERFACE_PATH_AFTER_KEY.length() + 1;
    private static final String BD_PATH_BEFORE_KEY = "/v3po:vpp/v3po:bridge-domains/v3po:bridge-domain[v3po:name='";
    private static final String BD_PATH_AFTER_KEY = "']";

    private VppPathMapper() {}

    public static String interfaceToRestPath(String interfaceName) {
        return INTERFACE_PATH_BEFORE_KEY + interfaceName + INTERFACE_PATH_AFTER_KEY;
    }

    public static Optional<InstanceIdentifier<Interface>> interfaceToInstanceIdentifier(@Nullable String restPath) {
        if (Strings.isNullOrEmpty(restPath)) {
            return Optional.absent();
        }
        if (restPath.length() < INTERFACE_PATH_MIN_LENGTH) {
            return Optional.absent();
        }
        if (!restPath.startsWith(INTERFACE_PATH_BEFORE_KEY)) {
            return Optional.absent();
        }
        if (!restPath.endsWith(INTERFACE_PATH_AFTER_KEY)) {
            return Optional.absent();
        }
        int endIndexInterfaceName = restPath.length() - INTERFACE_PATH_AFTER_KEY.length();
        String interfaceName = restPath.substring(INTERFACE_PATH_BEFORE_KEY.length(), endIndexInterfaceName);
        return Optional.of(VppIidFactory.getInterfaceIID(new InterfaceKey(interfaceName)));
    }

    public static Optional<String> interfacePathToInterfaceName(@Nullable String restPath) {
        if (Strings.isNullOrEmpty(restPath)) {
            return Optional.absent();
        }
        if (restPath.length() < INTERFACE_PATH_MIN_LENGTH) {
            return Optional.absent();
        }
        if (!restPath.startsWith(INTERFACE_PATH_BEFORE_KEY)) {
            return Optional.absent();
        }
        if (!restPath.endsWith(INTERFACE_PATH_AFTER_KEY)) {
            return Optional.absent();
        }
        int endIndexInterfaceName = restPath.length() - INTERFACE_PATH_AFTER_KEY.length();
        String interfaceName = restPath.substring(INTERFACE_PATH_BEFORE_KEY.length(), endIndexInterfaceName);
        return Optional.of(interfaceName);
    }

    public static String bridgeDomainToRestPath(String bridgeDomainName) {
        return BD_PATH_BEFORE_KEY + bridgeDomainName + BD_PATH_AFTER_KEY;
    }

}
