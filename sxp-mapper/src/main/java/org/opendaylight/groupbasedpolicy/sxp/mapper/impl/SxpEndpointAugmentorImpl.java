/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map.Entry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.ReadableByKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EpPolicyTemplateValueKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EpPolicyTemplateValueKeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.ContainmentEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class SxpEndpointAugmentorImpl implements EndpointAugmentor {

    private final ReadableByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> epPolicyTemplateDao;
    private final EpPolicyTemplateValueKeyFactory epPolicyTemplateKeyFactory;

    public SxpEndpointAugmentorImpl(final ReadableByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> epPolicyTemplateDao,
                                   final EpPolicyTemplateValueKeyFactory epPolicyTemplateKeyFactory) {
        this.epPolicyTemplateKeyFactory = Preconditions.checkNotNull(epPolicyTemplateKeyFactory, "epPolicyTemplateKeyFactory can not be null");
        this.epPolicyTemplateDao = Preconditions.checkNotNull(epPolicyTemplateDao, "epPolicyTemplateDao can not be null");
    }

    @Override
    public Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> buildAddressEndpointAugmentation(
            AddressEndpointReg input) {
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> buildContainmentEndpointAugmentation(
            ContainmentEndpointReg input) {
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>> buildAddressEndpointWithLocationAugmentation(
            AddressEndpoint input) {
        final EpPolicyTemplateValueKey templateValueKey = epPolicyTemplateKeyFactory.createKey(
                input.getTenant(), input.getEndpointGroup(), input.getCondition());

        final Collection<EndpointPolicyTemplateBySgt> epPolicyTemplates = epPolicyTemplateDao.readBy(templateValueKey);
        final Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>> entry;

        if (epPolicyTemplates.isEmpty()) {
            entry = null;
        } else {
            //TODO: cover case when multiple templates found
            final Augmentation<AddressEndpointWithLocation> addressEndpointWithLocationAug =
                    new AddressEndpointWithLocationAugBuilder()
                            .setSgt(Iterables.getFirst(epPolicyTemplates, null).getSgt())
                            .build();
            entry = new AbstractMap.SimpleEntry<>(AddressEndpointWithLocationAug.class, addressEndpointWithLocationAug);
        }

        return entry;
    }

    @Override
    public Entry<Class<? extends Augmentation<ContainmentEndpointWithLocation>>, Augmentation<ContainmentEndpointWithLocation>> buildContainmentEndpointWithLocationAugmentation(
            ContainmentEndpoint input) {
        return null;
    }

}
