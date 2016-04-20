/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorResolvedEndpointsByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.resolved.endpoints.by.tenant.by.endpointgroup.id.IovisorResolvedEndpointByTenantByEndpointgroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class IovisorResolvedEndpointListener
        extends DataTreeChangeHandler<IovisorResolvedEndpointsByTenantByEndpointgroupId> {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorResolvedEndpointListener.class);

    protected IovisorResolvedEndpointListener(DataBroker dataprovider) {
        super(dataprovider);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                IovisorIidFactory.iovisorResolvedEndpointsByTenantIdByEndpointGroupIdWildCardIid()));
    }

    @Override
    protected void onWrite(DataObjectModification<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootNode,
            InstanceIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootIdentifier) {
        onSubtreeModified(rootNode, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootNode,
            InstanceIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootIdentifier) {
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    @Override
    protected void onSubtreeModified(DataObjectModification<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootNode,
            InstanceIdentifier<IovisorResolvedEndpointsByTenantByEndpointgroupId> rootIdentifier) {
        for (IovisorResolvedEndpointByTenantByEndpointgroupId element : rootNode.getDataAfter()
            .getIovisorResolvedEndpointByTenantByEndpointgroupId()) {
            endpointPolicyUpdated(element.getTenantId(), element.getEndpointgroupId(),
                    this.dataProvider.newWriteOnlyTransaction());
        }
    }

    @VisibleForTesting
    void endpointPolicyUpdated(TenantId tenantId, EndpointGroupId epgId, WriteTransaction wTx) {
        // TODO a renderer should remove followed-EPG and followed-tenant at some point
        FollowedEndpointGroup followedEpg = new FollowedEndpointGroupBuilder().setId(epgId).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                IidFactory.followedEndpointgroupIid(IovisorRenderer.RENDERER_NAME, tenantId, epgId), followedEpg, true);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.info("IovisorRenderer following Tenant {} EndpointGroup {}", tenantId.getValue(), epgId.getValue());
            return;
        } else {
            LOG.error("IovisorRenderer could not follow Tenant {} EndpointGroup {}", tenantId.getValue(),
                    epgId.getValue());
            return;
        }
    }

}
