package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

public class EgKeyTest {

    private EgKey egKey;
    private TenantId tenantId;
    private EndpointGroupId egId;
    private String value;

    @Before
    public void initialisation() {
        tenantId = mock(TenantId.class);
        egId = mock(EndpointGroupId.class);

        value = "value";
        when(tenantId.getValue()).thenReturn(value);
        when(egId.getValue()).thenReturn(value);

        egKey = new EgKey(tenantId, egId);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(tenantId, egKey.getTenantId());
        Assert.assertEquals(egId, egKey.getEgId());
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(egKey.equals(egKey));
        Assert.assertFalse(egKey.equals(null));
        Assert.assertFalse(egKey.equals(new Object()));

        EgKey other;
        other = new EgKey(null, egId);
        Assert.assertFalse(egKey.equals(other));
        Assert.assertFalse(other.equals(egKey));

        other = new EgKey(tenantId, null);
        Assert.assertFalse(egKey.equals(other));
        Assert.assertFalse(other.equals(egKey));

        other = new EgKey(tenantId, egId);
        Assert.assertTrue(egKey.equals(other));

        egKey = new EgKey(null, null);
        other = new EgKey(null, null);
        Assert.assertTrue(egKey.equals(other));
    }

    @Test
    public void compareToTest() {
        EgKey other = new EgKey(tenantId, egId);
        Assert.assertEquals(0, egKey.compareTo(other));

        other = new EgKey(null, null);
        Assert.assertEquals(-1, egKey.compareTo(other));
        Assert.assertEquals(1, other.compareTo(egKey));

        String valueOther = "valu";
        TenantId tenantIdOther = mock(TenantId.class);
        when(tenantIdOther.getValue()).thenReturn(valueOther);
        other = new EgKey(tenantIdOther, egId);
        Assert.assertEquals(1, egKey.compareTo(other));
        Assert.assertEquals(-1, other.compareTo(egKey));

        EndpointGroupId egIdOther = mock(EndpointGroupId.class);
        when(egIdOther.getValue()).thenReturn(valueOther);
        other = new EgKey(tenantId, egIdOther);
        Assert.assertEquals(1, egKey.compareTo(other));
        Assert.assertEquals(-1, other.compareTo(egKey));

        egKey = new EgKey(tenantIdOther, egId);
        Assert.assertEquals(-1, egKey.compareTo(other));
        Assert.assertEquals(1, other.compareTo(egKey));
    }

    @Test
    public void toStringTest() {
        String string = egKey.toString();
        Assert.assertNotNull(string);
        Assert.assertFalse(string.isEmpty());
        Assert.assertTrue(string.contains(tenantId.toString()));
        Assert.assertTrue(string.contains(egId.toString()));
    }
}
