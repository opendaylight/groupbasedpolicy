/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateDaoFacade;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SgtGeneratorImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.SxpEpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: add template provider fallback to {@link EPPolicyTemplateDaoImpl}
 */
public class EPPolicyTemplateDaoFacadeImpl implements EPPolicyTemplateDaoFacade {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateDaoFacadeImpl.class);

    private final EPPolicyTemplateDaoImpl epPolicyTemplateDao;
    private final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateCachedDao;
    private final SgtGeneratorImpl sgtGenerator;
    private final DataBroker dataBroker;

    private EPPolicyTemplateProvider templateProvider;

    public EPPolicyTemplateDaoFacadeImpl(final DataBroker dataBroker, final EPPolicyTemplateDaoImpl epPolicyTemplateDao,
                                         final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateCachedDao,
                                         final SgtGeneratorImpl sgtGenerator) {
        this.dataBroker = dataBroker;
        this.epPolicyTemplateDao = epPolicyTemplateDao;
        this.epPolicyTemplateCachedDao = epPolicyTemplateCachedDao;
        this.sgtGenerator = sgtGenerator;
    }

    @Override
    public void setTemplateProvider(final EPPolicyTemplateProvider templateProvider) {
        this.templateProvider = templateProvider;
    }


    @Override
    public ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> read(@Nonnull final Sgt key) {
        // read from delegate
        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> templateFu = epPolicyTemplateDao.read(key);

        // involve fallback if template is absent
        return Futures.transformAsync(templateFu, new AsyncFunction<Optional<EndpointPolicyTemplateBySgt>, Optional<EndpointPolicyTemplateBySgt>>() {
            @Override
            public ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> apply(
                    @Nonnull final Optional<EndpointPolicyTemplateBySgt> templateOpt) throws Exception {

                return templateOpt.transform(template -> Futures.immediateFuture(templateOpt))
                        // failed to read template -> invoke fallback if available
                        .or(() -> java.util.Optional.ofNullable(templateProvider)
                                .map(provider -> templateProvider.provideTemplate(key))
                                .map(template -> rewrapOptionalToGuavaOptional(template))
                                .orElse(Futures.immediateFuture(Optional.absent()))
                        );
            }
        }, MoreExecutors.directExecutor());
    }

    private <T> ListenableFuture<Optional<T>> rewrapOptionalToGuavaOptional(final ListenableFuture<java.util.Optional<T>> templateFu) {
        return Futures.transform(templateFu, new Function<java.util.Optional<T>, Optional<T>>() {
                    @Nullable
                    @Override
                    public Optional<T> apply(@Nullable final java.util.Optional<T> input) {
                        return java.util.Optional.ofNullable(input)
                                .map(origNonnullInput -> Optional.fromNullable(origNonnullInput.orElse(null)))
                                .orElse(Optional.absent());
                    }
                },
            MoreExecutors.directExecutor());
    }


    private Function<Void, Collection<EndpointPolicyTemplateBySgt>> createStoreOutcomeHandlerToCollection(final EndpointPolicyTemplateBySgt template) {
        return new Function<Void, Collection<EndpointPolicyTemplateBySgt>>() {
            @Nullable
            @Override
            public Collection<EndpointPolicyTemplateBySgt> apply(@Nullable final Void aVoid) {
                return Collections.singletonList(template);
            }
        };
    }

    @Override
    public ListenableFuture<Collection<EndpointPolicyTemplateBySgt>> readBy(@Nonnull final EpPolicyTemplateValueKey templateLookupKey) {
        //TODO: expose to ios-xe renderer,
        final Collection<EndpointPolicyTemplateBySgt> templatesFromDao = epPolicyTemplateDao.readBy(templateLookupKey);
        final ListenableFuture<Collection<EndpointPolicyTemplateBySgt>> result;
        if (!templatesFromDao.isEmpty()) {
            result = Futures.immediateFuture(templatesFromDao);
        } else {
            // generate
            result = sgtGenerator.generateNextSgt(epPolicyTemplateCachedDao)
                    // build ep-policy-template
                    .map(sgt -> buildEpPolicyTemplate(templateLookupKey, sgt))
                    // store the template
                    .map(this::storeTemplate)
                    .orElse(Futures.immediateFuture(Collections.emptyList()));
        }
        return result;
    }

    private ListenableFuture<Collection<EndpointPolicyTemplateBySgt>> storeTemplate(final EndpointPolicyTemplateBySgt template) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        // store ep-policy-template
        final Sgt sgt = template.getSgt();
        LOG.trace("storing generated epPolicyTemplate: {}", sgt.getValue());
        final InstanceIdentifier<EndpointPolicyTemplateBySgt> epPolicyTemplatePath = InstanceIdentifier
                .create(SxpEpMapper.class)
                .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(sgt));
        wTx.put(LogicalDatastoreType.CONFIGURATION, epPolicyTemplatePath, template, true);

        return Futures.transform(wTx.submit(), createStoreOutcomeHandlerToCollection(template),
            MoreExecutors.directExecutor());
    }

    private EndpointPolicyTemplateBySgt buildEpPolicyTemplate(final EpPolicyTemplateValueKey templateLookupKey, final Sgt sgt) {
        return new EndpointPolicyTemplateBySgtBuilder()
                .setOrigin(TemplateGenerated.class)
                .setTenant(templateLookupKey.getTenantId())
                .setSgt(sgt)
                .setEndpointGroups(templateLookupKey.getEpgId())
                .setConditions(templateLookupKey.getConditionName())
                .build();
    }
}
