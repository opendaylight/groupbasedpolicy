/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.InheritanceUtils;
import org.opendaylight.groupbasedpolicy.util.PolicyInfoUtils;
import org.opendaylight.groupbasedpolicy.util.PolicyResolverUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPoliciesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;

/**
 * The policy resolver is a utility for renderers to help in resolving
 * group-based policy into a form that is easier to apply to the actual network.
 * For any pair of endpoint groups, there is a set of rules that could apply to
 * the endpoints on that group based on the policy configuration. The exact list
 * of rules that apply to a given pair of endpoints depends on the conditions
 * that are active on the endpoints.
 * We need to be able to query against this policy model, enumerate the relevant
 * classes of traffic and endpoints, and notify renderers when there are changes
 * to policy as it applies to active sets of endpoints and endpoint groups.
 * The policy resolver will maintain the necessary state for all tenants in its
 * control domain, which is the set of tenants for which policy listeners have
 * been registered.
 */
public class PolicyResolver implements PolicyValidatorRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyResolver.class);

    private final DataBroker dataProvider;

    protected final ConcurrentMap<TenantId, IndexedTenant> resolvedTenants;

    private PolicyChangeListener tenantChangeListener;

    /*
     * Store validators for ActionDefinitions from Renderers
     *
     */
    private SetMultimap<ActionDefinitionId, Validator<ActionInstance>> actionInstanceValidatorsByDefinition =
            Multimaps.synchronizedSetMultimap(HashMultimap.<ActionDefinitionId, Validator<ActionInstance>>create());
    private SetMultimap<ClassifierDefinitionId, Validator<ClassifierInstance>> classifierInstanceValidatorsByDefinition =
            Multimaps
                .synchronizedSetMultimap(HashMultimap.<ClassifierDefinitionId, Validator<ClassifierInstance>>create());

    public PolicyResolver(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
        resolvedTenants = new ConcurrentHashMap<>();
        tenantChangeListener =
                new PolicyChangeListener(dataProvider, new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Tenants.class).child(Tenant.class).build()));
        LOG.debug("Initialized renderer common policy resolver");
    }

    // *************
    // AutoCloseable
    // *************
    @Override
    public void close() throws Exception {
        if (tenantChangeListener != null) {
            tenantChangeListener.close();
        }
    }

    // *************************
    // PolicyResolutionValidatorRegistrar
    // *************************

    @Override
    public void register(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        actionInstanceValidatorsByDefinition.put(actionDefinitionId, validator);
    }

    @Override
    public void unregister(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        actionInstanceValidatorsByDefinition.remove(actionDefinitionId, validator);
    }

    @Override
    public void register(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        classifierInstanceValidatorsByDefinition.put(classifierDefinitionId, validator);
    }

    @Override
    public void unregister(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        classifierInstanceValidatorsByDefinition.remove(classifierDefinitionId, validator);
    }

    @VisibleForTesting
    void updateTenant(final TenantId tenantId, final Tenant unresolvedTenant) {
        if (dataProvider == null) {
            LOG.error("Tenant {} will not be resolved because because dataProvider is NULL", tenantId.getValue());
            return;
        }

        if (unresolvedTenant == null) {
            LOG.info("Tenant {} not found in CONF; check&delete from OPER", tenantId);
            resolvedTenants.remove(tenantId);
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, IidFactory.tenantIid(tenantId), rwTx);
            updateResolvedPolicy(rwTx);
            if (DataStoreHelper.submitToDs(rwTx)) {
                LOG.debug("Removed resolved tenant {} and wrote resolved policies to Datastore.", tenantId.getValue());
            } else {
                LOG.error("Failed to remove resolved tenant {} and to write resolved policies to Datastore.",
                        tenantId.getValue());
            }
        } else {
            LOG.debug("Resolving of tenant inheritance and policy triggered by a change in tenant {}", tenantId);
            Tenant resolvedTenant = InheritanceUtils.resolveTenant(unresolvedTenant);
            if (isPolicyValid(resolvedTenant.getPolicy())) {
                // Update the policy cache and notify listeners
                resolvedTenants.put(tenantId, new IndexedTenant(resolvedTenant));
                WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.tenantIid(tenantId), resolvedTenant, true);
                updateResolvedPolicy(wTx);
                if (DataStoreHelper.submitToDs(wTx)) {
                    LOG.debug("Wrote resolved tenant {} and resolved policies to Datastore.", tenantId.getValue());
                } else {
                    LOG.error("Failed to write resolved tenant {} and resolved policies to Datastore.",
                            tenantId.getValue());
                }
            }
        }
    }

    private void updateResolvedPolicy(WriteTransaction wTx) {
        Set<IndexedTenant> indexedTenants = getIndexedTenants(resolvedTenants.values());
        Table<EgKey, EgKey, org.opendaylight.groupbasedpolicy.dto.Policy> policyMap =
                PolicyResolverUtils.resolvePolicy(indexedTenants);
        ResolvedPolicies resolvedPolicies =
                new ResolvedPoliciesBuilder().setResolvedPolicy(PolicyInfoUtils.buildResolvedPolicy(policyMap, resolvedTenants)).build();

        wTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(ResolvedPolicies.class).build(),
                resolvedPolicies, true);
    }

    private Set<IndexedTenant> getIndexedTenants(Collection<IndexedTenant> tenantCtxs) {
        Set<IndexedTenant> result = new HashSet<>();
        for (IndexedTenant tenant : tenantCtxs) {
            if (tenant != null) {
                result.add(tenant);
            }
        }
        return result;
    }

    @VisibleForTesting
    boolean isPolicyValid(Policy policy) {
        if (policy != null && policy.getSubjectFeatureInstances() != null) {
            SubjectFeatureInstances subjectFeatureInstances = policy.getSubjectFeatureInstances();
            if (actionInstancesAreValid(subjectFeatureInstances.getActionInstance())
                    && classifierInstancesAreValid(subjectFeatureInstances.getClassifierInstance())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validation of action instances.
     *
     * @param actionInstances list of instances to validate
     * @return true if instances are valid or if <code>actionInstances</code>
     *         is <code>null</code>, Otherwise returns false.
     */
    private boolean actionInstancesAreValid(List<ActionInstance> actionInstances) {
        if (actionInstances == null) {
            return true;
        }
        for (ActionInstance actionInstance : actionInstances) {
            Set<Validator<ActionInstance>> actionInstanceValidators =
                    actionInstanceValidatorsByDefinition.get(actionInstance.getActionDefinitionId());
            for (Validator<ActionInstance> actionInstanceValidator : actionInstanceValidators) {
                ValidationResult validationResult = actionInstanceValidator.validate(actionInstance);
                if (!validationResult.isValid()) {
                    LOG.error("ActionInstance {} is not valid! {}", actionInstance.getName().getValue(),
                            validationResult.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validation of classifier instances.
     *
     * @param classifierInstances list of instances to validate
     * @return true if instances are valid or if <code>classifierInstances</code>
     *         is <code>null</code>, Otherwise returns false.
     */
    private boolean classifierInstancesAreValid(List<ClassifierInstance> classifierInstances) {
        if (classifierInstances == null) {
            return true;
        }
        for (ClassifierInstance classifierInstance : classifierInstances) {
            Set<Validator<ClassifierInstance>> classifierInstanceValidators =
                    classifierInstanceValidatorsByDefinition.get(classifierInstance.getClassifierDefinitionId());
            for (Validator<ClassifierInstance> classifierInstanceValidator : classifierInstanceValidators) {
                ValidationResult validationResult = classifierInstanceValidator.validate(classifierInstance);
                if (!validationResult.isValid()) {
                    LOG.error("ClassifierInstance {} is not valid! {}", classifierInstance.getName().getValue(),
                            validationResult.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    @Immutable
    private class PolicyChangeListener extends DataTreeChangeHandler<Tenant> {

        protected PolicyChangeListener(DataBroker dataProvider, DataTreeIdentifier<Tenant> pointOfInterest) {
            super(dataProvider);
            registerDataTreeChangeListener(pointOfInterest);
        }

        @Override
        protected void onWrite(DataObjectModification<Tenant> rootNode, InstanceIdentifier<Tenant> rootIdentifier) {
            Tenant tenantAfter = rootNode.getDataAfter();
            updateTenant(tenantAfter.getId(), tenantAfter);
        }

        @Override
        protected void onDelete(DataObjectModification<Tenant> rootNode, InstanceIdentifier<Tenant> rootIdentifier) {
            TenantId tenantId = rootIdentifier.firstKeyOf(Tenant.class).getId();
            updateTenant(tenantId, null);
        }

        @Override
        protected void onSubtreeModified(DataObjectModification<Tenant> rootNode,
                InstanceIdentifier<Tenant> rootIdentifier) {
            Tenant tenantAfter = rootNode.getDataAfter();
            updateTenant(tenantAfter.getId(), tenantAfter);
        }

    }

}
