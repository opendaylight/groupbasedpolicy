package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3NatInput;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class OfOverlayL3NatAug implements EpRendererAugmentation, AutoCloseable {

    public OfOverlayL3NatAug(DataBroker dataProvider, RpcProviderRegistry rpcRegistry) {
        EndpointRpcRegistry.register(dataProvider, rpcRegistry, this);
    }

    @Override
    public Augmentation<Endpoint> buildEndpointAugmentation(RegisterEndpointInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Augmentation<EndpointL3> buildEndpointL3Augmentation(RegisterEndpointInput input) {
        if (input.getAugmentation(OfOverlayL3NatInput.class) != null) {
            return new OfOverlayL3NatBuilder(input.getAugmentation(OfOverlayL3NatInput.class)).build();
        }
        return null;
    }

    @Override
    public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() throws Exception {
        EndpointRpcRegistry.unregister(this);
    }
}
