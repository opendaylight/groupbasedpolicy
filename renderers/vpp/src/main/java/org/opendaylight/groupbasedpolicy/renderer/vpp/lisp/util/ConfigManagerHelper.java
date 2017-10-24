/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.LispStateManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispNotFoundException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.InterfaceUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.LispUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.Containment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.hmac.key.grouping.HmacKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManagerHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigManagerHelper.class);


    public ConfigManagerHelper() {
    }


    public static ExternalLocationCase resolveAndValidateLocation(AddressEndpointWithLocation addrEpWithLoc) {
        Preconditions.checkNotNull(addrEpWithLoc.getAbsoluteLocation(), "Absolute location for " +
                "AddressEndpointWithLocation missing: " + addrEpWithLoc.toString() );
        LocationType locationType = addrEpWithLoc.getAbsoluteLocation().getLocationType();
        if (!(locationType instanceof ExternalLocationCase)) {
            throw new IllegalArgumentException("Endpoint does not have external location " + addrEpWithLoc);
        }
        ExternalLocationCase result = (ExternalLocationCase) locationType;
        if (result.getExternalNodeMountPoint() == null || result.getExternalNodeConnector() == null) {
            throw new IllegalArgumentException(
                    "Endpoint does not have external-node-mount-point or external-node-connector " + addrEpWithLoc);
        }
        return result;
    }

    //This is almost identical to VBD's equivalent method
    public ListenableFuture<String> getLispDataRlocInterfaceName(@Nonnull String hostName) {
        Preconditions.checkNotNull(hostName, "Hostname is null!");

        PhysicalInterfaces physicalInterfaces = HostRelatedInfoContainer.getInstance()
                .getPhysicalInterfaceState(hostName);

        String publicInterfaceName = physicalInterfaces == null ? "" : physicalInterfaces
                .getName(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC);

        final Optional<InterfacesState> opInterfaceState =
                GbpNetconfTransaction.read(LispUtil.hostnameToIid(hostName), LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.create(InterfacesState.class), GbpNetconfTransaction.RETRY_COUNT);

        if (!opInterfaceState.isPresent()) {
            LOG.debug("There appear to be no interfaces on node {}.", hostName);
            return Futures.immediateFailedFuture(new LispNotFoundException("No interfaces found"));
        }

        String interfaceName = null;
        for(Interface intf: opInterfaceState.get().getInterface()) {
            if(!ipAddressPresent(intf)) {
                continue;
            }
            interfaceName = intf.getName();
        }

        final Optional<Interfaces> opInterfaces =
                GbpNetconfTransaction.read(LispUtil.hostnameToIid(hostName), LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(Interfaces.class), GbpNetconfTransaction.RETRY_COUNT);


        if (opInterfaces.isPresent()) {

            List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.
                    interfaces.Interface> hostInterfaceFromOpDS = opInterfaces.get().getInterface();

            for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.
                    interfaces.Interface intf : hostInterfaceFromOpDS) {
                if (Constants.TENANT_INTERFACE.equals(intf.getDescription())
                        && ipAddressPresent(intf)
                        && intf.getType().equals(EthernetCsmacd.class)) {
                    return Futures.immediateFuture(intf.getName());
                }
            }

            for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.
                    interfaces.Interface intf : hostInterfaceFromOpDS) {
                if (ipAddressPresent(intf)
                        && intf.getType().equals(EthernetCsmacd.class)
                        && !intf.getName().equalsIgnoreCase(publicInterfaceName)) {
                    return Futures.immediateFuture(intf.getName());
                }
            }
        }

        if (interfaceName == null) {
            LOG.warn("No interface with IP found for host {}", hostName);
            return Futures.immediateFailedFuture(new LispNotFoundException("No interface with Ip address found!"));
        }
        return Futures.immediateFuture(interfaceName);
    }

    private boolean ipAddressPresent(Interface intf) {
        Interface2 augIntf = intf.getAugmentation(Interface2.class);

        if (augIntf == null) {
            return false;
        }

        Ipv4 ipv4 = augIntf.getIpv4();

        if (ipv4 == null) {
            return false;
        }

        final List<Address> addresses = ipv4.getAddress();

        if (addresses == null || addresses.isEmpty()) {
            return false;
        }

        final Ipv4AddressNoZone ip = addresses.iterator().next().getIp();
        return ip != null;
    }

    private String ipCidr(Interface intf) {
        Interface2 augIntf = intf.getAugmentation(Interface2.class);

        if (augIntf == null) {
            return null;
        }

        Ipv4 ipv4 = augIntf.getIpv4();

        if (ipv4 == null) {
            return null;
        }

        final List<Address> addresses = ipv4.getAddress();

        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        Address firstAddress = addresses.get(0);
        String ipString = firstAddress.getIp().getValue();
        String length = "";
        if (firstAddress.getSubnet().getImplementedInterface().equals(PrefixLength.class)) {
            length = "" + ((PrefixLength)firstAddress.getSubnet()).getPrefixLength();
        }

        if (length.isEmpty()) {
            return null;
        }

        return ipString + "/" + length;
    }

    private boolean ipAddressPresent(final org.opendaylight.yang.gen.v1.urn.ietf.
            params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface intf) {
        final Interface1 augIntf = intf.getAugmentation(Interface1.class);

        if (augIntf == null) {
            LOG.debug("Cannot get Interface1 augmentation for intf {}", intf);
            return false;
        }

        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4
            ipv4 = augIntf.getIpv4();
        if (ipv4 == null) {
            return false;
        }

        final List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address>
            addresses = ipv4.getAddress();
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }

        final Ipv4AddressNoZone ip = addresses.iterator().next().getIp();
        return ip != null;
    }

    public String getLispCpRlocInterfaceName(@Nonnull String hostName) {
        List<Interface> operationalInterfaceList =
                InterfaceUtil.getOperationalInterfaces(LispUtil.hostnameToIid(hostName));

        if (operationalInterfaceList == null) {
            return null;
        } else {
            int maxLen = -1;
            String outgoingInterface = "";

            for (Interface intf : operationalInterfaceList) {
                String ipCidr = ipCidr(intf);

                if (ipCidr == null) {
                    continue;
                }

                if (IpAddressUtil.ipInRange(ConfigUtil.getInstance().getOdlIp().getIpv4Address(),
                        IpAddressUtil.startIpOfSubnet(ipCidr), IpAddressUtil.endIpOfSubnet(ipCidr))) {
                    int tmpLen = IpAddressUtil.maskLen(ipCidr);
                    if (tmpLen > maxLen) {
                        maxLen = tmpLen;
                        outgoingInterface = intf.getName();
                    }
                }
            }
            return outgoingInterface;
        }
    }

    public String constructLocatorSetName(int locatorSetCount) {
        return LispStateManager.DEFAULT_LOCATOR_SET_NAME_PREFIX + (locatorSetCount + 1);
    }

    public String constructLocatorSetNameForItrRloc() {
        return LispStateManager.DEFAULT_LOCATOR_SET_NAME_PREFIX + "_itr_rloc";
    }

    public String constructEidMappingName(AddressEndpointWithLocation addressEp, String interfaceName) {
        String ipAddress = getInterfaceIp(addressEp).getValue();
        return LispStateManager.DEFAULT_MAPPING_RECORD_NAME_PREFIX + interfaceName + "_" + ipAddress;
    }

    public String getSubnet(AddressEndpointWithLocation addressEp) {
        String subnetUuid = null;
        Containment containment = addressEp.getNetworkContainment().getContainment();
        if (containment instanceof NetworkDomainContainment) {
            NetworkDomainContainment networkDomainContainment = (NetworkDomainContainment) containment;
            subnetUuid = networkDomainContainment.getNetworkDomainId().getValue();
        }
        return subnetUuid;
    }

    public Eid getEid(AddressEndpointWithLocation addressEp, long vni) {
        String ipPrefix = getIpWithPrefixOfEndpoint(addressEp);
        return LispUtil.toEid(LispUtil.toIpv4(ipPrefix), vni, Ipv4Afi.class);
    }

    private static String getIpWithPrefixOfEndpoint(AddressEndpointWithLocation addressEp) {
        String ipPrefix = null;
        if (addressEp.getAddressType().equals(IpPrefixType.class)) {
            ipPrefix = addressEp.getAddress();
        } else if (addressEp.getAddressType().equals(MacAddressType.class)) {
            ParentEndpointChoice parentEndpointChoice = addressEp.getParentEndpointChoice();
            if (parentEndpointChoice instanceof ParentEndpointCase) {
                java.util.Optional<ParentEndpoint> endpointOptional =
                    ((ParentEndpointCase) parentEndpointChoice).getParentEndpoint().stream()
                        .filter(choice -> choice.getAddressType().equals(IpPrefixType.class))
                        .filter(choice -> choice.getContextType().equals(L3Context.class)).findFirst();
                if (endpointOptional.isPresent()) {
                    ipPrefix = endpointOptional.get().getAddress();
                }
            }
        }
        return Preconditions.checkNotNull(ipPrefix, "No IP address found for Address Endpoint: {}", addressEp);
    }

    public static Ipv4Address getInterfaceIp(AddressEndpointWithLocation addressEp) {
        String ipPrefix = getIpWithPrefixOfEndpoint(addressEp);
        return LispUtil.toIpv4(ipPrefix).getIpv4();
    }

    public Ipv4Prefix getInterfaceIpAsPrefix(AddressEndpointWithLocation addressEp) {
        if (getInterfaceIp(addressEp).getValue().contains("/")) {
            return new Ipv4Prefix(getInterfaceIp(addressEp).getValue());
        }
        else {
            return new Ipv4Prefix(getInterfaceIp(addressEp).getValue() + "/32");
        }
    }

    public Optional<String> getInterfaceName(AddressEndpointWithLocation addedEp) {
        ExternalLocationCase epLoc = resolveAndValidateLocation(addedEp);
        String interfacePath = epLoc.getExternalNodeConnector();

        return VppPathMapper.interfacePathToInterfaceName(interfacePath);
    }

    public Optional<String> getInterfaceName(ExternalLocation externalLocation) {
        String interfacePath = externalLocation.getExternalNodeConnector();
        return VppPathMapper.interfacePathToInterfaceName(interfacePath);
    }


    public HmacKey getDefaultHmacKey() {
        return LispUtil.toHmacKey(HmacKeyType.Sha196Key, LispStateManager.DEFAULT_XTR_KEY);
    }


    public static boolean isMetadataPort(AddressEndpointWithLocation addressEp) {
        return IpAddressUtil.isMetadataIp(getInterfaceIp(addressEp));
    }
}
