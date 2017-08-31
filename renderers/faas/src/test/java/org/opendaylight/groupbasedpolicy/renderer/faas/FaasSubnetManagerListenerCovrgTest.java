/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.gateways.Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.gateways.PrefixesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UlnDatastoreApi.class)
public class FaasSubnetManagerListenerCovrgTest {

    private InstanceIdentifier<Subnet> subnetIid;
    private FaasSubnetManagerListener listener;
    private final TenantId gbpTenantId = new TenantId("gbpTenantId");
    private final SubnetId subnetId = new SubnetId("subnetId");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final Uuid faasSubnetId = new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private DataBroker dataProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        subnetIid = mock(InstanceIdentifier.class);
        listener = new FaasSubnetManagerListener(dataProvider, gbpTenantId, faasTenantId,
                MoreExecutors.directExecutor());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged() throws ReadFailedException {
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.submitSubnetToDs(any(
                org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.Subnet.class));
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.removeSubnetFromDsIfExists(any(Uuid.class), any(Uuid.class));

        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        WriteTransaction woTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(rwTx.submit()).thenReturn(futureVoid);
        when(woTx.submit()).thenReturn(futureVoid);

        CheckedFuture<Optional<MappedSubnet>, ReadFailedException> futureMappedSubnet = mock(CheckedFuture.class);
        Optional<MappedSubnet> optMappedSubnet = mock(Optional.class);
        when(optMappedSubnet.isPresent()).thenReturn(true);
        when(futureMappedSubnet.checkedGet()).thenReturn(optMappedSubnet);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL, FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId)))
            .thenReturn(futureMappedSubnet);
        doNothing().when(woTx).put(eq(LogicalDatastoreType.OPERATIONAL),
                eq(FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId)), any(MappedSubnet.class));

        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(woTx);

        Subnet subnet = new SubnetBuilder().setId(subnetId).build();

        DataTreeModification<Subnet> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Subnet> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(subnet).when(mockModification).getDataAfter();

        listener.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));
    }

    @Test
    public void testLoadAll() {
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.submitSubnetToDs(any(
                org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.Subnet.class));

        List<Subnet> subnets = new ArrayList<>();
        List<MappedSubnet> mpSubnets = new ArrayList<>();

        Subnet subnet = new SubnetBuilder().setId(subnetId).build();
        subnets.add(subnet);
        MappedSubnet mappedSubnet =
                new MappedSubnetBuilder().setGbpSubnetId(subnetId).setFaasSubnetId(faasSubnetId).build();
        mpSubnets.add(mappedSubnet);

        listener.loadAll(subnets, mpSubnets);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitSubnetBuilder() {
        WriteTransaction wTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(wTx.submit()).thenReturn(futureVoid);
        doNothing().when(wTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataObject.class));
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wTx);

        List<Gateways> gatewaysList = new ArrayList<>();
        List<Prefixes> prefixesList = new ArrayList<>();
        Prefixes prefixes = new PrefixesBuilder().setPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/8"))).build();
        prefixesList.add(prefixes);
        Gateways gateways = new GatewaysBuilder().setGateway(new IpAddress(new Ipv4Address("10.0.0.55")))
            .setPrefixes(prefixesList)
            .build();
        gatewaysList.add(gateways);

        Subnet subnet = new SubnetBuilder().setId(subnetId).setGateways(gatewaysList).build();
        listener.initSubnetBuilder(subnet);
    }

}
