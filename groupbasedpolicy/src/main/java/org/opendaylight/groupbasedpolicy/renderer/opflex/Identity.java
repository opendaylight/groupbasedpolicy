/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;

import com.google.common.net.InetAddresses;


/**
 * An Identity for OpFlex. Identities can take on many
 * forms, so it's possible that this class may be replaced
 * by an abstract class with different concrete types.
 * At the moment, we're only dealing with IP and MAC
 * addresses.
 * 
 * This class also provides methods for getting the identity
 * in forms by the yang model, and are therefore usable by
 * other classes in the policy model (e.g. the objects
 * needed by the Endpoint Registry).
 *
 */
public class Identity {
    enum IdentityType {
        UNKNOWN, IP_ADDRESS, MAC_ADDRESS;
    }
    private IdentityType type = IdentityType.UNKNOWN;
    private L3ContextId l3Context = null;
    private IpAddress primaryIp = null;
    private Set<IpAddress> ips = null;
    private L2BridgeDomainId l2Context = null;
    private MacAddress mac = null;
    public Identity(String id) {
        /*
         * Determine the ID type and populate
         */
        if (idIsIp(id)) {
            type = IdentityType.IP_ADDRESS;
            ips = Collections.newSetFromMap(new ConcurrentHashMap<IpAddress, Boolean>());
            if (primaryIp == null) primaryIp = normalizeIpAddress(id);
            ips.add(normalizeIpAddress(id));
        }
        else if (idIsMac(id)) {
            type = IdentityType.MAC_ADDRESS;
            mac = normalizeMacAddress(id);
        }
        
    }
    
    public void setContext(String context) {
        switch (type) {
        case MAC_ADDRESS:
            l2Context = new L2BridgeDomainId(context);
            break;
        case IP_ADDRESS:
            l3Context = new L3ContextId(context);
            break;
        default:
            break;
        }
    }
    
    /**
     * Adds a new identifier to the list. Some types of
     * identities allow for list of identifiers (e.g. L3).
     * 
     * @param id The new identifier to add to the list
     */
    public void addId(String id) {
        switch (type) {
        case IP_ADDRESS:
            ips.add(normalizeIpAddress(id));
            break;
        default:
            break;
        }
    }
    
    private boolean idIsIp(String id) {
        return InetAddresses.isInetAddress(id);
    }
    
    /*
     * Verifies MAC addresses with the following formats:
     * 0xAA:0xBB:0xCC:0xDD:0xEE:0xFF
     * AA:BB:CC:DD:EE:FF
     * 0xAA:BB:CC:DD:EE:FF
     * 0xAA-0xBB-0xCC-0xDD-0xEE-0xFF
     */
    private boolean idIsMac(String id) {
        /*
         * First check/remove separators
         */
        String[] sixFields = id.split(":");
        if (sixFields.length != 6) {
            sixFields = id.split("-");
            if (sixFields.length != 6) {
                return false;
            }
        }

        for (String field : sixFields) {
            /* Strip '0x' if present */
            field = field.replace("0x", "");
            if (field.length() > 2 || field.length() <1) {
                return false;
            }
            if (!Pattern.matches("[0-9a-fA-F]{1,2}", field)) {
                return false;
            }
        }
        return true;        
    }
    
    /**
     * Check if this {@link Identity} is an L3 type (Ip Address)
     * 
     * @return true if L3, false if not
     */
    public boolean isL3() {
        return (type == IdentityType.IP_ADDRESS);
    }
    
    /**
     * Check if this {@link Identity} is an L2 type (MAC Address) 
     * 
     * @return true if L2, false if not
     */
    public boolean isL2() {
        return (type == IdentityType.MAC_ADDRESS);
    }

    /**
     * Return the context, regardless of type, as a string.
     * 
     * @return String representing the context for this Identity
     */
    public String contextAsString() {
        switch (type) {
        case MAC_ADDRESS:
            return l2Context.toString();
        case IP_ADDRESS:
            return l3Context.toString();
        default:
            return null;
        }
    }

    /**
     * Returns the identity as a string. The format
     * of the string depends on the identity type.
     * When the identity is a actually a list, only
     * the first identity is returned.
     * 
     * @return null if type is UKNOWN, otherwise String
     */
    public String identityAsString() {
        switch (type) {
        case MAC_ADDRESS:
            return mac.getValue();
        case IP_ADDRESS:
            List<IpAddress> ipl = new ArrayList<IpAddress>(ips);
            IpAddress i = ipl.get(0);
            if (i.getIpv4Address() != null) {
                return i.getIpv4Address().getValue();
            }
            else if (i.getIpv6Address() != null) {
                return i.getIpv6Address().getValue();
            }
        default:
        }
        return null;
    }
    
    /**
     * Get the L2 context in an Endpoint Registry 
     * compatible format
     * 
     * @return The Layer 2 context
     */
    public L2BridgeDomainId getL2Context() {
        return l2Context;
    }
    
    /**
     * Get the L2 identity in an Endpoint Registry 
     * compatible format
     * 
     * @return The Layer 2 identity
     */
    public MacAddress getL2Identity() {
        return mac;
    }

    /**
     * Get the L3 context in an Endpoint Registry 
     * compatible format
     * 
     * @return The Layer 3 context
     */    
    public L3ContextId getL3Context() {
        return l3Context;
    }

    /**
     * Get the L3 identity in an Endpoint Registry 
     * compatible format
     * 
     * @return The Layer 3 identity
     */
    public IpAddress getL3Identity() {
        return primaryIp;
    }

    public List<L3Address> getL3Addresses() {

        List<L3Address> l3List= new ArrayList<L3Address>();
        List<IpAddress> ipList = new ArrayList<IpAddress>();
        ipList.addAll(ips);
        for (IpAddress i: ipList){ 
            L3AddressBuilder l3ab = new L3AddressBuilder();
            l3ab.setIpAddress(i);
            l3ab.setL3Context(l3Context);
            l3List.add(l3ab.build());
        }

        return l3List;
    }

    private IpAddress normalizeIpAddress(String identifier) {
        return IpAddressBuilder.getDefaultInstance(identifier);
    }
    
    private MacAddress normalizeMacAddress(String identifier) {
        MacAddress m = new MacAddress(identifier);
        return m;
    }
}
