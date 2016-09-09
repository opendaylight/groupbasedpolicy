/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronFloatingIpAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronNetworkAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronPortAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronRouterAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSubnetAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule.NeutronSecurityRuleAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.NeutronBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.SecurityGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.groups.attributes.security.groups.SecurityGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRulesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class NeutronMapper implements DataTreeChangeListener<Neutron>, AutoCloseable {

    public static final String EXC_MSG_UNKNOWN_MODIFICATION_TYPE_WITHIN_DATA = "Unknown modification type within data ";

    private final static SecurityRuleBuilder EIG_INGRESS_IPV4_SEC_RULE_BUILDER = new SecurityRuleBuilder()
        .setUuid(new Uuid("0a629f80-2408-11e6-b67b-9e71128cae77"))
        .setDirection(DirectionIngress.class)
        .setEthertype(EthertypeV4.class)
        .setSecurityGroupId(MappingUtils.EIG_UUID);
    private final static SecurityRuleBuilder EIG_EGRESS_IPV4_SEC_RULE_BUILDER = new SecurityRuleBuilder()
        .setUuid(new Uuid("0f1789be-2408-11e6-b67b-9e71128cae77"))
        .setDirection(DirectionEgress.class)
        .setEthertype(EthertypeV4.class)
        .setSecurityGroupId(MappingUtils.EIG_UUID);
    private final static SecurityRuleBuilder EIG_INGRESS_IPV6_SEC_RULE_BUILDER = new SecurityRuleBuilder()
        .setUuid(new Uuid("139b7f90-2408-11e6-b67b-9e71128cae77"))
        .setDirection(DirectionIngress.class)
        .setEthertype(EthertypeV6.class)
        .setSecurityGroupId(MappingUtils.EIG_UUID);
    private final static SecurityRuleBuilder EIG_EGRESS_IPV6_SEC_RULE_BUILDER = new SecurityRuleBuilder()
        .setUuid(new Uuid("17517202-2408-11e6-b67b-9e71128cae77"))
        .setDirection(DirectionEgress.class)
        .setEthertype(EthertypeV6.class)
        .setSecurityGroupId(MappingUtils.EIG_UUID);
    private final static SecurityGroupBuilder EIG_SEC_GROUP_BUILDER =
            new SecurityGroupBuilder().setUuid(MappingUtils.EIG_UUID);

    private final NeutronNetworkAware networkAware;
    private final NeutronSecurityGroupAware securityGroupAware;
    private final NeutronSecurityRuleAware securityRuleAware;
    private final NeutronSubnetAware subnetAware;
    private final NeutronPortAware portAware;
    private final NeutronRouterAware routerAware;
    private final NeutronFloatingIpAware floatingIpAware;

    private final ListenerRegistration<NeutronMapper> registerDataTreeChangeListener;
    private Neutron neutronBefore;
    private Neutron neutronAfter;

    public NeutronMapper(DataBroker dataProvider, EndpointService epService,
            BaseEndpointService baseEpService) {
        EndpointRegistrator epRegistrator = new EndpointRegistrator(epService, baseEpService);
        networkAware = new NeutronNetworkAware(dataProvider);
        securityGroupAware = new NeutronSecurityGroupAware(dataProvider);
        securityRuleAware = new NeutronSecurityRuleAware(dataProvider);
        subnetAware = new NeutronSubnetAware(dataProvider, epRegistrator);
        portAware = new NeutronPortAware(dataProvider, epRegistrator);
        routerAware = new NeutronRouterAware(dataProvider, epRegistrator);
        floatingIpAware = new NeutronFloatingIpAware(dataProvider);
        registerDataTreeChangeListener =
                dataProvider.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Neutron.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Neutron>> changes) {
        for (DataTreeModification<Neutron> change : changes) {
            DataObjectModification<Neutron> neutronModif = change.getRootNode();
            resolveAndSetNeutron(neutronModif);
            // network
            List<DataObjectModification<Network>> networkModifs =
                    findModifiedData(NeutronNetworkAware.NETWORK_WILDCARD_IID, neutronModif);
            for (Uuid tenantFromCreatedRouterExternalNetwork : filterCreatedRouterExternalNetworksAndTransformToTenants(
                    networkModifs)) {
                SecurityGroup eigSecGroup =
                        EIG_SEC_GROUP_BUILDER.setTenantId(tenantFromCreatedRouterExternalNetwork).build();
                securityGroupAware.onCreated(eigSecGroup, neutronAfter);
                List<SecurityRule> eigSecRules = createEigSecurityRules(tenantFromCreatedRouterExternalNetwork);
                for (SecurityRule eigSecRule : eigSecRules) {
                    securityRuleAware.onCreated(eigSecRule, neutronAfter);
                }
            }
            onDataObjectModification(networkModifs, networkAware);
            // security group
            List<DataObjectModification<SecurityGroup>> secGroupModifs =
                    findModifiedData(NeutronSecurityGroupAware.SECURITY_GROUP_WILDCARD_IID, neutronModif);
            onDataObjectModification(secGroupModifs, securityGroupAware);
            // security rules
            List<DataObjectModification<SecurityRule>> secRuleModifs =
                    findModifiedData(NeutronSecurityRuleAware.SECURITY_RULE_WILDCARD_IID, neutronModif);
            onDataObjectModification(secRuleModifs, securityRuleAware);
            // subnet
            List<DataObjectModification<Subnet>> subnetModifs = findModifiedData(NeutronSubnetAware.SUBNET_WILDCARD_IID, neutronModif);
            onDataObjectModification(subnetModifs, subnetAware);
            // port
            List<DataObjectModification<Port>> portModifs = findModifiedData(NeutronPortAware.PORT_WILDCARD_IID, neutronModif);
            onDataObjectModification(portModifs, portAware);
            // router
            List<DataObjectModification<Router>> routerModifs = findModifiedData(NeutronRouterAware.ROUTER_WILDCARD_IID, neutronModif);
            onDataObjectModification(routerModifs, routerAware);
            // floating IP
            List<DataObjectModification<Floatingip>> floatingIpModifs = findModifiedData(NeutronFloatingIpAware.FLOATING_IP_WILDCARD_IID, neutronModif);
            onDataObjectModification(floatingIpModifs, floatingIpAware);
        }
    }

    private <T extends DataObject> void onDataObjectModification(List<DataObjectModification<T>> dataModifs,
            NeutronAware<T> neutronAware) {
        for (DataObjectModification<T> dataModif : dataModifs) {
            switch (dataModif.getModificationType()) {
                case DELETE:
                    neutronAware.onDeleted(dataModif.getDataBefore(), neutronBefore, neutronAfter);
                    break;
                case SUBTREE_MODIFIED:
                    neutronAware.onUpdated(dataModif.getDataBefore(), dataModif.getDataAfter(), neutronBefore,
                            neutronAfter);
                    break;
                case WRITE:
                    if (dataModif.getDataBefore() == null) {
                        neutronAware.onCreated(dataModif.getDataAfter(), neutronAfter);
                    } else {
                        neutronAware.onUpdated(dataModif.getDataBefore(), dataModif.getDataAfter(), neutronBefore,
                                neutronAfter);
                    }
                    break;
                default:
                    throw new IllegalStateException(EXC_MSG_UNKNOWN_MODIFICATION_TYPE_WITHIN_DATA + dataModif);
            }
        }
    }

    private Set<Uuid> filterCreatedRouterExternalNetworksAndTransformToTenants(
            List<DataObjectModification<Network>> modifiedNetworks) {
        return FluentIterable.from(modifiedNetworks).filter(new Predicate<DataObjectModification<Network>>() {

            @Override
            public boolean apply(DataObjectModification<Network> modifiedNetwork) {
                return (ModificationType.WRITE == modifiedNetwork.getModificationType()
                        && NetworkUtils.isRouterExternal(modifiedNetwork.getDataAfter()));
            }
        }).transform(new Function<DataObjectModification<Network>, Uuid>() {

            @Override
            public Uuid apply(DataObjectModification<Network> modifiedNetwork) {
                return modifiedNetwork.getDataAfter().getTenantId();
            }
        }).toSet();
    }

    private void resolveAndSetNeutron(DataObjectModification<Neutron> originalNeutron) {
        Neutron oldNeutronBefore = originalNeutron.getDataBefore();
        neutronBefore = resolveAndCreateNewNeutron(oldNeutronBefore);
        Neutron oldNeutronAfter = originalNeutron.getDataAfter();
        neutronAfter = resolveAndCreateNewNeutron(oldNeutronAfter);
    }

    private @Nullable Neutron resolveAndCreateNewNeutron(@Nullable Neutron originalNeutron) {
        if (originalNeutron == null) {
            return null;
        }
        NeutronBuilder newNeutronBuilder = new NeutronBuilder(originalNeutron);
        resolveAndAddSecurityRulesAndGroups(originalNeutron, newNeutronBuilder);
        return newNeutronBuilder.build();
    }

    private void resolveAndAddSecurityRulesAndGroups(Neutron originalNeutron, NeutronBuilder newNeutronBuilder) {
        List<SecurityRule> eigSecRulesAndOriginalSecRules = new ArrayList<>();
        List<SecurityGroup> eigSecGroupAndOriginalSecGroup = new ArrayList<>();
        // resolve EIG sec rules and groups
        List<Network> routerExternalNetworks = NetworkUtils.findRouterExternalNetworks(originalNeutron.getNetworks());
        Set<Uuid> tenantsFromRouterExternalNetwork = resolveTenantsFromNetworks(routerExternalNetworks);
        for (Uuid tenantFromRouterExternalNetwork : tenantsFromRouterExternalNetwork) {
            eigSecRulesAndOriginalSecRules.addAll(createEigSecurityRules(tenantFromRouterExternalNetwork));
            eigSecGroupAndOriginalSecGroup
                .add(EIG_SEC_GROUP_BUILDER.setTenantId(tenantFromRouterExternalNetwork).build());
        }
        // set new sec rules
        SecurityRules newSecRules = null;
        if (originalNeutron.getSecurityRules() != null) {
            List<SecurityRule> originalSecRules = originalNeutron.getSecurityRules().getSecurityRule();
            if (originalSecRules != null) {
                eigSecRulesAndOriginalSecRules.addAll(originalSecRules);
            }
            newSecRules = new SecurityRulesBuilder(originalNeutron.getSecurityRules())
                .setSecurityRule(eigSecRulesAndOriginalSecRules).build();
        } else {
            newSecRules = new SecurityRulesBuilder().setSecurityRule(eigSecRulesAndOriginalSecRules).build();
        }
        newNeutronBuilder.setSecurityRules(newSecRules);
        // set new sec groups
        SecurityGroups newSecGroups = null;
        if (originalNeutron.getSecurityGroups() != null) {
            List<SecurityGroup> originalSecGroups = originalNeutron.getSecurityGroups().getSecurityGroup();
            if (originalSecGroups != null) {
                eigSecGroupAndOriginalSecGroup.addAll(originalSecGroups);
            }
            newSecGroups = new SecurityGroupsBuilder(originalNeutron.getSecurityGroups())
                .setSecurityGroup(eigSecGroupAndOriginalSecGroup).build();
        } else {
            newSecGroups = new SecurityGroupsBuilder().setSecurityGroup(eigSecGroupAndOriginalSecGroup).build();
        }
        newNeutronBuilder.setSecurityGroups(newSecGroups);
    }

    private Set<Uuid> resolveTenantsFromNetworks(List<Network> networks) {
        return FluentIterable.from(networks).transform(new Function<Network, Uuid>() {

            @Override
            public Uuid apply(Network network) {
                return network.getTenantId();
            }
        }).toSet();
    }

    private List<SecurityRule> createEigSecurityRules(Uuid tenant) {
        List<SecurityRule> eigSecRules = new ArrayList<>();
        eigSecRules.add(EIG_INGRESS_IPV4_SEC_RULE_BUILDER.setTenantId(tenant).build());
        eigSecRules.add(EIG_EGRESS_IPV4_SEC_RULE_BUILDER.setTenantId(tenant).build());
        eigSecRules.add(EIG_INGRESS_IPV6_SEC_RULE_BUILDER.setTenantId(tenant).build());
        eigSecRules.add(EIG_EGRESS_IPV6_SEC_RULE_BUILDER.setTenantId(tenant).build());
        return eigSecRules;
    }

    /**
     * Finds all modified subnodes of given type in {@link Neutron} node.
     *
     * @param <T>
     * @param iid path to data in root node
     * @param rootNode modified data of {@link Neutron} node
     * @return {@link List} of modified subnodes
     */
    private <T extends DataObject> List<DataObjectModification<T>> findModifiedData(InstanceIdentifier<T> iid,
            DataObjectModification<Neutron> rootNode) {
        List<DataObjectModification<T>> modDtos = new ArrayList<>();
        PeekingIterator<PathArgument> pathArgs = Iterators.peekingIterator(iid.getPathArguments().iterator());
        DataObjectModification<? extends DataObject> modifDto = rootNode;
        while (pathArgs.hasNext()) {
            pathArgs.next();
            for (DataObjectModification<? extends DataObject> childDto : modifDto.getModifiedChildren()) {
                if (pathArgs.hasNext() && childDto.getDataType().equals(pathArgs.peek().getType())) {
                    if (childDto.getDataType().equals(iid.getTargetType())) {
                        modDtos.add((DataObjectModification<T>) childDto);
                    } else {
                        modifDto = childDto;
                        break;
                    }
                }
            }
        }
        return modDtos;
    }

    @Override
    public void close() {
        registerDataTreeChangeListener.close();
    }

}
