package org.opendaylight.controller.config.yang.config.openstack_endpoint_provider.impl;

import org.opendaylight.groupbasedpolicy.integration.openstackgbp.OpenstackGbpEndpoint;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.OpenstackEndpointService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class OpenstackEndpointProviderModule
        extends
        org.opendaylight.controller.config.yang.config.openstack_endpoint_provider.impl.AbstractOpenstackEndpointProviderModule {
    public OpenstackEndpointProviderModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OpenstackEndpointProviderModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.openstack_endpoint_provider.impl.OpenstackEndpointProviderModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        return new OpenstackGbpEndpoint(getDataBrokerDependency(),
                getRpcRegistryDependency());

    }

}
