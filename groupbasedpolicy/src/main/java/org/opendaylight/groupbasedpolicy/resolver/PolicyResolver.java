/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.groupbasedpolicy.resolver.validator.PolicyValidator;
import org.opendaylight.groupbasedpolicy.resolver.validator.ValidationResult;

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
public class PolicyResolver implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyResolver.class);

    private final DataBroker dataProvider;
    private final ScheduledExecutorService executor;

    /**
     * Keep track of the current relevant policy scopes.
     */
    protected CopyOnWriteArrayList<PolicyScope> policyListenerScopes;

    protected ConcurrentMap<TenantId, TenantContext> resolvedTenants;

    /**
     * Store a policy object for each endpoint group pair. The table is stored
     * with the key as (consumer, provider). Two endpoints could appear in both
     * roles at the same time, in which case both policies would apply.
     */
    AtomicReference<PolicyInfo> policy = new AtomicReference<>();

    /*
     * Store validators for ActionDefinitions from Renderers
     *
     */
    protected ConcurrentMap<ActionDefinitionId, ActionInstanceValidator> registeredActions = new ConcurrentHashMap<>();

    public PolicyResolver(DataBroker dataProvider,
            ScheduledExecutorService executor) {
        super();
        this.dataProvider = dataProvider;
        this.executor = executor;
        policyListenerScopes = new CopyOnWriteArrayList<>();
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
    // PolicyResolver public API
    // *************************
    /**
     * Get a snapshot of the current policy
     *
     * @return the {@link PolicyInfo} object representing an immutable snapshot
     * of the policy state
     */
    public PolicyInfo getCurrentPolicy() {
        return policy.get();
    }

    /**
     * Get the normalized tenant for the given ID
     *
     * @param tenant the tenant ID
     * @return the {@link Tenant}
     */
    public IndexedTenant getTenant(TenantId tenant) {
        TenantContext tc = resolvedTenants.get(tenant);
        if (tc == null) {
            return null;
        }
        return tc.tenant.get();
    }

    public void registerActionDefinitions(ActionDefinitionId actionDefinitionId, ActionInstanceValidator validator) {
        registeredActions.putIfAbsent(actionDefinitionId, validator);
    }

    /**
     * Register a listener to receive update events.
     *
     * @param listener the {@link PolicyListener} object to receive the update
     * events
     */
    public PolicyScope registerListener(PolicyListener listener) {
        PolicyScope ps = new PolicyScope(this, listener);
        policyListenerScopes.add(ps);

        return ps;
    }

    /**
     * Remove the listener registered for the given {@link PolicyScope}.
     *
     * @param scope the scope to remove
     * @see PolicyResolver#registerListener(PolicyListener)
     */
    public void removeListener(PolicyScope scope) {
        policyListenerScopes.remove(scope);
    }

    // **************
    // Implementation
    // **************
    /**
     * Atomically update the active policy and notify policy listeners of
     * relevant changes
     *
     * @param policyMap the new policy to set
     * @param egConditions the map of endpoint groups to relevant condition sets
     * @return the set of groups with updated policy
     */
    protected Set<EgKey> updatePolicy(Table<EgKey, EgKey, Policy> policyMap,
            Map<EgKey, Set<ConditionSet>> egConditions,
            List<PolicyScope> policyListenerScopes) {
        PolicyInfo newPolicy = new PolicyInfo(policyMap, egConditions);
        PolicyInfo oldPolicy = policy.getAndSet(newPolicy);

        HashSet<EgKey> notifySet = new HashSet<>();

        for (Cell<EgKey, EgKey, Policy> cell : newPolicy.getPolicyMap().cellSet()) {
            Policy newp = cell.getValue();
            Policy oldp = null;
            if (oldPolicy != null) {
                oldp = oldPolicy.getPolicyMap().get(cell.getRowKey(),
                        cell.getColumnKey());
            }
            if (oldp == null || !newp.equals(oldp)) {
                notifySet.add(cell.getRowKey());
                notifySet.add(cell.getColumnKey());
            }
        }
        if (oldPolicy != null) {
            for (Cell<EgKey, EgKey, Policy> cell : oldPolicy.getPolicyMap().cellSet()) {
                if (!newPolicy.getPolicyMap().contains(cell.getRowKey(),
                        cell.getColumnKey())) {
                    notifySet.add(cell.getRowKey());
                    notifySet.add(cell.getColumnKey());
                }
            }
        }
        return notifySet;
    }

    /**
     * Notify the policy listeners about a set of updated groups
     */
    private void notifyListeners(Set<EgKey> updatedGroups) {
        for (final PolicyScope scope : policyListenerScopes) {
            Set<EgKey> filtered
                    = Sets.filter(updatedGroups, new Predicate<EgKey>() {
                        @Override
                        public boolean apply(EgKey input) {
                            return scope.contains(input.getTenantId(),
                                    input.getEgId());
                        }
                    });
            if (!filtered.isEmpty()) {
                scope.getListener().policyUpdated(filtered);
            }
        }
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

                Tenant t = InheritanceUtils.resolveTenant(result.get());
                if (isValidTenant(t)) {
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

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Count not get tenant {}", tenantId, t);
            }
        }, executor);
    }

    private void deleteOperTenantIfExists(final InstanceIdentifier<Tenant> tiid, final TenantId tenantId) {
        final ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();

        ListenableFuture<Optional<Tenant>> readFuture = rwTx.read(LogicalDatastoreType.OPERATIONAL, tiid);
        Futures.addCallback(readFuture, new FutureCallback<Optional<Tenant>>() {
            @Override
            public void onSuccess(Optional<Tenant> result) {
                if(result.isPresent()){
                    unsubscribeTenant(tenantId);
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
        }, executor);

    }

    protected void updatePolicy() {
        try {
            Map<EgKey, Set<ConditionSet>> egConditions = new HashMap<>();
            Set<IndexedTenant> indexedTenants = getIndexedTenants(resolvedTenants.values());
            Table<EgKey, EgKey, Policy> policyMap = PolicyResolverUtils.resolvePolicy(indexedTenants, egConditions);
            Set<EgKey> updatedGroups = updatePolicy(policyMap, egConditions, policyListenerScopes);

            notifyListeners(updatedGroups);
        } catch (Exception e) {
            LOG.error("Failed to update policy", e);
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

    private boolean isValidTenant(Tenant t) {
        ValidationResult validationResult = PolicyValidator.validate(t, this);
        if (validationResult == null) {
            return true;
        }
        return validationResult.getResult().getValue();
    }

    public ActionInstanceValidator getActionInstanceValidator(ActionDefinitionId actionDefinitionId) {
        if (registeredActions == null) {
            return null;
        }
        return registeredActions.get(actionDefinitionId);

    }

}
