package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.faas.test.DataChangeListenerTester;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasEndpointManagerListenerCovrgTest {

    private static final L2BridgeDomainId L_2_BRIDGE_DOMAIN_ID = new L2BridgeDomainId("L2BridgeDomainId");
    private static final MacAddress MAC_ADDRESS = new MacAddress("00:00:00:00:35:02");

    private InstanceIdentifier<Endpoint> epIid;
    private FaasEndpointManagerListener listener;
    private TenantId gbpTenantId = new TenantId("gbpTenantId");
    private SubnetId subnetId = new SubnetId("subnetId");
    private Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private Uuid faasSubnetId = new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private DataChangeListenerTester tester;
    private DataBroker dataProvider;
    private FaasPolicyManager faasPolicyManager;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws ReadFailedException {
        MappedEndpointKey mappedEndpointKey = new MappedEndpointKey(L_2_BRIDGE_DOMAIN_ID, MAC_ADDRESS);

        dataProvider = mock(DataBroker.class);

        WriteTransaction woTx = mock(WriteTransaction.class);
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);

        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(woTx.submit()).thenReturn(futureVoid);
        when(rwTx.submit()).thenReturn(futureVoid);
        doNothing().when(woTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataObject.class));

        CheckedFuture<Optional<MappedEndpoint>, ReadFailedException> futureMappedEndpoint = mock(CheckedFuture.class);
        Optional<MappedEndpoint> optMappedEndpoint = mock(Optional.class);
        when(optMappedEndpoint.isPresent()).thenReturn(false);
        when(futureMappedEndpoint.checkedGet()).thenReturn(optMappedEndpoint);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedEndpointIid(gbpTenantId, mappedEndpointKey))).thenReturn(futureMappedEndpoint);

        when(dataProvider.newWriteOnlyTransaction()).thenReturn(woTx);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);

        epIid = mock(InstanceIdentifier.class);
        faasPolicyManager = spy(new FaasPolicyManager(dataProvider, executor));
        doNothing().when(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
        listener = new FaasEndpointManagerListener(faasPolicyManager, dataProvider, executor);
        tester = new DataChangeListenerTester(listener);
        tester.setRemovedPath(epIid);
    }

    @Test
    public void testOnDataChanged_Endpoint() {
        Endpoint ep = new EndpointBuilder().setTenant(gbpTenantId)
            .setL2Context(L_2_BRIDGE_DOMAIN_ID)
            .setMacAddress(MAC_ADDRESS)
            .build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

    @Test
    public void testOnDataChanged_EndpointL3() {
        EndpointL3 ep = new EndpointL3Builder().setTenant(gbpTenantId)
            .setL2Context(L_2_BRIDGE_DOMAIN_ID)
            .setMacAddress(MAC_ADDRESS)
            .build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

    @Test
    public void testOnDataChanged_EndpointL3Prefix() {
        EndpointL3Prefix ep = new EndpointL3PrefixBuilder().setTenant(gbpTenantId).build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

}
