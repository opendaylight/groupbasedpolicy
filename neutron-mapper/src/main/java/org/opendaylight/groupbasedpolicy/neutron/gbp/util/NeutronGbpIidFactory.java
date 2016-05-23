/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.gbp.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByBaseEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ProviderNetworksAsL2FloodDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ProviderPhysicalNetworksAsL2FloodDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.endpoints.ExternalGatewayAsEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.endpoints.ExternalGatewayAsEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronGbpIidFactory {

    public static InstanceIdentifier<BaseEndpointByPort> baseEndpointByPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(BaseEndpointsByPorts.class)
            .child(BaseEndpointByPort.class, new BaseEndpointByPortKey(portId))
            .build();
    }

    @Deprecated
    public static InstanceIdentifier<EndpointByPort> endpointByPortIid(UniqueId portId) {
        return InstanceIdentifier
            .builder(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByPorts.class)
            .child(EndpointByPort.class, new EndpointByPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<PortByBaseEndpoint> portByBaseEndpointIid(PortByBaseEndpointKey baseEpkey) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(PortsByBaseEndpoints.class)
            .child(PortByBaseEndpoint.class, baseEpkey)
            .build();
    }

    @Deprecated
    public static InstanceIdentifier<PortByEndpoint> portByEndpointIid(L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier
            .builder(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(PortsByEndpoints.class)
            .child(PortByEndpoint.class, new PortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<ExternalGatewayAsEndpoint> externalGatewayAsEndpoint(ContextId contextId,
            IpPrefix ipPrefix, Class<? extends ContextType> contextType) {
        ExternalGatewayAsEndpointKey key = new ExternalGatewayAsEndpointKey(
                new String(ipPrefix.getValue()),
                IpPrefixType.class,
                contextId,
                contextType);
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(ExternalGatewaysAsEndpoints.class)
            .child(ExternalGatewayAsEndpoint.class, key)
            .build();
    }

    @Deprecated
    public static InstanceIdentifier<ExternalGatewayAsL3Endpoint> externalGatewayAsL3Endpoint(L3ContextId l3Context,
            IpAddress ipAddress) {
        return InstanceIdentifier
            .builder(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(ExternalGatewaysAsL3Endpoints.class)
            .child(ExternalGatewayAsL3Endpoint.class, new ExternalGatewayAsL3EndpointKey(ipAddress, l3Context))
            .build();
    }

    public static InstanceIdentifier<ProviderPhysicalNetworkAsL2FloodDomain> providerPhysicalNetworkAsL2FloodDomainIid(
            TenantId tenantId, ContextId domainId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(ProviderNetworksAsL2FloodDomains.class)
            .child(ProviderPhysicalNetworkAsL2FloodDomain.class,
                    new ProviderPhysicalNetworkAsL2FloodDomainKey(domainId, tenantId))
            .build();
    }

    @Deprecated
    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomain> providerPhysicalNetworkAsL2FloodDomainIid(
            TenantId tenantId, L2FloodDomainId l2FdId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(ProviderPhysicalNetworksAsL2FloodDomains.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomain.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomainKey(l2FdId, tenantId))
            .build();
    }
}
