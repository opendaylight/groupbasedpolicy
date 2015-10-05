package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorKey;

public class NetworkClientTest extends GbpDataBrokerTest {

    private final String tenantID = "00000000-0000-0000-0000-000000000001";
    private final SelectorName selector = new SelectorName("dummy-selector");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void instantiate() {
        NetworkClient client = new NetworkClient();
        assertNotNull(client);
    }

    @Test
    public void testWriteNetworkClientEntitiesToTenant() throws Exception {
        //write
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        NetworkClient.writeNetworkClientEntitiesToTenant(new TenantId(tenantID), rwTx);
        rwTx.submit().get();

        //read
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantID, NetworkClient.EPG_ID.getValue());
    }

    @Test
    public void testWriteConsumerNamedSelector() throws Exception {
        //create selector
        ConsumerNamedSelectorBuilder cnsb = new ConsumerNamedSelectorBuilder();
        cnsb.setName(selector).setKey(new ConsumerNamedSelectorKey(selector));

        //write
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        NetworkClient.writeConsumerNamedSelector(new TenantId(tenantID), cnsb.build(), rwTx);
        rwTx.submit().get();

        //read
        PolicyAssert.assertConsumerNamedSelectorExists(dataBroker, tenantID, NetworkClient.EPG_ID.getValue(),
                cnsb.getName().getValue());
    }

    @Test
    public void testWriteConsumerNamedSelectorWithoutKey() throws Exception {
        //create selector
        ConsumerNamedSelectorBuilder cnsb = new ConsumerNamedSelectorBuilder();
        //test exception
        ReadWriteTransaction rwTx = getDataBroker().newReadWriteTransaction();
        exception.expect(IllegalArgumentException.class);
        NetworkClient.writeConsumerNamedSelector(new TenantId(tenantID), cnsb.build(), rwTx);
    }

}
