/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Manage all things SFC
 *
 * This is a bit of a place-holder for functionality that we'll need to add for
 * SFC integration. This will likely change a lot.
 *
 * TODO Move SfcManager out of ofoverlay renderer -- should be something that's
 * shared by renderers, not specific to ofoverlay
 *
 */
public class SfcManager implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(SfcManager.class);

    private final DataBroker dataBroker;
    // currently unused
    private final PolicyResolver policyResolver;
    // currently unused
    private final RpcProviderRegistry rpcRegistry;
    private final ScheduledExecutorService executor;
    private final InstanceIdentifier<ActionInstance> allActionInstancesIid;
    private final ListenerRegistration<DataChangeListener> actionListener;

    // place-holder - not sure what we'll call it
    private final String SFC_CHAIN_ACTION = "CHAIN";
    private final String SFC_CHAIN_NAME = "sfc-chain-name";
    private final String SFC_RSP_NAME = "rsp-sfc-gbp";

    public SfcManager(DataBroker dataBroker,
            PolicyResolver policyResolver,
            RpcProviderRegistry rpcRegistry,
            ScheduledExecutorService executor) {
        this.dataBroker = dataBroker;
        this.policyResolver = policyResolver;
        this.rpcRegistry = rpcRegistry;
        this.executor = executor;

        /*
         * For now, listen to all changes in rules
         */
        allActionInstancesIid =
                InstanceIdentifier.builder(Tenants.class)
                        .child(Tenant.class)
                        .child(SubjectFeatureInstances.class)
                        .child(ActionInstance.class)
                        .build();
        actionListener = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                allActionInstancesIid, this, DataChangeScope.ONE);
        LOG.debug("SfcManager: Started");
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof ActionInstance) {
                ActionInstance ai = (ActionInstance) dao;
                LOG.debug("New ActionInstance created");
                executor.execute(new MatchActionDefTask(ai));
            }
        }

        // TODO: how to handle deletes (comment out for now)
        // for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
        // DataObject old = change.getOriginalData().get(iid);
        // if (old != null && old instanceof ActionInstance) {
        //
        // }
        // }

        for (DataObject dao : change.getUpdatedData().values()) {
            if (dao instanceof ActionInstance) {
                ActionInstance ai = (ActionInstance) dao;
                executor.execute(new MatchActionDefTask(ai));
            }
        }
    }

    /**
     * Private internal class that gets the action definition referenced by the
     * instance. If the definition has an action of "chain" (or whatever we
     * decide to use here), then we need to invoke the SFC API to go get the
     * chain information, which we'll eventually use during policy resolution.
     *
     * @author tbachman
     *
     */
    private class MatchActionDefTask implements Runnable,
            FutureCallback<Optional<ActionDefinition>> {
        private final ActionInstance actionInstance;
        private final InstanceIdentifier<ActionDefinition> adIid;
        private final ActionDefinitionId id;

        public MatchActionDefTask(ActionInstance actionInstance) {
            this.actionInstance = actionInstance;
            this.id = actionInstance.getActionDefinitionId();

            adIid = InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                    .child(ActionDefinition.class,
                            new ActionDefinitionKey(this.id))
                    .build();

        }

        /**
         * Create read transaction with callback
         */
        @Override
        public void run() {
            ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();
            ListenableFuture<Optional<ActionDefinition>> dao =
                    rot.read(LogicalDatastoreType.OPERATIONAL, adIid);
            Futures.addCallback(dao, this, executor);

        }

        @Override
        public void onFailure(Throwable arg0) {
            LOG.error("Failure reading ActionDefinition {}", id.getValue());
        }

        @Override
        public void onSuccess(Optional<ActionDefinition> dao) {
            LOG.debug("Found ActionDefinition {}", id.getValue());
            if (dao instanceof ActionDefinition) {
                ActionDefinition ad = (ActionDefinition) dao;
                if (ad.getName().getValue().equals(SFC_CHAIN_ACTION)) {
                    /*
                     * We have the state we need: 1) it's a "CHAIN" action 2)
                     * the name is defined in the ActionInstance
                     */
                    if (actionInstance.getParameterValue() != null) {
                        getSfcRsp();
                    }
                }
            }
        }

        /**
         * Go get the RenderedServicePath from SFC
         *
         * TBD: what to do with this once we have it - who to give it to
         */
        private void getSfcRsp() {
            for (ParameterValue pv : actionInstance.getParameterValue()) {
                if (pv.getName().getValue().equals(SFC_CHAIN_NAME)) {
                    // TODO: check for rspFirstHop.getTransportType()
                    ReadRenderedServicePathFirstHopInputBuilder builder =
                            new ReadRenderedServicePathFirstHopInputBuilder();
                    builder.setName(SFC_CHAIN_NAME);
                    Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result =
                            SfcProviderRpc.getSfcProviderRpc().readRenderedServicePathFirstHop(builder.build());
                    try {
                        RpcResult<ReadRenderedServicePathFirstHopOutput> output = result.get();
                        if (output.isSuccessful()) {
                            RenderedServicePathFirstHop rspFirstHop =
                                    output.getResult().getRenderedServicePathFirstHop();
                            IpAddress ip = rspFirstHop.getIp();
                            PortNumber pn = rspFirstHop.getPort();
                            // TODO: use NSI, NSP, SPI
                            // Short nsi = rspFirstHop.getStartingIndex();
                            // Long nsp = rspFirstHop.getPathId();
                            // Long spi = rspFirstHop.getSymmetricPathId();
                        }
                    } catch (Exception e) {
                        // TODO: proper exception handling
                    }
                }
            }
        }

    }

    @Override
    public void close() throws Exception {
        if (actionListener != null)
            actionListener.close();

    }
}
