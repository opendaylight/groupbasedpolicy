/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

public class EgressAccessListWrapper extends AccessListWrapper {

    @Override
    public ACE_DIRECTION getDirection() {
        return AccessListUtil.ACE_DIRECTION.EGRESS;
    }

    @Override
    public void writeAclRefOnIface(@Nonnull InstanceIdentifier<Node> vppIid, @Nonnull InstanceIdentifier<Interface> ifaceIid) {
        InstanceIdentifier<Egress> egressRefIid = VppIidFactory.getAclInterfaceRef(ifaceIid).child(Egress.class);
        VppAcls vppAcl = new VppAclsBuilder().setName(resolveAclName(ifaceIid.firstKeyOf(Interface.class)))
            .setType(VppAcl.class)
            .build();
        Egress egressAcl = new EgressBuilder().setVppAcls(ImmutableList.<VppAcls>of(vppAcl)).build();
        GbpNetconfTransaction.netconfSyncedWrite(vppIid, egressRefIid, egressAcl,
            GbpNetconfTransaction.RETRY_COUNT);
    }
}
