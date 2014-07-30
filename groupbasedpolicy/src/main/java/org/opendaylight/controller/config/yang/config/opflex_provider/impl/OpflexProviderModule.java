package org.opendaylight.controller.config.yang.config.opflex_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.opflex.OpflexConnectionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

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
        DataBroker dataBrokerService = getDataBrokerDependency();

        connectionService.setDataProvider(dataBrokerService);
        final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration =
                dataBrokerService
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        OpflexConnectionService.DISCOVERY_IID,
                        connectionService, DataChangeScope.SUBTREE );

        final class AutoCloseableConnectionService implements AutoCloseable {
            @Override
            public void close() throws Exception {
                connectionService.stopping();
                dataChangeListenerRegistration.close();
            }
        }
        return new AutoCloseableConnectionService();
    }

}
