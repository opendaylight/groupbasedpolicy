/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.base.Function;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: query ise in order to get name of sgt for given tenant and build {@link EndpointPolicyTemplateBySgt}
 */
public class EPPolicyTemplateProviderIseImpl implements EPPolicyTemplateProviderFacade {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateProviderIseImpl.class);

    private Optional<IseContext> iseContext = Optional.empty();
    private GbpIseSgtHarvester iseSgtHarvester;

    @Override
    public ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> provideTemplate(@Nonnull final Sgt sgt) {
        return findIseSourceConfigBySgt(sgt)
                .map(iseContext -> queryIseAndBuildTemplate(iseContext, sgt))
                .orElse(Futures.immediateFuture(Optional.empty()));
    }

    private ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> queryIseAndBuildTemplate(final IseContext iseContext, final Sgt sgt) {
        final ListenableFuture<Optional<String>> sgtNameFu = queryIseOnSgt(iseContext, sgt);
        return Futures.transform(sgtNameFu, new Function<Optional<String>, Optional<EndpointPolicyTemplateBySgt>>() {
            @Nullable
            @Override
            public Optional<EndpointPolicyTemplateBySgt> apply(@Nullable final Optional<String> input) {
                return Optional.ofNullable(input)
                        .flatMap(i -> i.map(sgtName -> buildTemplate(sgt, iseContext.getIseSourceConfig().getTenant(), sgtName)));
            }
        });
    }

    private EndpointPolicyTemplateBySgt buildTemplate(final @Nonnull Sgt sgt, final @Nonnull TenantId tenantId,
                                                      final @Nonnull String sgtName) {
        return new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(sgt)
                .setEndpointGroups(Collections.singletonList(new EndpointGroupId(sgtName)))
                .setTenant(tenantId)
                // no conditions
                .setOrigin(TemplateGenerated.class)
                .build();
    }

    private ListenableFuture<Optional<String>> queryIseOnSgt(final IseContext iseContext, @Nonnull final Sgt sgt) {
        final ListenableFuture<Collection<SgtInfo>> sgtUpdateFu = iseSgtHarvester.harvestAll(iseContext);

        Futures.addCallback(sgtUpdateFu, new FutureCallback<Collection<SgtInfo>>() {
            @Override
            public void onSuccess(@Nullable final Collection<SgtInfo> result) {
                final Integer amount = Optional.ofNullable(result).map(Collection::size).orElse(0);
                LOG.debug("[epPolicyTemplateProvider] harvestAll succeeded: {}", amount);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.debug("[epPolicyTemplateProvider] harvestAll FAILED: {}", t.getMessage());
            }
        });

        return Futures.transform(sgtUpdateFu, new Function<Collection<SgtInfo>, Optional<String>>() {
            @Nullable
            @Override
            public Optional<String> apply(@Nullable final Collection<SgtInfo> input) {
                // pick first sgtInfo which equals to given sgt
                return Optional.ofNullable(input)
                        .flatMap(safeInput -> safeInput.stream()
                                .filter(sgtInfo -> sgt.equals(sgtInfo.getSgt())).findFirst()
                                .map(SgtInfo::getName));
            }
        });
    }

    private Optional<IseContext> findIseSourceConfigBySgt(final Sgt sgt) {
        // expected relation (ise : tenant) == (1:1)
        return iseContext
                .filter(context ->
                        context.getIseSourceConfig() != null && Range.closed(
                                context.getIseSourceConfig().getSgtRangeMin().getValue(),
                                context.getIseSourceConfig().getSgtRangeMax().getValue()
                        ).contains(sgt.getValue())
                );
    }

    @Override
    public void assignIseContext(final @Nullable IseContext iseContext) {
        this.iseContext = Optional.ofNullable(iseContext);
    }

    @Override
    public void setIseSgtHarvester(final GbpIseSgtHarvester iseSgtHarvester) {
        this.iseSgtHarvester = iseSgtHarvester;
    }
}
