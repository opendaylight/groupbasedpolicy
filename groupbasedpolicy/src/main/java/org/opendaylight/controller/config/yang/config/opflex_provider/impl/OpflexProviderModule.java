package org.opendaylight.controller.config.yang.config.opflex_provider.impl;

import org.opendaylight.groupbasedpolicy.renderer.OpflexConnectionService;

public class OpflexProviderModule extends org.opendaylight.controller.config.yang.config.opflex_provider.impl.AbstractOpflexProviderModule {
    public OpflexProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OpflexProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.opflex_provider.impl.OpflexProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpflexConnectionService connectionService = new OpflexConnectionService();
        connectionService.setDataProvider(getDataBrokerDependency());

        final class AutoCloseableConnectionService implements AutoCloseable {
            @Override
            public void close() throws Exception {
                connectionService.stopping();
            }
        }
        return new AutoCloseableConnectionService();
    }

}
