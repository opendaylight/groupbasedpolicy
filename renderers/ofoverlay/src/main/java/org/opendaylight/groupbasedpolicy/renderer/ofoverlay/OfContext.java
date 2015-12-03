/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.InheritanceUtils;
import org.opendaylight.groupbasedpolicy.util.PolicyResolverUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class OfContext {

    private static final Logger LOG = LoggerFactory.getLogger(OfContext.class);
    private static final InstanceIdentifier<Tenants> TENANTS_IID = InstanceIdentifier.builder(Tenants.class).build();
    private final DataBroker dataBroker;
    private final PolicyManager policyManager;
    private final SwitchManager switchManager;
    private final EndpointManager epManager;
    private final Map<TenantId, IndexedTenant> resolvedTenants = new HashMap<>();
    private PolicyInfo policyInfo;

    private final ScheduledExecutorService executor;

    public OfContext(DataBroker dataBroker, PolicyManager policyManager, SwitchManager switchManager,
            EndpointManager endpointManager, ScheduledExecutorService executor) {
        this.dataBroker = dataBroker;
        this.policyManager = policyManager;
        this.switchManager = switchManager;
        this.epManager = endpointManager;
        this.executor = executor;
        if (dataBroker == null) {
            LOG.error("DataBroker is null. Cannot read resolved tenants and resolved policy from DS.");
        } else {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<Tenants> potentialTenants =
                    DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, TENANTS_IID, rTx);
            if (potentialTenants.isPresent() && potentialTenants.get().getTenant() != null) {
                for (Tenant tenant : potentialTenants.get().getTenant()) {
                    resolvedTenants.put(tenant.getId(), new IndexedTenant(tenant));
                }
                // TODO we should read resolved-policy from DS instead of this
                policyInfo = resolvePolicy(resolvedTenants);
            }
            rTx.close();
        }
    }

    @VisibleForTesting
    void addTenantAndResolvePolicy(Tenant unresolvedTenant) {
        Tenant t = InheritanceUtils.resolveTenant(unresolvedTenant);
        IndexedTenant it = new IndexedTenant(t);
        resolvedTenants.put(unresolvedTenant.getId(), it);
        policyInfo = resolvePolicy(resolvedTenants);
    }

    private static PolicyInfo resolvePolicy(Map<TenantId, IndexedTenant> resolvedTenants) {
        if (resolvedTenants.isEmpty()) {
            return null;
        }
        return PolicyResolverUtils.resolvePolicyInfo(getIndexedTenants(resolvedTenants.values()));
    }

    private static Set<IndexedTenant> getIndexedTenants(Collection<IndexedTenant> tenants) {
        Set<IndexedTenant> result = new HashSet<>();
        for (IndexedTenant t : tenants) {
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    public PolicyManager getPolicyManager() {
        return this.policyManager;
    }

    public SwitchManager getSwitchManager() {
        return this.switchManager;
    }

    public EndpointManager getEndpointManager() {
        return this.epManager;
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public IndexedTenant getTenant(TenantId tenant) {
        return resolvedTenants.get(tenant);
    }

    /**
     * Get a snapshot of the current policy
     *
     * @return the {@link PolicyInfo} object representing an immutable snapshot
     *         of the policy state
     */
    public PolicyInfo getCurrentPolicy() {
        return policyInfo;
    }

    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }

}
