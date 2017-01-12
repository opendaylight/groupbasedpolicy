/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.vpp_provider.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.adapter.VppRpcServiceImpl;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.AclManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.RendererPolicyListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppEndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppNodeListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.manager.VppNodeManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.BridgeDomainManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.ForwardingManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.VppRendererPolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.IpProtoClassifier;
import org.opendaylight.groupbasedpolicy.renderer.vpp.sf.L4Classifier;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class VppRenderer implements AutoCloseable, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(VppRenderer.class);

    public static final RendererName NAME = new RendererName("vpp-renderer");

    private final List<SupportedActionDefinition> actionDefinitions =
            ImmutableList.of(new SupportedActionDefinitionBuilder().setActionDefinitionId(new AllowAction().getId())
                .setSupportedParameterValues(new AllowAction().getSupportedParameterValues())
                .build());
    private final List<SupportedClassifierDefinition> classifierDefinitions;

    private final DataBroker dataBroker;

    private VppNodeManager vppNodeManager;
    private InterfaceManager interfaceManager;
    private AclManager aclManager;
    private VppRendererPolicyManager vppRendererPolicyManager;

    private VppNodeListener vppNodeListener;
    private VppEndpointListener vppEndpointListener;
    private RendererPolicyListener rendererPolicyListener;
    private VppRpcServiceImpl vppRpcServiceImpl;

    public VppRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        bindingAwareBroker.registerProvider(this);
        EtherTypeClassifier etherTypeClassifier = new EtherTypeClassifier(null);
        IpProtoClassifier ipProtoClassifier = new IpProtoClassifier(etherTypeClassifier);
        classifierDefinitions =
                buildClassifierDefinitions(etherTypeClassifier, ipProtoClassifier, new L4Classifier(ipProtoClassifier));
    }

    private List<SupportedClassifierDefinition> buildClassifierDefinitions(Classifier ... classifs) {
        List<SupportedClassifierDefinition> clDefs = new ArrayList<>();
        SupportedClassifierDefinitionBuilder clDefBuilder = new SupportedClassifierDefinitionBuilder();
        for (Classifier classif : classifs) {
            if (classif.getParent() != null) {
                clDefBuilder.setParentClassifierDefinitionId(classif.getParent().getId());
            }
            clDefs.add(clDefBuilder.setClassifierDefinitionId(classif.getId())
                .setSupportedParameterValues(classif.getSupportedParameterValues())
                .build());
        }
        return clDefs;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing Vpp renderer");
        if (vppNodeListener != null) {
            vppNodeListener.close();
        }
        if (vppEndpointListener != null) {
            vppEndpointListener.close();
        }
        if (rendererPolicyListener != null) {
            rendererPolicyListener.close();
        }
        if (interfaceManager != null) {
            interfaceManager.close();
        }
        unregisterFromRendererManager();
    }

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("starting vpp renderer");

        MountPointService mountService =
                Preconditions.checkNotNull(providerContext.getSALService(MountPointService.class));
        MountedDataBrokerProvider mountDataProvider = new MountedDataBrokerProvider(mountService, dataBroker);
        vppNodeManager = new VppNodeManager(dataBroker, providerContext);

        EventBus dtoEventBus = new EventBus((exception, context) -> LOG.error("Could not dispatch event: {} to {}",
                context.getSubscriber(), context.getSubscriberMethod(), exception));
        interfaceManager = new InterfaceManager(mountDataProvider, dataBroker);
        aclManager = new AclManager(mountDataProvider);
        dtoEventBus.register(interfaceManager);
        BridgeDomainManager bdManager = new BridgeDomainManagerImpl(dataBroker);
        ForwardingManager fwManager = new ForwardingManager(interfaceManager, aclManager, bdManager, dataBroker);
        vppRendererPolicyManager = new VppRendererPolicyManager(fwManager, aclManager, dataBroker);
        dtoEventBus.register(vppRendererPolicyManager);

        vppNodeListener = new VppNodeListener(dataBroker, vppNodeManager, dtoEventBus);
        vppEndpointListener = new VppEndpointListener(dataBroker, dtoEventBus);
        rendererPolicyListener = new RendererPolicyListener(dataBroker, dtoEventBus);
        vppRpcServiceImpl = new VppRpcServiceImpl(dataBroker, mountDataProvider, bdManager, interfaceManager);
        registerToRendererManager();
    }

    private void registerToRendererManager() {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        Renderer renderer = new RendererBuilder().setName(VppRenderer.NAME)
            .setRendererNodes(new RendererNodesBuilder().build())
            .setCapabilities(new CapabilitiesBuilder().setSupportedActionDefinition(actionDefinitions)
                .setSupportedClassifierDefinition(classifierDefinitions)
                .build())
            .build();

        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererIID(renderer.getKey()),
                renderer, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = writeTransaction.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Could not register renderer {}: {}", renderer, throwable);
            }

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Renderer {} successfully registered.", renderer);
            }
        });
    }

    private void unregisterFromRendererManager() {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL,
                VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)));

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTransaction.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Could not unregister renderer {}: {}", VppRenderer.NAME, throwable);
            }

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Renderer {} successfully unregistered.", VppRenderer.NAME);
            }
        });
    }

    VppRpcServiceImpl getVppRpcServiceImpl() {
        return vppRpcServiceImpl;
    }
}
