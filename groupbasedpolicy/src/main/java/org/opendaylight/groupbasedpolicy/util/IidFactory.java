/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Interests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.FollowedTenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.FollowedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.FollowedTenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IidFactory {

    private IidFactory() {
        throw new UnsupportedOperationException();
    }

    private static final InstanceIdentifier<Endpoints> ENDPOINTS_IID = InstanceIdentifier.builder(Endpoints.class).build();

    public static InstanceIdentifier<Tenant> tenantIid(TenantId id) {
        return InstanceIdentifier.builder(Tenants.class).child(Tenant.class, new TenantKey(id)).build();
    }

    public static InstanceIdentifier<EndpointGroup> endpointGroupIid(TenantId tenantId, EndpointGroupId epgId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .build();
    }

    public static InstanceIdentifier<Contract> contractIid(TenantId tenantId, ContractId contractId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(Contract.class, new ContractKey(contractId))
            .build();
    }

    public static InstanceIdentifier<Subject> subjectIid(TenantId tenantId, ContractId contractId,
            SubjectName subjectName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .build();
    }

    public static InstanceIdentifier<ProviderNamedSelector> providerNamedSelectorIid(TenantId tenantId,
            EndpointGroupId epgId, SelectorName providerSelectorName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .child(ProviderNamedSelector.class, new ProviderNamedSelectorKey(providerSelectorName))
            .build();
    }

    public static InstanceIdentifier<ConsumerNamedSelector> consumerNamedSelectorIid(TenantId tenantId,
            EndpointGroupId epgId, SelectorName consumerSelectorName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(EndpointGroup.class, new EndpointGroupKey(epgId))
            .child(ConsumerNamedSelector.class, new ConsumerNamedSelectorKey(consumerSelectorName))
            .build();
    }

    public static InstanceIdentifier<Clause> clauseIid(TenantId tenantId, ContractId contractId, ClauseName clauseName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(Contract.class, new ContractKey(contractId))
            .child(Clause.class, new ClauseKey(clauseName))
            .build();
    }

    public static InstanceIdentifier<Rule> ruleIid(TenantId tenantId, ContractId contractId, SubjectName subjectName,
            RuleName ruleName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .child(Rule.class, new RuleKey(ruleName))
            .build();
    }

    public static InstanceIdentifier<ActionInstance> actionInstanceIid(TenantId tenantId, ActionName actionName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(SubjectFeatureInstances.class)
            .child(ActionInstance.class, new ActionInstanceKey(actionName))
            .build();
    }

    public static InstanceIdentifier<ClassifierInstance> classifierInstanceIid(TenantId tenantId,
            ClassifierName classifierName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(SubjectFeatureInstances.class)
            .child(ClassifierInstance.class, new ClassifierInstanceKey(classifierName))
            .build();
    }

    public static InstanceIdentifier<ClassifierDefinition> classifierDefinitionIid(
            ClassifierDefinitionId classifierDefinitionId) {
        return InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
            .child(ClassifierDefinition.class, new ClassifierDefinitionKey(classifierDefinitionId))
            .build();
    }

    public static InstanceIdentifier<ActionDefinition> actionDefinitionIid(ActionDefinitionId actionDefinitionId) {
        return InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
            .child(ActionDefinition.class, new ActionDefinitionKey(actionDefinitionId))
            .build();
    }

    public static InstanceIdentifier<ClassifierRef> classifierRefIid(TenantId tenantId, ContractId contractId,
            SubjectName subjectName, RuleName ruleName, ClassifierName classifierRefName) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(Policy.class)
            .child(Contract.class, new ContractKey(contractId))
            .child(Subject.class, new SubjectKey(subjectName))
            .child(Rule.class, new RuleKey(ruleName))
            .child(ClassifierRef.class, new ClassifierRefKey(classifierRefName))
            .build();
    }

    public static InstanceIdentifier<L2FloodDomain> l2FloodDomainIid(TenantId tenantId, L2FloodDomainId l2FloodDomainId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(ForwardingContext.class)
            .child(L2FloodDomain.class, new L2FloodDomainKey(l2FloodDomainId))
            .build();
    }

    public static InstanceIdentifier<L2BridgeDomain> l2BridgeDomainIid(TenantId tenantId,
            L2BridgeDomainId l2BridgeDomainId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(ForwardingContext.class)
            .child(L2BridgeDomain.class, new L2BridgeDomainKey(l2BridgeDomainId))
            .build();
    }

    public static InstanceIdentifier<L3Context> l3ContextIid(TenantId tenantId, L3ContextId l3ContextId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(ForwardingContext.class)
            .child(L3Context.class, new L3ContextKey(l3ContextId))
            .build();
    }

    /**
     * Get the {@link Endpoint} {@link InstanceIdentifier} based on the {@link EndpointKey}
     * @param endpointKey The {@link EndpointKey} of a particular {@link Endpoint}
     * @return The {@link InstanceIdentifier} of the {@link Endpoint}
     */
    public static InstanceIdentifier<Endpoint> endpointIid(EndpointKey endpointKey) {
        return InstanceIdentifier.builder(Endpoints.class).child(Endpoint.class, endpointKey).build();
    }

    public static InstanceIdentifier<Endpoint> endpointIid(L2BridgeDomainId l2Context, MacAddress macAddress) {
        return IidFactory.endpointIid(new EndpointKey(l2Context, macAddress));
    }

    public static InstanceIdentifier<EndpointL3> l3EndpointIid(L3ContextId l3Context, IpAddress ipAddress) {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3.class, new EndpointL3Key(ipAddress, l3Context))
            .build();
    }

    public static InstanceIdentifier<EndpointL3> l3EndpointsIidWildcard() {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3.class)
            .build();
    }

    public static InstanceIdentifier<EndpointL3Prefix> endpointL3PrefixIid(L3ContextId l3Context, IpPrefix ipPrefix) {
        return InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3Prefix.class, new EndpointL3PrefixKey(ipPrefix, l3Context))
            .build();
    }

    public static InstanceIdentifier<Endpoints> endpointsIidWildcard() {
        return ENDPOINTS_IID;
    }

    public static InstanceIdentifier<Subnet> subnetIid(TenantId tenantId, SubnetId subnetId) {
        return InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .child(ForwardingContext.class)
            .child(Subnet.class, new SubnetKey(subnetId))
            .build();
    }

    public static InstanceIdentifier<FollowedEndpointGroup> followedEndpointgroupIid(RendererName rendererName,
            TenantId tenantId, EndpointGroupId epgId) {
        return InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, new RendererKey(rendererName))
            .child(Interests.class)
            .child(FollowedTenants.class)
            .child(FollowedTenant.class, new FollowedTenantKey(tenantId))
            .child(FollowedEndpointGroup.class, new FollowedEndpointGroupKey(epgId))
            .build();
    }

}
