/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;

/**
 * Match on the IP address of IP traffic
 */
public class IpAddressClassifier extends EtherTypeClassifier {
    public static final ClassifierDefinitionId ID = new ClassifierDefinitionId("55b83ba4-a741-4d26-91ca-7cd197750a3f");

    protected static final String SRC_ADDR = "src_addr";
    protected static final String DST_ADDR = "dst_addr";
    protected static final ClassifierDefinition DEF = new ClassifierDefinitionBuilder()
            .setId(ID)
            .setParent(EtherTypeClassifier.ID)
            .setName(new ClassifierName("ip_address"))
            .setDescription(new Description("Match on the IP address of IP traffic"))
            .setParameter(
                    ImmutableList.of(
                            new ParameterBuilder()
                                    .setName(new ParameterName(SRC_ADDR))
                                    .setDescription(new Description("Source IP prefix to match against."))
                                    .setIsRequired(IsRequired.Optional)
                                    .setType(Type.String)
                                    .build(),
                            new ParameterBuilder()
                                    .setName(new ParameterName(DST_ADDR))
                                    .setDescription(new Description("Destination IP prefix to match against."))
                                    .setIsRequired(IsRequired.Optional)
                                    .setType(Type.String).build()))
                                    .build();
    private static final Map<String, Object> ipv4 = ImmutableMap.<String, Object> of(TYPE, FlowUtils.IPv4);
    private static final Map<String, Object> ipv6 = ImmutableMap.<String, Object> of(TYPE, FlowUtils.IPv6);

    private enum IpAddressContext {
        SRC, DST
    }

    @Override
    public ClassifierDefinitionId getId() {
        return ID;
    }

    @Override
    public ClassifierDefinition getClassDef() {
        return DEF;
    }

    @Override
    public List<MatchBuilder> updateMatch(List<MatchBuilder> matches, Map<String, Object> params) {
        Object t = params.get(SRC_ADDR);
        String srcAddr = null;
        if (t instanceof String) {
            srcAddr = (String) t;
        }
        t = params.get(DST_ADDR);
        String dstAddr = null;
        if (t instanceof String) {
            dstAddr = (String) t;
        }
        List<MatchBuilder> r = new ArrayList<>();
        for (MatchBuilder b : matches) {
            if (srcAddr != null && dstAddr != null) {
                boolean isSrcIpv4Addr = isIpv4Address(srcAddr);
                boolean isDstIpv4Addr = isIpv4Address(dstAddr);
                if (isSrcIpv4Addr != isDstIpv4Addr) {
                    throw new IllegalArgumentException("Ip address [" + srcAddr + "] is not the same version as ["
                            + dstAddr + "].");
                }
                r.addAll(updateMatch(b, srcAddr, dstAddr, getIpVersion(isSrcIpv4Addr)));
            } else if (srcAddr == null && dstAddr == null) {
                r.addAll(super.updateMatch(Collections.singletonList(new MatchBuilder(b.build())), ipv4));
                r.addAll(super.updateMatch(Collections.singletonList(new MatchBuilder(b.build())), ipv6));
            } else {
                if (srcAddr != null) {
                    r.addAll(updateMatch(new MatchBuilder(b.build()), srcAddr, null,
                            getIpVersion(isIpv4Address(srcAddr))));
                } else {
                    r.addAll(updateMatch(new MatchBuilder(b.build()), null, dstAddr,
                            getIpVersion(isIpv4Address(dstAddr))));
                }
            }
        }
        return r;
    }

    private List<MatchBuilder> updateMatch(MatchBuilder matchBuilder, String srcAddr, String dstAddr,
            Map<String, Object> params) {
        List<MatchBuilder> r = super.updateMatch(Collections.singletonList(matchBuilder), params);
        Long ipVersion = (Long) params.get(TYPE);
        for (MatchBuilder mb : r) {
            Layer3Match l3m = mb.getLayer3Match();
            l3m = updateIpAddressField(ipVersion, srcAddr, IpAddressContext.SRC, l3m);
            l3m = updateIpAddressField(ipVersion, dstAddr, IpAddressContext.DST, l3m);
            mb.setLayer3Match(l3m);
        }
        return r;
    }

    private Map<String, Object> getIpVersion(boolean isIpv4) {
        if (isIpv4 == true) {
            return ipv4;
        }
        return ipv6;
    }

    private boolean isIpv4Address(String cidr) {
        String[] ipAndPrefix = cidr.split("/");
        if (ipAndPrefix.length != 2) {
            throw new IllegalStateException(cidr + " does not match CIDR format.");
        }
        InetAddress ip = InetAddresses.forString(ipAndPrefix[0]);
        if (ip instanceof Inet4Address) {
            return true;
        }
        return false;
    }

    private Layer3Match updateIpAddressField(Long ipVersion, String ipAddr, IpAddressContext ctx, Layer3Match l3m) {
        if (Strings.isNullOrEmpty(ipAddr)) {
            return l3m;
        }
        if (ipVersion.equals(FlowUtils.IPv4)) {
            l3m = updateIpv4AddressField(ipAddr, ctx, l3m);
        } else if (ipVersion.equals(FlowUtils.IPv6)) {
            l3m = updateIpv6AddressField(ipAddr, ctx, l3m);
        }
        return l3m;
    }

    private Layer3Match updateIpv4AddressField(String ipAddr, IpAddressContext ctx, Layer3Match l3match) {
        Ipv4MatchBuilder ipv4mb;
        if (l3match == null) {
            ipv4mb = new Ipv4MatchBuilder();
        } else {
            ipv4mb = new Ipv4MatchBuilder((Ipv4Match) l3match);
        }
        if (ctx.equals(IpAddressContext.SRC)) {
            return ipv4mb.setIpv4Source(new Ipv4Prefix(ipAddr)).build();
        }
        return ipv4mb.setIpv4Destination(new Ipv4Prefix(ipAddr)).build();
    }

    private Layer3Match updateIpv6AddressField(String ipAddr, IpAddressContext ctx, Layer3Match l3match) {
        Ipv6MatchBuilder ipv6mb;
        if (l3match == null) {
            ipv6mb = new Ipv6MatchBuilder();
        } else {
            ipv6mb = new Ipv6MatchBuilder((Ipv6Match) l3match);
        }
        if (ctx.equals(IpAddressContext.SRC)) {
            return ipv6mb.setIpv6Source(new Ipv6Prefix(ipAddr)).build();
        }
        return ipv6mb.setIpv6Destination(new Ipv6Prefix(ipAddr)).build();
    }
}
