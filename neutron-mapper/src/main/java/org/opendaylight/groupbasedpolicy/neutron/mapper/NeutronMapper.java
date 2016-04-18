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
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronListener;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.NeutronGbpMapperService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.Floatingips;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;

public class NeutronMapper implements AutoCloseable {

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private final DataBroker dataProvider;
    private final RpcProviderRegistry providerRegistry;
    private final EndpointService epService;
    private RpcRegistration<NeutronGbpMapperService> rpcRegistration;
    private NeutronListener neutronListener;

    public NeutronMapper(DataBroker dataProvider, RpcProviderRegistry rpcProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        this.providerRegistry = checkNotNull(rpcProvider);
        this.epService = rpcProvider.getRpcService(EndpointService.class);
        neutronListener = new NeutronListener(dataProvider);
        registerAwareProviders();
    }

    private void registerAwareProviders() {
        SecGroupDao secGroupDao = new SecGroupDao();
        SecRuleDao secRuleDao = new SecRuleDao();
        NeutronNetworkDao networkDao = new NeutronNetworkDao();
        NeutronSecurityRuleAware securityRuleAware = new NeutronSecurityRuleAware(dataProvider, secRuleDao, secGroupDao);
        NeutronSecurityGroupAware securityGroupAware = new NeutronSecurityGroupAware(dataProvider, securityRuleAware, secGroupDao);
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(Floatingips.class).child(Floatingip.class).build(),
                new NeutronFloatingIpAware(dataProvider));
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(Ports.class).child(Port.class).build(),
                new NeutronPortAware(dataProvider, epService, securityRuleAware, securityGroupAware));
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(Routers.class).child(Router.class).build(),
                new NeutronRouterAware(dataProvider, epService));
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(SecurityRules.class).child(SecurityRule.class).build(),
                securityRuleAware);
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(SecurityGroups.class).child(SecurityGroup.class).build(),
                securityGroupAware);
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(Subnets.class).child(Subnet.class).build(),
                new NeutronSubnetAware(dataProvider, networkDao));
        neutronListener.registerMappingProviders(
                InstanceIdentifier.builder(Neutron.class).child(Networks.class).child(Network.class).build(),
                new NeutronNetworkAware(dataProvider, securityGroupAware, networkDao));
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
        if (neutronListener != null) {
            neutronListener.close();
            neutronListener = null;
        }
        rpcRegistration.close();
    }

}
