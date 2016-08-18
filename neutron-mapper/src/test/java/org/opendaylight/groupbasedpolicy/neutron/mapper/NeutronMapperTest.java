/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronNetworkAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.ext.rev150712.NetworkL3Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.ext.rev150712.NetworkL3ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.NeutronBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class NeutronMapperTest extends NeutronMapperDataBrokerTest {

    private final Uuid tenantUuid = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final Uuid networkUuid = new Uuid("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private final Uuid networkUuid2 = new Uuid("dddddddd-dddd-dddd-dddd-ddddddddddd2");

    private DataBroker dataBroker;
    private RpcProviderRegistry rpcProvider;
    private EndpointService epService;
    private BaseEndpointService baseEpService;

    private DataObjectModification<Neutron> rootNode;
    private Set<DataTreeModification<Neutron>> changes;
    private Neutron oldNeutronBefore;
    private Neutron oldNeutronAfter;

    private NeutronMapper mapper;
    private DataObjectModification<Network> networkModif;
    private Network networkBefore;
    private Network networkAfter;
    private NetworkL3Extension networkL3Extension;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        epService = mock(EndpointService.class);
        baseEpService = mock(BaseEndpointService.class);

        mapper = new NeutronMapper(dataBroker, epService, baseEpService);

        networkL3Extension = new NetworkL3ExtensionBuilder().setExternal(true).build();

        rootNode = mock(DataObjectModification.class);

        networkModif = mock(DataObjectModification.class);
        networkBefore = new NetworkBuilder().setTenantId(tenantUuid).setUuid(networkUuid).build();
        networkAfter = new NetworkBuilder().setTenantId(tenantUuid)
            .setUuid(networkUuid2)
            .addAugmentation(NetworkL3Extension.class, networkL3Extension)
            .build();
        when(networkModif.getDataType()).thenReturn(NeutronNetworkAware.NETWORK_WILDCARD_IID.getTargetType());

        when(networkModif.getDataBefore()).thenReturn(networkBefore);
        when(networkModif.getDataAfter()).thenReturn(networkAfter);

        when(rootNode.getModifiedChildren()).thenReturn(ImmutableSet.of(networkModif));

        DataTreeModification<Neutron> change = mock(DataTreeModification.class);
        when(change.getRootNode()).thenReturn(rootNode);

        changes = ImmutableSet.of(change);

        oldNeutronBefore = new NeutronBuilder().build();
        SecurityGroup sg = new SecurityGroupBuilder().setTenantId(tenantUuid).setUuid(MappingUtils.EIG_UUID).build();
        SecurityGroups securityGroups = new SecurityGroupsBuilder().setSecurityGroup(ImmutableList.of(sg)).build();
        oldNeutronAfter = new NeutronBuilder().setSecurityGroups(securityGroups).build();
    }

    @Test
    public void testConstructor() throws IOException {
        DataBroker dataBrokerSpy = spy(dataBroker);
        NeutronMapper other = new NeutronMapper(dataBrokerSpy, epService, baseEpService);

        verify(dataBrokerSpy).registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Neutron.class).build()), other);

        other.close();
    }

    @Test
    public void test_Write_withNetworkCreate() {
        when(networkModif.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(networkModif.getDataBefore()).thenReturn(null);
        when(rootNode.getDataBefore()).thenReturn(oldNeutronBefore);
        when(rootNode.getDataAfter()).thenReturn(oldNeutronAfter);

        mapper.onDataTreeChanged(changes);
        assertNetworkExists(networkAfter);
    }

    @Test
    public void test_Write_withNetworkUpdate() {
        when(networkModif.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(networkModif.getDataBefore()).thenReturn(null);
        when(networkModif.getDataAfter()).thenReturn(networkBefore);
        when(rootNode.getDataBefore()).thenReturn(oldNeutronBefore);
        when(rootNode.getDataAfter()).thenReturn(oldNeutronAfter);

        mapper.onDataTreeChanged(changes);

        assertNetworkExists(networkBefore);
        assertNetworkNotExists(networkAfter);

        when(networkModif.getDataBefore()).thenReturn(networkBefore);
        when(networkModif.getDataAfter()).thenReturn(networkAfter);

        mapper.onDataTreeChanged(changes);
        // no-op in NeutronNetworkAware#onUpdated
    }

    @Test
    public void test_Delete() {
        when(networkModif.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(rootNode.getDataBefore()).thenReturn(oldNeutronBefore);
        when(rootNode.getDataAfter()).thenReturn(oldNeutronAfter);

        mapper.onDataTreeChanged(changes);
    }

    @Test
    public void test_SubtreeModified() {
        when(networkModif.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(oldNeutronBefore);
        when(rootNode.getDataAfter()).thenReturn(oldNeutronAfter);

        mapper.onDataTreeChanged(changes);
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
