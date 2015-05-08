/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ui.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolverUtils;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetEndpointsFromEndpointGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetEndpointsFromEndpointGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetEndpointsFromEndpointGroupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetSubjectsBetweenEndpointGroupsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetSubjectsBetweenEndpointGroupsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.GetSubjectsBetweenEndpointGroupsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.UiBackendService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.endpoints.from.endpoint.group.output.UiEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.endpoints.from.endpoint.group.output.UiEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.input.FromOperData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.EndpointGroupPairWithSubject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.EndpointGroupPairWithSubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.endpoint.group.pair.with.subject.UiSubject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.endpoint.group.pair.with.subject.UiSubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.endpoint.group.pair.with.subject.ui.subject.UiRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ui.backend.rev150511.get.subjects.between.endpoint.groups.output.endpoint.group.pair.with.subject.ui.subject.UiRuleBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class UiBackendServiceImpl implements UiBackendService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(UiBackendServiceImpl.class);

    private final DataBroker dataProvider;
    private final BindingAwareBroker.RpcRegistration<UiBackendService> rpcRegistration;
    private final InstanceIdentifier<Endpoints> ENDPOINTS_IID = InstanceIdentifier.builder(Endpoints.class).build();

    public UiBackendServiceImpl(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
        Preconditions.checkNotNull(dataBroker);
        Preconditions.checkNotNull(rpcRegistry);
        this.dataProvider = dataBroker;
        rpcRegistration = rpcRegistry.addRpcImplementation(UiBackendService.class, this);
    }

    @Override
    public Future<RpcResult<GetEndpointsFromEndpointGroupOutput>> getEndpointsFromEndpointGroup(
            GetEndpointsFromEndpointGroupInput input) {
        LOG.trace("getEndpointsFromEndpointGroup: {}", input);
        final TenantId tenantId = input.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant-Id in RPC input.");
        }
        final EndpointGroupId epgId = input.getEndpointGroupId();
        if (epgId == null) {
            throw new IllegalArgumentException("Missing endpoint-group-id in RPC input.");
        }
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        CheckedFuture<Optional<Endpoints>, ReadFailedException> futureEndpoints = rTx.read(
                LogicalDatastoreType.OPERATIONAL, ENDPOINTS_IID);
        return Futures.transform(futureEndpoints,
                new Function<Optional<Endpoints>, RpcResult<GetEndpointsFromEndpointGroupOutput>>() {

                    @Override
                    public RpcResult<GetEndpointsFromEndpointGroupOutput> apply(Optional<Endpoints> potentialEndpoints) {
                        GetEndpointsFromEndpointGroupOutputBuilder outputBuilder = new GetEndpointsFromEndpointGroupOutputBuilder();
                        if (!potentialEndpoints.isPresent() || potentialEndpoints.get().getEndpoint() == null) {
                            LOG.trace("No endpoint in datastore.");
                            return RpcResultBuilder.success(outputBuilder.build()).build();
                        }

                        List<Endpoint> endpoints = potentialEndpoints.get().getEndpoint();
                        List<UiEndpoint> uiEndpoints = new ArrayList<>();
                        for (Endpoint ep : endpoints) {
                            if (tenantId.equals(ep.getTenant()) && isEpInEpg(ep, epgId)) {
                                uiEndpoints.add(new UiEndpointBuilder(ep).build());
                            }
                        }
                        outputBuilder.setUiEndpoint(uiEndpoints);
                        return RpcResultBuilder.success(outputBuilder.build()).build();
                    }
                });
    }

    private boolean isEpInEpg(Endpoint ep, EndpointGroupId epgId) {
        if (epgId.equals(ep.getEndpointGroup())) {
            return true;
        }
        if (ep.getEndpointGroups() != null) {
            for (EndpointGroupId epgFromEp : ep.getEndpointGroups()) {
                if (epgId.equals(epgFromEp)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Future<RpcResult<GetSubjectsBetweenEndpointGroupsOutput>> getSubjectsBetweenEndpointGroups(
            GetSubjectsBetweenEndpointGroupsInput input) {
        LOG.trace("getSubjectsBetweenEndpointGroups: {}", input);
        final TenantId tenantId = input.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant-Id in RPC input.");
        }
        final FromOperData fromOperData = input.getFromOperData();
        InstanceIdentifier<Tenant> tenantIid = InstanceIdentifier.builder(Tenants.class)
            .child(Tenant.class, new TenantKey(tenantId))
            .build();
        CheckedFuture<Optional<Tenant>, ReadFailedException> futureTenant;
        try (ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction()) {
            if (fromOperData == null) {
                futureTenant = rTx.read(LogicalDatastoreType.CONFIGURATION, tenantIid);
            } else {
                futureTenant = rTx.read(LogicalDatastoreType.OPERATIONAL, tenantIid);
            }
        }
        return Futures.transform(futureTenant,
                new Function<Optional<Tenant>, RpcResult<GetSubjectsBetweenEndpointGroupsOutput>>() {

                    @Override
                    public RpcResult<GetSubjectsBetweenEndpointGroupsOutput> apply(Optional<Tenant> potentialTenant) {
                        GetSubjectsBetweenEndpointGroupsOutputBuilder outputBuilder = new GetSubjectsBetweenEndpointGroupsOutputBuilder();
                        if (!potentialTenant.isPresent()) {
                            LOG.trace(
                                    "No tenant with id {} in {} datastore",
                                    tenantId.getValue(),
                                    fromOperData == null ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL);
                            return RpcResultBuilder.success(outputBuilder.build()).build();
                        }

                        Tenant tenant = potentialTenant.get();
                        Table<EgKey, EgKey, Policy> resolvedPolicy = PolicyResolverUtils.resolvePolicy(ImmutableSet.of(new IndexedTenant(
                                tenant)));
                        List<EndpointGroupPairWithSubject> epgPairsWithSubjects = new ArrayList<>();
                        for (Cell<EgKey, EgKey, Policy> policyByConsProvEpg : resolvedPolicy.cellSet()) {
                            Policy policy = policyByConsProvEpg.getValue();
                            List<RuleGroup> subjects = getUniqueSortedSubjects(policy);
                            List<UiSubject> uiSubjects = new ArrayList<>();
                            for (RuleGroup subject : subjects) {
                                UiSubject uiSubject = new UiSubjectBuilder().setName(subject.getRelatedSubject())
                                    .setOrder(subject.getOrder())
                                    .setUiRule(getUiRules(subject.getRules()))
                                    .build();
                                uiSubjects.add(uiSubject);
                            }
                            EgKey consEgKey = policyByConsProvEpg.getRowKey();
                            EgKey provEgKey = policyByConsProvEpg.getColumnKey();
                            LOG.trace(
                                    "Resolved policies from {} datastore: \nConsumer EPG: {}\nProvider EPG: {}\nPolicy: {}",
                                    fromOperData == null ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL,
                                    consEgKey, provEgKey, policy);
                            EndpointGroupPairWithSubject epgPairWithSubject = new EndpointGroupPairWithSubjectBuilder().setConsumerEndpointGroupId(
                                    consEgKey.getEgId())
                                .setConsumerTenantId(consEgKey.getTenantId())
                                .setProviderEndpointGroupId(provEgKey.getEgId())
                                .setProviderTenantId(provEgKey.getTenantId())
                                .setUiSubject(uiSubjects)
                                .build();
                            epgPairsWithSubjects.add(epgPairWithSubject);
                        }
                        GetSubjectsBetweenEndpointGroupsOutput result = outputBuilder.setEndpointGroupPairWithSubject(
                                epgPairsWithSubjects).build();
                        return RpcResultBuilder.success(result).build();
                    }
                });
    }

    private List<RuleGroup> getUniqueSortedSubjects(Policy policy) {
        Set<RuleGroup> uniqueSubjects = new HashSet<>();
        for (List<RuleGroup> subjects : policy.getRuleMap().values()) {
            for (RuleGroup subject : subjects) {
                uniqueSubjects.add(subject);
            }
        }
        ArrayList<RuleGroup> sortedSubjects = new ArrayList<>(uniqueSubjects);
        Collections.sort(sortedSubjects);
        return sortedSubjects;
    }

    private List<UiRule> getUiRules(List<Rule> rules) {
        if (rules == null) {
            return Collections.emptyList();
        }
        List<UiRule> uiRules = new ArrayList<>();
        for (Rule rule : rules) {
            UiRule uiRule = new UiRuleBuilder().setName(rule.getName())
                .setOrder(rule.getOrder())
                .setClassifierRef(rule.getClassifierRef())
                .setActionRef(rule.getActionRef())
                .build();
            uiRules.add(uiRule);
        }
        return uiRules;
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        rpcRegistration.close();
    }

}
