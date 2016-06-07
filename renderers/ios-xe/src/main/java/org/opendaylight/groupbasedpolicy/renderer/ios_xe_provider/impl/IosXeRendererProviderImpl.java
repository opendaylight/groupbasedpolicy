/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.IosXeRendererProvider;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener.IosXeCapableNodeListenerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener.RendererConfigurationListenerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.NodeManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerZipImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf.EtherTypeClassifier;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.sf.IpProtoClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinitionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Purpose: bootstrap provider implementation of Ios-xe renderer
 */
public class IosXeRendererProviderImpl implements IosXeRendererProvider, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeRendererProviderImpl.class);

    private final DataBroker dataBroker;
    private final RendererName rendererName;
    private RendererConfigurationListenerImpl rendererConfigurationListener;
    private IosXeCapableNodeListenerImpl iosXeCapableNodeListener;
    private PolicyManager policyManager;
    private NodeManager nodeManager;

    public IosXeRendererProviderImpl(final DataBroker dataBroker, final BindingAwareBroker broker,
                                     final RendererName rendererName) {
        LOG.debug("ios-xe renderer bootstrap");
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "missing dataBroker dependency");
        this.rendererName = Preconditions.checkNotNull(rendererName, "missing rendererName param");
        broker.registerProvider(this);
    }

    @Override
    public void close() {
        //TODO
        LOG.info("closing ios-xe renderer");
        if (rendererConfigurationListener != null) {
            rendererConfigurationListener.close();
        }
        if (iosXeCapableNodeListener != null) {
            iosXeCapableNodeListener.close();
        }
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("starting ios-xe renderer");
        //TODO register listeners:
        // node-manager
        nodeManager = new NodeManager(dataBroker, providerContext);
        // network-topology
        iosXeCapableNodeListener = new IosXeCapableNodeListenerImpl(dataBroker, nodeManager);

        // policy-manager and delegates
        policyManager = new PolicyManagerImpl(dataBroker);
        final PolicyManager policyManagerZip = new PolicyManagerZipImpl(policyManager);

        // renderer-configuration endpoints
        rendererConfigurationListener = new RendererConfigurationListenerImpl(dataBroker, rendererName, policyManagerZip);
        // supported node list maintenance
        // TODO: upkeep of available renderer-nodes

        // provide renderer capabilities
        writeRendererCapabilities();
    }

    private void writeRendererCapabilities() {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        ChainAction action = new ChainAction();
        List<SupportedActionDefinition> actionDefinitions =
                ImmutableList.of(new SupportedActionDefinitionBuilder().setActionDefinitionId(action.getId())
                    .setSupportedParameterValues(action.getSupportedParameterValues())
                    .build());

        Classifier etherClassifier = new EtherTypeClassifier(null);
        Classifier ipProtoClassifier = new IpProtoClassifier(etherClassifier.getId());
        List<SupportedClassifierDefinition> classifierDefinitions = ImmutableList
            .of(new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(etherClassifier.getId())
                .setParentClassifierDefinitionId(etherClassifier.getParent())
                .setSupportedParameterValues(etherClassifier.getSupportedParameterValues())
                .build(),
                new SupportedClassifierDefinitionBuilder().setClassifierDefinitionId(ipProtoClassifier.getId())
                .setParentClassifierDefinitionId(ipProtoClassifier.getParent())
                .setSupportedParameterValues(ipProtoClassifier.getSupportedParameterValues())
                .build());

        Renderer renderer = new RendererBuilder().setName(NodeManager.iosXeRenderer)
            .setCapabilities(new CapabilitiesBuilder().setSupportedActionDefinition(actionDefinitions)
                .setSupportedClassifierDefinition(classifierDefinitions)
                .build())
            .build();

        InstanceIdentifier<Renderer> iid = InstanceIdentifier.builder(Renderers.class)
        .child(Renderer.class, new RendererKey(new RendererName(NodeManager.iosXeRenderer)))
        .build();
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, iid, renderer, true);
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
}
