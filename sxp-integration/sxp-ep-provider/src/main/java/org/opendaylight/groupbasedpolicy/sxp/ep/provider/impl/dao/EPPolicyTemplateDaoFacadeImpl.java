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
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
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
        return Futures.transform(templateFu, new AsyncFunction<Optional<EndpointPolicyTemplateBySgt>, Optional<EndpointPolicyTemplateBySgt>>() {
            @Override
            public ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> apply(
                    @Nonnull final Optional<EndpointPolicyTemplateBySgt> templateOpt) throws Exception {

                return templateOpt.transform(template -> Futures.immediateFuture(templateOpt))
                        // failed to read template -> invoke fallback if available
                        .or(() -> java.util.Optional.ofNullable(templateProvider)
                                .flatMap(provider -> templateProvider.provideTemplate(key))
                                .map(template -> storeTemplateAndEpg(template))
                                .orElse(Futures.immediateFuture(Optional.absent()))
                        );
            }
        });
    }

    private ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> storeTemplateAndEpg(final EndpointPolicyTemplateBySgt template) {
        // store EPG (presume that it does not exist)
        final Sgt sgtValue = template.getSgt();
        LOG.trace("storing EPGs for generated epPolicyTemplate: {} [{}]",
                sgtValue.getValue(), template.getEndpointGroups().size());
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();

        boolean createParent = true;
        for (EndpointGroupId epgId : template.getEndpointGroups()) {
            final InstanceIdentifier<EndpointGroup> epgPath = IidFactory.endpointGroupIid(template.getTenant(), epgId);
            final EndpointGroup epg = new EndpointGroupBuilder()
                    .setId(epgId)
                    .setDescription(new Description("imported from ISE for sgt=" + sgtValue.getValue()))
                    .setName(new Name(String.format("%s_ISE_SGT_%d", epgId.getValue(), sgtValue.getValue())))
                    .build();
            wTx.put(LogicalDatastoreType.CONFIGURATION, epgPath, epg, createParent);
            createParent = false;
        }

        // store ep-policy-template
        LOG.trace("storing generated epPolicyTemplate: {}", sgtValue.getValue());
        final InstanceIdentifier<EndpointPolicyTemplateBySgt> epPolicyTemplatePath = InstanceIdentifier
                .create(SxpEpMapper.class)
                .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(sgtValue));
        wTx.put(LogicalDatastoreType.CONFIGURATION, epPolicyTemplatePath, template, true);

        return Futures.transform(wTx.submit(), createStoreOutcomeHandlerToOptional(template));
    }

    private Function<Void, Optional<EndpointPolicyTemplateBySgt>> createStoreOutcomeHandlerToOptional(final EndpointPolicyTemplateBySgt template) {
        return new Function<Void, Optional<EndpointPolicyTemplateBySgt>>() {
            @Nullable
            @Override
            public Optional<EndpointPolicyTemplateBySgt> apply(@Nullable final Void aVoid) {
                return Optional.of(template);
            }
        };
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

        return Futures.transform(wTx.submit(), createStoreOutcomeHandlerToCollection(template));
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
