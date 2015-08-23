package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.EndpointGroupPairToContractMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.NetworkMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.endpoint.group.pair.to.contract.mappings.EndpointGroupPairToContractMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class NeutronMapperIidFactory {

    public static InstanceIdentifier<NetworkMapping> networkMappingIid(UniqueId networkId) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(NetworkMappings.class)
            .child(NetworkMapping.class, new NetworkMappingKey(networkId))
            .build();
    }

    public static InstanceIdentifier<EndpointGroupPairToContractMapping> endpointGroupPairToContractMappingIid(
            EndpointGroupId providerEpg, EndpointGroupId consumerEpg) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(EndpointGroupPairToContractMappings.class)
            .child(EndpointGroupPairToContractMapping.class,
                    new EndpointGroupPairToContractMappingKey(consumerEpg, providerEpg))
            .build();
    }

}
