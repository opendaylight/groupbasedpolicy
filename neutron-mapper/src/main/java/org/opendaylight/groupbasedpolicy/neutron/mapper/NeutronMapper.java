/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronFloatingIpAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronNetworkAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronNetworkDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronPortAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronRouterAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSubnetAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.SecGroupDao;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronGbpMapperServiceImpl;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.SecRuleDao;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.INeutronSecurityGroupAware;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.NeutronGbpMapperService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronMapper implements AutoCloseable {

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private final DataBroker dataProvider;
    private final RpcProviderRegistry providerRegistry;
    private final BundleContext context;
    private final EndpointService epService;
    private RpcRegistration<NeutronGbpMapperService> rpcRegistration;

    public NeutronMapper(DataBroker dataProvider, RpcProviderRegistry rpcProvider, BundleContext context) {
        this.dataProvider = checkNotNull(dataProvider);
        this.providerRegistry = checkNotNull(rpcProvider);
        this.context = checkNotNull(context);
        this.epService = rpcProvider.getRpcService(EndpointService.class);
        registerAwareProviders();
    }

    private void registerAwareProviders() {
        SecGroupDao secGroupDao = new SecGroupDao();
        SecRuleDao secRuleDao = new SecRuleDao();
        NeutronNetworkDao networkDao = new NeutronNetworkDao();

        ServiceRegistration<INeutronSubnetAware> neutronSubnetAwareRegistration =
                context.registerService(INeutronSubnetAware.class, new NeutronSubnetAware(dataProvider, networkDao), null);
        registrations.add(neutronSubnetAwareRegistration);

        NeutronSecurityRuleAware securityRuleAware = new NeutronSecurityRuleAware(dataProvider, secRuleDao, secGroupDao);
        ServiceRegistration<INeutronSecurityRuleAware> neutronSecurityRuleAwareRegistration =
                context.registerService(INeutronSecurityRuleAware.class, securityRuleAware, null);
        registrations.add(neutronSecurityRuleAwareRegistration);

        NeutronSecurityGroupAware securityGroupAware = new NeutronSecurityGroupAware(dataProvider, securityRuleAware, secGroupDao);
        ServiceRegistration<INeutronSecurityGroupAware> neutronSecurityGroupAwareRegistration =
                context.registerService(INeutronSecurityGroupAware.class, securityGroupAware, null);
        registrations.add(neutronSecurityGroupAwareRegistration);

        ServiceRegistration<INeutronNetworkAware> neutronNetworkAwareRegistration = context.registerService(
                INeutronNetworkAware.class, new NeutronNetworkAware(dataProvider, securityGroupAware, networkDao), null);
        registrations.add(neutronNetworkAwareRegistration);

        NeutronPortAware portAware =
                new NeutronPortAware(dataProvider, epService, securityRuleAware, securityGroupAware);
        ServiceRegistration<INeutronPortAware> neutronPortAwareRegistration =
                context.registerService(INeutronPortAware.class, portAware, null);
        registrations.add(neutronPortAwareRegistration);

        NeutronRouterAware routerAware = new NeutronRouterAware(dataProvider, epService);
        ServiceRegistration<INeutronRouterAware> neutronRouterAwareRegistration =
                context.registerService(INeutronRouterAware.class, routerAware, null);
        registrations.add(neutronRouterAwareRegistration);

        ServiceRegistration<INeutronFloatingIPAware> neutronFloatingIpAwareRegistration = context
            .registerService(INeutronFloatingIPAware.class, new NeutronFloatingIpAware(dataProvider), null);
        registrations.add(neutronFloatingIpAwareRegistration);

        NeutronGbpMapperService neutronGbpMapperService = new NeutronGbpMapperServiceImpl(dataProvider, securityRuleAware);
        rpcRegistration = providerRegistry.addRpcImplementation(NeutronGbpMapperService.class, neutronGbpMapperService);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
        rpcRegistration.close();
    }

}
