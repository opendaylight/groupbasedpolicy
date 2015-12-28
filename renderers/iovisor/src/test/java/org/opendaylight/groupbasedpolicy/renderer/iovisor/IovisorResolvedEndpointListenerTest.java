/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;

import com.google.common.base.Optional;

public class IovisorResolvedEndpointListenerTest extends GbpIovisorDataBrokerTest {

    private DataBroker dataBroker;
    private IovisorResolvedEndpointListener iovisorResolvedEndpointListener;
    private final TenantId tenant1 = new TenantId("tenant1");
    private final EndpointGroupId epg1 = new EndpointGroupId("client");

    @Before
    public void iovisorInit() {
        dataBroker = getDataBroker();
        iovisorResolvedEndpointListener = new IovisorResolvedEndpointListener(dataBroker);

    }

    @Test
    public void endpointPolicyUpdatedTest() {
        iovisorResolvedEndpointListener.endpointPolicyUpdated(tenant1, epg1, dataBroker.newWriteOnlyTransaction());
        Optional<FollowedEndpointGroup> readFromDs = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.followedEndpointgroupIid(IovisorRenderer.RENDERER_NAME, tenant1, epg1),
                dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(readFromDs.isPresent());
        Assert.assertEquals(epg1, readFromDs.get().getId());
    }
}
