package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_mapper;
/**
* sxp-mapper impl module
*/
public class SxpMapperProviderModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_mapper.AbstractSxpMapperProviderModule {
    public SxpMapperProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SxpMapperProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_mapper.SxpMapperProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
