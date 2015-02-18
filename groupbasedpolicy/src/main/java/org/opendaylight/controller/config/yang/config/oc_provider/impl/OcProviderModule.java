package org.opendaylight.controller.config.yang.config.oc_provider.impl;

import org.opendaylight.groupbasedpolicy.renderer.oc.OcRenderer;

public class OcProviderModule extends org.opendaylight.controller.config.yang.config.oc_provider.impl.AbstractOcProviderModule {
    public OcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.oc_provider.impl.OcProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new OcRenderer(getDataBrokerDependency(),
                getRpcRegistryDependency());
}

}
