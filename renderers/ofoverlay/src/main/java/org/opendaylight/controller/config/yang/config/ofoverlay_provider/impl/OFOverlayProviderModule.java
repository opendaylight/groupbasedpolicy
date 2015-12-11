package org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl;


import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFOverlayProviderModule extends org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl.AbstractOFOverlayProviderModule {
    private static final Logger LOG = LoggerFactory
            .getLogger(OFOverlayProviderModule.class);

    public OFOverlayProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OFOverlayProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl.OFOverlayProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("OFoffset: {}",getGbpOfoverlayTableOffset());
        return new OFOverlayRenderer(getDataBrokerDependency(),
                                     getRpcRegistryDependency(),
                                     getNotificationAdapterDependency(),
                                     getEpRendererAugmentationRegistryDependency(),
                                     getPolicyValidatorRegistryDependency(),
                                     getGbpOfoverlayTableOffset().shortValue());
    }

}
