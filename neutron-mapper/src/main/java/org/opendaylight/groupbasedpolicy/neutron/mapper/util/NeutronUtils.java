/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

public final class NeutronUtils {

    public static final String EGRESS = "egress";
    public static final String INGRESS = "ingress";
    public static final String IPv6 = "IPv6";
    public static final String IPv4 = "IPv4";
    public static final String NULL = "null";
    public static final String UDP = "udp";
    public static final String TCP = "tcp";
    public static final String ICMP = "icmp";

    private NeutronUtils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }
}
