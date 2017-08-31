/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import java.util.Collection;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaasTenantManagerListener implements DataTreeChangeListener<Tenant> {

    private static final Logger LOG = LoggerFactory.getLogger(FaasTenantManagerListener.class);
    private final Executor executor;
    private final TenantId gbpTenantId;
    private final Uuid faasTenantId;
    private final FaasPolicyManager policyManager;

    public FaasTenantManagerListener(FaasPolicyManager policyManager, TenantId gbpTenantId, Uuid faasTenantId,
            Executor executor) {
        this.executor = executor;
        this.faasTenantId = faasTenantId;
        this.gbpTenantId = gbpTenantId;
        this.policyManager = policyManager;
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Tenant>> changes) {
        executor.execute(() -> executeEvent(changes));
    }

    private void executeEvent(final Collection<DataTreeModification<Tenant>> changes) {
        for (DataTreeModification<Tenant> change: changes) {
            DataObjectModification<Tenant> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case DELETE:
                    final Tenant tenant = rootNode.getDataBefore();
                    if (tenant.getId().equals(gbpTenantId)) {
                        LOG.info("Removed gbp Tenant {}  -- faas Tenant {}", gbpTenantId.getValue(),
                                faasTenantId.getValue());
                        this.policyManager.removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
