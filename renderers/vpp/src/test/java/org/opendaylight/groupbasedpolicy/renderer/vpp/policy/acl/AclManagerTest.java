/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class AclManagerTest extends TestResources {

    private PolicyContext ctx;
    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;
    private AclManager aclManager;

    @Before
    public void init() {
        ctx = super.createPolicyContext(createAddressEndpoints(), createRendEps(), createRuleGroups(),
                createForwarding());
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = getDataBroker();
        aclManager = new AclManager(mountedDataProviderMock, Mockito.mock(InterfaceManager.class));
        Mockito.when(mountedDataProviderMock.resolveDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(mountPointDataBroker);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(VppIidFactory.getNetconfNodeIid(new NodeId("node1")),
                new AbstractMap.SimpleEntry<DataBroker, ReentrantLock>(mountPointDataBroker, new ReentrantLock()));
    }

    @Test
    public void oneEndpointChanged() {
        List<Acl> acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp1.getKey())), true);
        Assert.assertEquals(acls.size(), 6);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                Assert.assertEquals(2 + 4, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(2 + 2, acl.getAccessListEntries().getAce().size());
            }
        });
        acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp1.getKey())), false);
        Assert.assertEquals(acls.size(), 6);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            }
        });
    }

    @Test
    public void oneEndpointChanged_createRulesOnlyForChangedEpInPeer() {
        List<Acl> acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp2.getKey())), true);
        Assert.assertEquals(acls.size(), 4);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                // TODO fix so that the following assert is uncomemnted
                // Assert.assertEquals(2 + 2, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(2 + 2, acl.getAccessListEntries().getAce().size());
            }
        });
        acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp2.getKey())), false);
        Assert.assertEquals(acls.size(), 4);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            }
        });
    }

    /**
     * Metadata use case. DHCP endpoint and metadata endpoint are on the same access interface and
     * so their entries meet in one ACL.
     */
    @Test
    public void oneEndpointChanged_metadataUseCase() {
        createContextFormetadataUseCase();
        List<Acl> acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp2.getKey())), true);
        Assert.assertEquals(acls.size(), 4);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                Assert.assertEquals(10, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(6, acl.getAccessListEntries().getAce().size());
            }
        });
        acls = processChangedEndpoints(Sets.newHashSet(rendererEndpointKey(l3AddrEp2.getKey())), false);
        Assert.assertEquals(acls.size(), 4);
        acls.forEach(acl -> {
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            } else {
                Assert.assertEquals(2, acl.getAccessListEntries().getAce().size());
            }
        });
    }

    public void createContextFormetadataUseCase() {
        String ep4Mac = "11:11:22:22:44:44";
        String ep4Ip = "10.0.0.40/32";
        final AddressEndpointWithLocation l2MetadataEp =
                l2AddressEndpointWithLocation(ep4Mac, L2_BD_ID, ep4Ip, L3_CTX_ID);
        final AddressEndpointWithLocation l3MetadataEp = appendLocationToEndpoint(
                l3AddressEndpointWithLocation(ep4Mac, L2_BD_ID, ep4Ip, L3_CTX_ID), NODE1, NODE1_CONNECTOR_1);
        List<AddressEndpointWithLocation> addrEps = createAddressEndpoints();
        addrEps.add(l2MetadataEp);
        addrEps.add(l3MetadataEp);
        List<RendererEndpoint> rEps = new ArrayList<>();
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp1.getKey()), SECURITY_GROUP.SERVER,
                peerEndpointKey(l3AddrEp2.getKey()), peerEndpointKey(l3AddrEp3.getKey())));
        rEps.add(createRendEndpoint(rendererEndpointKey(l3MetadataEp.getKey()), SECURITY_GROUP.SERVER,
                peerEndpointKey(l3AddrEp2.getKey()), peerEndpointKey(l3AddrEp3.getKey())));
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp2.getKey()), SECURITY_GROUP.CLIENT,
                peerEndpointKey(l3AddrEp1.getKey()), peerEndpointKey(l3MetadataEp.getKey())));
        rEps.add(createRendEndpoint(rendererEndpointKey(l3AddrEp3.getKey()), SECURITY_GROUP.CLIENT,
                peerEndpointKey(l3AddrEp1.getKey()), peerEndpointKey(l3MetadataEp.getKey())));
        ctx = super.createPolicyContext(addrEps, rEps, createRuleGroups(), createForwarding());
    }

    public List<Acl> processChangedEndpoints(Set<RendererEndpointKey> changedEndpoints, boolean created) {
        aclManager.cacheEndpointsByInterfaces(ctx);
        SetView<RendererEndpointKey> deltaEndpoints = Sets.difference(changedEndpoints, Sets.newHashSet());
        SetView<RuleGroupKey> deltaRules = Sets.intersection(Sets.newHashSet(), Sets.newHashSet());
        aclManager.resolveRulesToConfigure(ctx, deltaEndpoints, deltaRules, created);
        ReadOnlyTransaction rTx = mountPointDataBroker.newReadOnlyTransaction();
        Optional<AccessLists> readAccessLists = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(AccessLists.class).build(), rTx);
        rTx.close();
        Assert.assertTrue(readAccessLists.isPresent());
        return readAccessLists.get().getAcl();
    }

    @Test
    public void oneRuleChanged() {
        processAndAssertChangedRules(Sets.newHashSet(new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME, TENANT_ID)), true);
        processAndAssertChangedRules(Sets.newHashSet(new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME, TENANT_ID)), false);
    }

    @Test
    public void twoRulesChanged() {
        processAndAssertChangedRules(Sets.newHashSet(new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME, TENANT_ID),
                new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME2, TENANT_ID)), true);
        processAndAssertChangedRules(Sets.newHashSet(new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME, TENANT_ID),
                new RuleGroupKey(CONTRACT_ID, SUBJECT_NAME2, TENANT_ID)), false);
    }

    public void processAndAssertChangedRules(Set<RuleGroupKey> createdRuleGroups, boolean created) {
        aclManager.cacheEndpointsByInterfaces(ctx);
        SetView<RendererEndpointKey> deltaRendEp = Sets.intersection(Sets.newHashSet(), Sets.newHashSet());
        SetView<RuleGroupKey> deltaRuleGroups = Sets.difference(createdRuleGroups, Sets.newHashSet());
        aclManager.resolveRulesToConfigure(ctx, deltaRendEp, deltaRuleGroups, created);
        ReadOnlyTransaction rTx = mountPointDataBroker.newReadOnlyTransaction();
        Optional<AccessLists> readAccessLists = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(AccessLists.class).build(), rTx);
        rTx.close();
        Assert.assertTrue(readAccessLists.isPresent());
        List<Acl> acls = readAccessLists.get().getAcl();
        Assert.assertEquals(acls.size(), 6);
        acls.forEach(acl -> {
            int diff;
            if (acl.getAclName().contains(VppPathMapper.interfacePathToInterfaceName(NODE1_CONNECTOR_1).get())) {
                diff = 2 * createdRuleGroups.size() * ((created) ? 1 : 0);
            } else {
                diff = createdRuleGroups.size() * ((created) ? 1 : 0);
            }
            Assert.assertEquals(2 + diff, acl.getAccessListEntries().getAce().size());
        });
    }
}
