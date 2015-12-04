package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpRendererAugmentationRegistryImplModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractEpRendererAugmentationRegistryImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(EpRendererAugmentationRegistryImplModule.class);

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
        final DataBroker dataProvider = getDataBrokerDependency();
        final RpcProviderRegistry rpcRegistry = getRpcRegistryDependency();

        EndpointRpcRegistry endpointRpcRegistry = new EndpointRpcRegistry(dataProvider, rpcRegistry);
        LOG.info("{} successfully started.", EpRendererAugmentationRegistryImplModule.class.getCanonicalName());
        return endpointRpcRegistry;
    }

}
