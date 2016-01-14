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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.ClassifierDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Renderer that converts GBP services to IOVisor Agents
 */
public class IovisorRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorRenderer.class);

    public static final RendererName RENDERER_NAME = new RendererName("Iovisor");
    public static String IOVISOR_MODULE_LISTENER_BASE_URL = "/policies/";

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
        String baseUrl = getIovisorModuleBaseUrl();
        if (baseUrl != null) {
            // TODO FIXME Need to set listener on BaseUrl
            IOVISOR_MODULE_LISTENER_BASE_URL = baseUrl;
        }
        endpointManager = new EndpointManager(dataBroker, epRendererAugmentationRegistry);
        endpointListener = new EndpointListener(dataBroker, endpointManager);
        this.resolvedPolicyListener = new ResolvedPolicyListener(dataBroker, endpointManager.getIovisorModuleManager());
        this.resolvedEndpointListener = new IovisorResolvedEndpointListener(dataBroker);
        classifierDefinitionListener = new ClassifierDefinitionListener(dataBroker);
        actionDefinitionListener = new ActionDefinitionListener(dataBroker);

        for (Entry<ActionDefinitionId, Action> entry : SubjectFeatures.getActions().entrySet()) {
            policyValidatorRegistry.register(entry.getKey(), entry.getValue());
        }
        LOG.info("Iovisor Renderer has Started");

    }

    public IovisorResolvedEndpointListener getResolvedEndpointListener() {
        return resolvedEndpointListener;
    }

    public EndpointManager getEndPointManager() {
        return endpointManager;
    }

    private String getIovisorModuleBaseUrl() {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<IovisorModuleInstances> readFromDs = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IovisorIidFactory.iovisorModuleInstanceWildCardIid(), rTx);
        if (readFromDs.isPresent()) {
            return readFromDs.get().getBaseUrl();
        }
        return null;
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
