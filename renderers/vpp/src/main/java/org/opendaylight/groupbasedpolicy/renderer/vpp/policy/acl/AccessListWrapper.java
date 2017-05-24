/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AccessListWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AccessListWrapper.class);
    private List<GbpAceBuilder> rules;

    public AccessListWrapper() {
        rules = new ArrayList<>();
    }

    public void writeRule(GbpAceBuilder rule) {
        if (rule != null) {
            this.rules.add(rule);
        }
    }

    public void writeRules(List<GbpAceBuilder> rules) {
        if (rules != null) {
            rules.forEach(this::writeRule);
        }
    }

    public List<GbpAceBuilder> readRules() {
        return rules;
    }

    protected String resolveAclName(InterfaceKey key) {
        return key.getName() + getDirection();
    }

    public abstract AccessListUtil.ACE_DIRECTION getDirection();

    public abstract void writeAclRefOnIface(@Nonnull InstanceIdentifier<Node> vppIid,
            @Nonnull InstanceIdentifier<Interface> ifaceIid);

    public Acl buildVppAcl(@Nonnull InterfaceKey ifaceKey) {
        List<Ace> aces = new ArrayList<>();
        for (GbpAceBuilder rule : rules) {
            aces.add(rule.build());
        }
        AccessListEntries entries = new AccessListEntriesBuilder().setAce(aces).build();
        return new AclBuilder().setAclType(VppAcl.class)
            .setAclName(resolveAclName(ifaceKey))
            .setAccessListEntries(entries)
            .build();
    }

    public void writeAcl(@Nonnull InstanceIdentifier<Node> vppIid, @Nonnull InterfaceKey ifaceKey) {
        Acl builtAcl = this.buildVppAcl(ifaceKey);
        LOG.trace("Writing access-list {}", builtAcl.getAclName());
        boolean write = GbpNetconfTransaction.netconfSyncedWrite(vppIid,
                VppIidFactory.getVppAcl(resolveAclName(ifaceKey)), builtAcl, GbpNetconfTransaction.RETRY_COUNT);
        if (!write) {
            LOG.error("Failed to write rule {}", builtAcl);
        }
    }

    public static void removeAclsForInterface(@Nonnull InstanceIdentifier<Node> vppIid, @Nonnull InterfaceKey ifaceKey) {
        LOG.debug("Removing access-list {}", ifaceKey);
        for (ACE_DIRECTION dir : new ACE_DIRECTION[] {ACE_DIRECTION.INGRESS, ACE_DIRECTION.EGRESS}) {
            GbpNetconfTransaction.netconfSyncedDelete(vppIid,
                VppIidFactory.getVppAcl(ifaceKey.getName() + dir), GbpNetconfTransaction.RETRY_COUNT);
        }
    }

    public static void removeAclRefFromIface(@Nonnull InstanceIdentifier<Node> vppIid, @Nonnull InterfaceKey ifaceKey) {
        LOG.debug("Removing access-lists from interface {}", ifaceKey.getName());
        GbpNetconfTransaction.netconfSyncedDelete(vppIid,
                VppIidFactory.getAclInterfaceRef(VppIidFactory.getInterfaceIID(ifaceKey)),
                GbpNetconfTransaction.RETRY_COUNT);
    }
}
