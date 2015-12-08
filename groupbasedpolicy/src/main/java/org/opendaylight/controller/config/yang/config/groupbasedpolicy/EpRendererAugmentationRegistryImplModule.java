package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;

public class EpRendererAugmentationRegistryImplModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractEpRendererAugmentationRegistryImplModule {
    public EpRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public EpRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.EpRendererAugmentationRegistryImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataProvider = Preconditions.checkNotNull(getDataBrokerDependency());
        final RpcProviderRegistry rpcRegistry = Preconditions.checkNotNull(getRpcRegistryDependency());

        return new EndpointRpcRegistry(dataProvider, rpcRegistry);
    }

}
