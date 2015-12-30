/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.restclient.RestClient;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.modules.by.tenant.by.endpointgroup.id.iovisor.module.by.tenant.by.endpointgroup.id.IovisorModuleInstanceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class ResolvedPolicyListener extends DataTreeChangeHandler<ResolvedPolicy> {

    private static final Logger LOG = LoggerFactory.getLogger(ResolvedPolicyListener.class);

    private IovisorModuleManager iovisorModuleManager;

    public ResolvedPolicyListener(DataBroker dataBroker, IovisorModuleManager iovisorModuleManager) {
        super(dataBroker, new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build()));
        this.iovisorModuleManager = iovisorModuleManager;
    }

    @Override
    protected void onWrite(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        processResolvedPolicyNotification(rootNode.getDataAfter());
        LOG.trace("Called processResolvedPolicyNotification with ResolvedPolicyKey {} ",
                rootNode.getDataAfter().getKey());
    }

    @Override
    protected void onDelete(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @VisibleForTesting
    void processResolvedPolicyNotification(ResolvedPolicy resolvedPolicy) {
        checkNotNull(resolvedPolicy);
        Set<IovisorModuleInstanceId> ioms = new HashSet<>();
        List<IovisorModuleInstanceId> tempIoms = new ArrayList<>();

        tempIoms = iovisorModuleManager.getIovisorModulesByTenantByEpg(resolvedPolicy.getProviderTenantId(),
                resolvedPolicy.getProviderEpgId());
        if (tempIoms == null || tempIoms.isEmpty()) {
            // TODO In Multi Renderer environment ResolvedPolicies will have to only resolve
            // policies between EPGs where an EP is present. Not just one.
            LOG.info("No IovisorModule found for Tenant: {} EndpointGroup: {}. Therefore no endpoints to process.",
                    resolvedPolicy.getProviderTenantId().getValue(), resolvedPolicy.getProviderEpgId().getValue());
            return;
        }
        ioms.addAll(tempIoms);

        tempIoms = iovisorModuleManager.getIovisorModulesByTenantByEpg(resolvedPolicy.getConsumerTenantId(),
                resolvedPolicy.getConsumerEpgId());
        if (tempIoms == null || tempIoms.isEmpty()) {
            // TODO In Multi Renderer environment ResolvedPolicies will have to only resolve
            // policies between EPGs where an EP is present. Not just one.
            LOG.info("No IovisorModule found for Tenant: {} EndpointGroup: {}. Therefore no endpoints to process.",
                    resolvedPolicy.getConsumerTenantId().getValue(), resolvedPolicy.getConsumerEpgId().getValue());
            return;
        }
        ioms.addAll(tempIoms);

        for (IovisorModuleInstanceId iom : ioms) {
            IovisorModuleInstance iomInstance = iovisorModuleManager.getActiveIovisorModule(iom.getId());
            RestClient restClient = new RestClient("http://" + iomInstance.getUri().getValue());
            restClient.post(IovisorRenderer.IOVISOR_MODULE_LISTENER_BASE_URL, buildPolicyUris(resolvedPolicy));
        }
    }

    @VisibleForTesting
    String buildPolicyUris(ResolvedPolicy resolvedPolicy) {
        // TODO Move String definition of URIs to common place, perhaps something like IidFactory ?
        StringBuilder base =
                new StringBuilder("/restconf/operational/resolved-policy:resolved-policies/resolved-policy/");
        base.append(resolvedPolicy.getConsumerTenantId().getValue());
        base.append("/");
        base.append(resolvedPolicy.getConsumerEpgId().getValue());
        base.append("/");
        base.append(resolvedPolicy.getProviderTenantId().getValue());
        base.append("/");
        base.append(resolvedPolicy.getProviderEpgId().getValue());
        base.append("/");
        return base.toString();
    }

}
