package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DataStoreHelper.class)
public class PolicyResolverTest extends GbpDataBrokerTest {

    private DataBroker dataProvider;
    private PolicyResolver policyResolver;
    private final TenantId tenantId = new TenantId("tenant-1");

    @Before
    public void init() {
        dataProvider = getDataBroker();
        policyResolver = new PolicyResolver(dataProvider);
    }

    @After
    public void teardown() throws Exception {
        policyResolver.close();
    }

    @Test
    public void testConstructor() throws Exception {
        PolicyResolver other = new PolicyResolver(dataProvider);
        other.close();
    }

    @Test
    public void testSubscribeTenant_Unknown() {
        int oldSize = policyResolver.subscribersPerTenant.count(tenantId);
        Assume.assumeTrue(oldSize == 0);

        policyResolver.subscribeTenant(tenantId);

        assertEquals(policyResolver.subscribersPerTenant.count(tenantId), oldSize + 1);
    }

    @Test
    public void testSubscribeTenant_Known() {
        Tenant unresolvedTenant = new TenantBuilder().setId(tenantId).build();

        Optional<Tenant> potentialTenant = mock(Optional.class);
        when(potentialTenant.isPresent()).thenReturn(true);
        when(potentialTenant.get()).thenReturn(unresolvedTenant);

        PowerMockito.mockStatic(DataStoreHelper.class);
        when(DataStoreHelper.readFromDs(eq(LogicalDatastoreType.CONFIGURATION),
                Mockito.<InstanceIdentifier<Tenant>>any(), any(ReadOnlyTransaction.class))).thenReturn(potentialTenant);

        PolicyResolver spy = spy(policyResolver);

        int oldSize = spy.subscribersPerTenant.count(tenantId);

        spy.subscribeTenant(tenantId);

        assertEquals(spy.subscribersPerTenant.count(tenantId), oldSize + 1);
        verify(spy).updateTenant(eq(tenantId), any(Tenant.class));
    }

    @Test
    public void testUnsubscribeTenant() {
        int oldSize = policyResolver.subscribersPerTenant.count(tenantId);
        Assume.assumeTrue(oldSize == 0);

        policyResolver.subscribeTenant(tenantId);
        assertEquals(policyResolver.subscribersPerTenant.count(tenantId), oldSize + 1);

        policyResolver.unsubscribeTenant(tenantId);
        assertEquals(policyResolver.subscribersPerTenant.count(tenantId), oldSize);
    }

}
