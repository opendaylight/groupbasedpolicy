/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util;

public class FlowCacheCons {

    public static final String EQ = "=";
    public static final String OR = "|";

    public static enum Key {
        TCP_DST_PORT("tcpdestinationport"),
        TCP_SRC_PORT("tcpsourceport"),
        UDP_DST_PORT("udpdestinationport"),
        UDP_SRC_PORT("udpsourceport"),
        IP_PROTOCOL("ipprotocol"),
        ETH_PROTOCOL("ethernetprotocol"),
        IP_SOURCE("ipsource"),
        IP_DESTINATION("ipdestination");

        private String val;

        private Key(String val) {
            this.val = val;
        }

        public String get() {
            return val;
        }
    }

    public static enum Value {
        BYTES("bytes"),
        FRAMES("frames");

        private String val;

        private Value(String val) {
            this.val = val;
        }

        public String get() {
            return val;
        }
    }

}
