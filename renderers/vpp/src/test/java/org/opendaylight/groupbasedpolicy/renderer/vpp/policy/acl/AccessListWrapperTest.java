/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil.ACE_DIRECTION;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;

import com.google.common.collect.ImmutableList;

public class AccessListWrapperTest {

    private GbpAceBuilder rule1;

    @Before
    public void init() {
        rule1 = Mockito.mock(GbpAceBuilder.class);
    }

    @Test
    public void testGetDirection_ingress() {
        AccessListWrapper aclWrapper = new IngressAccessListWrapper();
        Assert.assertEquals(ACE_DIRECTION.INGRESS, aclWrapper.getDirection());
        aclWrapper = new EgressAccessListWrapper();
        Assert.assertEquals(ACE_DIRECTION.EGRESS, aclWrapper.getDirection());
    }

    @Test
    public void writeRulesTest() {
        AccessListWrapper aclWrapper = new IngressAccessListWrapper();
        List<GbpAceBuilder> rules = ImmutableList.of(rule1);
        aclWrapper.writeRules(rules);
        Assert.assertEquals(rule1, aclWrapper.readRules().get(0));
        Assert.assertEquals(1, aclWrapper.readRules().size());
    }

    @Test
    public void buildVppAclTest() {
        AccessListWrapper aclWrapper = new IngressAccessListWrapper();
        aclWrapper.writeRule(rule1);
        String key1 = "key1";
        Acl acl = aclWrapper.buildVppAcl(new InterfaceKey(key1));
        Assert.assertEquals(VppAcl.class, acl.getAclType());
        Assert.assertEquals(key1 + ACE_DIRECTION.INGRESS, acl.getAclName());
        Assert.assertEquals(1, acl.getAccessListEntries().getAce().size());
    }
}
