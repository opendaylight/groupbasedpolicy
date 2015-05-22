package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronOvsdb implements AutoCloseable {

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private final TerminationPointDataChangeListener tpListener;
    private final NodeDataChangeListener nodeListener;
    private final NeutronGbpMapperListener neutronGbpMapperListener;

    public NeutronOvsdb(DataBroker dataProvider, RpcProviderRegistry rpcProvider, BundleContext context) {
        checkNotNull(dataProvider);
        checkNotNull(rpcProvider);
        checkNotNull(context);

        EndpointService epService = rpcProvider.getRpcService(EndpointService.class);
        tpListener = new TerminationPointDataChangeListener(dataProvider, epService);
        nodeListener = new NodeDataChangeListener(dataProvider);
        neutronGbpMapperListener = new NeutronGbpMapperListener(dataProvider);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
    }

}
