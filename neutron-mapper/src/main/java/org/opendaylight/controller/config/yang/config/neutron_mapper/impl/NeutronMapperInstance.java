/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.neutron_mapper.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.neutron.mapper.NeutronMapper;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronMapperInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronMapperInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private final RpcProviderRegistry rpcBroker;
    private IpPrefix metadataIpPrefix;
    private int metadataPort;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private NeutronMapper mapper;

    public NeutronMapperInstance(final DataBroker dataBroker,
                                 final RpcProviderRegistry rpcBroker,
                                 final ClusterSingletonServiceProvider clusterSingletonService,
                                 final String metadataIp,
                                 final String metadataPort) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcBroker = Preconditions.checkNotNull(rpcBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        try {
            InetAddress inetAddr = InetAddress.getByName(metadataIp);
            if (inetAddr instanceof Inet4Address) {
                this.metadataIpPrefix = new IpPrefix(new Ipv4Prefix(Preconditions.checkNotNull(metadataIp) + "/32"));
            } else if (inetAddr instanceof Inet6Address) {
                this.metadataIpPrefix = new IpPrefix(new Ipv6Prefix(Preconditions.checkNotNull(metadataIp) + "/128"));
            }
            this.metadataPort = Integer.parseInt(Preconditions.checkNotNull(metadataPort));
            LOG.info("Resolved Metadata CIDR: {} and port {}.", metadataIpPrefix, metadataPort);
        } catch (Exception ex) {
            if (ex instanceof NumberFormatException) {
                LOG.warn("Metadata port cannot be resolved. Provided value: {}. Continue without support for metadata.",
                    metadataPort);
            } else {
                LOG.warn("MetadataIP could not be resolved. Provided value: {}. Continue without support for metadata.",
                    metadataIp);
            }
            this.metadataIpPrefix = null;
        }
    }

    public void instantiate() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
            singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
        }
        catch (Exception e) {
            LOG.warn("Exception while registering candidate ... ", e);
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        final EndpointService epService = rpcBroker.getRpcService(EndpointService.class);
        final BaseEndpointService baseEndpointService = rpcBroker.getRpcService(BaseEndpointService.class);
        mapper = new NeutronMapper(dataBroker, epService, baseEndpointService, metadataIpPrefix, metadataPort);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        mapper.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

}
