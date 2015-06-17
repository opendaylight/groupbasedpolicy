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
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronFloatingIpAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronNetworkAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronPortAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronRouterAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSubnetAware;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.INeutronSecurityGroupAware;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronMapper implements AutoCloseable {

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    public NeutronMapper(DataBroker dataProvider, RpcProviderRegistry rpcProvider, BundleContext context) {
        checkNotNull(dataProvider);
        checkNotNull(rpcProvider);
        checkNotNull(context);
        EndpointService epService = rpcProvider.getRpcService(EndpointService.class);

        registerAwareProviders(dataProvider, epService, context);
    }

    private void registerAwareProviders(DataBroker dataProvider, EndpointService epService, BundleContext context) {
        ServiceRegistration<INeutronNetworkAware> neutronNetworkAwareRegistration = context.registerService(
                INeutronNetworkAware.class, new NeutronNetworkAware(dataProvider), null);
        registrations.add(neutronNetworkAwareRegistration);

        ServiceRegistration<INeutronSubnetAware> neutronSubnetAwareRegistration = context.registerService(
                INeutronSubnetAware.class, new NeutronSubnetAware(dataProvider), null);
        registrations.add(neutronSubnetAwareRegistration);

        ServiceRegistration<INeutronPortAware> neutronPortAwareRegistration = context.registerService(
                INeutronPortAware.class, new NeutronPortAware(dataProvider, epService), null);
        registrations.add(neutronPortAwareRegistration);

        ServiceRegistration<INeutronSecurityGroupAware> neutronSecurityGroupAwareRegistration = context.registerService(
                INeutronSecurityGroupAware.class, new NeutronSecurityGroupAware(dataProvider), null);
        registrations.add(neutronSecurityGroupAwareRegistration);

        ServiceRegistration<INeutronSecurityRuleAware> neutronSecurityRuleAwareRegistration = context.registerService(
                INeutronSecurityRuleAware.class, new NeutronSecurityRuleAware(dataProvider), null);
        registrations.add(neutronSecurityRuleAwareRegistration);

        NeutronRouterAware.init(dataProvider, epService);
        ServiceRegistration<INeutronRouterAware> neutronRouterAwareRegistration = context.registerService(
                INeutronRouterAware.class, NeutronRouterAware.getInstance(), null);
        registrations.add(neutronRouterAwareRegistration);

        ServiceRegistration<INeutronFloatingIPAware> neutronFloatingIpAwareRegistration = context.registerService(
                INeutronFloatingIPAware.class, new NeutronFloatingIpAware(dataProvider, epService), null);
        registrations.add(neutronFloatingIpAwareRegistration);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
    }

}
