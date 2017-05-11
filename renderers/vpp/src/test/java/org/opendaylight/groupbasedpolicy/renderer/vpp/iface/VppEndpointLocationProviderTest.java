/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

@SuppressWarnings("unchecked")
public class VppEndpointLocationProviderTest {

    private final String INTERFACE_NAME = "interface-name";
    private final NodeId nodeId = new NodeId("vpp-node");
    private final ContextId contextId = new ContextId("context-id");
    private final DataBroker dataProvider = mock(DataBroker.class);
    private final BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
    private final WriteTransaction wTx = mock(WriteTransaction.class);
    private final ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
    private final CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
    private final CheckedFuture<Optional<DataObject>,ReadFailedException> readFuture = mock(CheckedFuture.class);
    private final Optional<DataObject> vppEpOpt = mock(Optional.class);
    private final VppEndpointKey VPP_EP_KEY =
            new VppEndpointKey("192.168.192.168/32", IpPrefixType.class, new ContextId("TEST_CTX"), L3Context.class);

    @Before
    public void init() throws ReadFailedException {
        when(dataProvider.createTransactionChain(any())).thenReturn(transactionChain);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(wTx);
        when(transactionChain.newReadWriteTransaction()).thenReturn(rwTx);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);
        when(rwTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(readFuture);
        when(readFuture.checkedGet()).thenReturn(vppEpOpt);
        when(vppEpOpt.isPresent()).thenReturn(true);
        when(wTx.submit()).thenReturn(future);
        when(rwTx.submit()).thenReturn(future);
    }

    @Test
    public void constructorTest() {
        new VppEndpointLocationProvider(dataProvider);
        verify(dataProvider, times(1)).createTransactionChain(any());
        verify(transactionChain, times(1)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(wTx, times(1)).submit();
    }

    @Test
    public void createLocationForVppEndpointTest() {
        final VppEndpointLocationProvider locationProvider = new VppEndpointLocationProvider(dataProvider);
        final ListenableFuture<Void> result = locationProvider.createLocationForVppEndpoint(vppEndpointBuilder());
        Assert.assertNotNull(result);
        verify(dataProvider, times(1)).createTransactionChain(any());
        verify(transactionChain, times(1)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(ProviderAddressEndpointLocation.class), eq(true));
        verify(wTx, times(1)).submit();
    }

    @Test
    public void deleteLocationForVppEndpointTest() {
        final VppEndpointLocationProvider locationProvider = new VppEndpointLocationProvider(dataProvider);
        final ListenableFuture<Void> result = locationProvider.deleteLocationForVppEndpoint(vppEndpointBuilder());
        Assert.assertNotNull(result);
        verify(dataProvider, times(1)).createTransactionChain(any());
        verify(transactionChain, times(1)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(rwTx, times(1)).delete(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class));
        verify(wTx, times(1)).submit();
        verify(rwTx, times(1)).submit();
    }


    private VppEndpoint vppEndpointBuilder() {
        final VppEndpointBuilder vppEndpointBuilder = new VppEndpointBuilder();
        vppEndpointBuilder.setKey(VPP_EP_KEY).setVppNodeId(nodeId).setVppInterfaceName(INTERFACE_NAME);
        return vppEndpointBuilder.build();
    }

}