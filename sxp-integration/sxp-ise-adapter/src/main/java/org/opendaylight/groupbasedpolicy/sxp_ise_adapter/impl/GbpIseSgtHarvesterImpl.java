/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.IseReplyUtil;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.util.RestClientFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.connection.config.Header;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Purpose: harvest sgt + names available via ise-rest-api
 */
public class GbpIseSgtHarvesterImpl implements GbpIseSgtHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(GbpIseSgtHarvesterImpl.class);

    private final SgtInfoProcessor[] sgtInfoProcessors;

    /**
     * @param sgtInfoProcessors generator delegate
     */
    public GbpIseSgtHarvesterImpl(final SgtInfoProcessor... sgtInfoProcessors) {
        this.sgtInfoProcessors = sgtInfoProcessors;
    }

    @Override
    public ListenableFuture<Integer> harvest(@Nonnull final IseSourceConfig configuration) {
        final ConnectionConfig connectionConfig = configuration.getConnectionConfig();
        ListenableFuture<Integer> result;
        try {
            final Client iseClient = RestClientFactory.createIseClient(connectionConfig);
            final WebResource baseWebResource = iseClient.resource(connectionConfig.getIseRestUrl().getValue());

            final WebResource.Builder requestBuilder = RestClientFactory.createRequestBuilder(baseWebResource,
                    connectionConfig.getHeader(), RestClientFactory.PATH_ERS_CONFIG_SGT);
            final String rawSgtSummary = IseReplyUtil.deliverResponse(requestBuilder);

            final List<SgtInfo> sgtInfos = harvestDetails(rawSgtSummary, baseWebResource, connectionConfig.getHeader());

            ListenableFuture<Void> processingResult = Futures.immediateCheckedFuture(null);
            for (SgtInfoProcessor processor : sgtInfoProcessors) {
                processingResult = Futures.transform(processingResult, new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(final Void input) throws Exception {
                        LOG.debug("entering stg-info processor {}", processor.getClass().getSimpleName());
                        return processor.processSgtInfo(configuration.getTenant(), sgtInfos);
                    }
                });
            }
            result = Futures.transform(processingResult, new Function<Void, Integer>() {
                @Nullable
                @Override
                public Integer apply(@Nullable final Void input) {
                    // always success, otherwise there will be TransactionCommitFailedException thrown
                    return sgtInfos.size();
                }
            });
        } catch (Exception e) {
            LOG.debug("failed to harvest ise", e);
            result = Futures.immediateFailedFuture(e);
        }

        return result;
    }

    private List<SgtInfo> harvestDetails(final String rawSgtSummary, final WebResource baseWebResource, final List<Header> headers) {
        LOG.trace("rawSgtSummary: {}", rawSgtSummary);
        final List<SgtInfo> sgtInfos = new ArrayList<>();

        // parse sgtSummary
        final XPath xpath = IseReplyUtil.setupXpath();

        InputSource inputSource = IseReplyUtil.createInputSource(rawSgtSummary);
        try {
            final NodeList sgtLinkNodes = (NodeList) xpath.evaluate(IseReplyUtil.EXPRESSION_SGT_ALL_LINK_HREFS, inputSource,
                    XPathConstants.NODESET);
            for (int i = 0; i < sgtLinkNodes.getLength(); i++) {
                final String sgtLinkHrefValue = sgtLinkNodes.item(i).getNodeValue();
                LOG.debug("found sgt resource [{}]: {}", i, sgtLinkHrefValue);

                // query all sgt entries (serial-vise)
                final URI hrefToSgtDetailUri = URI.create(sgtLinkHrefValue);
                final WebResource.Builder requestBuilder = RestClientFactory.createRequestBuilder(baseWebResource, headers, hrefToSgtDetailUri.getPath());
                final String rawSgtDetail = IseReplyUtil.deliverResponse(requestBuilder);
                LOG.trace("rawSgtDetail: {}", rawSgtDetail);

                final Node sgtNode = (Node) xpath.evaluate(IseReplyUtil.EXPRESSION_SGT_DETAIL, IseReplyUtil.createInputSource(rawSgtDetail),
                        XPathConstants.NODE);
                final Node sgtName = (Node) xpath.evaluate(IseReplyUtil.EXPRESSION_SGT_NAME_ATTR, sgtNode, XPathConstants.NODE);
                final Node sgtValue = (Node) xpath.evaluate(IseReplyUtil.EXPRESSION_SGT_VALUE, sgtNode, XPathConstants.NODE);
                LOG.debug("sgt value [{}]: {} -> {}", i, sgtValue, sgtName);

                // store replies into list of SgtInfo
                final Sgt sgt = new Sgt(Integer.parseInt(sgtValue.getNodeValue(), 10));
                final SgtInfo sgtInfo = new SgtInfo(sgt, sgtName.getNodeValue());
                sgtInfos.add(sgtInfo);
            }
        } catch (XPathExpressionException e) {
            LOG.warn("failed to parse all-sgt response", e);
        }

        return sgtInfos;
    }

}
