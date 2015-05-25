/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.FloatingIpAssociationMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.FloatingIpPortsByInternalPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.InternalPortsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.floating.ip.ports.by.internal.ports.FloatingIpPortByInternalPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.floating.ip.ports.by.internal.ports.FloatingIpPortByInternalPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByRouterGatewayPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByRouterInterfacePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.floating.ip.ports.EndpointByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.floating.ip.ports.EndpointByFloatingIpPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router._interface.ports.EndpointByRouterInterfacePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router._interface.ports.EndpointByRouterInterfacePortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router.gateway.ports.EndpointByRouterGatewayPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router.gateway.ports.EndpointByRouterGatewayPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.FloatingIpPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterGatewayPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterInterfacePortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.EndpointGroupPairToContractMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.NetworkMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3ContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubnetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IidFactory {

    private IidFactory() {
        throw new UnsupportedOperationException();
    }

    public static InstanceIdentifier<Tenant> tenantIid(TenantId id) {
        return InstanceIdentifier.builder(Tenants.class).child(Tenant.class, new TenantKey(id)).build();
    }

    public static InstanceIdentifier<EndpointGroup> endpointGroupIid(TenantId tenantId, EndpointGroupId epgId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .build();
    }

    public static InstanceIdentifier<Contract> contractIid(TenantId tenantId, ContractId contractId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Contract.class, new ContractKey(contractId))
            .build();
    }

    public static InstanceIdentifier<Subject> subjectIid(TenantId tenantId, ContractId contractId,
            SubjectName subjectName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .build();
    }

    public static InstanceIdentifier<ProviderNamedSelector> providerNamedSelectorIid(TenantId tenantId,
            EndpointGroupId epgId, SelectorName providerSelectorName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .child(ProviderNamedSelector.class, new ProviderNamedSelectorKey(providerSelectorName))
            .build();
    }

    public static InstanceIdentifier<ConsumerNamedSelector> consumerNamedSelectorIid(TenantId tenantId,
            EndpointGroupId epgId, SelectorName consumerSelectorName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .child(ConsumerNamedSelector.class, new ConsumerNamedSelectorKey(consumerSelectorName))
            .build();
    }

    public static InstanceIdentifier<Clause> clauseIid(TenantId tenantId, ContractId contractId, ClauseName clauseName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Contract.class, new ContractKey(contractId))
            .child(Clause.class, new ClauseKey(clauseName))
            .build();
    }

    public static InstanceIdentifier<Rule> ruleIid(TenantId tenantId, ContractId contractId, SubjectName subjectName,
            RuleName ruleName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .child(Rule.class, new RuleKey(ruleName))
            .build();
    }

    public static InstanceIdentifier<ActionInstance> actionInstanceIid(TenantId tenantId, ActionName actionName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(SubjectFeatureInstances.class)
            .child(ActionInstance.class, new ActionInstanceKey(actionName))
            .build();
    }

    public static InstanceIdentifier<ClassifierInstance> classifierInstanceIid(TenantId tenantId,
            ClassifierName classifierName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(SubjectFeatureInstances.class)
            .child(ClassifierInstance.class, new ClassifierInstanceKey(classifierName))
            .build();
    }

    public static InstanceIdentifier<ClassifierRef> classifierRefIid(TenantId tenantId, ContractId contractId,
            SubjectName subjectName, RuleName ruleName, ClassifierName classifierRefName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .child(Rule.class, new RuleKey(ruleName))
            .child(ClassifierRef.class, new ClassifierRefKey(classifierRefName))
            .build();
    }

    public static InstanceIdentifier<NetworkMapping> networkMappingIid(UniqueId networkId) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(NetworkMappings.class)
            .child(NetworkMapping.class, new NetworkMappingKey(networkId))
            .build();
    }

    public static InstanceIdentifier<EndpointGroupPairToContractMapping> endpointGroupPairToContractMappingIid(
            EndpointGroupId providerEpg, EndpointGroupId consumerEpg) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(EndpointGroupPairToContractMappings.class)
            .child(EndpointGroupPairToContractMapping.class,
                    new EndpointGroupPairToContractMappingKey(consumerEpg, providerEpg))
            .build();
    }

    public static InstanceIdentifier<L2FloodDomain> l2FloodDomainIid(TenantId tenantId, L2FloodDomainId l2FloodDomainId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(L2FloodDomain.class, new L2FloodDomainKey(l2FloodDomainId))
            .build();
    }

    public static InstanceIdentifier<L2BridgeDomain> l2BridgeDomainIid(TenantId tenantId,
            L2BridgeDomainId l2BridgeDomainId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(L2BridgeDomain.class, new L2BridgeDomainKey(l2BridgeDomainId))
            .build();
    }

    public static InstanceIdentifier<L3Context> l3ContextIid(TenantId tenantId, L3ContextId l3ContextId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(L3Context.class, new L3ContextKey(l3ContextId))
            .build();
    }

    public static InstanceIdentifier<Endpoint> endpointIid(L2BridgeDomainId l2Context, MacAddress macAddress) {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(Endpoint.class, new EndpointKey(l2Context, macAddress))
            .build();
    }

    public static InstanceIdentifier<EndpointL3> endpointL3Iid(L3ContextId l3Context, IpAddress ipAddress) {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3.class, new EndpointL3Key(ipAddress, l3Context))
            .build();
    }

    public static InstanceIdentifier<EndpointL3Prefix> endpointL3PrefixIid(L3ContextId l3Context, IpPrefix ipPrefix) {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3Prefix.class, new EndpointL3PrefixKey(ipPrefix, l3Context))
            .build();
    }

    public static InstanceIdentifier<Subnet> subnetIid(TenantId tenantId, SubnetId subnetId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Subnet.class, new SubnetKey(subnetId))
            .build();
    }

    public static InstanceIdentifier<EndpointByPort> endpointByPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByPorts.class)
            .child(EndpointByPort.class, new EndpointByPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<PortByEndpoint> portByEndpointIid(L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(PortsByEndpoints.class)
            .child(PortByEndpoint.class, new PortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByRouterGatewayPort> endpointByRouterGatewayPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByRouterGatewayPorts.class)
            .child(EndpointByRouterGatewayPort.class, new EndpointByRouterGatewayPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<RouterGatewayPortByEndpoint> routerGatewayPortByEndpointIid(
            L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(RouterGatewayPortsByEndpoints.class)
            .child(RouterGatewayPortByEndpoint.class, new RouterGatewayPortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByRouterInterfacePort> endpointByRouterInterfacePortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByRouterInterfacePorts.class)
            .child(EndpointByRouterInterfacePort.class, new EndpointByRouterInterfacePortKey(portId))
            .build();
    }

    public static InstanceIdentifier<RouterInterfacePortByEndpoint> routerInterfacePortByEndpointIid(
            L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(RouterInterfacePortsByEndpoints.class)
            .child(RouterInterfacePortByEndpoint.class, new RouterInterfacePortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByFloatingIpPort> endpointByFloatingIpPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByFloatingIpPorts.class)
            .child(EndpointByFloatingIpPort.class, new EndpointByFloatingIpPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<FloatingIpPortByEndpoint> floatingIpPortByEndpointIid(L2BridgeDomainId l2BdId,
            MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(FloatingIpPortsByEndpoints.class)
            .child(FloatingIpPortByEndpoint.class, new FloatingIpPortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<InternalPortByFloatingIpPort> internalPortByFloatingIpPortIid(
            UniqueId floatingIpPortId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(FloatingIpAssociationMappings.class)
            .child(InternalPortsByFloatingIpPorts.class)
            .child(InternalPortByFloatingIpPort.class, new InternalPortByFloatingIpPortKey(floatingIpPortId))
            .build();
    }

    public static InstanceIdentifier<FloatingIpPortByInternalPort> floatingIpPortByInternalPortIid(
            UniqueId floatingIpPortId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(FloatingIpAssociationMappings.class)
            .child(FloatingIpPortsByInternalPorts.class)
            .child(FloatingIpPortByInternalPort.class, new FloatingIpPortByInternalPortKey(floatingIpPortId))
            .build();
    }
}
