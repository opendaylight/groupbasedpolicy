/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistrar;
import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.InheritanceUtils;
import org.opendaylight.groupbasedpolicy.util.PolicyInfoUtils;
import org.opendaylight.groupbasedpolicy.util.PolicyResolverUtils;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPoliciesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * The policy resolver is a utility for renderers to help in resolving
 * group-based policy into a form that is easier to apply to the actual network.
 *
 * For any pair of endpoint groups, there is a set of rules that could apply to
 * the endpoints on that group based on the policy configuration. The exact list
 * of rules that apply to a given pair of endpoints depends on the conditions
 * that are active on the endpoints.
 *
 * We need to be able to query against this policy model, enumerate the relevant
 * classes of traffic and endpoints, and notify renderers when there are changes
 * to policy as it applies to active sets of endpoints and endpoint groups.
 *
 * The policy resolver will maintain the necessary state for all tenants in its
 * control domain, which is the set of tenants for which policy listeners have
 * been registered.
 *
 */
public class PolicyResolver implements PolicyValidatorRegistrar, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyResolver.class);

    private final DataBroker dataProvider;

    protected ConcurrentMap<TenantId, TenantContext> resolvedTenants;

    /*
     * Store validators for ActionDefinitions from Renderers
     *
     */
    protected SetMultimap<ActionDefinitionId, Validator<ActionInstance>> actionInstanceValidatorsByDefinition = Multimaps.synchronizedSetMultimap(HashMultimap.<ActionDefinitionId, Validator<ActionInstance>>create());
    protected SetMultimap<ClassifierDefinitionId, Validator<ClassifierInstance>> classifierInstanceValidatorsByDefinition = Multimaps.synchronizedSetMultimap(HashMultimap.<ClassifierDefinitionId, Validator<ClassifierInstance>>create());

    public PolicyResolver(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
        resolvedTenants = new ConcurrentHashMap<>();
        LOG.debug("Initialized renderer common policy resolver");
    }

    // *************
    // AutoCloseable
    // *************
    @Override
    public void close() throws Exception {
        for (TenantContext ctx : resolvedTenants.values()) {
            if (ctx.registration != null) {
                ctx.registration.close();
            }
        }
    }

    // *************************
    // PolicyResolutionValidatorRegistrar
    // *************************

    @Override
    public void register(ActionDefinitionId actionDefinitionId,
            Validator<ActionInstance> validator) {
        actionInstanceValidatorsByDefinition.put(actionDefinitionId, validator);
    }

    @Override
    public void unregister(ActionDefinitionId actionDefinitionId,
            Validator<ActionInstance> validator) {
        actionInstanceValidatorsByDefinition.remove(actionDefinitionId, validator);
    }

    @Override
    public void register(ClassifierDefinitionId classifierDefinitionId,
            Validator<ClassifierInstance> validator) {
        classifierInstanceValidatorsByDefinition.put(classifierDefinitionId, validator);
    }

    @Override
    public void unregister(ClassifierDefinitionId classifierDefinitionId,
            Validator<ClassifierInstance> validator) {
        classifierInstanceValidatorsByDefinition.remove(classifierDefinitionId, validator);
    }

    /**
     * Subscribe the resolver to updates related to a particular tenant Make
     * sure that this can't be called concurrently with subscribe
     *
     * @param tenantId the tenant ID to subscribe to
     */
    protected void subscribeTenant(TenantId tenantId) {
        if (!resolvedTenants.containsKey(tenantId)) {
            updateTenant(tenantId);
        }
    }

    /**
     * Unsubscribe the resolver from updates related to a particular tenant Make
     * sure that this can't be called concurrently with subscribe
     *
     * @param tenantId the tenant ID to subscribe to
     */
    protected void unsubscribeTenant(TenantId tenantId) {
        TenantContext context = resolvedTenants.get(tenantId);
        if (context != null) {
            resolvedTenants.remove(tenantId);
            context.registration.close();
        }
    }

    private void updateTenant(final TenantId tenantId) {
        if (dataProvider == null) {
            return;
        }

        TenantContext context = resolvedTenants.get(tenantId);
        if (context == null) {
            ListenerRegistration<DataChangeListener> registration = null;
            registration = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                            TenantUtils.tenantIid(tenantId),
                            new PolicyChangeListener(tenantId),
                            DataChangeScope.SUBTREE);
            LOG.debug("Data change listener for tenant {} in CONF DS is registered.", tenantId.getValue());

            context = new TenantContext(registration);
            TenantContext oldContext
                    = resolvedTenants.putIfAbsent(tenantId, context);
            if (oldContext != null) {
                // already registered in a different thread; just use the other
                // context
                registration.close();
                context = oldContext;
            } else {
                LOG.info("Added tenant {} to policy scope", tenantId);
            }
        }

        // Resolve the new tenant and update atomically
        final AtomicReference<IndexedTenant> tenantRef = context.tenant;
        final IndexedTenant ot = tenantRef.get();
        ReadOnlyTransaction transaction
                = dataProvider.newReadOnlyTransaction();
        final InstanceIdentifier<Tenant> tiid = TenantUtils.tenantIid(tenantId);
        ListenableFuture<Optional<Tenant>> unresolved;

        unresolved = transaction.read(LogicalDatastoreType.CONFIGURATION, tiid);

        Futures.addCallback(unresolved, new FutureCallback<Optional<Tenant>>() {
            @Override
            public void onSuccess(Optional<Tenant> result) {
                if (!result.isPresent()) {
                    LOG.info("Tenant {} not found in CONF; check&delete from OPER", tenantId);
                    deleteOperTenantIfExists(tiid, tenantId);
                    return;
                }
                LOG.debug("Resolving of tenant inheritance and policy triggered by a change in tenant {}", tenantId);
                Tenant t = InheritanceUtils.resolveTenant(result.get());
                if (t.getPolicy() != null && t.getPolicy().getSubjectFeatureInstances() != null) {
                    SubjectFeatureInstances subjectFeatureInstances = t.getPolicy().getSubjectFeatureInstances();
                    // TODO log and remove invalid action instances
                    if (actionInstancesAreValid(subjectFeatureInstances.getActionInstance())
                            && classifierInstancesAreValid(subjectFeatureInstances.getClassifierInstance())) {
                    IndexedTenant it = new IndexedTenant(t);
                    if (!tenantRef.compareAndSet(ot, it)) {
                        // concurrent update of tenant policy. Retry
                        updateTenant(tenantId);
                    } else {
                        // Update the policy cache and notify listeners
                        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
                        wt.put(LogicalDatastoreType.OPERATIONAL, tiid, t, true);
                        wt.submit();
                        updatePolicy();
                    }
                }
            }
        }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Count not get tenant {}", tenantId, t);
            }
        });
    }

    private void deleteOperTenantIfExists(final InstanceIdentifier<Tenant> tiid, final TenantId tenantId) {
        final ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();

        ListenableFuture<Optional<Tenant>> readFuture = rwTx.read(LogicalDatastoreType.OPERATIONAL, tiid);
        Futures.addCallback(readFuture, new FutureCallback<Optional<Tenant>>() {
            @Override
            public void onSuccess(Optional<Tenant> result) {
                if(result.isPresent()){
                    TenantContext tenantContext = resolvedTenants.get(tenantId);
                    tenantContext.tenant.set(null);
                    rwTx.delete(LogicalDatastoreType.OPERATIONAL, tiid);
                    rwTx.submit();
                    updatePolicy();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to read operational datastore: {}", t);
                rwTx.cancel();
            }
        });
    }

    protected void updatePolicy() {
        try {
            Set<IndexedTenant> indexedTenants = getIndexedTenants(resolvedTenants.values());
            Table<EgKey, EgKey, Policy> policyMap = PolicyResolverUtils.resolvePolicy(indexedTenants);
            updatePolicyInDataStore(policyMap);
        } catch (Exception e) {
            LOG.error("Failed to update policy", e);
        }
    }

    private void updatePolicyInDataStore(Table<EgKey, EgKey, Policy> policyMap) {
        if (dataProvider == null) {
            LOG.error("Couldn't Write Resolved Tenants Policy Info to Datastore because dataProvider is NULL");
            return;
        }
        ResolvedPolicies resolvedPolicies = new ResolvedPoliciesBuilder().setResolvedPolicy(
                PolicyInfoUtils.buildResolvedPolicy(policyMap)).build();

        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        t.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(ResolvedPolicies.class).build(),
                resolvedPolicies, true);
        if (DataStoreHelper.submitToDs(t)) {
            LOG.debug("Wrote resolved policies to Datastore");
        } else {
            LOG.error("Failed to write resolved policies to Datastore.");
        }
    }

    private Set<IndexedTenant> getIndexedTenants(Collection<TenantContext> tenantCtxs) {
        Set<IndexedTenant> result = new HashSet<>();
        for (TenantContext tenant : tenantCtxs) {
            IndexedTenant t = tenant.tenant.get();
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Validation of action instances.
     *
     * @param actionInstances list of instances to validate
     * @return true if instances are valid or if <code>actionInstances</code>
     * is <code>null</code>, Otherwise returns false.
     *
     */
    private boolean actionInstancesAreValid(List<ActionInstance> actionInstances) {
        if (actionInstances == null) {
            return true;
        }
        for (ActionInstance actionInstance : actionInstances) {
            Set<Validator<ActionInstance>> actionInstanceValidators = actionInstanceValidatorsByDefinition.get(actionInstance.getActionDefinitionId());
            for (Validator<ActionInstance> actionInstanceValidator : actionInstanceValidators) {
                ValidationResult validationResult = actionInstanceValidator.validate(actionInstance);
                if (!validationResult.isValid()) {
                    LOG.error("ActionInstance {} is not valid!", actionInstance.getName());
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
     * is <code>null</code>, Otherwise returns false.
     *
     */
    private boolean classifierInstancesAreValid(List<ClassifierInstance> classifierInstances) {
        if (classifierInstances == null) {
            return true;
        }
        for (ClassifierInstance classifierInstance : classifierInstances) {
            Set<Validator<ClassifierInstance>> classifierInstanceValidators = classifierInstanceValidatorsByDefinition.get(classifierInstance.getClassifierDefinitionId());
            for (Validator<ClassifierInstance> classifierInstanceValidator : classifierInstanceValidators) {
                ValidationResult validationResult = classifierInstanceValidator.validate(classifierInstance);
                if (!validationResult.isValid()) {
                    LOG.error("ClassifierInstance {} is not valid!", classifierInstance.getName());
                    return false;
                }
            }
        }
        return true;
    }

    protected static class TenantContext {

        ListenerRegistration<DataChangeListener> registration;

        AtomicReference<IndexedTenant> tenant = new AtomicReference<>();

        public TenantContext(ListenerRegistration<DataChangeListener> registration) {
            super();
            this.registration = registration;
        }
    }

    @Immutable
    private class PolicyChangeListener implements DataChangeListener {

        final TenantId tenantId;

        public PolicyChangeListener(TenantId tenantId) {
            super();
            this.tenantId = tenantId;
        }

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
            updateTenant(tenantId);
        }

    }

}
