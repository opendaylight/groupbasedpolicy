package org.opendaylight.controller.config.yang.config.opflex_provider.impl;

import org.opendaylight.groupbasedpolicy.renderer.opflex.OpflexRenderer;

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
        // TODO: getRpcRegistryDependency()
        return new OpflexRenderer(getDataBrokerDependency(), null);   
    }

}
