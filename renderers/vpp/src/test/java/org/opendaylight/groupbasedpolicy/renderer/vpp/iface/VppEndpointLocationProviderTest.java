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

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("unchecked")
public class VppEndpointLocationProviderTest {

    private final String INTERFACE_NAME = "interface-name";
    private final NodeId nodeId = new NodeId("vpp-node");
    private final ContextId contextId = new ContextId("context-id");
    private final DataBroker dataProvider = mock(DataBroker.class);
    private final BindingTransactionChain transactionChain = mock(BindingTransactionChain.class);
    private final WriteTransaction wTx = mock(WriteTransaction.class);
    private final CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);

    @Before
    public void init() {
        when(dataProvider.createTransactionChain(any())).thenReturn(transactionChain);
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(wTx);
        when(wTx.submit()).thenReturn(future);
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
        verify(transactionChain, times(2)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(ProviderAddressEndpointLocation.class), eq(true));
        verify(wTx, times(2)).submit();
    }

    @Test
    public void createProviderAddressEndpointLocationTest() {
        final ProviderAddressEndpointLocation result =
                VppEndpointLocationProvider.createProviderAddressEndpointLocation(vppEndpointBuilder());
        Assert.assertNotNull(result);
        final AbsoluteLocation location = result.getAbsoluteLocation();
        Assert.assertNotNull(location);
        Assert.assertEquals(location.getLocationType().getImplementedInterface(), ExternalLocationCase.class);
        final ExternalLocationCase locationType = (ExternalLocationCase) location.getLocationType();
        Assert.assertEquals(locationType.getExternalNodeMountPoint().firstKeyOf(Node.class).getNodeId(), nodeId);
        Assert.assertTrue(locationType.getExternalNodeConnector().contains(INTERFACE_NAME));

    }

    @Test
    public void deleteLocationForVppEndpointTest() {
        final VppEndpointLocationProvider locationProvider = new VppEndpointLocationProvider(dataProvider);
        final ListenableFuture<Void> result = locationProvider.deleteLocationForVppEndpoint(vppEndpointBuilder());

        Assert.assertNotNull(result);
        verify(dataProvider, times(1)).createTransactionChain(any());
        verify(transactionChain, times(2)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(wTx, times(1)).delete(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class));
        verify(wTx, times(2)).submit();
    }

    @Test
    public void replaceLocationForEndpointTest() {
        final VppEndpointLocationProvider locationProvider = new VppEndpointLocationProvider(dataProvider);
        final ListenableFuture<Void> result =
                locationProvider.replaceLocationForEndpoint(externalLocationCaseBuilder(), getAddressEpKey());
        Assert.assertNotNull(result);
        verify(dataProvider, times(1)).createTransactionChain(any());
        verify(transactionChain, times(2)).newWriteOnlyTransaction();
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(LocationProvider.class), eq(true));
        verify(wTx, times(1)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(ProviderAddressEndpointLocation.class));
        verify(wTx, times(2)).submit();
    }


    private VppEndpoint vppEndpointBuilder() {
        final VppEndpointBuilder vppEndpointBuilder = new VppEndpointBuilder();
        vppEndpointBuilder.setVppNodeId(nodeId)
                .setVppInterfaceName(INTERFACE_NAME);
        return vppEndpointBuilder.build();
    }

    private ExternalLocationCase externalLocationCaseBuilder() {
        final ExternalLocationCaseBuilder externalLocationCaseBuilder = new ExternalLocationCaseBuilder();
        return externalLocationCaseBuilder.build();
    }

    private AddressEndpointWithLocationKey getAddressEpKey() {
        final String ADDRESS_EP_KEY = "address-ep-key";
        return new AddressEndpointWithLocationKey(ADDRESS_EP_KEY, IpPrefixType.class, contextId, L3Context.class);
    }

}