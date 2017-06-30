/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util;

import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2L3IidFactory {

    public static InstanceIdentifier<NetworkDomain> subnetIid(TenantId tenantId, NetworkDomainId id) {
        NetworkDomainKey domainKey = new NetworkDomainKey(id, Subnet.class);
        return IidFactory.forwardingByTenantIid(tenantId).child(NetworkDomain.class, domainKey);
    }

    public static InstanceIdentifier<ForwardingContext> l2FloodDomainIid(TenantId tenantId, ContextId id) {
        ForwardingContextKey domainKey = new ForwardingContextKey(id, L2FloodDomain.class);
        return IidFactory.forwardingByTenantIid(tenantId).child(ForwardingContext.class, domainKey);
    }

    public static InstanceIdentifier<ForwardingContext> l2BridgeDomainIid(TenantId tenantId, ContextId id) {
        ForwardingContextKey domainKey = new ForwardingContextKey(id, L2BridgeDomain.class);
        return IidFactory.forwardingByTenantIid(tenantId).child(ForwardingContext.class, domainKey);
    }

    public static InstanceIdentifier<ForwardingContext> l3ContextIid(TenantId tenantId, ContextId id) {
        ForwardingContextKey domainKey = new ForwardingContextKey(id, L3Context.class);
        return IidFactory.forwardingByTenantIid(tenantId).child(ForwardingContext.class, domainKey);
    }

}
