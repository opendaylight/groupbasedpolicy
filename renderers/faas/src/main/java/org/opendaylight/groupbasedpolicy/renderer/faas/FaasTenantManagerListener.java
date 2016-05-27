/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import java.util.concurrent.ScheduledExecutorService;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaasTenantManagerListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(FaasTenantManagerListener.class);
    private final ScheduledExecutorService executor;
    private final TenantId gbpTenantId;
    private final Uuid faasTenantId;
    private final FaasPolicyManager policyManager;

    public FaasTenantManagerListener(FaasPolicyManager policyManager, TenantId gbpTenantId, Uuid faasTenantId,
            ScheduledExecutorService executor) {
        this.executor = executor;
        this.faasTenantId = faasTenantId;
        this.gbpTenantId = gbpTenantId;
        this.policyManager = policyManager;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        executor.execute(new Runnable() {

            public void run() {
                executeEvent(change);
            }
        });
    }

    @VisibleForTesting
    void executeEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Tenant) {
                Tenant tenant = (Tenant) old;
                if (tenant.getId().equals(gbpTenantId)) {
                    LOG.info("Removed gbp Tenant {}  -- faas Tenant {}", gbpTenantId.getValue(),
                            faasTenantId.getValue());
                    this.policyManager.removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
                }
            }
        }
    }
}
