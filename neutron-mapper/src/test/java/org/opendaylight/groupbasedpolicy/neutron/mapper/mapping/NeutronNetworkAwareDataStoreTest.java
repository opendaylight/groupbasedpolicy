/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NeutronNetworkAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    private final Uuid tenantUuid = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final Uuid networkUuid = new Uuid("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final long METADATA_IPV4_SERVER_PORT = 80;
    private static final IpPrefix METADATA_IP_PREFIX = new IpPrefix(new Ipv4Prefix("169.254.169.254/32"));

    private DataBroker dataBroker;
    private NeutronNetworkAware networkAware;
    private Neutron neutron;
    private Future<RpcResult<Void>> futureRpcResult;
    private Future<RpcResult<Void>> futureRpcFail;
    private RpcResult<Void> rpcResult;
    private RpcResult<Void> rpcFail;
    private Network network;
    private NetworkProviderExtension providerExtension;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        futureRpcResult = mock(Future.class);
        futureRpcFail = mock(Future.class);
        rpcResult = mock(RpcResult.class);
        rpcFail = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(rpcFail.isSuccessful()).thenReturn(false);
        dataBroker = getDataBroker();
        neutron = mock(Neutron.class);

        when(futureRpcResult.get()).thenReturn(rpcResult);
        when(futureRpcFail.get()).thenReturn(rpcFail);

        providerExtension = new NetworkProviderExtensionBuilder().setPhysicalNetwork("physicalNetwork")
            .setSegmentationId("segmentationId")
            .build();

        network = new NetworkBuilder().setTenantId(tenantUuid)
            .setUuid(networkUuid)
            .setName("networkName")
            .addAugmentation(NetworkProviderExtension.class, providerExtension)
            .build();

        networkAware = new NeutronNetworkAware(dataBroker, METADATA_IPV4_SERVER_PORT);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_invalidArgument() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new NeutronNetworkAware(null, METADATA_IPV4_SERVER_PORT);
    }

    @Test
    public void testOnCreated() {

        networkAware.onCreated(network, neutron);

        assertNetworkExists(network);
    }

    @Test
    public void testOnCreated_incorrectName() {
        Network network = new NetworkBuilder().setTenantId(tenantUuid).setName("123").setUuid(networkUuid).build();

        networkAware.onCreated(network, neutron);

        assertNetworkExists(network);
    }

    @Test
    public void testOnDeleted() {
        Network network =
                new NetworkBuilder().setTenantId(tenantUuid).setName("networkName").setUuid(networkUuid).build();

        assertNetworkNotExists(network);

        networkAware.onCreated(network, neutron);

        assertNetworkExists(network);

        networkAware.onDeleted(network, neutron, neutron);

        assertNetworkNotExists(network);
    }

    @Test
    public void testOnUpdated() {
        Network network =
                new NetworkBuilder().setTenantId(tenantUuid).setName("networkName").setUuid(networkUuid).build();

        networkAware.onUpdated(network, network, neutron, neutron);
        // no op
    }

    private void assertNetworkExists(Network network) {
        Optional<L3Context> opt = getL3ContextOptional(network);
        if (opt.isPresent()) {
            assertEquals(network.getUuid().getValue(), opt.get().getId().getValue());
        } else {
            fail("no network in DS, Uuid:" + network.getUuid());
        }
    }

    private void assertNetworkNotExists(Network network) {
        Optional<L3Context> opt = getL3ContextOptional(network);
        if (opt.isPresent()) {
            assertNotEquals(network.getUuid().getValue(), opt.get().getId().getValue());
        }
    }

    private Optional<L3Context> getL3ContextOptional(Network network) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        TenantId tenantId = new TenantId(network.getTenantId().getValue());
        ContextId l3CtxId = new ContextId(network.getUuid().getValue());
        L3ContextId l3ContextId = new L3ContextId(l3CtxId);
        InstanceIdentifier<L3Context> l3ContextIid = IidFactory.l3ContextIid(tenantId, l3ContextId);
        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l3ContextIid, rTx);
    }

}
