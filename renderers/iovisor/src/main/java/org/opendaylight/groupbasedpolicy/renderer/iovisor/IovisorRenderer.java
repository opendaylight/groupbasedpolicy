/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ClassifierDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to IOVisor Agents
 */
public class IovisorRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorRenderer.class);

    private EndpointManager endPointManager;
    private IovisorModuleManager iovisorModuleManager;
    public static final RendererName RENDERER_NAME = new RendererName("IOVisor");
    private final ClassifierDefinitionListener classifierDefinitionListener;
    private ActionDefinitionListener actionDefinitionListener;
    private ResolvedPolicyListener resolvedPolicyListener;
    private DataBroker dataBroker;

    public IovisorRenderer(DataBroker dataBroker, EpRendererAugmentationRegistry epRendererAugmentationRegistry,
            PolicyValidatorRegistry policyValidatorRegistry) {
        if (dataBroker != null)
            this.dataBroker = dataBroker;
        this.endPointManager = new EndpointManager(dataBroker, epRendererAugmentationRegistry);
        this.iovisorModuleManager = new IovisorModuleManager(dataBroker);
        this.resolvedPolicyListener =
                new ResolvedPolicyListener(dataBroker, new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build()));
        classifierDefinitionListener = new ClassifierDefinitionListener(dataBroker, new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(SubjectFeatureDefinitions.class).child(ClassifierDefinition.class).build()));
        actionDefinitionListener = new ActionDefinitionListener(dataBroker, new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class).build()));

        for (Entry<ActionDefinitionId, Action> entry : SubjectFeatures.getActions().entrySet()) {
            policyValidatorRegistry.register(entry.getKey(), entry.getValue());
        }
        LOG.info("IOVisor Renderer has Started");

    }

    // TODO PUNT THIS....
    public void endpointPolicyUpdated(TenantId tenantId, EndpointGroupId epgId, EpKey epKey) {
        // TODO a renderer should remove followed-EPG and followed-tenant at some point
        if (dataBroker == null) {
            LOG.error("DataBroker is null. Cannot write followed-epg {}", epKey);
            return;
        }
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        FollowedEndpointGroup followedEpg = new FollowedEndpointGroupBuilder().setId(epgId).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.followedEndpointgroupIid(RENDERER_NAME, tenantId, epgId),
                followedEpg, true);
        DataStoreHelper.submitToDs(wTx);
    }

    @Override
    public void close() throws Exception {
        if (endPointManager != null) {
            endPointManager.close();
        }
        if (classifierDefinitionListener != null)
            classifierDefinitionListener.close();
        if (actionDefinitionListener != null)
            actionDefinitionListener.close();
        if (iovisorModuleManager != null) {
            iovisorModuleManager.close();
            if (resolvedPolicyListener != null)
                resolvedPolicyListener.close();
        }
    }
}
