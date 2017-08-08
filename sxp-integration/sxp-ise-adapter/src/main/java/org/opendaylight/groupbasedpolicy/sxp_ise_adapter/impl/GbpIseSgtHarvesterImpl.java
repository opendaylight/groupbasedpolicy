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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
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
    public ListenableFuture<Collection<SgtInfo>> harvestAll(@Nonnull final IseContext iseContext) {
        LOG.debug("ise-source: harvestAll {} -> {}", iseContext.getIseSourceConfig().getTenant(),
                iseContext.getIseSourceConfig().getConnectionConfig().getIseRestUrl());
        ListenableFuture<Collection<SgtInfo>> result;
        try {
            final IseSourceConfig iseSourceConfig = iseContext.getIseSourceConfig();
            final ConnectionConfig connectionConfig = iseSourceConfig.getConnectionConfig();
            final WebResource baseWebResource = createWebResource(connectionConfig);

            final WebResource.Builder requestBuilder = RestClientFactory.createRequestBuilder(baseWebResource,
                    connectionConfig.getHeader(), RestClientFactory.PATH_ERS_CONFIG_SGT);
            final String rawSgtSummary = IseReplyUtil.deliverResponse(requestBuilder);

            final List<SgtInfo> sgtInfos = harvestDetails(rawSgtSummary, baseWebResource, connectionConfig, iseContext.getUuidToSgtMap());

            ListenableFuture<Void> processingResult = Futures.immediateCheckedFuture(null);
            for (SgtInfoProcessor processor : sgtInfoProcessors) {
                processingResult = Futures.transformAsync(processingResult, new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(final Void input) throws Exception {
                        LOG.debug("entering stg-info processor {}", processor.getClass().getSimpleName());
                        return processor.processSgtInfo(iseSourceConfig.getTenant(), sgtInfos);
                    }
                }, MoreExecutors.directExecutor());
            }
            result = Futures.transform(processingResult, new Function<Void, Collection<SgtInfo>>() {
                @Nullable
                @Override
                public Collection<SgtInfo> apply(@Nullable final Void input) {
                    // update uuid map
                    for (SgtInfo sgtInfo : sgtInfos) {
                        iseContext.getUuidToSgtMap().put(sgtInfo.getUuid(), sgtInfo.getSgt().getValue());
                    }
                    //TODO: store harvest stats to DS/operational
                    // always success, otherwise there will be TransactionCommitFailedException thrown
                    return sgtInfos;
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            LOG.debug("failed to harvest ise", e);
            result = Futures.immediateFailedFuture(e);
        }

        return result;
    }

    private WebResource createWebResource(final ConnectionConfig connectionConfig) throws GeneralSecurityException {
        final Client iseClient = RestClientFactory.createIseClient(connectionConfig);
        return iseClient.resource(connectionConfig.getIseRestUrl().getValue());
    }

    private List<SgtInfo> harvestDetails(final String rawSgtSummary, final WebResource baseWebResource,
                                         final ConnectionConfig connectionConfig, final Map<String, Integer> uuidToSgtMap) {
        LOG.trace("rawSgtSummary: {}", rawSgtSummary);
        final List<Future<SgtInfo>> sgtInfoFutureBag = new ArrayList<>();

        // prepare worker pool
        final ExecutorService pool = Executors.newFixedThreadPool(
                10, new ThreadFactoryBuilder().setNameFormat("ise-sgt-worker-%d").build());

        // parse sgtSummary
        final XPath xpath = IseReplyUtil.setupXpath();

        final InputSource inputSource = IseReplyUtil.createInputSource(rawSgtSummary);
        try {
            final NodeList sgtResources = IseReplyUtil.findAllSgtResourceNodes(xpath, inputSource);
            final Collection<Node> sgtLinkNodes = IseReplyUtil.filterNewResourcesByID(uuidToSgtMap, xpath, sgtResources);

            int counter = 0;
            for (Node sgtLinkNode : sgtLinkNodes) {
                final String sgtLinkHrefValue = sgtLinkNode.getNodeValue();
                LOG.debug("found sgt resource: {}", sgtLinkHrefValue);

                // submit all query tasks to pool
                final int idx = counter++;
                sgtInfoFutureBag.add(pool.submit(new Callable<SgtInfo>() {
                    @Override
                    public SgtInfo call() {
                        SgtInfo sgtInfo = null;
                        try {
                            sgtInfo = querySgtDetail(baseWebResource, connectionConfig.getHeader(), xpath, idx, sgtLinkHrefValue);
                        } catch (XPathExpressionException e) {
                            LOG.info("failed to parse sgt response for {}: {}", sgtLinkHrefValue, e.getMessage());
                        }
                        return sgtInfo;
                    }
                }));
            }

            // stop pool
            pool.shutdown();
            final boolean terminated = pool.awaitTermination(1, TimeUnit.MINUTES);
            if (! terminated) {
                LOG.debug("NOT all sgt-detail queries succeeded - timed out");
                pool.shutdownNow();
            }
        } catch (InterruptedException | XPathExpressionException e) {
            LOG.warn("failed to query all-sgt details", e);
        }

        // harvest available details
        return sgtInfoFutureBag.stream()
                .map(this::gainSgtInfoSafely)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SgtInfo gainSgtInfoSafely(final Future<SgtInfo> response) {
        SgtInfo result = null;
        if (response.isDone() && ! response.isCancelled()) {
            try {
                result = response.get();
            } catch (Exception e) {
                LOG.debug("sgt-detail query failed even when future was DONE", e);
            }
        }
        return result;
    }

    private SgtInfo querySgtDetail(final WebResource baseWebResource, final List<Header> headers, final XPath xpath,
                                   final int idx, final String sgtLinkHrefValue) throws XPathExpressionException {
        // query all sgt entries (serial-vise)
        final URI hrefToSgtDetailUri = URI.create(sgtLinkHrefValue);
        final WebResource.Builder requestBuilder = RestClientFactory.createRequestBuilder(baseWebResource, headers,
                hrefToSgtDetailUri.getPath());
        // time consuming operation - wait for rest response
        final String rawSgtDetail = IseReplyUtil.deliverResponse(requestBuilder);
        LOG.trace("rawSgtDetail: {}", rawSgtDetail);

        // process response xml
        final Node sgtNode = IseReplyUtil.findSgtDetailNode(xpath, rawSgtDetail);
        final Node sgtName = IseReplyUtil.gainSgtName(xpath, sgtNode);
        final Node sgtUuid = IseReplyUtil.gainSgtUuid(xpath, sgtNode);
        final Node sgtValue = IseReplyUtil.gainSgtValue(xpath, sgtNode);
        LOG.debug("sgt value [{}]: {} -> {}", idx, sgtValue, sgtName);

        // store replies into list of SgtInfo
        final Sgt sgt = new Sgt(Integer.parseInt(sgtValue.getNodeValue(), 10));
        return new SgtInfo(sgt, sgtName.getNodeValue(), sgtUuid.getNodeValue());
    }

}
