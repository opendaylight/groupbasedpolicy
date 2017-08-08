/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nullable;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateDaoFacade;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPToSgtMapper;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKey;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKeyFactory;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Purpose: mapper implementation based on {@link ReadableAsyncByKey} (e.g.: {@link EPPolicyTemplateDaoFacade})
 */
public class EPToSgtMapperImpl implements EPToSgtMapper {

    public static final Function<Collection<EndpointPolicyTemplateBySgt>, Collection<Sgt>> TRANSFORM_TEMPLATE_TO_SGT =
            new Function<Collection<EndpointPolicyTemplateBySgt>, Collection<Sgt>>() {
                @Nullable
                @Override
                public Collection<Sgt> apply(@Nullable final Collection<EndpointPolicyTemplateBySgt> input) {
                    final Collection<Sgt> sgtBag = new HashSet<>();
                    for (EndpointPolicyTemplateBySgt template : input) {
                        sgtBag.add(template.getSgt());
                    }
                    return sgtBag;
                }
            };

    private final ReadableAsyncByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> epPolicyTemplateReader;
    private final EpPolicyTemplateValueKeyFactory keyFactory;

    public EPToSgtMapperImpl(final ReadableAsyncByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> epPolicyTemplateReader) {
        this.epPolicyTemplateReader = epPolicyTemplateReader;
        keyFactory = new EpPolicyTemplateValueKeyFactory(
                EPTemplateUtil.createEndpointGroupIdOrdering(), EPTemplateUtil.createConditionNameOrdering());

    }

    @Override
    public ListenableFuture<Collection<Sgt>> findSgtForEP(final AddressEndpointWithLocation endpointWithLocation) {
        final EpPolicyTemplateValueKey rawKey = new EpPolicyTemplateValueKey(endpointWithLocation);
        final EpPolicyTemplateValueKey key = keyFactory.sortValueKeyLists(rawKey);
        return Futures.transform(epPolicyTemplateReader.readBy(key), TRANSFORM_TEMPLATE_TO_SGT,
            MoreExecutors.directExecutor());
    }
}
