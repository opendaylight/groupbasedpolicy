/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class AccessListUtilTest extends TestResources {

    private PolicyContext ctx;
    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;

    @Before
    public void init() {
        ctx = super.createPolicyContext(createAddressEndpoints(), createRendEps(), createRuleGroups(),
                createForwarding());
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = Mockito.mock(DataBroker.class);
        Mockito.when(mountedDataProviderMock.resolveDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(mountPointDataBroker));
    }

    @Test
    public void resolveAclsOnInterfaceTest() throws Exception {
        AclManager aclManager = new AclManager(mountedDataProviderMock, Mockito.mock(InterfaceManager.class));
        List<AccessListWrapper> acls =
                aclManager.resolveAclsOnInterface(rendererEndpointKey(l3AddrEp2.getKey()), ctx).get();
        Assert.assertEquals(2, acls.size());
        Assert.assertEquals(2, acls.stream().map(AccessListWrapper::getDirection).collect(Collectors.toSet()).size());
        acls.stream().forEach(ace -> {
            // allow peer + deny rest of tenant net + permit external
            if (ace instanceof IngressAccessListWrapper) {
                Assert.assertEquals(4, ace.readRules().size());
            } else if (ace instanceof EgressAccessListWrapper) {
                Assert.assertEquals(4, ace.readRules().size());
            }
        });
    }
}
