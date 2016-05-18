/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.FollowedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IidFactoryTest {

    private final String LOCATION_PROVIDER_NAME = "location-provider";
    private final String IP_ADDRESS = "192.68.50.71";
    private final String IP_PREFIX = "192.168.50.0/24";
    private final String L3_CONTEXT_ID = "l3Context";
    private final String CONNECTOR = "connector";

    private TenantId tenantId;
    private EndpointGroupId epgId;
    private ContractId contractId;
    private SubjectName subjectName;
    private RuleName ruleName;
    private RendererName rendererName;

    private InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("node"))).build();
    private InstanceIdentifier<NodeConnector> connectorIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("node")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("connector")))
            .build();

    @Before
    public void initialise() {
        tenantId = mock(TenantId.class);
        epgId = mock(EndpointGroupId.class);
        contractId = mock(ContractId.class);
        subjectName = mock(SubjectName.class);
        ruleName = mock(RuleName.class);
        rendererName = mock(RendererName.class);
    }

    @Test
    public void testTenantId() {
        InstanceIdentifier<Tenant> identifier = IidFactory.tenantIid(tenantId);
        Assert.assertEquals(tenantId, InstanceIdentifier.keyOf(identifier).getId());
    }

    @Test
    public void testEndpointGroupIid() {
        InstanceIdentifier<EndpointGroup> identifier = IidFactory.endpointGroupIid(tenantId, epgId);
        Assert.assertEquals(epgId, InstanceIdentifier.keyOf(identifier).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testContractIid() {
        InstanceIdentifier<Contract> identifier = IidFactory.contractIid(tenantId, contractId);
        Assert.assertEquals(contractId, InstanceIdentifier.keyOf(identifier).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testContractWildcardIid() {
        InstanceIdentifier<Contract> identifier = IidFactory.contractWildcardIid(tenantId);
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testSubjectIid() {
        SubjectName subjectName = mock(SubjectName.class);
        InstanceIdentifier<Subject> identifier = IidFactory.subjectIid(tenantId, contractId, subjectName);
        Assert.assertEquals(subjectName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(contractId, identifier.firstKeyOf(Contract.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testProviderNamedSelectorIid() {
        SelectorName providerSelectorName = mock(SelectorName.class);
        InstanceIdentifier<ProviderNamedSelector> identifier = IidFactory.providerNamedSelectorIid(tenantId, epgId,
                providerSelectorName);
        Assert.assertEquals(providerSelectorName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(epgId, identifier.firstKeyOf(EndpointGroup.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testConsumerNamedSelectorIid() {
        SelectorName consumerSelectorName = mock(SelectorName.class);
        InstanceIdentifier<ConsumerNamedSelector> identifier = IidFactory.consumerNamedSelectorIid(tenantId, epgId,
                consumerSelectorName);
        Assert.assertEquals(consumerSelectorName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(epgId, identifier.firstKeyOf(EndpointGroup.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testClauseIid() {
        ClauseName clauseName = mock(ClauseName.class);
        InstanceIdentifier<Clause> identifier = IidFactory.clauseIid(tenantId, contractId, clauseName);
        Assert.assertEquals(clauseName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(contractId, identifier.firstKeyOf(Contract.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testRuleId() {
        InstanceIdentifier<Rule> identifier = IidFactory.ruleIid(tenantId, contractId, subjectName, ruleName);
        Assert.assertEquals(ruleName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(subjectName, identifier.firstKeyOf(Subject.class).getName());
        Assert.assertEquals(contractId, identifier.firstKeyOf(Contract.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testActionInstanceIid() {
        ActionName actionName = mock(ActionName.class);
        InstanceIdentifier<ActionInstance> identifier = IidFactory.actionInstanceIid(tenantId, actionName);
        Assert.assertEquals(actionName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testClassifierInstanceIid() {
        ClassifierName classifierName = mock(ClassifierName.class);
        InstanceIdentifier<ClassifierInstance> identifier = IidFactory.classifierInstanceIid(tenantId, classifierName);
        Assert.assertEquals(classifierName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testClassifierRefIid() {
        ClassifierName classifierRefName = mock(ClassifierName.class);
        InstanceIdentifier<ClassifierRef> identifier = IidFactory.classifierRefIid(tenantId, contractId, subjectName,
                ruleName, classifierRefName);
        Assert.assertEquals(classifierRefName, InstanceIdentifier.keyOf(identifier).getName());
        Assert.assertEquals(ruleName, identifier.firstKeyOf(Rule.class).getName());
        Assert.assertEquals(subjectName, identifier.firstKeyOf(Subject.class).getName());
        Assert.assertEquals(contractId, identifier.firstKeyOf(Contract.class).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testL2FloodDomainIid() {
        L2FloodDomainId l2FloodDomainId = mock(L2FloodDomainId.class);
        InstanceIdentifier<L2FloodDomain> identifier = IidFactory.l2FloodDomainIid(tenantId, l2FloodDomainId);
        Assert.assertEquals(l2FloodDomainId, InstanceIdentifier.keyOf(identifier).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testL2BridgeDomainIid() {
        L2BridgeDomainId l2BridgeDomainId = mock(L2BridgeDomainId.class);
        InstanceIdentifier<L2BridgeDomain> identifier = IidFactory.l2BridgeDomainIid(tenantId, l2BridgeDomainId);
        Assert.assertEquals(l2BridgeDomainId, InstanceIdentifier.keyOf(identifier).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testL3ContextIid() {
        L3ContextId l3ContextId = mock(L3ContextId.class);
        InstanceIdentifier<L3Context> identifier = IidFactory.l3ContextIid(tenantId, l3ContextId);
        Assert.assertEquals(l3ContextId, InstanceIdentifier.keyOf(identifier).getId());
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testEndpointIid() {
        L2BridgeDomainId l2Context = mock(L2BridgeDomainId.class);
        MacAddress macAddress = mock(MacAddress.class);
        InstanceIdentifier<Endpoint> identifier = IidFactory.endpointIid(l2Context, macAddress);
        Assert.assertEquals(l2Context, InstanceIdentifier.keyOf(identifier).getL2Context());
        Assert.assertEquals(macAddress, InstanceIdentifier.keyOf(identifier).getMacAddress());

        EndpointKey key = mock(EndpointKey.class);
        identifier = IidFactory.endpointIid(key);
        Assert.assertEquals(key, identifier.firstKeyOf(Endpoint.class));
    }

    @Test
    public void testL3EndpointIid() {
        L3ContextId l3ContextId = mock(L3ContextId.class);
        IpAddress ipAddress = mock(IpAddress.class);
        InstanceIdentifier<EndpointL3> identifier = IidFactory.l3EndpointIid(l3ContextId, ipAddress);
        Assert.assertEquals(l3ContextId, InstanceIdentifier.keyOf(identifier).getL3Context());
        Assert.assertEquals(ipAddress, InstanceIdentifier.keyOf(identifier).getIpAddress());
    }

    @Test
    public void testL3EndpointIidWildcard() {
        InstanceIdentifier<EndpointL3> identifier = IidFactory.l3EndpointsIidWildcard();
        Assert.assertNotNull(identifier);
    }

    @Test
    public void testEndpointL3PrefixIid() {
        L3ContextId l3Context = mock(L3ContextId.class);
        IpPrefix ipPrefix = mock(IpPrefix.class);
        InstanceIdentifier<EndpointL3Prefix> identifier = IidFactory.endpointL3PrefixIid(l3Context, ipPrefix);
        Assert.assertEquals(l3Context, InstanceIdentifier.keyOf(identifier).getL3Context());
        Assert.assertEquals(ipPrefix, InstanceIdentifier.keyOf(identifier).getIpPrefix());
    }

    @Test
    public void testEndpointIidWildcard() {
        InstanceIdentifier<Endpoints> identifier = IidFactory.endpointsIidWildcard();
        Assert.assertNotNull(identifier);
    }

    @Test
    public void testSubnetIid() {
        SubnetId subnetId = mock(SubnetId.class);
        InstanceIdentifier<Subnet> identifier = IidFactory.subnetIid(tenantId, subnetId);
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
        Assert.assertEquals(subnetId, identifier.firstKeyOf(Subnet.class).getId());
    }

    @Test
    public void testSubnetWildcardIid() {
        InstanceIdentifier<Subnet> identifier = IidFactory.subnetWildcardIid(tenantId);
        Assert.assertEquals(tenantId, identifier.firstKeyOf(Tenant.class).getId());
    }

    @Test
    public void testClassifierDefinitionIid() {
        ClassifierDefinitionId classifierDefinitionId = mock(ClassifierDefinitionId.class);
        InstanceIdentifier<ClassifierDefinition> identifier = IidFactory.classifierDefinitionIid(
                classifierDefinitionId);
        Assert.assertEquals(classifierDefinitionId, identifier.firstKeyOf(ClassifierDefinition.class).getId());
    }

    @Test
    public void testActionDefinitionIid() {
        ActionDefinitionId actionDefinitionId = mock(ActionDefinitionId.class);
        InstanceIdentifier<ActionDefinition> identifier = IidFactory.actionDefinitionIid(
                actionDefinitionId);
        Assert.assertEquals(actionDefinitionId, identifier.firstKeyOf(ActionDefinition.class).getId());
    }

    @Test
    public void testSupportedActionDefinitionIidWildcard() {
        InstanceIdentifier<SupportedActionDefinition>
                identifier = IidFactory.supportedActionDefinitionIidWildcard();
        Assert.assertNotNull(identifier);
    }

    @Test
    public void testSupportedClassifierDefinitionIidWildcard() {
        InstanceIdentifier<SupportedClassifierDefinition>
                identifier = IidFactory.supportedClassifierDefinitionIidWildcard();
        Assert.assertNotNull(identifier);
    }

    @Test
    public void  testFollowedEndpointgroupIid(){
        InstanceIdentifier<FollowedEndpointGroup>
                identifier = IidFactory.followedEndpointgroupIid(rendererName, tenantId, epgId);
        Assert.assertEquals(epgId, identifier.firstKeyOf(FollowedEndpointGroup.class).getId());
    }

    @Test
    public void  testFollowedTenantIid(){
        InstanceIdentifier<FollowedTenant>
                identifier = IidFactory.followedTenantIid(rendererName, tenantId);
        Assert.assertEquals(tenantId, identifier.firstKeyOf(FollowedTenant.class).getId());
    }

    @Test
    public void  testRendererIid(){
        InstanceIdentifier<Renderer>
                identifier = IidFactory.rendererIid(rendererName);
        Assert.assertEquals(rendererName, identifier.firstKeyOf(Renderer.class).getName());
    }

    @Test
    public void  testRenderersIid(){
        InstanceIdentifier<Renderers>
                identifier = IidFactory.renderersIid();
        Assert.assertNotNull(identifier);
    }

    @Test
    public void  testeExternalImplicitGroupIid(){
        InstanceIdentifier<ExternalImplicitGroup>
                identifier = IidFactory.externalImplicitGroupIid(tenantId, epgId);
        Assert.assertEquals(epgId, identifier.firstKeyOf(ExternalImplicitGroup.class).getId());
    }

    @Test
    public void testProviderAddressEndpointLocationIid() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        InstanceIdentifier<ProviderAddressEndpointLocation> identifier = IidFactory
            .providerAddressEndpointLocationIid(LOCATION_PROVIDER_NAME, IpPrefixType.class, IP_ADDRESS,
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                    l3Context);
        Assert.assertEquals(LOCATION_PROVIDER_NAME,
                identifier.firstKeyOf(LocationProvider.class).getProvider().getValue());
        Assert.assertEquals(IP_ADDRESS, identifier.firstKeyOf(ProviderAddressEndpointLocation.class).getAddress());
        Assert.assertEquals(l3Context, identifier.firstKeyOf(ProviderAddressEndpointLocation.class).getContextId());
    }

    @Test
    public void testAddressEndpointIid() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        InstanceIdentifier<AddressEndpoint> identifier =
                IidFactory.addressEndpointIid(new AddressEndpointKey(IP_ADDRESS, IpPrefixType.class, l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class
                        ));
        Assert.assertEquals(IpPrefixType.class, identifier.firstKeyOf(AddressEndpoint.class).getAddressType());
        Assert.assertEquals(IP_ADDRESS, identifier.firstKeyOf(AddressEndpoint.class).getAddress());
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                identifier.firstKeyOf(AddressEndpoint.class).getContextType());
        Assert.assertEquals(l3Context, identifier.firstKeyOf(AddressEndpoint.class).getContextId());
    }

    @Test
    public void testAddressEndpointLocationIid() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        AddressEndpointLocationKey addrEndpointLocationKey =
                new AddressEndpointLocationKey(IP_ADDRESS, IpPrefixType.class, l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        InstanceIdentifier<AddressEndpointLocation> iid = IidFactory.addressEndpointLocationIid(addrEndpointLocationKey);
        Assert.assertEquals(IpPrefixType.class, iid.firstKeyOf(AddressEndpointLocation.class).getAddressType());
        Assert.assertEquals(IP_ADDRESS, iid.firstKeyOf(AddressEndpointLocation.class).getAddress());
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(AddressEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(AddressEndpointLocation.class).getContextId());
    }

    @Test
    public void testContainmentEndpointLocationIid() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        ContainmentEndpointLocationKey contEndpointLocationKey =
                new ContainmentEndpointLocationKey(l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        InstanceIdentifier<ContainmentEndpointLocation> iid = IidFactory.containmentEndpointLocationIid(contEndpointLocationKey);
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(ContainmentEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(ContainmentEndpointLocation.class).getContextId());
    }

    @Test
    public void internalLocationIid_AddrEndpoint() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        AddressEndpointLocationKey addrEndpointLocationKey =
                new AddressEndpointLocationKey(IP_ADDRESS, IpPrefixType.class, l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        InternalLocationKey internalLocationKey = new InternalLocationKey(nodeIid, connectorIid);
        InstanceIdentifier<InternalLocation> iid = IidFactory.internalLocationIid(addrEndpointLocationKey, internalLocationKey);
        Assert.assertEquals(IpPrefixType.class, iid.firstKeyOf(AddressEndpointLocation.class).getAddressType());
        Assert.assertEquals(IP_ADDRESS, iid.firstKeyOf(AddressEndpointLocation.class).getAddress());
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(AddressEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(AddressEndpointLocation.class).getContextId());
        Assert.assertEquals(nodeIid, iid.firstKeyOf(InternalLocation.class).getInternalNode());
        Assert.assertEquals(connectorIid, iid.firstKeyOf(InternalLocation.class).getInternalNodeConnector());
    }

    @Test
    public void internalLocationIid_ContEndpoint() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        ContainmentEndpointLocationKey contEndpointLocationKey =
                new ContainmentEndpointLocationKey(l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        InternalLocationKey internalLocationKey = new InternalLocationKey(nodeIid, connectorIid);
        InstanceIdentifier<InternalLocation> iid = IidFactory.internalLocationIid(contEndpointLocationKey, internalLocationKey);
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(ContainmentEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(ContainmentEndpointLocation.class).getContextId());
        Assert.assertEquals(nodeIid, iid.firstKeyOf(InternalLocation.class).getInternalNode());
        Assert.assertEquals(connectorIid, iid.firstKeyOf(InternalLocation.class).getInternalNodeConnector());
    }

    @Test
    public void externalLocationIid_AddrEndpoint() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        AddressEndpointLocationKey addrEndpointLocationKey =
                new AddressEndpointLocationKey(IP_ADDRESS, IpPrefixType.class, l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        ExternalLocationKey externalLocationKey = new ExternalLocationKey(CONNECTOR, nodeIid);
        InstanceIdentifier<ExternalLocation> iid = IidFactory.externalLocationIid(addrEndpointLocationKey, externalLocationKey);
        Assert.assertEquals(IpPrefixType.class, iid.firstKeyOf(AddressEndpointLocation.class).getAddressType());
        Assert.assertEquals(IP_ADDRESS, iid.firstKeyOf(AddressEndpointLocation.class).getAddress());
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(AddressEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(AddressEndpointLocation.class).getContextId());
        Assert.assertEquals(CONNECTOR, iid.firstKeyOf(ExternalLocation.class).getExternalNodeConnector());
        Assert.assertEquals(nodeIid, iid.firstKeyOf(ExternalLocation.class).getExternalNodeMountPoint());
    }

    @Test
    public void externalLocationIid_ContEndpoint() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        ContainmentEndpointLocationKey addrEndpointLocationKey =
                new ContainmentEndpointLocationKey(l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        ExternalLocationKey externalLocationKey = new ExternalLocationKey(CONNECTOR, nodeIid);
        InstanceIdentifier<ExternalLocation> iid = IidFactory.externalLocationIid(addrEndpointLocationKey, externalLocationKey);
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(ContainmentEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(ContainmentEndpointLocation.class).getContextId());
        Assert.assertEquals(CONNECTOR, iid.firstKeyOf(ExternalLocation.class).getExternalNodeConnector());
        Assert.assertEquals(nodeIid, iid.firstKeyOf(ExternalLocation.class).getExternalNodeMountPoint());
    }

    @Test
    public void absoluteLocationIid() {
        ContextId l3Context = new ContextId(L3_CONTEXT_ID);
        AddressEndpointLocationKey addrEndpointLocationKey =
                new AddressEndpointLocationKey(IP_ADDRESS, IpPrefixType.class, l3Context,
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class);
        InstanceIdentifier<AbsoluteLocation> iid = IidFactory.absoluteLocationIid(addrEndpointLocationKey);
        Assert.assertEquals(IpPrefixType.class, iid.firstKeyOf(AddressEndpointLocation.class).getAddressType());
        Assert.assertEquals(IP_ADDRESS, iid.firstKeyOf(AddressEndpointLocation.class).getAddress());
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context.class,
                iid.firstKeyOf(AddressEndpointLocation.class).getContextType());
        Assert.assertEquals(l3Context, iid.firstKeyOf(AddressEndpointLocation.class).getContextId());
    }
}
