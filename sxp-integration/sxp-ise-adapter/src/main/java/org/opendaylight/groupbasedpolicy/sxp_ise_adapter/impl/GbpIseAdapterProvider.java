/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.sxp.ise.adapter.model.rev160630.GbpSxpIseAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Purpose: main provider of gbp-ise adapter (for reading sgts and generating EndpointPolicyTemplates)
 */
public class GbpIseAdapterProvider implements AutoCloseable, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GbpIseAdapterProvider.class);

    private final DataBroker dataBroker;
    private ListenerRegistration<ClusteredDataTreeChangeListener<IseSourceConfig>> registration;

    public GbpIseAdapterProvider(final DataBroker dataBroker, final BindingAwareBroker brokerDependency) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "provided dataBroker must not be null");
        brokerDependency.registerProvider(this);
    }

    @Override
    public void close() {
        if (registration != null) {
            LOG.info("closing GbpIseAdapterProvider");
            registration.close();
            registration = null;
        }
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("Starting GbpIseAdapterProvider ..");

        // setup harvesting and processing pipeline
        final SgtInfoProcessor epgGenerator = new SgtToEpgGeneratorImpl(dataBroker);
        final SgtInfoProcessor templateGenerator = new SgtToEPTemplateGeneratorImpl(dataBroker);
        final GbpIseSgtHarvester gbpIseSgtHarvester = new GbpIseSgtHarvesterImpl(epgGenerator, templateGenerator);
        final GbpIseConfigListenerImpl gbpIseConfigListener = new GbpIseConfigListenerImpl(dataBroker, gbpIseSgtHarvester);

        // build data-tree path
        final DataTreeIdentifier<IseSourceConfig> dataTreePath = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(GbpSxpIseAdapter.class).child(IseSourceConfig.class));

        // register config listener
        registration = dataBroker.registerDataTreeChangeListener(dataTreePath,
                gbpIseConfigListener);

        LOG.info("Started");
    }
}
