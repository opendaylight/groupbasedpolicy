/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ip.sgt.distribution.service.cfg;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.ip.sgt.distribution.service.impl.IpSgtDistributionServiceImpl;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.IpSgtDistributionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.RemoveIpSgtBindingFromPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SendIpSgtBindingToPeerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpSgtDistributionServiceInstance
        implements ClusterSingletonService, IpSgtDistributionService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IpSgtDistributionServiceInstance.class);
    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private IpSgtDistributionServiceImpl ipSgtDistributionServiceImpl;
    private final DataBroker dataBroker;
    private final SxpControllerService sxpService;
    private final IpAddress sourceIp;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private final RpcProviderRegistry rpcProviderRegistry;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private BindingAwareBroker.RpcRegistration<IpSgtDistributionService> rpcRegistration;

    public IpSgtDistributionServiceInstance(final DataBroker dataBroker,
                                            final String sourceIp, final ClusterSingletonServiceProvider clusterSingletonService,
                                            final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.sxpService = Preconditions.checkNotNull(rpcProviderRegistry.getRpcService(SxpControllerService.class));
        this.sourceIp = new IpAddress(Preconditions.checkNotNull(sourceIp.toCharArray()));
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e);
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public Future<RpcResult<Void>> removeIpSgtBindingFromPeer(RemoveIpSgtBindingFromPeerInput input) {
        return ipSgtDistributionServiceImpl.removeIpSgtBindingFromPeer(input);
    }

    @Override
    public Future<RpcResult<Void>> sendIpSgtBindingToPeer(SendIpSgtBindingToPeerInput input) {
        return ipSgtDistributionServiceImpl.sendIpSgtBindingToPeer(input);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
            ipSgtDistributionServiceImpl.close();
            rpcRegistration.close();
        } catch (Exception e) {
            LOG.error("Closing {} wasnt succesfull", ipSgtDistributionServiceImpl.getClass().getSimpleName());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void instantiateServiceInstance() {
        ipSgtDistributionServiceImpl = new IpSgtDistributionServiceImpl(dataBroker, sxpService, sourceIp);
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(IpSgtDistributionService.class, this);
    }

}
