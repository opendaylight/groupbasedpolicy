/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage all things SFC
 *
 * This is a bit of a place-holder for functionality
 * that we'll need to add for SFC integration. This
 * will likely change a lot.
 *
 * One problem is that the ActionInstance can reference
 * an ActionDefintion that doesn't yet exist. Since the
 * life cycles of these two objects are independent, we'd
 * need some way of doing this check if there's ever an
 * update that creates the ActionDefinition with a "chain"
 * action  after the ActionInstance that references it
 * (i.e the creation of the ActionDefinition event).
 *
 * There are some other questions that need resolving -- how are
 * updates to SFC chains received and managed by GBP?  We
 * may need to either add an RPC that SFC calls for updates
 * or implement a data change listener to manage SFC state
 * updates.
 *
 * TODO Move SfcManager out out ofoverlay renderer -- should be something
 *       that's shared by renderers, not specific to ofoverlay
 *
 * @author tbachman
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

        // For now, just go off new ActionInstance objects
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof ActionInstance) {
                ActionInstance ai = (ActionInstance)dao;
                LOG.debug("New ActionInstance created");
                executor.execute(new MatchActionDefTask(ai));
            }
        }

        // We'll worry about udpates and deletes later
//        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
//            DataObject old = change.getOriginalData().get(iid);
//            if (old != null && old instanceof Rule) {
//
//            }
//        }
//
//        for (DataObject dao : change.getUpdatedData().values()) {
//
//        }
    }

    /**
     * Private internal class that gets the action definition
     * referenced by the instance. If the definition has an
     * action of "chain" (or whatever we decide to use
     * here), then we need to invoke the SFC API to go
     * get the chain information, which we'll eventually
     * use during policy resolution.
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
                    rot.read(LogicalDatastoreType.CONFIGURATION, adIid);
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
                ActionDefinition ad = (ActionDefinition)dao;
                if (ad.getName().getValue().equals(SFC_CHAIN_ACTION)) {
                    /*
                     * We have the state we need:
                     *  1) it's a "CHAIN" action
                     *  2) the name is defined in the ActionInstance
                     */
                    if (actionInstance.getParameterValue() != null) {
                        getSfcChain();
                    }
                }
            }
        }

        /**
         * Go get the RenderedServicePath from SFC
         *
         * TBD: what to do with this once we have it - who to
         * give it to
         */
        private void getSfcChain() {
            for (ParameterValue pv: actionInstance.getParameterValue()) {
                if (pv.getName().getValue().equals(SFC_CHAIN_NAME)) {

                    // Go get the RSP
//                    RenderedServicePath rsp =
//                            createRenderedServicePathAndState(pv.getStringValue());

                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (actionListener != null) actionListener.close();

    }
}
