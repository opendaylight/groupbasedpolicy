/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.collect.Range;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.IseReplyUtil;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.RestClientFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Purpose: query ise in order to get name of sgt for given tenant and build {@link EndpointPolicyTemplateBySgt}
 */
public class EPPolicyTemplateProviderIseImpl implements EPPolicyTemplateProviderFacade {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateProviderIseImpl.class);

    private Optional<IseSourceConfig> iseSourceConfig = Optional.empty();

    @Override
    public Optional<EndpointPolicyTemplateBySgt> provideTemplate(@Nonnull final Sgt sgt) {
        return findIseSourceConfigBySgt(sgt)
                .flatMap(iseSourceConfig -> queryIseOnSgt(iseSourceConfig.getConnectionConfig(), sgt)
                        .map(sgtName -> buildTemplate(sgt, iseSourceConfig.getTenant(), sgtName)));
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

    private Optional<String> queryIseOnSgt(final ConnectionConfig connectionConfig, final Sgt sgt) {
        String sgtName = null;
        try {
            final Client iseClient = RestClientFactory.createIseClient(connectionConfig);
            final WebResource baseWebResource = iseClient.resource(connectionConfig.getIseRestUrl().getValue());

            final String pathToSgtDetail = String.format("%s/%d", RestClientFactory.PATH_ERS_CONFIG_SGT, sgt.getValue());
            final WebResource.Builder requestBuilder = RestClientFactory.createRequestBuilder(baseWebResource,
                    connectionConfig.getHeader(), pathToSgtDetail);
            final String rawSgtDetail = IseReplyUtil.deliverResponse(requestBuilder);

            final XPath xpath = IseReplyUtil.setupXpath();
            final Node sgtNameNode = (Node) xpath.evaluate(
                    IseReplyUtil.EXPRESSION_SGT_NAME_ATTR, IseReplyUtil.createInputSource(rawSgtDetail),
                    XPathConstants.NODE);
            sgtName = sgtNameNode.getNodeValue();
            LOG.debug("obtained sgt/name: {}/{}", sgt.getValue(), sgtName);
        } catch (Exception e) {
            LOG.debug("failed to read sgt detail on ISE", e);
        }
        return Optional.ofNullable(sgtName);
    }

    private Optional<IseSourceConfig> findIseSourceConfigBySgt(final Sgt sgt) {
        //TODO: cover multiple sources / tenants .. lookup (option: multiple servers per tenant?)
        return iseSourceConfig
                .filter(config ->
                        Range.closed(config.getSgtRangeMin().getValue(), config.getSgtRangeMax().getValue())
                                .contains(sgt.getValue()));
    }

    @Override
    public void assignIseSourceConfig(final @Nullable IseSourceConfig iseSourceConfig) {
        this.iseSourceConfig = Optional.ofNullable(iseSourceConfig);
    }
}
