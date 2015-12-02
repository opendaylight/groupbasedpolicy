package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;

public class MockOfContext extends OfContext {

    public MockOfContext(DataBroker dataBroker, PolicyManager policyManager, SwitchManager switchManager,
            EndpointManager endpointManager, ScheduledExecutorService executor) {
        super(dataBroker, policyManager, switchManager, endpointManager, executor);
    }

    public void addTenant(Tenant unresolvedTenant) {
        addTenantAndResolvePolicy(unresolvedTenant);
    }

}
