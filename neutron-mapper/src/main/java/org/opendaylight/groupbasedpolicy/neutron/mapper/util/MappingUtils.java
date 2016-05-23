/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.Parent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.ParentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.AllowActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.allow.action._case.AllowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;

public final class MappingUtils {

    public static final String NEUTRON_ROUTER = "neutron_router-";
    public static final String NEUTRON_EXTERNAL = "neutron_external_network-";
    public static final String NEUTRON_GROUP = "neutron_group-";
    public static final IpPrefix DEFAULT_ROUTE = new IpPrefix(new Ipv4Prefix("0.0.0.0/0"));
    public static final ActionInstance ACTION_ALLOW = new ActionInstanceBuilder().setName(
            new ActionName("Allow"))
        .setActionDefinitionId(AllowActionDefinition.DEFINITION.getId())
        .build();
    public static final ActionChoice ALLOW_ACTION_CHOICE = new AllowActionCaseBuilder().setAllow(
            new AllowBuilder().build()).build();
    public static final ActionRef ACTION_REF_ALLOW =
            new ActionRefBuilder().setName(ACTION_ALLOW.getName()).setOrder(0).build();
    public static final Uuid EIG_UUID = new Uuid("eeeaa3a2-e9ba-44e0-a462-bea923d30e38");
    public static final EndpointGroupId EPG_EXTERNAL_ID = new EndpointGroupId(EIG_UUID.getValue());

    public static final String NAME_VALUE_DELIMETER = "-";
    public static final String NAME_DELIMETER = "_";
    public static final String NAME_DOUBLE_DELIMETER = "__";

    // Network domains defined here to overcome package conflict resolution
    public static final Class<L3Context> L3_CONTEXT = L3Context.class;
    public static final Class<L2BridgeDomain> L2_BRDIGE_DOMAIN = L2BridgeDomain.class;
    public static final Class<L2FloodDomain> L2_FLOOD_DOMAIN = L2FloodDomain.class;
    public static final Class<Subnet> SUBNET = Subnet.class;

    private MappingUtils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static ActionRef createSfcActionRef(String sfcChainName) {
        return new ActionRefBuilder().setName(new ActionName(sfcChainName)).setOrder(0).build();
    }

    public static ActionChoice createSfcActionChoice(String chainName) {
        return new SfcActionCaseBuilder().setSfcChainName(chainName).build();
    }

    public static <T extends ContextType> Parent createParent(ContextId id, Class<T> domainType) {
        return new ParentBuilder().setContextId(id).setContextType(domainType).build();
    }

    public static <T extends ContextType> Parent createParent(NetworkDomainId id, Class<T> domainType) {
        return new ParentBuilder().setContextId(new ContextId(id.getValue())).setContextType(domainType).build();
    }

    public static IpPrefix ipAddressToIpPrefix(char[] address) {
        return ipAddressToIpPrefix(new IpAddress(address));
    }

    public static IpPrefix ipAddressToIpPrefix(IpAddress address) {
        if (address.getIpv4Address() != null) {
            return new IpPrefix(new Ipv4Prefix(ipv4PrefixOf(address)));
        } else if (address.getIpv6Address() != null) {
            return new IpPrefix(new Ipv6Prefix(ipv6PrefixOf(address)));
        }
        throw new IllegalArgumentException("Ip address [{}] is not a valid Ipv4 or Ipv6 address." + address);
    }

    public static String ipAddressToStringIpPrefix(String address) {
        return ipAddressToStringIpPrefix(new IpAddress(address.toCharArray()));
    }

    public static String ipAddressToStringIpPrefix(char[] address) {
        return ipAddressToStringIpPrefix(new IpAddress(address));
    }

    public static String ipAddressToStringIpPrefix(IpAddress address) {
        if (address.getIpv4Address() != null) {
            return ipv4PrefixOf(address);
        } else if (address.getIpv6Address() != null) {
            return ipv6PrefixOf(address);
        }
        throw new IllegalArgumentException("Ip address [{}] is not a valid Ipv4 or Ipv6 address." + address);
    }

    private static String ipv4PrefixOf(IpAddress ipAddress) {
        return new String(ipAddress.getValue()) + "/32";
    }

    private static String ipv6PrefixOf(IpAddress ipAddress) {
        return new String(ipAddress.getValue()) + "/128";
    }

    public static IpAddress ipPrefixToIpAddress(char[] ipPrefix) {
        return ipPrefixToIpAddress(new IpPrefix(ipPrefix));
    }

    public static IpAddress ipPrefixToIpAddress(IpPrefix prefix) {
        return new IpAddress(ipPrefixToStringIpAddress(prefix).toCharArray());
    }

    public static String ipPrefixToStringIpAddress(IpPrefix prefix) {
        if (prefix.getIpv4Prefix() != null) {
            String[] split = prefix.getIpv4Prefix().getValue().split("/");
            return split[0];

        } else if (prefix.getIpv6Prefix() != null) {
            String[] split = prefix.getIpv6Prefix().getValue().split("/");
            return split[0];
        }
        throw new IllegalArgumentException("Cannot extract IP prefix from IP address {}" + prefix);
    }
}
