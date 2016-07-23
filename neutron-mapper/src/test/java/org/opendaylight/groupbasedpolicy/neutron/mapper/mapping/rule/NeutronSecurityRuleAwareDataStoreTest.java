/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.ConfigDataStoreReader;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.NeutronBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.SecurityRuleAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRulesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * END 2 END TESTING - inputs are Neutron entities and expected outputs are GBP entities in
 * datastore
 */

public class NeutronSecurityRuleAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    @Test
    public final void testAddNeutronSecurityRule_rulesWithRemoteIpPrefix() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        SecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, EthertypeV4.class, DirectionIngress.class, goldSecGrp,
                null);
        SecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, EthertypeV4.class, DirectionEgress.class, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule serverIn80Tcp10_1_1_0 = new SecurityRuleBuilder().setUuid(new Uuid("9dbb533d-d9b2-4dc9-bae7-ee60c8df184d"))
                .setTenantId(new Uuid(tenant))
                .setEthertype(EthertypeV4.class)
                .setProtocol(protocolTcp)
                .setPortRangeMin(80)
                .setPortRangeMax(80)
                .setDirection(DirectionIngress.class)
                .setSecurityGroupId(new Uuid(serverSecGrp))
                .setRemoteIpPrefix(new IpPrefix(new Ipv4Prefix("10.1.1.0/24")))
                .build();
        SecurityRule serverInIp20_1_1_0 = new SecurityRuleBuilder().setUuid(new Uuid("adf7e558-de47-4f9e-a9b8-96e19db5d1ac"))
                .setTenantId(new Uuid(tenant))
                .setEthertype(EthertypeV4.class)
                .setDirection(DirectionIngress.class)
                .setSecurityGroupId(new Uuid(serverSecGrp))
                .setRemoteIpPrefix(new IpPrefix(new Ipv4Prefix("20.1.1.0/24")))
                .build();
        SecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, EthertypeV4.class, DirectionEgress.class, serverSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroups.add(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        Neutron neutron = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build())
            .setSecurityRules(new SecurityRulesBuilder().setSecurityRule(
                    ImmutableList.of(goldInIpv4, goldOutIpv4, serverIn80Tcp10_1_1_0, serverInIp20_1_1_0, serverOutIpv4))
                .build())
            .build();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80Tcp10_1_1_0, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIp20_1_1_0, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, neutron, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Tenant> potentialTenant = rwTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.tenantIid(tenantId))
            .get();
        assertTrue(potentialTenant.isPresent());
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverIn80Tcp10_1_1_0.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContractWithEic(contract, serverIn80Tcp10_1_1_0);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIp20_1_1_0.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContractWithEic(contract, serverInIp20_1_1_0);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getUuid().getValue()),
                        new ContractId(goldOutIpv4.getUuid().getValue()),
                        new ContractId(serverIn80Tcp10_1_1_0.getUuid().getValue()),
                        new ContractId(serverInIp20_1_1_0.getUuid().getValue()),
                        new ContractId(serverOutIpv4.getUuid().getValue())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(epg, ImmutableSet.of(
                new ContractId(serverIn80Tcp10_1_1_0.getUuid().getValue()),
                new ContractId(serverInIp20_1_1_0.getUuid().getValue()),
                new ContractId(goldInIpv4.getUuid().getValue())));
    }

    @Test
    public final void testAddAndDeleteNeutronSecurityRule() throws Exception {
        DataBroker dataBroker = getDataBroker();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker);

        final String tenantId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        final String secGroupId1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        final String secGroupId2 = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        final String secRuleId1 = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        final String secRuleId2 = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(secGroupId1, tenantId));
        secGroups.add(NeutronEntityFactory.securityGroup(secGroupId2, tenantId));
        SecurityRule secRule1 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId1, tenantId,
                EthertypeV4.class, DirectionEgress.class, secGroupId1, secGroupId2);
        SecurityRule secRule2 = NeutronEntityFactory.securityRuleWithEtherType(secRuleId2, tenantId,
                EthertypeV4.class, DirectionIngress.class, secGroupId2, secGroupId1);
        NeutronBuilder neutronBuilder = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build())
            .setSecurityRules(new SecurityRulesBuilder()
                    .setSecurityRule(ImmutableList.of(secRule1)).build());

        ruleAware.onCreated(secRule1, neutronBuilder.build());

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        Optional<Contract> contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        Optional<EndpointGroup> epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        neutronBuilder.setSecurityRules(new SecurityRulesBuilder()
                .setSecurityRule(ImmutableList.of(secRule1, secRule2)).build());

        ruleAware.onCreated(secRule2, neutronBuilder.build());

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId2)));

        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId2);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId2);
        PolicyAssert.assertContract(contract.get(), secRule2);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 2);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        Optional<EndpointGroup> epGroup2 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertProviderNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId2)));
        PolicyAssert.assertConsumerNamedSelectors(epGroup2.get(), ImmutableSet.of(new ContractId(secRuleId1)));

        ruleAware.onDeleted(secRule2, neutronBuilder.build(), null);

        PolicyAssert.assertTenantExists(dataBroker, tenantId);
        PolicyAssert.assertContractExists(dataBroker, tenantId, secRuleId1);
        contract = ConfigDataStoreReader.readContract(dataBroker, tenantId, secRuleId1);
        PolicyAssert.assertContract(contract.get(), secRule1);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        epGroup1 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertProviderNamedSelectors(epGroup1.get(), ImmutableSet.of(new ContractId(secRuleId1)));
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId1);

        PolicyAssert.assertContractNotExists(dataBroker, tenantId, secRuleId2);
        PolicyAssert.assertContractCount(dataBroker, tenantId, 1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        epGroup2 = ConfigDataStoreReader.readEndpointGroup(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);

        PolicyAssert.assertClassifierInstanceExists(dataBroker, secRule1);
        PolicyAssert.assertActionInstanceExists(dataBroker, tenantId, MappingUtils.ACTION_ALLOW.getName());

        neutronBuilder.setSecurityRules(new SecurityRulesBuilder()
                .setSecurityRule(ImmutableList.of(secRule1)).build());

        ruleAware.onDeleted(secRule1, neutronBuilder.build(), null);

        PolicyAssert.assertContractCount(dataBroker, tenantId, 0);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, secGroupId2);
        PolicyAssert.assertNoProviderNamedSelectors(dataBroker, tenantId, secGroupId1);
        PolicyAssert.assertNoConsumerNamedSelectors(dataBroker, tenantId, secGroupId2);

        PolicyAssert.assertClassifierInstanceNotExists(dataBroker, secRule1);
        // TODO: Uncomment this when the life cycle of the Allow ActionInstance will be clarified.
        // PolicyAssert.assertActionInstanceNotExists(dataBroker, tenantId,
        // MappingUtils.ACTION_ALLOW.getName());
    }

    @Test
    public final void testAddNeutronSecurityRule_rulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        SecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, EthertypeV4.class, DirectionIngress.class, goldSecGrp,
                null);
        SecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, EthertypeV4.class, DirectionEgress.class, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        SecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, EthertypeV4.class, DirectionEgress.class, serverSecGrp,
                null);
        SecurityRule serverInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "adf7e558-de47-4f9e-a9b8-96e19db5d1ac", tenant, EthertypeV4.class, DirectionIngress.class, serverSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroups.add(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        Neutron neutron = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build())
            .setSecurityRules(new SecurityRulesBuilder()
                    .setSecurityRule(ImmutableList.of(goldInIpv4, goldOutIpv4, serverOutIpv4, serverInIpv4)).build())
            .build();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverInIpv4, neutron, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverInIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverInIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getUuid().getValue()),
                        new ContractId(goldOutIpv4.getUuid().getValue()),
                        new ContractId(serverOutIpv4.getUuid().getValue()),
                        new ContractId(serverInIpv4.getUuid().getValue())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getUuid().getValue()),
                        new ContractId(goldOutIpv4.getUuid().getValue()),
                        new ContractId(serverOutIpv4.getUuid().getValue()),
                        new ContractId(serverInIpv4.getUuid().getValue())));
    }

    @Test
    public final void testAddNeutronSecurityRule_asymmetricRulesWithoutRemote() throws Exception {
        String tenant = "ad4c6c25-2424-4ad3-97ee-f9691ce03645";
        String goldSecGrp = "fe40e28f-ad6a-4a2d-b12a-47510876344a";
        SecurityRule goldInIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "166aedab-fdf5-4788-9e36-2b00b5f8722f", tenant, EthertypeV4.class, DirectionIngress.class, goldSecGrp,
                null);
        SecurityRule goldOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "dabfd4da-af89-45dd-85f8-181768c1b4c9", tenant, EthertypeV4.class, DirectionEgress.class, goldSecGrp,
                null);
        String serverSecGrp = "71cf4fe5-b146-409e-8151-cd921298ce32";
        SecurityRule serverOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "8b9c48d3-44a8-46be-be35-6f3237d98071", tenant, EthertypeV4.class, DirectionEgress.class, serverSecGrp,
                null);
        SecurityRuleAttributes.Protocol protocolTcp = new SecurityRuleAttributes.Protocol(ProtocolTcp.class);
        SecurityRule serverIn80TcpIpv4 = new SecurityRuleBuilder().setUuid(new Uuid("adf7e558-de47-4f9e-a9b8-96e19db5d1ac"))
                .setTenantId(new Uuid(tenant))
                .setEthertype(EthertypeV4.class)
                .setProtocol(protocolTcp)
                .setPortRangeMin(80)
                .setPortRangeMax(80)
                .setDirection(DirectionIngress.class)
                .setSecurityGroupId(new Uuid(serverSecGrp))
                .build();
        DataBroker dataBroker = getDataBroker();
        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(goldSecGrp, tenant));
        secGroups.add(NeutronEntityFactory.securityGroup(serverSecGrp, tenant));
        Neutron neutron = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build())
            .setSecurityRules(new SecurityRulesBuilder()
                .setSecurityRule(ImmutableList.of(goldInIpv4, goldOutIpv4, serverOutIpv4, serverIn80TcpIpv4)).build())
            .build();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(goldInIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(goldOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(serverIn80TcpIpv4, neutron, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldInIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldInIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(goldOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, goldOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(serverIn80TcpIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, serverIn80TcpIpv4);
        Optional<EndpointGroup> potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(goldSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        EndpointGroup epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getUuid().getValue()),
                        new ContractId(goldOutIpv4.getUuid().getValue()),
                        new ContractId(serverOutIpv4.getUuid().getValue()),
                        new ContractId(serverIn80TcpIpv4.getUuid().getValue())));
        potentialEpg = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, new EndpointGroupId(serverSecGrp))).get();
        assertTrue(potentialEpg.isPresent());
        epg = potentialEpg.get();
        PolicyAssert.assertConsumerNamedSelectors(
                epg,
                ImmutableSet.of(new ContractId(goldInIpv4.getUuid().getValue()),
                        new ContractId(serverIn80TcpIpv4.getUuid().getValue())));
    }

    @Test
    public final void testAddNeutronSecurityRule_defaultSecGrp() throws Exception {
        String tenant = "111aaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String defaultSecGrp = "111fffff-ffff-ffff-ffff-ffffffffffff";
        SecurityRule defaultInIpv4Default = NeutronEntityFactory.securityRuleWithEtherType(
                "111ccccc-111c-cccc-cccc-cccccccccccc", tenant, EthertypeV4.class, DirectionIngress.class, defaultSecGrp,
                defaultSecGrp);
        SecurityRule defaultInIpv6Default = NeutronEntityFactory.securityRuleWithEtherType(
                "222ccccc-111c-cccc-cccc-cccccccccccc", tenant, EthertypeV4.class, DirectionIngress.class, defaultSecGrp,
                defaultSecGrp);
        SecurityRule defaultOutIpv4 = NeutronEntityFactory.securityRuleWithEtherType(
                "333ccccc-111c-cccc-cccc-cccccccccccc", tenant, EthertypeV4.class, DirectionEgress.class, defaultSecGrp,
                null);
        SecurityRule defaultOutIpv6 = NeutronEntityFactory.securityRuleWithEtherType(
                "444ccccc-111c-cccc-cccc-cccccccccccc", tenant, EthertypeV4.class, DirectionEgress.class, defaultSecGrp,
                null);
        DataBroker dataBroker = getDataBroker();
        List<SecurityGroup> secGroups = new ArrayList<>();
        secGroups.add(NeutronEntityFactory.securityGroup(defaultSecGrp, tenant));
        Neutron neutron = new NeutronBuilder()
            .setSecurityGroups(new SecurityGroupsBuilder().setSecurityGroup(secGroups).build()).build();
        NeutronSecurityRuleAware ruleAware = new NeutronSecurityRuleAware(dataBroker);
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ruleAware.addNeutronSecurityRule(defaultInIpv4Default, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(defaultInIpv6Default, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv4, neutron, rwTx);
        ruleAware.addNeutronSecurityRule(defaultOutIpv6, neutron, rwTx);
        TenantId tenantId = new TenantId(tenant);
        Optional<Contract> potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv4Default.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        Contract contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultInIpv4Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultInIpv6Default.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultInIpv6Default);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv4.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultOutIpv4);
        potentialContract = rwTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.contractIid(tenantId, new ContractId(defaultOutIpv6.getUuid().getValue()))).get();
        assertTrue(potentialContract.isPresent());
        contract = potentialContract.get();
        PolicyAssert.assertContract(contract, defaultOutIpv6);
    }

    @Test
    public void testConstructor_invalidArgument() throws Exception {
        try {
            new NeutronSecurityRuleAware(null);
            fail(NullPointerException.class.getName() + " expected");
        } catch (NullPointerException ex) {
            // do nothing
        }
    }

}
