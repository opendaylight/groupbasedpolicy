/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroupBuilder;

public class NeutronSecurityGroupAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    private final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";

    private DataBroker dataBroker;
    private NeutronSecurityGroupAware groupAware;
    private SecurityGroup secGroup1;
    private SecurityGroup secGroup2;
    private EndpointRegistrator epRegistrator;

    @Before
    @SuppressWarnings("checkstyle:LineLength") // Longer lines in this method are caused by long package names,
                                               // this will be removed when deprecated classes will be cleared.
    public void init() {
        dataBroker = getDataBroker();

        secGroup1 = NeutronEntityFactory.securityGroup(secGroupId1, tenantId);
        secGroup2 = NeutronEntityFactory.securityGroup(secGroupId2, tenantId);


        epRegistrator = mock(EndpointRegistrator.class);
        when(epRegistrator.registerEndpoint(any(RegisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput.class)))
                    .thenReturn(true);
        when(epRegistrator.unregisterEndpoint(any(UnregisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.unregisterEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput.class)))
                    .thenReturn(true);
        groupAware = new NeutronSecurityGroupAware(dataBroker, new NeutronSecurityRuleAware(dataBroker, epRegistrator));
    }

    @Test
    public void testAddAndDeleteNeutronSecurityGroup_noSecurityRules() throws Exception {
        groupAware.onCreated(secGroup1, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);

        groupAware.onDeleted(secGroup1, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);

        groupAware.onCreated(secGroup1, null);
        groupAware.onCreated(secGroup2, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId1, IntraGroupPolicy.RequireContract);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertIntraGroupPolicy(dataBroker, tenantId, secGroupId2, IntraGroupPolicy.RequireContract);

        groupAware.onDeleted(secGroup2, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        groupAware.onDeleted(secGroup1, null, null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupCount(dataBroker, tenantId, 0);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_invalidArgument() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new NeutronSecurityGroupAware(null, null);
    }

    @Test
    public void testAddNeutronSecurityGroup_ExternalImplicitGroup() throws Exception {
        String uuid = MappingUtils.EIG_UUID.getValue();
        SecurityGroup secGroupA = new SecurityGroupBuilder().setUuid(new Uuid(uuid))
            .setTenantId(new Uuid(tenantId))
            .setName("correctName")
            .build();
        groupAware.onCreated(secGroupA, null);

        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, uuid);
    }

    @Test
    public void testAddNeutronSecurityGroup_incorrectNameDescription() throws Exception {
        String uuid = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        SecurityGroup secGroupA = new SecurityGroupBuilder().setUuid(new Uuid(uuid))
            .setTenantId(new Uuid(tenantId))
            .setName("123")
            .build();
        groupAware.onCreated(secGroupA, null);

        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, uuid);
    }

    @Test
    public void testOnDelete_NonExistingSecGroup() throws Exception {
        PolicyAssert.assertEndpointGroupNotExists(dataBroker, tenantId, secGroupId2);

        groupAware.onDeleted(secGroup2, null, null);

        PolicyAssert.assertEndpointGroupNotExists(dataBroker, tenantId, secGroupId2);
    }

}
