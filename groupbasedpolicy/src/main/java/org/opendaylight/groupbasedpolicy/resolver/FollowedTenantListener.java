/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.Interests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.FollowedTenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.FollowedTenant;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class FollowedTenantListener implements DataTreeChangeListener<FollowedTenant>, Closeable {

    private final PolicyResolver policyResolver;
    private final ListenerRegistration<FollowedTenantListener> listenerRegistration;
    private final Multiset<TenantId> countedFollowedTenants = HashMultiset.create();

    public FollowedTenantListener(DataBroker dataProvider, PolicyResolver policyResolver) {
        this.policyResolver = policyResolver;
        listenerRegistration = dataProvider.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Renderers.class)
                            .child(Renderer.class)
                            .child(Interests.class)
                            .child(FollowedTenants.class)
                            .child(FollowedTenant.class)
                            .build()),
                this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<FollowedTenant>> changes) {
        for (DataTreeModification<FollowedTenant> change : changes) {
            DataObjectModification<FollowedTenant> rootNode = change.getRootNode();
            TenantId tenantId = change.getRootPath().getRootIdentifier().firstKeyOf(FollowedTenant.class).getId();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    policyResolver.subscribeTenant(tenantId);
                    countedFollowedTenants.add(tenantId);
                    break;
                case DELETE:
                    countedFollowedTenants.remove(tenantId);
                    if (countedFollowedTenants.count(tenantId) == 0) {
                        policyResolver.unsubscribeTenant(tenantId);
                    }
                    break;
                case SUBTREE_MODIFIED:
                    // NOOP
                    break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (listenerRegistration != null)
            listenerRegistration.close();
    }

}
