/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpointsByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpointsByTenantByEndpointgroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IovisorResolvedEndpointListenerTest extends GbpIovisorDataBrokerTest {

    private DataBroker dataBroker;
    private IovisorResolvedEndpointListener iovisorResolvedEndpointListener;
    private DataObjectModification<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootNode;
    private Set<DataTreeModification<IovisorResolvedEndpointsByTenantByEndpointgroupId>> changes;
    private InstanceIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootIdentifier;

    private final TenantId tenant1 = new TenantId("tenant1");
    private final EndpointGroupId epg1 = new EndpointGroupId("client");

    @Before
    public void iovisorInit() {
        dataBroker = getDataBroker();
        iovisorResolvedEndpointListener = spy(new IovisorResolvedEndpointListener(dataBroker));

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = IovisorIidFactory.iovisorResolvedEndpointsByTenantIdByEndpointGroupIdWildCardIid();
        DataTreeIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        IovisorIidFactory.iovisorResolvedEndpointsByTenantIdByEndpointGroupIdWildCardIid());
        DataTreeModification<IovisorResolvedEndpointsByTenantByEndpointgroupId> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        IovisorResolvedEndpointByTenantByEndpointgroupId testElement = new IovisorResolvedEndpointByTenantByEndpointgroupIdBuilder()
                .setTenantId(tenant1)
                .setEndpointgroupId(epg1)
                .build();
        List<IovisorResolvedEndpointByTenantByEndpointgroupId> list = ImmutableList.of(testElement);

        IovisorResolvedEndpointsByTenantByEndpointgroupId testData = new IovisorResolvedEndpointsByTenantByEndpointgroupIdBuilder()
                .setIovisorResolvedEndpointByTenantByEndpointgroupId(list)
                .build();

        when(rootNode.getDataAfter()).thenReturn(testData);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        iovisorResolvedEndpointListener.onDataTreeChanged(changes);

        verify(iovisorResolvedEndpointListener).onSubtreeModified(rootNode, rootIdentifier);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        iovisorResolvedEndpointListener.onDataTreeChanged(changes);
    }

    @Test
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        iovisorResolvedEndpointListener.onDataTreeChanged(changes);

        verify(iovisorResolvedEndpointListener).endpointPolicyUpdated(any(TenantId.class), any(EndpointGroupId.class), any(WriteTransaction.class));
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
