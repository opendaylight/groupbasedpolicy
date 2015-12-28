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
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ClassifierDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.SubjectFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to IOVisor Agents
 */
public class IovisorRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorRenderer.class);

    public static final RendererName RENDERER_NAME = new RendererName("IOVisor");

    private final ClassifierDefinitionListener classifierDefinitionListener;
    private EndpointManager endpointManager;
    private EndpointListener endpointListener;
    private final IovisorResolvedEndpointListener resolvedEndpointListener;
    private ActionDefinitionListener actionDefinitionListener;
    private ResolvedPolicyListener resolvedPolicyListener;

    private DataBroker dataBroker;

    public IovisorRenderer(DataBroker passedDataBroker, EpRendererAugmentationRegistry epRendererAugmentationRegistry,
            PolicyValidatorRegistry policyValidatorRegistry) {
        if (passedDataBroker != null)
            dataBroker = passedDataBroker;
        endpointManager = new EndpointManager(dataBroker, epRendererAugmentationRegistry);
        endpointListener = new EndpointListener(dataBroker, endpointManager);
        this.resolvedPolicyListener = new ResolvedPolicyListener(dataBroker);
        this.resolvedEndpointListener = new IovisorResolvedEndpointListener(dataBroker);
        classifierDefinitionListener = new ClassifierDefinitionListener(dataBroker);
        actionDefinitionListener = new ActionDefinitionListener(dataBroker);

        for (Entry<ActionDefinitionId, Action> entry : SubjectFeatures.getActions().entrySet()) {
            policyValidatorRegistry.register(entry.getKey(), entry.getValue());
        }
        LOG.info("IOVisor Renderer has Started");

    }

    public IovisorResolvedEndpointListener getResolvedEndpointListener() {
        return resolvedEndpointListener;
    }

    public EndpointManager getEndPointManager() {
        return endpointManager;
    }

    @Override
    public void close() throws Exception {
        if (endpointManager != null)
            endpointManager.close();
        if (endpointListener != null)
            endpointListener.close();
        if (classifierDefinitionListener != null)
            classifierDefinitionListener.close();
        if (actionDefinitionListener != null)
            actionDefinitionListener.close();
        if (resolvedPolicyListener != null)
            resolvedPolicyListener.close();
        if (resolvedEndpointListener != null)
            resolvedEndpointListener.close();

    }
}
