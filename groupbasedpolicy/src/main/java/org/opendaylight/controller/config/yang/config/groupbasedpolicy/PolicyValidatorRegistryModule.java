package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyValidatorRegistryModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractPolicyValidatorRegistryModule {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyValidatorRegistryModule.class);

    public PolicyValidatorRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PolicyValidatorRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.PolicyValidatorRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataProvider = getDataBrokerDependency();

        PolicyResolver policyResolver = new PolicyResolver(dataProvider);
        LOG.info("{} successfully started.", PolicyValidatorRegistryModule.class.getCanonicalName());
        return policyResolver;
    }

}
