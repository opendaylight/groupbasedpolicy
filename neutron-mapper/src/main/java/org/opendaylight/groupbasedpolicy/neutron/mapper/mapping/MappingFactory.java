/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.endpoints.ExternalGatewayAsEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.endpoints.ExternalGatewayAsEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpointBuilder;


public class MappingFactory {

    private MappingFactory() {
        throw new UnsupportedOperationException("cannot create an instance");
    }

    public static BaseEndpointByPort createBaseEndpointByPort(AddressEndpointKey addrEpKey, UniqueId portId) {
        return new BaseEndpointByPortBuilder().setPortId(portId)
            .setAddressType(addrEpKey.getAddressType())
            .setAddress(addrEpKey.getAddress())
            .setContextType(addrEpKey.getContextType())
            .setContextId(addrEpKey.getContextId())
            .build();
    }

    public static PortByBaseEndpoint createPortByBaseEndpoint(UniqueId portId, AddressEndpointKey addrEpKey) {
        return new PortByBaseEndpointBuilder().setPortId(portId)
            .setAddressType(addrEpKey.getAddressType())
            .setAddress(addrEpKey.getAddress())
            .setContextType(addrEpKey.getContextType())
            .setContextId(addrEpKey.getContextId())
            .build();
    }

    @Deprecated
    public static EndpointByPort createEndpointByPort(EndpointKey epKey, UniqueId portId) {
        return new EndpointByPortBuilder().setPortId(portId)
            .setL2Context(epKey.getL2Context())
            .setMacAddress(epKey.getMacAddress())
            .build();
    }

    @Deprecated
    public static PortByEndpoint createPortByEndpoint(UniqueId portId, EndpointKey epKey) {
        return new PortByEndpointBuilder().setPortId(portId)
            .setL2Context(epKey.getL2Context())
            .setMacAddress(epKey.getMacAddress())
            .build();
    }

    public static ExternalGatewayAsEndpoint createEaxternalGatewayAsEndpoint(ContextId contextId, IpPrefix ipPrefix) {
        return new ExternalGatewayAsEndpointBuilder().setAddress(new String(ipPrefix.getValue()))
            .setAddressType(IpPrefixType.class)
            .setContextId(contextId)
            .setContextType(MappingUtils.L3_CONTEXT)
            .build();
    }

    @Deprecated
    public static ExternalGatewayAsL3Endpoint createExternalGatewayByL3Endpoint(EndpointL3Key epL3Key) {
        return new ExternalGatewayAsL3EndpointBuilder().setKey(
                new ExternalGatewayAsL3EndpointKey(epL3Key.getIpAddress(), epL3Key.getL3Context()))
            .setIpAddress(epL3Key.getIpAddress())
            .setL3Context(epL3Key.getL3Context())
            .build();
    }

}
