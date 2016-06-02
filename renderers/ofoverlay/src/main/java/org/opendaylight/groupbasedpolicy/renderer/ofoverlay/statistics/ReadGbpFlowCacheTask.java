/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheData;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.SFlowQueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ReadGbpFlowCacheTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ReadGbpFlowCacheTask.class);

    private static final Type LIST_OF_FLOW_CACHE_DATA = new TypeToken<List<FlowCacheData>>() {}.getType();
    private static final Gson GSON = new Gson();

    private final SFlowRTConnection sFlowRTConnection;
    private final StatisticsManager statisticsManager;
    private final String maxFlows;
    private final String minValue;
    private final String aggMode;
    private final String path;

    public ReadGbpFlowCacheTask(String flowCacheName, SFlowRTConnection sFlowRTConnection,
            StatisticsManager statisticsManager, Integer maxFlows, Double minValue, String aggMode) {
        this.path = "/activeflows/ALL/" + checkNotNull(flowCacheName) + "/json";
        this.sFlowRTConnection = checkNotNull(sFlowRTConnection);
        this.statisticsManager = checkNotNull(statisticsManager);
        this.maxFlows = String.valueOf(checkNotNull(maxFlows));
        this.minValue = String.valueOf(checkNotNull(minValue));
        this.aggMode = checkNotNull(aggMode);
    }

    @Override
    public void run() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add(SFlowQueryParams.MAX_FLOWS, maxFlows);
        params.add(SFlowQueryParams.MIN_VALUE, minValue);
        params.add(SFlowQueryParams.AGG_MODE, aggMode);
        JsonRestClientResponse result = sFlowRTConnection.get(path, params);

        if (result != null && result.getJsonResponse() != null) {
            List<FlowCacheData> dataList = GSON.fromJson(result.getJsonResponse(), LIST_OF_FLOW_CACHE_DATA);
            if (dataList == null) {
                LOG.trace("Empty reply, not processing");
                return;
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("Got sFlow reply: {}", result.getJsonResponse());
            }

            if (result.getStatusCode() < 300) {
                sFlowRTConnection.getExecutor().execute((new ProcessDataTask(sFlowRTConnection.getFlowCache(), dataList,
                        BigInteger.valueOf(new Date().getTime()), statisticsManager)));
            } else if (result.getStatusCode() < 400) {
                LOG.warn("Status code {}, not processing data. Response: {}", result.getStatusCode(),
                        result.getClientResponse().toString());
            } else {
                LOG.error("Status code {}, not processing data. Response: {}", result.getStatusCode(),
                        result.getClientResponse().toString());
            }
        }
    }
}
