/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.dto.ConsEpgKey;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.groupbasedpolicy.dto.ProvEpgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.FlowCacheCons;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.IidSflowNameUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class OFStatisticsManager implements AutoCloseable {

    // key is String (not a full IpAddress) because
    // we will get String from REST query to sFlow
    private static ConcurrentMap<String, EndpointL3> endpointL3ByIpMap = new ConcurrentHashMap<>();
    private static final int CONNECT_TIMEOUT_MILLISEC = 20000;
    private static final int READ_TIMEOUT_MILLISEC = 30000;

    private static final Logger LOG = LoggerFactory.getLogger(OFStatisticsManager.class);

    private final ScheduledExecutorService executor;
    private final StatisticsManager statisticsManager;
    private final Set<String> flowCacheNames = new HashSet<>();
    private static final SetMultimap<ContractId, Pair<ConsEpgKey, ProvEpgKey>> epgsByContractId = HashMultimap.create();
    private List<ScheduledFuture<?>> collectStatsTasks = new ArrayList<>();

    private static int MAX_FLOWS = 100;
    private static double MIN_VALUE_IN_FLOW = 0.1;
    private static final String AGG_MODE = "sum";
    private Long delay;
    private String sflowCollectorUri;

    public OFStatisticsManager(ScheduledExecutorService executor, StatisticsManager statisticsManager) {
        this.executor = executor;
        this.statisticsManager = statisticsManager;
    }

    public synchronized void pullStatsForClassifier(InstanceIdentifier<Classifier> classifierIid,
            Classifier classifier) {
        Preconditions.checkNotNull(sflowCollectorUri);
        Preconditions.checkNotNull(delay);
        FlowCache flowCache = FlowCacheFactory.createFlowCache(classifierIid, classifier, FlowCacheCons.Value.BYTES);
        setStatsPulling(flowCache, classifierIid);
        flowCache = FlowCacheFactory.createFlowCache(classifierIid, classifier, FlowCacheCons.Value.FRAMES);
        setStatsPulling(flowCache, classifierIid);
    }

    private void setStatsPulling(FlowCache flowCache, InstanceIdentifier<Classifier> classifierIid) {
        if (flowCache == null) {
            LOG.trace("Flow cache is null for classifier {}", classifierIid);
            return;
        }
        ResolvedPolicyKey resolvedPolicyKey = classifierIid.firstKeyOf(ResolvedPolicy.class);
        ConsEpgKey consEpgKey =
                new EpgKeyDto(resolvedPolicyKey.getConsumerEpgId(), resolvedPolicyKey.getConsumerTenantId());
        ProvEpgKey provEpgKey =
                new EpgKeyDto(resolvedPolicyKey.getProviderEpgId(), resolvedPolicyKey.getProviderTenantId());
        String flowCacheName = flowCache.getName();
        ContractId contractId = IidSflowNameUtil.resolveContractIdFromFlowCacheName(flowCacheName);
        epgsByContractId.put(contractId, Pair.of(consEpgKey, provEpgKey));
        boolean isFlowCacheNew = flowCacheNames.add(flowCacheName);
        if (isFlowCacheNew) {
            SFlowRTConnection sFlowRTConnection = new SFlowRTConnection(executor, sflowCollectorUri, flowCache, new JsonRestClient(sflowCollectorUri, CONNECT_TIMEOUT_MILLISEC,
                    READ_TIMEOUT_MILLISEC));
            ScheduledFuture<?> collectStatsTask = this.executor.scheduleWithFixedDelay(new ReadGbpFlowCacheTask(flowCacheName, sFlowRTConnection,
                    statisticsManager, MAX_FLOWS, MIN_VALUE_IN_FLOW, AGG_MODE), 0, delay, TimeUnit.SECONDS);
            collectStatsTasks.add(collectStatsTask);
        }
    }

    public synchronized static Set<Pair<ConsEpgKey, ProvEpgKey>> getEpgsForContract(ContractId contractId) {
        return epgsByContractId.get(contractId);
    }

    public synchronized void setSflowCollectorUri(String sflowCollectorUri) {
        this.sflowCollectorUri = sflowCollectorUri;
    }

    public synchronized void setDelay(Long delay) {
        this.delay = delay;
    }

    public static EndpointL3 getEndpointL3ForIp(@Nullable String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        return endpointL3ByIpMap.get(ipAddress);
    }

    public static void addL3Endpoint(EndpointL3 endpointL3) {
        endpointL3ByIpMap.put(getStringIpAddress(endpointL3.getIpAddress()), endpointL3);
    }

    public static void removeL3Endpoint(EndpointL3 endpointL3) {
        endpointL3ByIpMap.remove(getStringIpAddress(endpointL3.getIpAddress()));
    }

    private static String getStringIpAddress(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }
        return ipAddress.getIpv6Address().getValue();
    }

    @Override
    public synchronized void close() throws Exception {
        Iterator<ScheduledFuture<?>> tasksIterator = collectStatsTasks.iterator();
        while (tasksIterator.hasNext()) {
            ScheduledFuture<?> scheduledFuture = tasksIterator.next();
            scheduledFuture.cancel(false);
            tasksIterator.remove();
        }
        epgsByContractId.clear();
    }

}
