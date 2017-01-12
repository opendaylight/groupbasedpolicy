/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpCodeRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpTypeRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.IpProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.OtherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.UdpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.icmp.IcmpNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.other.OtherNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.tcp.TcpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.tcp.TcpNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.udp.UdpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.udp.UdpNodesBuilder;

import com.google.common.base.Preconditions;

public class GbpAceBuilder {

    private final String name;
    private Short protocol;
    private SourcePortRangeBuilder sourcePortRangeBuilder;
    private DestinationPortRangeBuilder destinationPortRangeBuilder;
    private AceIpVersion aceIpVersion;
    private AceIpv4 aceIpv4;
    private AceIpv6 aceIpv6;
    private IpProtocol ipProtocol;
    private Actions action; // deny is a default action in the model

    private static final Short FIRST_ICMP = 0;
    private static final Short LAST_ICMP = 254;
    
    GbpAceBuilder(String name) {
        this.name = Preconditions.checkNotNull(name, "Cannot build rule with empty name.");
        this.sourcePortRangeBuilder = new SourcePortRangeBuilder();
        this.destinationPortRangeBuilder = new DestinationPortRangeBuilder();
    }

    public String getName() {
        return name;
    }

    public Short getProtocol() {
        return protocol;
    }

    public SourcePortRangeBuilder getSourcePortRangeBuilder() {
        return sourcePortRangeBuilder;
    }

    public DestinationPortRangeBuilder getDestinationPortRangeBuilder() {
        return destinationPortRangeBuilder;
    }

    public AceIpVersion getAceIpVersion() {
        return aceIpVersion;
    }

    public Actions getAction() {
        return action;
    }

    public GbpAceBuilder setProtocol(short protocol) {
        this.protocol = protocol;
        return this;
    }

    public GbpAceBuilder setSourcePortRange(SourcePortRangeBuilder sourcePortRangeBuilder) {
        if (sourcePortRangeBuilder != null) {
            this.sourcePortRangeBuilder = sourcePortRangeBuilder;
        }
        return this;
    }

    public GbpAceBuilder setDestinationPortRange(DestinationPortRangeBuilder destPortRangeBuilder) {
        if (destPortRangeBuilder != null) {
            this.destinationPortRangeBuilder = destPortRangeBuilder;
        }
        return this;
    }

    public GbpAceBuilder setIpAddresses(@Nullable Ipv4Prefix srcIp, @Nullable Ipv4Prefix dstIp) {
        AceIpv4Builder aceIpv4Builder = (aceIpv4 != null) ? new AceIpv4Builder(aceIpv4) : new AceIpv4Builder();
        if (srcIp != null) {
            aceIpv4Builder.setSourceIpv4Network(srcIp);
        }
        if (dstIp != null) {
            aceIpv4Builder.setDestinationIpv4Network(dstIp);
        }
        this.aceIpv4 = aceIpv4Builder.build();
        this.aceIpVersion = aceIpv4;
        return this;
    }

    public GbpAceBuilder setIpAddresses(@Nullable Ipv6Prefix srcIp, @Nullable Ipv6Prefix dstIp) {
        AceIpv6Builder aceIpv6Builder = (aceIpv6 != null) ? new AceIpv6Builder(aceIpv6) : new AceIpv6Builder();
        if (srcIp != null) {
            aceIpv6Builder.setSourceIpv6Network(srcIp);
        }
        if (dstIp != null) {
            aceIpv6Builder.setDestinationIpv6Network(dstIp);
        }
        this.aceIpv6 = aceIpv6Builder.build();
        this.aceIpVersion = aceIpv6;
        return this;
    }

    public GbpAceBuilder setPermit() {
        this.action = new ActionsBuilder().setPacketHandling(new PermitBuilder().setPermit(true).build()).build();
        return this;
    }

    public GbpAceBuilder setDeny() {
        this.action = new ActionsBuilder().setPacketHandling(new DenyBuilder().setDeny(true).build()).build();
        return this;
    }

    public GbpAceBuilder setAction(Actions actions) {
        this.action = actions;
        return this;
    }

    public Ace build() {
        if (protocol == null || protocol == 0) {
            ipProtocol =
                    new OtherBuilder().setOtherNodes(new OtherNodesBuilder().setProtocol((short) 0).build()).build();
        } else {
            if (protocol == 1) {
                ipProtocol = new IcmpBuilder().setIcmpNodes(new IcmpNodesBuilder()
                    .setIcmpTypeRange(new IcmpTypeRangeBuilder().setFirst(FIRST_ICMP).setLast(LAST_ICMP).build())
                    .setIcmpCodeRange(new IcmpCodeRangeBuilder().setFirst(FIRST_ICMP).setLast(LAST_ICMP).build())
                    .build()).build();
            }
            SourcePortRange sourcePortRange = (sourcePortRangeBuilder.getLowerPort() != null
                    && sourcePortRangeBuilder.getUpperPort() != null) ? sourcePortRangeBuilder.build() : null;
            DestinationPortRange destPortRange = (destinationPortRangeBuilder.getLowerPort() != null
                    && destinationPortRangeBuilder.getUpperPort() != null) ? destinationPortRangeBuilder.build() : null;
            if (protocol == 6) {
                TcpNodes tcpNodes = new TcpNodesBuilder().setSourcePortRange(sourcePortRange)
                    .setDestinationPortRange(destPortRange)
                    .build();
                ipProtocol = new TcpBuilder().setTcpNodes(tcpNodes).build();
            }
            if (protocol == 17) {
                UdpNodes udpNodes = new UdpNodesBuilder().setSourcePortRange(sourcePortRange)
                    .setDestinationPortRange(destPortRange)
                    .build();
                ipProtocol = new UdpBuilder().setUdpNodes(udpNodes).build();
                ipProtocol = new UdpBuilder().setUdpNodes(udpNodes).build();
            }
        }
        VppAce vppAce = new VppAceBuilder()
            .setVppAceNodes(new VppAceNodesBuilder().setAceIpVersion(aceIpVersion).setIpProtocol(ipProtocol).build())
            .build();
        Matches matches = new MatchesBuilder().setAceType(vppAce).build();
        AceBuilder aceBuilder = new AceBuilder();
        aceBuilder.setMatches(matches);
        aceBuilder.setActions(action);
        aceBuilder.setRuleName(name);
        return aceBuilder.build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendNonNullTo(sb, "GbpAceBuilder [name=", name);
        if (sourcePortRangeBuilder != null) {
            appendNonNullTo(sb, ", srcPort=lower:", sourcePortRangeBuilder.getLowerPort());
            appendNonNullTo(sb, ", srcPort=upper:", sourcePortRangeBuilder.getUpperPort());
        }
        if (sourcePortRangeBuilder != null) {
            appendNonNullTo(sb, ", dstPort=lower:", destinationPortRangeBuilder.getLowerPort());
            appendNonNullTo(sb, ", dstPort=upper:", destinationPortRangeBuilder.getUpperPort());
        }
        appendNonNullTo(sb, ", protocol=", protocol);
        appendNonNullTo(sb, ", aceIpVersion=", aceIpVersion);
        appendNonNullTo(sb, ", action=", action);
        return sb.toString();
    }

    private void appendNonNullTo(StringBuilder sb, String key, Object value) {
        if (value != null && key!= null) {
            sb.append(key).append(value);
        }
    }
}
