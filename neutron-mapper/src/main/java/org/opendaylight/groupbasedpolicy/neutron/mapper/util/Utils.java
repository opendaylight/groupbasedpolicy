/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;

public class Utils {

    private Utils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    /**
     * This implementation does not use nameservice lookups (e.g. no DNS).
     *
     * @param cidr - format must be valid for regex in {@link Ipv4Prefix} or {@link Ipv6Prefix}
     * @return the {@link IpPrefix} having the given cidr string representation
     * @throws IllegalArgumentException - if the argument is not a valid CIDR string
     */
    public static IpPrefix createIpPrefix(String cidr) {
        checkArgument(!Strings.isNullOrEmpty(cidr), "Cannot be null or empty.");
        String[] ipAndPrefix = cidr.split("/");
        checkArgument(ipAndPrefix.length == 2, "Bad format.");
        InetAddress ip = InetAddresses.forString(ipAndPrefix[0]);
        if (ip instanceof Inet4Address) {
            return new IpPrefix(new Ipv4Prefix(cidr));
        }
        return new IpPrefix(new Ipv6Prefix(cidr));
    }

    /**
     * This implementation does not use nameservice lookups (e.g. no DNS).
     *
     * @param ipAddress - format must be valid for regex in {@link Ipv4Address} or
     *        {@link Ipv6Address}
     * @return the {@link IpAddress} having the given ipAddress string representation
     * @throws IllegalArgumentException - if the argument is not a valid IP address string
     */
    public static IpAddress createIpAddress(String ipAddress) {
        checkArgument(!Strings.isNullOrEmpty(ipAddress), "Cannot be null or empty.");
        InetAddress ip = InetAddresses.forString(ipAddress);
        if (ip instanceof Inet4Address) {
            return new IpAddress(new Ipv4Address(ipAddress));
        }
        return new IpAddress(new Ipv6Address(ipAddress));
    }

    public static String getStringIpPrefix(IpPrefix ipPrefix) {
        Preconditions.checkNotNull(ipPrefix);
        if (ipPrefix.getIpv4Prefix() != null) {
            String ipPrefixIpv4 = ipPrefix.getIpv4Prefix().getValue();
            return ipPrefixIpv4.replace('/', '_');
        }
        String ipPrefixIpv6 = ipPrefix.getIpv6Prefix().getValue();
        return ipPrefixIpv6.replace('/', '_').replace(':', '.');
    }

    public static String getStringIpAddress(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }
        return ipAddress.getIpv6Address().getValue();
    }

    public static String normalizeUuid(String string) {
        return string.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                "$1-$2-$3-$4-$5");
    }

    //TODO move to FloatingIpAware when deprecated API is removed
    public static void syncNat(ReadWriteTransaction rwTx, Floatingip oldFloatingIp, Floatingip newFloatingIp) {
        IpAddress oldEpIp = oldFloatingIp.getFixedIpAddress();
        IpAddress newEpIp = newFloatingIp.getFixedIpAddress();
        IpAddress epNatIp = newFloatingIp.getFloatingIpAddress();
        if (epNatIp != null && newEpIp != null) {
            InstanceIdentifier<BaseEndpointByPort> baseEpByPortId =
                    NeutronGbpIidFactory.baseEndpointByPortIid(new UniqueId(newFloatingIp.getPortId().getValue()));
            Optional<BaseEndpointByPort> optional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, baseEpByPortId, rwTx);
            if (!optional.isPresent()) {
                return;
            }
            NatAddress nat = new NatAddressBuilder().setNatAddress(epNatIp).build();
            AddressEndpointKey addrEpKey = new AddressEndpointKey(optional.get().getAddress(),
                    optional.get().getAddressType(), optional.get().getContextId(), optional.get().getContextType());
            rwTx.put(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.addressEndpointIid(addrEpKey).augmentation(NatAddress.class), nat, true);
        }
        if (oldEpIp != null) {
            InstanceIdentifier<BaseEndpointByPort> baseEpByPortId =
                    NeutronGbpIidFactory.baseEndpointByPortIid(new UniqueId(oldFloatingIp.getPortId().getValue()));
            Optional<BaseEndpointByPort> optional =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, baseEpByPortId, rwTx);
            if (!optional.isPresent()) {
                return;
            }
            AddressEndpointKey addrEpKey = new AddressEndpointKey(optional.get().getAddress(),
                    optional.get().getAddressType(), optional.get().getContextId(), optional.get().getContextType());
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.addressEndpointIid(addrEpKey).augmentation(NatAddress.class), rwTx);
        }
    }

    public static void removeNat(ReadWriteTransaction rwTx, Floatingip removedFloatingIp) {
        if (removedFloatingIp.getFixedIpAddress() == null) {
            // NAT augmentation should have been already removed
            return;
        }
        InstanceIdentifier<BaseEndpointByPort> baseEpByPortId =
                NeutronGbpIidFactory.baseEndpointByPortIid(new UniqueId(removedFloatingIp.getPortId().getValue()));
        Optional<BaseEndpointByPort> optional =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, baseEpByPortId, rwTx);
        if (!optional.isPresent()) {
            return;
        }
        AddressEndpointKey addrEpKey = new AddressEndpointKey(optional.get().getAddress(),
                optional.get().getAddressType(), optional.get().getContextId(), optional.get().getContextType());
        rwTx.delete(LogicalDatastoreType.OPERATIONAL,
                IidFactory.addressEndpointIid(addrEpKey).augmentation(NatAddress.class));
    }
}
