/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

import com.google.common.net.InetAddresses;


public class SfcNshHeader {
    private final Ipv4Address nshTunIpDst;
    private final PortNumber nshTunUdpPort;
    private final Long nshNspToChain;
    private final Short nshNsiToChain;
    private final Long nshNspFromChain;
    private final Short nshNsiFromChain;
    private final Long nshMetaC1;
    private final Long nshMetaC2;

    private static final int TYPE = 1;

    private SfcNshHeader(SfcNshHeaderBuilder builder) {
        this.nshMetaC1 = builder.nshMetaC1;
        this.nshMetaC2 = builder.nshMetaC2;
        this.nshTunIpDst = builder.nshTunIpDst;
        this.nshTunUdpPort = builder.nshTunUdpPort;
        this.nshNspToChain = builder.nshNspToChain;
        this.nshNspFromChain = builder.nshNspFromChain;
        this.nshNsiToChain = builder.nshNsiToChain;
        this.nshNsiFromChain = builder.nshNsiFromChain;

    }

    public boolean isValid(SfcNshHeader sfcNshHeader) {
        if (sfcNshHeader.nshTunIpDst == null) return false;
        if (sfcNshHeader.nshNspToChain == null) return false;
        if (sfcNshHeader.nshNspFromChain == null) return false;
        if (sfcNshHeader.nshNsiToChain == null) return false;
        if (sfcNshHeader.nshNsiFromChain == null) return false;
        if (sfcNshHeader.nshMetaC1 == null) return false;
        if (sfcNshHeader.nshMetaC2 == null) return false;
        return true;
    }

    public Ipv4Address getNshTunIpDst() {
        return nshTunIpDst;
    }


    public PortNumber getNshTunUdpPort() {
        return nshTunUdpPort;
    }


    public Long getNshNspToChain() {
        return nshNspToChain;
    }

    public Short getNshNsiToChain() {
        return nshNsiToChain;
    }

    public Long getNshNspFromChain() {
        return nshNspFromChain;
    }



    public Short getNshNsiFromChain() {
        return nshNsiFromChain;
    }


    public Long getNshMetaC1() {
        return nshMetaC1;
    }

    public Long getNshMetaC2() {
        return nshMetaC2;
    }

    public static Long convertIpAddressToLong(Ipv4Address ipv4Address) {
        return (InetAddresses.coerceToInteger(InetAddresses.forString(ipv4Address.getValue()))) & 0xFFFFFFFFL;
    }

    public static int getType() {
        return TYPE;
    }

    public static class SfcNshHeaderBuilder {
        private Ipv4Address nshTunIpDst;
        private PortNumber nshTunUdpPort;
        private Long nshNspToChain;
        private  Short nshNsiToChain;
        private Long nshNspFromChain;
        private Short nshNsiFromChain;
        private Long nshMetaC1;
        private Long nshMetaC2;

        public SfcNshHeader build() {
            SfcNshHeader sfcNshHeader = new SfcNshHeader(this);
            return sfcNshHeader;
        }

        /**
         * SfcNshHeaderBuilder requires following in constructor:
         */
        public SfcNshHeaderBuilder() {

        }

        public SfcNshHeaderBuilder(SfcNshHeader sfcNshHeader) {

            this.nshTunIpDst = sfcNshHeader.nshTunIpDst;
            this.nshTunUdpPort = sfcNshHeader.nshTunUdpPort;
            this.nshNspToChain = sfcNshHeader.nshNspToChain;
            this.nshNsiToChain = sfcNshHeader.nshNsiToChain;
            this.nshNspFromChain = sfcNshHeader.nshNspFromChain;
            this.nshNsiFromChain = sfcNshHeader.nshNsiFromChain;
            this.nshMetaC1 = sfcNshHeader.nshMetaC1;
            this.nshMetaC2 = sfcNshHeader.nshMetaC2;
        }

        public SfcNshHeaderBuilder setNshTunIpDst(Ipv4Address nshTunIpDst) {
            this.nshTunIpDst = nshTunIpDst;
            return this;
        }

        public SfcNshHeaderBuilder setNshTunUdpPort(PortNumber nshTunUdpPort) {
            this.nshTunUdpPort = nshTunUdpPort;
            return this;
        }

        public SfcNshHeaderBuilder setNshNspToChain(Long nshNspToChain) {
            this.nshNspToChain = nshNspToChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNsiToChain(Short nshNsiToChain) {
            this.nshNsiToChain = nshNsiToChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNspFromChain(Long nshNspFromChain) {
            this.nshNspFromChain = nshNspFromChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshNsiFromChain(Short nshNsiFromChain) {
            this.nshNsiFromChain = nshNsiFromChain;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC1(Long nshMetaC1) {
            this.nshMetaC1 = nshMetaC1;
            return this;
        }

        public SfcNshHeaderBuilder setNshMetaC2(Long nshMetaC2) {
            this.nshMetaC2 = nshMetaC2;
            return this;
        }

    }



}
